import copy
import sys

from qiskit.algorithms.minimum_eigensolvers import QAOA
from qiskit.algorithms.optimizers import COBYLA
from qiskit.primitives import Sampler
from qiskit_optimization import QuadraticProgram
from qiskit_optimization.algorithms import MinimumEigenOptimizer
from qiskit_optimization.problems import VarType

if len(sys.argv) != 3:
    raise TypeError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

qp = QuadraticProgram()
qp.read_from_lp_file(input_path)


def relax_problem(problem):
    """Change all variables to continuous."""
    relaxed_problem = copy.deepcopy(problem)
    for variable in relaxed_problem.variables:
        variable.vartype = VarType.CONTINUOUS

    return relaxed_problem

qubo = qp
# qubo = relax_problem(QuadraticProgramToQubo().convert(qp))
# print(qp.prettyprint())

qaoa_mes = QAOA(Sampler(), optimizer=COBYLA(), initial_point=[0.0, 1.0])

qaoa = MinimumEigenOptimizer(qaoa_mes)

qaoa_result = qaoa.solve(qubo)
print(qaoa_result.prettyprint())

f = open(output_path, 'w')
f.write(qaoa_result.prettyprint())
f.close()
