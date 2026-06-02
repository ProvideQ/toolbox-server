from BaseCircuit.circuit_core import Circuit
from BaseCircuit.simulator import Simulator
from QTG.QTG import QTG
from data.SimulateOptions import SimulationOptions
from knapsack.knapsack import KnapsackInstance


def run_solve(knapsack: KnapsackInstance):
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

    print(circuit.draw())
    # Add measurements
    circuit.measure_items("items_c")

    # Simulate
    sim_opts = SimulationOptions(method="automatic", shots=50000)
    simulator = Simulator(circuit)
    simulator.simulate(sim_opts)

    # Process results
    probabilities = simulator.get_probabilities_from_counts()

    return {
        "probabilities": probabilities,
        "knapsack": knapsack
    }
