import sys
from pytket.qasm import circuit_from_qasm_str
from pytket.extensions.qiskit import AerBackend

from qiskit_aer.noise import NoiseModel
from qiskit_aer.noise.errors import depolarizing_error

input_circuit = sys.argv[1]
shots = sys.argv[2]

try:
    circuit = circuit_from_qasm_str(input_circuit)
except Exception as e:
    print("Was not able to convert to OpenQASM: ", e)
    sys.exit(1)

# https://docs.quantinuum.com/tket/user-guide/manual/manual_noise.html
noise_model = NoiseModel()
noise_model.add_readout_error([[0.9, 0.1],[0.1, 0.9]], [0])
noise_model.add_readout_error([[0.95, 0.05],[0.05, 0.95]], [1])
noise_model.add_quantum_error(depolarizing_error(0.1, 2), ["cx"], [0, 1])

backend = AerBackend(noise_model)
c = backend.get_compiled_circuit(circuit)
handle = backend.process_circuit(c, n_shots=int(shots))
counts = backend.get_result(handle).get_counts()
print(counts)