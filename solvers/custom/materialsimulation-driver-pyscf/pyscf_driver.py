import argparse
import pickle
from qiskit_nature.second_q.drivers import PySCFDriver

parser = argparse.ArgumentParser(
    prog="PySCF Driver for Material Simulation",
    description="A CLI Program to get a ElectronicStructureProblem for a Material Simulation"
)
parser.add_argument("input_file")
parser.add_argument("output_file")
parser.add_argument("--charge", type=int, default=0, help="Charge of the molecule")
parser.add_argument("--spin", type=int, default=0, help="Spin of the molecule")
args = parser.parse_args()

with open(args.input_file, 'r') as input_file:
    molecule = input_file.read().strip()


driver = PySCFDriver(
        atom=molecule,
        basis="sto3g",
        charge=charge,
        spin=spin
    )

problem = driver.run()

with open(args.output_file, 'wb') as output_file:
    pickle.dump(problem, output_file)