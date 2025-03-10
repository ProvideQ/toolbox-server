import argparse
import os
from ortools.linear_solver import pywraplp
from lp_mips_converter import convert_model


parser = argparse.ArgumentParser(
    prog="OR-Tools CBC MIP Solver",
    description="A CLI Program to solve MIP problems in lp or mips format with Or-Tools CBC"
)

parser.add_argument("input_file")
parser.add_argument("--output-file")
args = parser.parse_args()

input_ext = os.path.splitext(args.input_file)[1].lower()
if input_ext == ".lp":
    convert_model(args.input_file, args.output_file)
    mps_file = args.output_file
else:
    mps_file = args.input_file


solver = pywraplp.Solver.CreateSolver("CBC")
model = solver.loadModelFromFile(mps_file)
status = solver.Solve()

if status == pywraplp.Solver.OPTIMAL:
    print("Solution:")
    print("Objective value =", solver.Objective().Value())
else:
    print("The problem does not have an optimal solution.")

