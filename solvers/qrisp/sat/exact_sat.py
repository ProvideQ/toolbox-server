import argparse
import os
import platform
import subprocess
import tempfile
from qrisp.grover import grovers_alg
from qrisp import *
from qrisp_sat_utils import parse_dimacs, sat_oracle, write_output

parser = argparse.ArgumentParser(
    prog="Qrisp Grover SAT Solver",
    description="A CLI Program to solve SAT problems in DIMACS CNF format with Qrisp"
)

parser.add_argument("input_file")
parser.add_argument("--sharp-sat-directory", required=True)
parser.add_argument("--output-file")
args = parser.parse_args()

sharp_sat_directory = os.path.abspath(args.sharp_sat_directory)
if not os.path.isdir(sharp_sat_directory):
    raise FileNotFoundError(f"Sharp-SAT directory not found: {sharp_sat_directory}")

# temporary .cnf file for the binary
input_file_path = os.path.abspath(args.input_file)
if not os.path.isfile(input_file_path):
    raise FileNotFoundError(f"Input file not found: {input_file_path}")

with tempfile.TemporaryDirectory() as temp_dir:
    temp_cnf_path = os.path.join(temp_dir, "problem.cnf")

    with open(input_file_path, "r") as src, open(temp_cnf_path, "w") as dest:
        dest.write(src.read())

    system = platform.system().lower()

    if system == "windows":
        binary_name = "sharp-sat-solver-win.exe"
    elif system == "darwin":  # macOS
        binary_name = "sharp-sat-solver-mac"
    elif system == "linux":
        binary_name = "sharp-sat-solver-linux"
    else:
        raise OSError("Unsupported operating system")

    binary_path = os.path.join(sharp_sat_directory, binary_name)

    if not os.path.isfile(binary_path):
        raise FileNotFoundError(f"Solver binary not found at {binary_path}")

    try:
        result = subprocess.run(
            [binary_path, temp_cnf_path],
            cwd=sharp_sat_directory,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True,
        )
        sharpSolverResult = result.stdout

    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Solver execution failed: {e.stderr}") from e

try:
    start_tag = "# solutions"
    end_tag = "# END"
    start_idx = sharpSolverResult.index(start_tag) + len(start_tag)
    end_idx = sharpSolverResult.index(end_tag)
    solutionNumber = int(sharpSolverResult[start_idx:end_idx].strip())
except (ValueError, IndexError):
    raise ValueError("Failed to parse the output. Ensure the output contains the expected tags.")

if solutionNumber == 0:
    if args.output_file:
        with open(args.output_file, 'w') as f:
            f.write("s UNSATISFIABLE\n")
    else:
        print("s UNSATISFIABLE")
    exit()

num_vars, clauses = parse_dimacs(args.input_file)
qvars = [QuantumBool() for _ in range(num_vars)]

grovers_alg(
    qvars,
    sat_oracle,
    exact=True,
    winner_state_amount=solutionNumber,
    kwargs={"sat_clauses": clauses},
)

write_output(qvars, args.output_file)
