from qiskit_nature.second_q.drivers import PySCFDriver
from qiskit_nature.second_q.mappers import ParityMapper
from qiskit_algorithms.optimizers import L_BFGS_B
from qiskit.primitives import Estimator
from qiskit_nature.second_q.circuit.library import HartreeFock, UCCSD
from qiskit_algorithms import VQE
from qiskit_nature.second_q.algorithms import GroundStateEigensolver
import sys

if len(sys.argv) != 2:
    raise TypeError('This script expects exactly 1 arguments: the molecule')

molecule = sys.argv[1]

driver = PySCFDriver(atom=molecule)
problem = driver.run()

mapper = ParityMapper(num_particles=problem.num_particles)

optimizer = L_BFGS_B()

estimator = Estimator()

ansatz = UCCSD(
    problem.num_spatial_orbitals,
    problem.num_particles,
    mapper,
    initial_state=HartreeFock(
        problem.num_spatial_orbitals,
        problem.num_particles,
        mapper,
    ),
)

vqe = VQE(estimator, ansatz, optimizer)
vqe.initial_point = [0] * ansatz.num_parameters

algorithm = GroundStateEigensolver(mapper, vqe)

electronic_structure_result = algorithm.solve(problem)
electronic_structure_result.formatting_precision = 6
print(electronic_structure_result) # we're going to calculate the total ground state energy

from qiskit_nature.second_q.drivers import PySCFDriver
from qiskit_nature.second_q.mappers import JordanWignerMapper
from qiskit_algorithms.optimizers import SLSQP
from qiskit.primitives import Estimator

driver = PySCFDriver(atom='H .0 .0 .0; H .0 .0 0.74279')
problem = driver.run()

mapper = JordanWignerMapper()

optimizer = SLSQP(maxiter=10000, ftol=1e-9)

estimator = Estimator()

from qiskit import QuantumCircuit
from qiskit.circuit.library import TwoLocal
from qiskit_algorithms import VQE
from qiskit_nature.second_q.algorithms import GroundStateEigensolver
import numpy as np

var_forms = [['ry', 'rz'], 'ry']
entanglements = ['full', 'linear']
entanglement_blocks = ['cx', 'cz', ['cx', 'cz']]
depths = list(range(1, 11))

reference_circuit = QuantumCircuit(4)
reference_circuit.x(0)
reference_circuit.x(2)

results = np.zeros((len(depths), len(entanglements), len(var_forms), len(entanglement_blocks)))

for i, d in enumerate(depths):
    print(f'Depth: {d} done.')
    for j, e in enumerate(entanglements):
        for k, vf in enumerate(var_forms):
            for l, eb in enumerate(entanglement_blocks):
                optimizer = SLSQP(maxiter=10000, ftol=1e-9)
                variational_form = TwoLocal(4, rotation_blocks=vf, entanglement_blocks=eb,entanglement=e, reps=d)

                ansatz = reference_circuit.compose(variational_form)

                vqe = VQE(estimator, ansatz, optimizer)
                vqe.initial_point = [0] * ansatz.num_parameters

                algorithm = GroundStateEigensolver(mapper, vqe)

                electronic_structure_result = algorithm.solve(problem)

                results[i, j, k, l] = electronic_structure_result.total_energies[0]

import matplotlib.pyplot as plt
from io import StringIO

fig1, axs1 = plt.subplots(2, 3, sharey=True, sharex=True)
fig2, axs2 = plt.subplots(2, 3, sharey=True, sharex=True)

fig1.supxlabel('Depth')
fig1.supylabel('Estimated ground state energy')
fig2.supxlabel('Depth')
fig2.supylabel('Estimated ground state energy')

for j, e in enumerate(entanglements):
    for l, eb in enumerate(entanglement_blocks):
        axs1[j, l].plot(depths, results[:, j, 0, l])
        axs1[j, l].set_title(f'{e}, ryrz, {eb}')
        axs1[j, l].text(0.90, 0.75, f'Min: {np.min(results[:, j, 0, l]):.3f}')

for j, e in enumerate(entanglements):
    for l, eb in enumerate(entanglement_blocks):
        axs2[j, l].plot(depths, results[:, j, 1, l])
        axs2[j, l].set_title(f'{e}, ry, {eb}')
        axs2[j, l].text(0.90, 0.75, f'Min: {np.min(results[:, j, 1, l]):.3f}')

def print_fig(fig):
    string_io = StringIO()
    fig.savefig(string_io, format='svg')
    print(string_io.getvalue())

print_fig(fig1)
print_fig(fig2)
