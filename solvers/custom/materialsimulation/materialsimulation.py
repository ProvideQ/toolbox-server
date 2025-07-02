import argparse
from qiskit import qpy
from qiskit_nature.units import DistanceUnit
from qiskit_nature.second_q.drivers import PySCFDriver
from qiskit_nature.second_q.mappers import JordanWignerMapper
from qiskit_algorithms import VQE
from qiskit_algorithms.optimizers import SLSQP
from qiskit.primitives import Estimator
from qiskit_nature.second_q.algorithms import GroundStateEigensolver
from qiskit_nature.second_q.algorithms.initial_points import HFInitialPoint
from qiskit_nature.second_q.circuit.library import HartreeFock, UCCSD
from tvha.tvha.tvha import VariationalHamiltonianAnsatz

AVAILABLE_ANSATZ = ["uccsd", "vha"]


parser = argparse.ArgumentParser(
    prog="Qiskit Material Simulation",
    description="A CLI Program to run Qiskit a Material Simulation"
)

parser.add_argument("input_file")
parser.add_argument("output_file")
parser.add_argument("ansatz", type=str, default="uccsd", help="Ansatz used")
parser.add_argument("--charge", type=int, default=0, help="Charge of the molecule")
parser.add_argument("--spin", type=int, default=0, help="Spin of the molecule")
args = parser.parse_args()

if args.ansatz not in AVAILABLE_ANSATZ:
    print(f"Error: {args.ansatz} is not a valid ansatz!")
    exit(1)

with open(args.input_file, 'r') as input_file:
    input_molecule = input_file.read().strip()

def get_problem(molecule, charge, spin):
    driver = PySCFDriver(
        atom=molecule,
        basis="sto3g",
        charge=charge,
        spin=spin,
        unit=DistanceUnit.ANGSTROM,
    )
    return driver.run()


def get_uccsd_ansatz(problem, mapper):
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

def get_vha_ansatz(problem, trotter_steps, threshold_gamma):
    ansatz = VariationalHamiltonianAnsatz(
        problem=problem,
        trotter_steps=trotter_steps,
        threshold_gamma=threshold_gamma,
        mapper=mapper,
    )
    return ansatz

problem = get_problem(input_molecule, args.charge, args.spin)
mapper = JordanWignerMapper()

ansatz = None
initial_point = None

if args.ansatz == "uccsd":
    ansatz = get_uccsd_ansatz(problem, mapper)
    hf_initial_point = HFInitialPoint()
    hf_initial_point.ansatz = ansatz
    initial_point = hf_initial_point.to_numpy_array()

elif args.ansatz == "vha":
    trotter_steps = 1
    threshold_gamma = 0.5
    ansatz = get_vha_ansatz(problem, trotter_steps, threshold_gamma)
    initial_point = ansatz.get_initial_point()

estimator = Estimator()
optimizer = SLSQP()

vqe_solver = VQE(
    Estimator(),
    ansatz=ansatz,
    optimizer=optimizer,
    initial_point=initial_point
)

ground_state_solver = GroundStateEigensolver(
    JordanWignerMapper(),
    vqe_solver
)

result = ground_state_solver.solve(problem)

with open(args.output_file, 'w') as output_file:
    output_file.write(str(result))