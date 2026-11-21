import gamspy as gp
import gurobipy
import pandas as pd
import json
import os
import re
import sys

# Import parse_qubo_from_lp from sibling _utility directory
# Add the parent (solvers) directory to enable the import
script_dir = os.path.dirname(os.path.abspath(__file__))
solvers_dir = os.path.join(script_dir, '..', '..')
sys.path.insert(0, solvers_dir)

from _utility.parse_qubo_from_lp import parse_qubo_from_lp

arg_count = len(sys.argv) - 1
if arg_count != 2:
    raise TypeError(f'This script expects exactly 2 arguments but got {arg_count}. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

def parse_lp_to_df(filepath="unsplittable_model.lp"):
    """Parses the LP file to extract the Q matrix coefficients and constant."""

    q_records = []

    model = gurobipy.read(filepath)
    (vars, quadratic_terms, linear_terms, constant_offset) = parse_qubo_from_lp(model)

    print(quadratic_terms)
    for ((var1, var2), coeff) in quadratic_terms.items():
        # restore the original name of the variables
        q_records.append({"i": vars[var1], "j": vars[var2], "value": coeff})

    q_df = pd.DataFrame(q_records)
    print("q_records", q_records)
    print("q_df", q_df)

    return q_df, constant_offset


q_df, constant = parse_lp_to_df(input_path)

m = gp.Container()

# Dynamically find all unique variables present in the parsed LP matrix
unique_vars = set(q_df["i"].unique()).union(set(q_df["j"].unique()))
print("uniqvar", unique_vars)

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