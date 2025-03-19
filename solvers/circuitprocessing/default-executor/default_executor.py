import sys
from pytket.qasm import circuit_from_qasm_str
from pytket.extensions.qiskit import AerBackend

input_circuit = sys.argv[1]
shots = int(sys.argv[2])

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