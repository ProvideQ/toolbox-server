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
