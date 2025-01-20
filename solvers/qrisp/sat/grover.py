import argparse

from qrisp.grover import grovers_alg
from qrisp import *
from grover_utils.py import parse_dimacs, sat_oracle, write_output

parser = argparse.ArgumentParser(
    prog="Grover-Search SAT (Qrisp)",
    description="A CLI Program to solve SAT problems in DIMACS CNF format with Qrisp (Grover)"
)

parser.add_argument("input_file")
parser.add_argument("--output-file")
args = parser.parse_args()

num_vars, clauses = parse_dimacs(args.input_file)
qvars = [QuantumBool() for _ in range(num_vars)]

grovers_alg(
    qvars,
    sat_oracle,
    kwargs={"sat_clauses": clauses},
)

write_output(qvars, args.output_file)

