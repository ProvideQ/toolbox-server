# to check argument count
import sys

# Qiskit
from qiskit.primitives import Sampler
from qiskit_algorithms.optimizers import COBYLA
from qiskit_optimization.algorithms import MinimumEigenOptimizer
from qiskit_optimization.applications import Knapsack
from qiskit_optimization.converters import QuadraticProgramToQubo
from qiskit_algorithms import QAOA

if len(sys.argv) != 3:
    raise TypeError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

# read input from file
with open(input_path, 'r') as input_file:
    lines = input_file.readlines()

# first line gives number of items
number_items : int = int(lines[0])

indexes: list[int] = []
values: list[int] = []
weights: list[int] = []

# read items into value, weight lists
for i in range(number_items):
    item = lines[i + 1].split(' ')
    indexes.append(int(item[0]))
    values.append(int(item[1]))
    weights.append(int(item[2]))

# last line contains maximum capacity of the knapsack
capacity: int = int(lines[-1])

# convert to form needed for solving
knapsack = Knapsack(values, weights, capacity)
qp = knapsack.to_quadratic_program()
converter = QuadraticProgramToQubo()
qubo = converter.convert(qp)
op, offset = qubo.to_ising()

# TODO: Sampler() has to be replaces with StatevectorSampler() in newer versions.
# (currently not yet supported by qiskit-optimization)
# TODO: add a dedicated mixer
qaoa_mes = QAOA(sampler=Sampler(), optimizer=COBYLA())
qaoa = MinimumEigenOptimizer(qaoa_mes)

qaoa_result = qaoa.solve(qubo)

# read and interpret results
z = knapsack.interpret(qaoa_result)

# format results for output
achieved_sum : int = 0
included_indexes : list[int] = []

for i in z:
    achieved_sum += values[i]
    included_indexes.append(indexes[i])

with open(output_path, 'w') as f:
    f.write(str(achieved_sum) + "\n")
    f.write(str(included_indexes))