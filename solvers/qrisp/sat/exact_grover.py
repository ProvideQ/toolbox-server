import argparse
from qrisp.grover import grovers_alg
from qrisp import *
from grover_utils import parse_dimacs, sat_oracle, write_output

# Set up command-line arguments
parser = argparse.ArgumentParser(
    prog="Qrisp Grover SAT Solver",
    description="A CLI Program to solve SAT problems in DIMACS CNF format with Qrisp"
)
parser.add_argument("input_file")
parser.add_argument("--solution-count", required=True)
parser.add_argument("--output-file")
args = parser.parse_args()

# convert the --solution-count to an integer
try:
    solution_count = int(args.solution_count)
except ValueError:
    raise ValueError("--solution-count argument must be an integer.")

if solution_count == 0:
    output_message = "s UNSATISFIABLE"
    if args.output_file:
        with open(args.output_file, 'w') as f:
            f.write(output_message + "\n")
    else:
        print(output_message)
    exit()

num_vars, clauses = parse_dimacs(args.input_file)
qvars = [QuantumBool() for _ in range(num_vars)]

grovers_alg(
    qvars,
    sat_oracle,
    exact=True,
    winner_state_amount=solution_count,
    kwargs={"sat_clauses": clauses},
)

write_output(qvars, args.output_file)
