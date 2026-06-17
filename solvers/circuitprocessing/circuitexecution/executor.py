import sys
from pytket.qasm import circuit_from_qasm_str

input_path = sys.argv[1]
num_runs = int(sys.argv[2])
backend_name = sys.argv[3]

with open(input_path, 'r') as input_file:
    text = input_file.read()

try:
    circuit = circuit_from_qasm_str(text)
except Exception as e:
    print("Was not able to convert to OpenQASM: ", e)
    sys.exit(1)

if backend_name == "aer":
    from pytket.extensions.qiskit import AerBackend
    backend = AerBackend()
elif backend_name == "qulacs":
    from pytket.extensions.qulacs import QulacsBackend
    backend = QulacsBackend()
elif backend_name == "aer_noisy":
    from pytket.extensions.qiskit import AerBackend
    from qiskit_aer.noise import NoiseModel
    from qiskit_aer.noise.errors import depolarizing_error
    # https://docs.quantinuum.com/tket/user-guide/manual/manual_noise.html
    noise_model = NoiseModel()
    noise_model.add_readout_error([[0.9, 0.1], [0.1, 0.9]], [0])
    noise_model.add_readout_error([[0.95, 0.05], [0.05, 0.95]], [1])
    noise_model.add_quantum_error(depolarizing_error(0.1, 2), ["cx"], [0, 1])
    backend = AerBackend(noise_model)
else:
    print(f"Unknown backend: {backend_name}", file=sys.stderr)
    sys.exit(1)

c = backend.get_compiled_circuit(circuit)
handle = backend.process_circuit(c, n_shots=num_runs)
counts = backend.get_result(handle).get_counts()
print(counts)
