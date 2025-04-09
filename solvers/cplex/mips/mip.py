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
    # Solve the model.
    cpx.solve()
except cplex.exceptions.CplexError as exc:
    print("Error during solve:", exc)
    exit()

status = cpx.solution.get_status()
# See the full list of status codes in the CPLEX documentation.
with open(args.output_file, 'w') as out_file:
    if status == cpx.solution.status.optimal:
        out_file.write("Solution is optimal.\n")
        out_file.write("Objective value = {}\n".format(cpx.solution.get_objective_value()))
        # Get the variable names and values.
        var_names = cpx.variables.get_names()
        var_values = cpx.solution.get_values()
        for name, val in zip(var_names, var_values):
            out_file.write("{:<15} = {}\n".format(name, val))
    else:
        out_file.write("No optimal solution found. Status: {}\n".format(status))
        # Optionally, you can also write additional status information:
        out_file.write("Solution status string: {}\n".format(cpx.solution.get_status_string()))
print("Solution written to", args.output_file)
