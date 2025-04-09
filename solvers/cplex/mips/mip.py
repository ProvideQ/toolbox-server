import argparse
import cplex

parser = argparse.ArgumentParser(
    description="Solve a MIP problem from an MPS file using IBM CPLEX."
)
parser.add_argument("input_file", help="Path to the input MPS file.")
parser.add_argument("output_file", help="Path to write the solution.")
args = parser.parse_args()

try:
    cpx = cplex.Cplex()
    cpx.read(args.input_file)
except cplex.exceptions.CplexError as exc:
    print("Error reading the MPS file:", exc)
    exit()


try:
    cpx.solve()
except cplex.exceptions.CplexError as exc:
    print("Error during solve:", exc)
    exit()

status = cpx.solution.get_status()
with open(args.output_file, 'w') as out_file:
    out_file.write("Solution status: {}\n".format(cpx.solution.get_status_string()))
    out_file.write("Objective value = {}\n".format(cpx.solution.get_objective_value()))
    var_names = cpx.variables.get_names()
    var_values = cpx.solution.get_values()
    for name, val in zip(var_names, var_values):
        out_file.write("{:<15} = {}\n".format(name, val))
print("Solution written to", args.output_file)
