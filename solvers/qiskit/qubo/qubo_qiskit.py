import os
import sys
import gurobipy as gp
from qiskit import qasm3
from qiskit_optimization import QuadraticProgram
from qiskit.circuit.library import qaoa_ansatz

# Import parse_qubo_from_lp from sibling _utility directory
# Add the parent (solvers) directory to enable the import
script_dir = os.path.dirname(os.path.abspath(__file__))
solvers_dir = os.path.join(script_dir, '..', '..')
sys.path.insert(0, solvers_dir)

from _utility.parse_qubo_from_lp import parse_qubo_from_lp

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

hamiltonian = qubo.to_ising()[0]

circuit = qaoa_ansatz(cost_operator=hamiltonian, reps=3)

# includes = [] to avoid including standard libraries in the output which are not understood by circuit processing
qasm_string = str(qasm3.dumps(circuit))

qasm_string = qasm_string.replace('_β', 'beta')
qasm_string = qasm_string.replace('_γ', 'gamma')

print("problem string:")
print(qasm_string)

f = open(output_path, 'w')
f.write(qasm_string)
f.close()