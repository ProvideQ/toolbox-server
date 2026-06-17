from qiskit import qasm2

from BaseCircuit.circuit_core import Circuit
from QTG.QTG import QTG
from knapsack.knapsack import KnapsackInstance


def run_retrieve_openqasm(knapsack: KnapsackInstance):
    # Prepare the quantum circuit
    circuit = Circuit("Knapsack_Demo")
    has_ancillas = False
    circuit.prepare_knapsack_circuit(knapsack, has_ancillas=has_ancillas)

    # Build the QTG
    bias = 0.5
    current_best_solution = "0" * len(knapsack.items)

    qtg = QTG(
        input_circuit=circuit,
        knapsack=knapsack,
        depth_interval=(0, -1),
        bias=bias,
        current_best_solution=current_best_solution,
        has_ancillas=has_ancillas
    )
    qtg.build_circuit()

    # Append QTG
    qreg = circuit.get_qubits_in_registers(all=True)
    circuit.append_subcircuit_as_instruction(qtg.current_circuit, qubits=qreg,
                                             name='qtg_circuit')

    circuit.measure_items("items_c")

    return qasm2.dumps(circuit.qc)
