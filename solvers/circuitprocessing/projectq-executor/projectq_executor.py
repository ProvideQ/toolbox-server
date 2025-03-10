import sys
from pytket.qasm import circuit_from_qasm_str
from pytket.extensions.projectq import ProjectQBackend

input_circuit = sys.argv[1]
shots = int(sys.argv[2])

try:
    circuit = circuit_from_qasm_str(input_circuit)
except Exception as e:
    print("Was not able to convert to OpenQASM: ", e)
    sys.exit(1)

backend = ProjectQBackend()
handle = backend.process_circuit(circuit, n_shots=shots)
result = backend.get_result(handle)
print(result.get_shots())