import gamspy as gp
import pandas as pd
import json
import re
import sys

arg_count = len(sys.argv) - 1
if arg_count != 2:
    raise TypeError(f'This script expects exactly 2 arguments but got {arg_count}. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

def parse_lp_to_df(filepath="unsplittable_model.lp"):
    """Parses the LP file to extract the Q matrix coefficients and constant."""
    with open(filepath, "r") as f:
        content = f.read()

    # 1. Extract the quadratic block inside [...]
    q_block_match = re.search(r"\[(.*?)\]", content, re.DOTALL)
    if not q_block_match:
        raise ValueError("Could not find the [...] block in the LP file.")

    # Clean up line breaks and excess spaces
    q_text = q_block_match.group(1).replace("\n", " ").replace("\r", "")
    q_text = re.sub(r"\s+", " ", q_text)

    # 2. Extract coefficients using prefix-agnostic Regex
    # Matches squares (e.g., "- 9600 x1 ^2") allowing spaces around the caret
    squares = re.findall(r"([+-]?\s*\d+(?:\.\d+)?)\s*([a-zA-Z]\d+)\s*\^\s*2", q_text)
    # Matches products (e.g., "+ 9600 x1 * x2")
    products = re.findall(
        r"([+-]?\s*\d+(?:\.\d+)?)\s*([a-zA-Z]\d+)\s*\*\s*([a-zA-Z]\d+)", q_text
    )

    q_records = []

    # Format diagonal terms (x_i * x_i)
    for coef_str, var_name in squares:
        coef = float(coef_str.replace(" ", ""))
        q_records.append({"i": var_name, "j": var_name, "value": coef})

    # Format off-diagonal terms (x_i * x_j)
    for coef_str, var1, var2 in products:
        coef = float(coef_str.replace(" ", ""))
        q_records.append({"i": var1, "j": var2, "value": coef})

    if not q_records:
        raise ValueError(
            "No quadratic terms matched. Please verify the LP file structure."
        )

    q_df = pd.DataFrame(q_records)

    # 3. Handle the constant from fixed boundary variables in the objective function
    constant = 0.0
    bounds_match = re.search(r"Bounds(.*?)(?:Binaries|End)", content, re.DOTALL)
    if bounds_match:
        bounds_text = bounds_match.group(1)
        # Parse fixed boundaries like: x47 = 42150
        fixed_vars = re.findall(
            r"([a-zA-Z]\d+)\s*=\s*([+-]?\d+(?:\.\d+)?)", bounds_text
        )
        fixed_map = {var: float(val) for var, val in fixed_vars}

        # Locate the linear parts of the objective function before the quadratic bracket block
        obj_match = re.search(r"Minimize\s+\w+:(.*?)\[", content, re.DOTALL)
        if obj_match:
            obj_text = obj_match.group(1).replace("\n", " ").replace("\r", "")
            obj_text = re.sub(r"\s+", " ", obj_text)
            linear_terms = re.findall(
                r"([+-]?)\s*(\d+(?:\.\d+)?)?\s*([a-zA-Z]\d+)", obj_text
            )

            for sign, coef_str, var in linear_terms:
                if var in fixed_map:
                    coef = float(coef_str) if coef_str else 1.0
                    if sign == "-":
                        coef = -coef
                    constant += coef * fixed_map[var]

    return q_df, constant


q_df, constant = parse_lp_to_df(input_path)

m = gp.Container()

# Dynamically find all unique variables present in the parsed LP matrix
unique_vars = set(q_df["i"].unique()).union(set(q_df["j"].unique()))

# Sort variables numerically regardless of character prefix (e.g., x1, x2, ... x46)
b_idx = sorted(list(unique_vars), key=lambda x: int("".join(filter(str.isdigit, x))))

i = gp.Set(m, name="i", records=b_idx)
alias_j = gp.Alias(m, name="j", alias_with=i)

Q = gp.Parameter(m, name="Q", domain=[i, alias_j], records=q_df)

x = gp.Variable(m, name="x", domain=[i], type="binary")

# Standard CPLEX LP format implicitly scales the quadratic block inside [...] by 0.5.
obj_expr = 0.5 * gp.Sum((i, alias_j), Q[i, alias_j] * x[i] * x[alias_j]) + constant

z = gp.Variable(m, name="z", type="free")
eq = gp.Equation(m, name="eq")
eq[...] = z == obj_expr

qubo_model = gp.Model(
    m, name="qubo", equations=[eq], problem="MIQCP", sense="min", objective=z
)

print("Solving model natively...")
qubo_model.solve(solver="CPLEX", output=sys.stdout, options=gp.Options(time_limit=60))

print(f"\n--- Solver Status: {qubo_model.status} ---")
print(f"--- Objective Value: {qubo_model.objective_value} ---")

solution_dict = {}

if x.records is not None and not x.records.empty:
    for _, row in x.records.iterrows():
        val = row["level"]
        if val > 0.5:
            var_name = row["i"]
            solution_dict[var_name] = float(round(val))

with open(output_path, "w") as f:
    json.dump(solution_dict, f, indent=4)

print(f"--- Extracted {len(solution_dict)} active variables to {output_path} ---")