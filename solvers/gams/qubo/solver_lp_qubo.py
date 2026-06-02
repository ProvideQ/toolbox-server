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

    # 2. Extract coefficients using Regex
    # Match squares (e.g., "- 18270 b1^2")
    squares = re.findall(r"([+-]?\s*\d+(?:\.\d+)?)\s*b(\d+)\^2", q_text)
    # Match products (e.g., "+ 6480 b1 * b2")
    products = re.findall(r"([+-]?\s*\d+(?:\.\d+)?)\s*b(\d+)\s*\*\s*b(\d+)", q_text)

    q_records = []

    # Format diagonal terms (b_i * b_i)
    for coef_str, idx in squares:
        coef = float(coef_str.replace(" ", ""))
        q_records.append({"i": f"b{idx}", "j": f"b{idx}", "value": coef})

    # Format off-diagonal terms (b_i * b_j)
    for coef_str, idx1, idx2 in products:
        coef = float(coef_str.replace(" ", ""))
        q_records.append({"i": f"b{idx1}", "j": f"b{idx2}", "value": coef})

    q_df = pd.DataFrame(q_records)

    # 3. Find the constant on the right-hand side
    # e1: -x143 + [...] = -590660
    const_match = re.search(r"=\s*([+-]?\d+(?:\.\d+)?)", content)
    if const_match:
        # Move RHS constant to the objective side
        constant = -(float(const_match.group(1).replace(" ", "")))
    else:
        constant = 0

    return q_df, constant


q_df, constant = parse_lp_to_df(input_path)

m = gp.Container()

# Dynamically find all unique variables present in the parsed LP matrix
unique_vars = set(q_df["i"].unique()).union(set(q_df["j"].unique()))

# Sort them properly (b1, b2, ... b150)
b_idx = sorted(list(unique_vars), key=lambda x: int(x[1:]))

i = gp.Set(m, name="i", records=b_idx)
alias_j = gp.Alias(m, name="j", alias_with=i)

Q = gp.Parameter(m, name="Q", domain=[i, alias_j], records=q_df)

x = gp.Variable(m, name="x", domain=[i], type="binary")

# Note: CPLEX LP format implies the [...] block is implicitly multiplied by 0.5.
# We add `0.5 *` here to mirror exactly how the solver reads standard LP files.
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