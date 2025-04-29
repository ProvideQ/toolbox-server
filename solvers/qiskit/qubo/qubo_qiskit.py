import sys
from qp_converter import QpConverter

from qiskit.primitives import Sampler
from qiskit_algorithms import QAOA
from qiskit_algorithms.optimizers import COBYLA
from qiskit_optimization.algorithms import MinimumEigenOptimizer

if len(sys.argv) != 3:
    raise ValueError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

qubo = QpConverter.convert_to_quadratic_program(input_path)
# parse LP file:
print(qubo.prettyprint())

# TODO: Sampler() has to be replaces with StatevectorSampler() in newer versions.
# (currently not yet supported by qiskit-optimization)
# TODO: add a dedicated mixer
qaoa_mes = QAOA(sampler=Sampler(), optimizer=COBYLA())
qaoa = MinimumEigenOptimizer(qaoa_mes)

qaoa_result = qaoa.solve(qubo)

f = open(output_path, 'w')
f.write(qaoa_result.prettyprint())
f.close()