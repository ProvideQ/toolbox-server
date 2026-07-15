import os
import sys
import gurobipy as gp
from qiskit_optimization import QuadraticProgram

# Import parse_qubo_from_lp from sibling _utility directory
# Add the parent (solvers) directory to enable the import
script_dir = os.path.dirname(os.path.abspath(__file__))
solvers_dir = os.path.join(script_dir, '..', '..')
sys.path.insert(0, solvers_dir)

from _utility.parse_qubo_from_lp import parse_qubo_from_lp

from qiskit.primitives import StatevectorSampler
from qiskit_optimization.minimum_eigensolvers import QAOA
from qiskit_optimization.optimizers import COBYLA
from qiskit_optimization.algorithms import MinimumEigenOptimizer

if len(sys.argv) != 3:
    raise ValueError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

model = gp.read(input_path)
(vars, quadratic_terms, linear_terms, constant_offset) = parse_qubo_from_lp(model)

qubo = QuadraticProgram()

# Add variables
for var in vars:
    qubo.binary_var(var)

# Set the objective sense
if model.ModelSense == 1:
    qubo.minimize(constant=constant_offset, linear=linear_terms, quadratic=quadratic_terms)
else:
    qubo.maximize(constant=constant_offset, linear=linear_terms, quadratic=quadratic_terms)

# Add constraints (empty in this example)
# You can extend this logic to handle any constraints

# parse LP file:
print(qubo.prettyprint())

# TODO: Sampler() has to be replaces with StatevectorSampler() in newer versions.
# (currently not yet supported by qiskit-optimization)
# TODO: add a dedicated mixer
qaoa_mes = QAOA(sampler=StatevectorSampler(), optimizer=COBYLA())
qaoa = MinimumEigenOptimizer(qaoa_mes)

qaoa_result = qaoa.solve(qubo)

f = open(output_path, 'w')
f.write(qaoa_result.prettyprint())
f.close()