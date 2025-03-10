import sys
from pytket.qasm import circuit_from_qasm_str, circuit_to_qasm_str
from pytket.predicates import CompilationUnit
from pytket.passes import RemoveRedundancies


input_circuit = sys.argv[1]

try:
    circuit = circuit_from_qasm_str(input_circuit)
except Exception as e:
    print("Was not able to convert to OpenQASM: ", e)
    sys.exit(1)

pass1 = RemoveRedundancies()
cu = CompilationUnit(circuit)
pass1.apply(cu)

print(circuit_to_qasm_str(cu.circuit))