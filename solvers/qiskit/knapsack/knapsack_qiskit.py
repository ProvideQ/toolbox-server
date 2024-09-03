# code partially taken from qiskit documentation

# useful additional packages
import sys
import numpy as np

# Qiskit
from qiskit.circuit.library import TwoLocal
from qiskit.primitives import Sampler
from qiskit_optimization.applications import Knapsack
from qiskit_algorithms import SamplingVQE
from qiskit_algorithms.optimizers import SPSA

if len(sys.argv) != 3:
    raise TypeError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

# read input from file
with open(input_path, 'r') as input_file:
    lines = input_file.readlines()

# first line gives number of items
number_items : int = int(lines[0])

values: list[int] = []
weights: list[int] = []

# read items into profits, weights lists
for i in range(number_items):
    item = lines[i + 1].split(' ')
    values.append(int(item[1]))
    weights.append(int(item[2]))

# last line contains maximum capacity of the knapsack
capacity: int = int(lines[-1])

knapsack = Knapsack(values, weights, capacity)
qp = knapsack.to_quadratic_program()
qubitOp, offset = qp.to_ising()

# construct VQE
optimizer=SPSA(maxiter=300)
ry = TwoLocal(qubitOp.num_qubits, "ry", "cz", reps=5, entanglement="linear")
vqe = SamplingVQE(sampler=Sampler(), ansatz=ry, optimizer=optimizer)

# run VQE
result = vqe.compute_minimum_eigenvalue(qubitOp)

x = knapsack.sample_most_likely(result.eigenstate)
z = knapsack.interpret(x)

achieved_sum = (np.array(values) * np.array(z)).sum()

with open(output_path, 'w') as of:
    of.write(achieved_sum)
    of.write(str(z))
