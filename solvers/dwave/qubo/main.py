import argparse
import os
import sys
from datetime import datetime
from typing import Literal
import gurobipy as gp

# Import parse_qubo_from_lp from sibling _utility directory
# Add the parent (solvers) directory to enable the import
script_dir = os.path.dirname(os.path.abspath(__file__))
solvers_dir = os.path.join(script_dir, '..', '..')
sys.path.insert(0, solvers_dir)

from _utility.parse_qubo_from_lp import parse_qubo_from_lp

from dimod import BINARY, BinaryQuadraticModel
from dimod.serialization import coo
from solver import solve_with

def main():
    parser = argparse.ArgumentParser(
        prog="DWave QUBO solver",
        description="A CLI Program to initiate solving COOrdinate files with DWave Systems",
        epilog="Made by Lucas Berger for scientific purposes",
    )

    parser.add_argument("file")
    parser.add_argument(
        "type", default="sim", choices=["sim", "hybrid", "qbsolv", "direct"]
    )
    parser.add_argument("--output-file")

    args = parser.parse_args()
    type: Literal["sim", "hybrid", "qbsolv", "direct"] = args.type

    bqm: BinaryQuadraticModel | None = None
    with open(args.file) as problem:
        bqm = coo.load(problem, vartype=BINARY)
        if len(bqm.quadratic) == 0:
            bqm = None

    if bqm is None:
        model = gp.read(args.file)
        (vars, quadratic_terms, linear_terms, constant_offset) = parse_qubo_from_lp(model)

        bqm = BinaryQuadraticModel(linear_terms, quadratic_terms, constant_offset, BINARY)
        if len(bqm.quadratic) == 0:
            bqm = None

    filename = os.path.basename(args.file)

    if bqm is None:
        raise Exception("Could not load file")

    last = datetime.now().timestamp()
    print("started")

    now = datetime.now().timestamp()
    print(f"connected after {now - last}. starting solver")
    sampleset = solve_with(bqm, type, filename)

    # accessing the sampleset's properties await for the future
    print(sampleset.info)

    now = datetime.now().timestamp()
    print(f"ended {now - last}")

    if args.output_file:
        with open(args.output_file, "w") as out:
            out.writelines([f"{bin}\n" for bin in sampleset.first.sample.values()])
    else:
        print(sampleset.first.energy)
        print(sampleset.first.sample)

    now = datetime.now().timestamp()
    print(f"connection closed after {now - last}")


if __name__ == "__main__":
    main()
