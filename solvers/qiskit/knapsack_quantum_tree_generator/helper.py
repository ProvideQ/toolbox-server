import sys

from knapsack.knapsack import KnapsackInstance, Item


def parse_input(input_data: str):
    lines = input_data.strip().split('\n')
    if not lines:
        raise ValueError("Input data is empty")

    number_items = int(lines[0])
    items = []
    # read items into value, weight lists
    # Note: the input format in the issue description has: index value weight
    # and the code snippet used indexes[i] in the output.
    # We should preserve the original index if we want to return it.
    # However, Item class doesn't store original index explicitly other than 'id' which we overwrite.
    # Let's add 'original_index' to Item or just use id.
    # Actually, the user's snippet does: indexes.append(int(item[0]))
    # and then included_indexes.append(indexes[i])

    # Let's store the original index in the Item object by adding a field or using metadata.
    # For now, let's just use the 'id' field to store the original index from the input.
    for i in range(number_items):
        parts = lines[i + 1].split(' ')
        orig_idx = int(parts[0])
        val = int(parts[1])
        weight = int(parts[2])
        items.append(Item(id=orig_idx, value=val, weight=weight))

    capacity = int(lines[-1])

    return KnapsackInstance(items=items, capacity=capacity, sort_by_value=False)


def run(solve_func):
    arg_count = len(sys.argv) - 1
    if arg_count != 2:
        raise TypeError(
            f'This script expects exactly 2 arguments but got {arg_count}. Input file (argument 1) and output file (argument 2).')

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    _run_with_files(input_path, output_path, solve_func)


def _run_with_files(input_path: str, output_path: str, solve_func):
    with open(input_path, 'r') as f:
        input_data = f.read()

    result = solve_func(input_data)

    with open(output_path, 'w') as f:
        f.write(result)
