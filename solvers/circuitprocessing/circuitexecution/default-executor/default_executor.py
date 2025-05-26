import sys
from pytket.qasm import circuit_from_qasm_str
from pytket.extensions.qiskit import AerBackend

input_path = sys.argv[1]
num_runs = sys.argv[2]

# read input from file
with open(input_path, 'r') as input_file:
    text = input_file.read()

input_circuit = text
shots = num_runs

try:
    circuit = circuit_from_qasm_str(input_circuit)
except Exception as e:
    print("Was not able to convert to OpenQASM: ", e)
    sys.exit(1)

backend = AerBackend()
c = backend.get_compiled_circuit(circuit)
handle = backend.process_circuit(c, n_shots=shots)
counts = backend.get_result(handle).get_counts()
print(counts)