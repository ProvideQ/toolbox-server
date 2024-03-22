import argparse
import sys
from math import ceil, factorial

import numpy as np
from qrisp.grover import grovers_alg
from qrisp_solver.oracle import eval_distance_threshold
from qrisp_solver.permutation import create_perm_specifiers, eval_perm
from qrisp_solver.vrp import calc_paths, normalize_vrp
from tsplib95 import load
from tsplib95.models import StandardProblem

from qrisp import QuantumCircuit, QuantumFloat, multi_measurement

parser = argparse.ArgumentParser(
    prog="Qrisp Grover VRP Solver",
    description="A CLI Program to initiate solving CVRP files with Qrisp",
    epilog="Made by Lucas Berger for scientific purposes",
)


parser.add_argument("tsplib_file")
parser.add_argument("--size-gate")
parser.add_argument("--output-file")

args = parser.parse_args()

problem = load(args.tsplib_file)

max_cap = problem.capacity

city_amount = problem.dimension

city_coords = np.array(list(problem.node_coords.values()))


distance_matrix = np.array(
    [
        [np.linalg.norm(city_coords[i] - city_coords[j]) for i in range(city_amount)]
        for j in range(city_amount)
    ]
)


city_demand = np.array(list(problem.demands.values()))

distance_matrix, scaling = normalize_vrp(distance_matrix, city_demand, max_cap)

print(distance_matrix)

dump_length, _ = calc_paths(distance_matrix, city_demand, max_cap, [1, 2, 3])

precision = 5 + int(ceil(np.log2(scaling)) / 2)

perm_specifiers = create_perm_specifiers(city_amount)

winner_state_amount = 2 ** sum([qv.size for qv in perm_specifiers]) / factorial(
    city_amount - 1
)

print("estimated winners: ", winner_state_amount)


grovers_alg(
    perm_specifiers,  # Permutation specifiers
    eval_distance_threshold,  # Oracle function
    kwargs={
        "threshold": dump_length * 0.8,
        "precision": precision,
        "city_amount": city_amount,
        "distance_matrix": distance_matrix,
        "city_demand": city_demand,
        "max_cap": max_cap,
    },  # Specify the keyword arguments for the Oracle
    winner_state_amount=1,
)  # Specify the estimated amount of winners

pre_compile_qubits = perm_specifiers[0].qs.num_qubits()
print(f"Before compilation number of qubits: {pre_compile_qubits}")

if args.size_gate and pre_compile_qubits > int(args.size_gate) * 5:
    print(
        f"Stop execution to prevent denial of service due to a large VRP. Size of the VRP circuit before compiling in qubit ({pre_compile_qubits})"
    )
    sys.exit(1)

compiled_qs: QuantumCircuit = perm_specifiers[0].qs.compile()

num_qubits = len(compiled_qs.qubits)
print("Number of qubits: ", num_qubits)
if args.size_gate and num_qubits > int(args.size_gate):
    print(
        f"Stop execution to prevent denial of service due to a large VRP. Size of the VRP circuit in qubit ({num_qubits})"
    )
    sys.exit(1)

res = multi_measurement(perm_specifiers)

best_perm_spec = sorted(list(res.items()), key=lambda x: x[1], reverse=True)[0][0]
best_perm_spec_qc: list[QuantumFloat] = []
for spec in best_perm_spec:
    new_qv = QuantumFloat(perm_specifiers[0].size)
    new_qv[:] = spec
    best_perm_spec_qc.append(new_qv)
best_perm = eval_perm(best_perm_spec_qc, city_amount=city_amount).most_likely()
print(best_perm)

paths = []
current_path = [0]
current_demand = 0

for i in best_perm:
    demand = city_demand[i]

    if max_cap < current_demand + demand:
        paths.append(current_path)
        current_path = [0]
        current_demand = 0

    current_path.append(i)
    current_demand += demand

paths.append(current_path)

print(paths)


tour = StandardProblem()

tour.tours = paths
tour.type = "TOUR"
tour.name = problem.name + " solution"

if args.output_file:
    tour.save(args.output_file)