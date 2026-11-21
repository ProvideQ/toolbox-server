import argparse

from lkh import LKHProblem, solve
from tsplib95.models import StandardProblem

parser = argparse.ArgumentParser(
    prog="LKH-3 Interface",
    description="A CLI Program to initiate solving CVRP files with LKH-3",
    epilog="Made by Lucas Berger for scientific purposes",
)


parser.add_argument("tsplib_file")
parser.add_argument("--lkh-instance", default="./bin/LKH-unix")
parser.add_argument("--output-file")
parser.add_argument("-t", "--max-trials", default=1000)
parser.add_argument("-r", "--runs", default=10)

args = parser.parse_args()

problem = LKHProblem.load(args.tsplib_file)

print(f"solving {args.tsplib_file}")

if sum(problem.demands.values()) <= problem.capacity:
    problem.type = "TSP"

if len(problem.node_coords.values()) > 2:
    extra = {}
    tours = solve(
        args.lkh_instance, problem=problem, max_trials=args.max_trials, **extra
    )

    tour = StandardProblem()

    tour.tours = [[*problem.depots, *path] for path in tours]
    tour.type = "TOUR"
    tour.name = problem.name + " solution"
else:
    tour = StandardProblem()
    tour.tours = [problem.node_coords.keys()]
    tour.type = "TOUR"
    tour.name = problem.name + " solution"


if args.output_file is not None:
    tour.save(args.output_file)
