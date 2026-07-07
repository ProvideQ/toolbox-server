from BaseCircuit.circuit_core import Circuit
from BaseCircuit.simulator import Simulator
from QTG.QTG import QTG
from data.SimulateOptions import SimulationOptions
from helper import parse_input
from knapsack.knapsack import KnapsackInstance
from helper import run


def _create_output(solve_results):
    probabilities = solve_results["probabilities"]
    knapsack = solve_results["knapsack"]

    # Best feasible
    best_feasible_bitstring = None
    best_value = -1

    for bitstring, _ in probabilities.items():
        weight = sum(
            knapsack.items[i].weight for i, b in enumerate(bitstring) if
            b == '1')
        if weight <= knapsack.capacity:
            value = sum(
                knapsack.items[i].value for i, b in enumerate(bitstring) if
                b == '1')
            if best_value == -1 or value > best_value:
                best_value = value
                best_feasible_bitstring = bitstring

    if best_feasible_bitstring:
        included_indexes = []
        for i, bit in enumerate(best_feasible_bitstring):
            if bit == '1':
                included_indexes.append(knapsack.items[i].id)

        # Sort indexes to match expected output if necessary,
        # but the original code just appended them.
        return f"{best_value}\n{included_indexes}"
    else:
        return "0\n[]"


def _get_solution(knapsack: KnapsackInstance):
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


def _solve(input_data):
    solution = _get_solution(parse_input(input_data))
    print(solution)
    return _create_output(solution)


if __name__ == "__main__":
    run(_solve)
