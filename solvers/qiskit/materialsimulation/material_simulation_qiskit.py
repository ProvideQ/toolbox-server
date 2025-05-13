import argparse
from qiskit_nature.units import DistanceUnit
from qiskit_nature.second_q.drivers import PySCFDriver
from qiskit_nature.second_q.mappers import JordanWignerMapper
from qiskit_algorithms import VQE
from qiskit_algorithms.optimizers import SLSQP
from qiskit.primitives import Estimator
from qiskit_nature.second_q.algorithms import GroundStateEigensolver, QEOM, EvaluationRule
from qiskit_nature.second_q.circuit.library import HartreeFock, UCCSD

parser = argparse.ArgumentParser(
    prog="Qiskit Material Simulation",
    description="A CLI Program to run Qiskit a Material Simulation"
)


def get_problem(molecule):
    driver = PySCFDriver(
        atom=molecule,
        basis="sto3g",
        charge=0,
        spin=0,
        unit=DistanceUnit.ANGSTROM,
    )
    return driver.run()


def get_ansatz(problem, mapper):
    ansatz = UCCSD(
        problem.num_spatial_orbitals,
        problem.num_particles,
        mapper,
        initial_state=HartreeFock(
            problem.num_orbitals,
            problem.num_particles,
            mapper
        )
    )
    return ansatz


parser.add_argument("input_file")
parser.add_argument("output-file")
args = parser.parse_args()

es_problem = get_problem(args.input_file)
mapper = JordanWignerMapper()
ansatz = get_ansatz(es_problem, mapper)

estimator = Estimator()
# This first part sets the ground state solver
# see more about this part in the ground state calculation tutorial
solver = VQE(estimator, ansatz, SLSQP())
solver.initial_point = [0.0] * ansatz.num_parameters
gse = GroundStateEigensolver(mapper, solver)

# The qEOM algorithm is simply instantiated with the chosen ground state solver and Estimator primitive
qeom_excited_states_solver = QEOM(gse, estimator, "sd", EvaluationRule.ALL)

# The qEOM algorithm is then run on the problem
qeom_result = qeom_excited_states_solver.solve(es_problem)

with open(args.output_file, 'w') as f:
    f.write(str(qeom_result))