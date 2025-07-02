import argparse
from qiskit_nature.units import DistanceUnit
from qiskit_nature.second_q.drivers import PySCFDriver
from qiskit_nature.second_q.mappers import JordanWignerMapper
from qiskit_algorithms import VQE
from qiskit_algorithms.optimizers import SLSQP
from qiskit.primitives import Estimator
from qiskit_nature.second_q.algorithms import GroundStateEigensolver, QEOM, EvaluationRule
from qiskit_nature.second_q.algorithms.initial_points import HFInitialPoint
from qiskit_nature.second_q.circuit.library import HartreeFock, UCCSD

parser = argparse.ArgumentParser(
    prog="Qiskit Material Simulation",
    description="A CLI Program to run Qiskit a Material Simulation"
)


def get_problem(molecule, charge, spin):
    driver = PySCFDriver(
        atom=molecule,
        basis="sto3g",
        charge=charge,
        spin=spin,
        unit=DistanceUnit.ANGSTROM,
    )
    return driver.run()


def get_ansatz(problem, mapper):
    ansatz = UCCSD(
        problem.num_spatial_orbitals,
        problem.num_particles,
        mapper,
        initial_state=HartreeFock(
            problem.num_spatial_orbitals,
            problem.num_particles,
            mapper
        )
    )
    return ansatz


parser.add_argument("input_file")
parser.add_argument("output_file")
parser.add_argument("--charge", type=int, default=0, help="Charge of the molecule")
parser.add_argument("--spin", type=int, default=0, help="Spin of the molecule")
args = parser.parse_args()

with open(args.input_file, 'r') as input_file:
    input_molecule = input_file.read().strip()

es_problem = get_problem(input_molecule, args.charge, args.spin)
mapper = JordanWignerMapper()
ansatz = get_ansatz(es_problem, mapper)

estimator = Estimator()
# This first part sets the ground state solver
solver = VQE(estimator, ansatz, SLSQP())

hf_initial_point = HFInitialPoint()
hf_initial_point.ansatz = ansatz
initial_point = hf_initial_point.to_numpy_array()

solver.initial_point = initial_point

gse = GroundStateEigensolver(mapper, solver)

# The qEOM algorithm is simply instantiated with the chosen ground state solver and Estimator primitive
qeom_excited_states_solver = QEOM(gse, estimator, "sd", EvaluationRule.ALL)

# The qEOM algorithm is then run on the problem
qeom_result = qeom_excited_states_solver.solve(es_problem)

with open(args.output_file, 'w') as f:
    f.write(str(qeom_result))