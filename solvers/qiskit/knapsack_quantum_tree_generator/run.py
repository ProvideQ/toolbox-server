import sys


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
