import argparse
import os
from datetime import datetime
from typing import Literal

from dimod import BINARY, BinaryQuadraticModel, binary, constrained, lp
from dimod.serialization import coo
from dwave.cloud import Client
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
        with open(args.file) as problem:
            cqm = lp.load(problem)
            converted, _ = constrained.cqm_to_bqm(cqm)

            linear_conv = {
                (int(str(x)[1:])): converted.linear[x] for x in converted.linear
            }
            quad_conv = {
                (int(str(x)[1:]), int(str(y)[1:])): converted.quadratic[(x, y)]
                for x, y in converted.quadratic
            }

            bqm = BinaryQuadraticModel(linear_conv, quad_conv, converted.offset, BINARY)
            if len(bqm.quadratic) == 0:
                bqm = None

    filename = os.path.basename(args.file)

    if bqm is None:
        raise Exception("Could not load file")

    last = datetime.now().timestamp()
    print("started")

    with Client.from_config() as _:
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
