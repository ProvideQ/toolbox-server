from input import parse_input
from run import run
from run_retrieve_openqasm import run_retrieve_openqasm


def _create_standard_output(solve_results):
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


def _retrieve_openqasm(input_data):
    return run_retrieve_openqasm(parse_input(input_data))


if __name__ == "__main__":
    run(_retrieve_openqasm)
