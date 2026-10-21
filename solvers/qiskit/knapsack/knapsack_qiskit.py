import argparse, re

# Qiskit
from qiskit.primitives import StatevectorSampler
from qiskit_optimization.minimum_eigensolvers import QAOA
from qiskit_optimization.optimizers import COBYLA
from qiskit_optimization.algorithms import MinimumEigenOptimizer
from qiskit_optimization.applications import Knapsack
from qiskit_optimization.converters import QuadraticProgramToQubo

parser = argparse.ArgumentParser(description='Solve a knapsack problem using QUBO transformation and reinterpretation')
parser.add_argument('input', help='Input file path')
parser.add_argument('output', help='Output file path')
parser.add_argument('--transform-only', action='store_true', help='Only perform QUBO transformation')
parser.add_argument('--qubo-solution', help='Perform QUBO solution reinterpretation with the provided solution file')

args = parser.parse_args()

input_path = args.input
output_path = args.output

# read input from file
with open(input_path, 'r') as input_file:
    lines = input_file.readlines()

# first line gives number of items
number_items : int = int(lines[0])

# read items into value, weight lists
indexes: list[int] = []
values: list[int] = []
weights: list[int] = []

# read items into value, weight lists
for i in range(number_items):
    item = lines[i + 1].split(' ')
    indexes.append(int(item[0]))
    values.append(int(item[1]))
    weights.append(int(item[2]))

# last line contains maximum capacity of the knapsack
capacity: int = int(lines[-1])

# convert to form needed for solving
knapsack = Knapsack(values, weights, capacity)


if not args.qubo_solution:
    qp = knapsack.to_quadratic_program()
    converter = QuadraticProgramToQubo()
    qubo = converter.convert(qp)

    if args.transform_only:
        problem = qubo.export_as_lp_string()

        # insert space between variable products for gurobipy to parse correctly, e.g. "x1*x2" -> "x1 * x2"
        prod_pattern = re.compile(r"(\S)\*(\S)", flags=re.MULTILINE)
        problem_spaced = prod_pattern.sub(r"\1 * \2", problem)

        # insert space between quadratic terms for gurobipy to parse correctly, e.g. "x1^2" -> "x1 * x1"
        quadratic_pattern = re.compile(r"(\S+)\^2", flags=re.MULTILINE)
        problem_quadratic = quadratic_pattern.sub(r" \1 * \1", problem_spaced)

        with open(output_path, 'w') as f:
            f.write(problem_quadratic)
    else:
        op, offset = qubo.to_ising()
        # TODO: Sampler() has to be replaces with StatevectorSampler() in newer versions.
        # (currently not yet supported by qiskit-optimization)
        # TODO: add a dedicated mixer
        qaoa_mes = QAOA(sampler=StatevectorSampler(), optimizer=COBYLA())
        qaoa = MinimumEigenOptimizer(qaoa_mes)

        qaoa_result = qaoa.solve(qubo)

        # read and interpret results
        z = knapsack.interpret(qaoa_result)

        # format results for output
        included_indexes : list[int] = []

        for i in z:
            # prevent misinterpreted indices from appearing in result
            if i < number_items and indexes[i] not in included_indexes:
                included_indexes.append(indexes[i])

        # grab optimal result from calculated result, negate because it got turned negative for the minimizing algorithm
        achieved_sum : int = -int(qaoa_result.fval)

        with open(output_path, 'w') as f:
            f.write(str(achieved_sum) + "\n")
            f.write(str(included_indexes))

else:
    # read QUBO solution from file
    with open(args.qubo_solution, 'r') as qubo_file:
        qubo_solution = qubo_file.readlines()
        if qubo_solution[-1] != 'status: SUCCESS':
            raise ValueError('QUBO solution file does not indicate a successful solution.')

        # read the objective function value from the solution, after the "fval:" label,
        # and remove any leading/trailing whitespace and the leading negative sign
        qubo_solution_fval = qubo_solution[0].split(':')[1].strip()[1:]

        # extract variable assignments from the solution, one group for variable index and one for variable value
        pattern = re.compile(r"x_(\d+)=([\d|\.]+)[,|\s]", flags=re.MULTILINE)
        matches = pattern.findall(qubo_solution[1])

        # Process the matches to create a list of variable assignments
        included_indexes = []
        for match in matches:
            var_index = int(match[0])
            var_value = float(match[1])
            # if the variable is set to 1, include the corresponding problem index in the result
            if var_value == 1.0:
                included_indexes.append(indexes[var_index])

        with open(output_path, 'w') as f:
            f.write(qubo_solution_fval)
            f.write('\n')
            f.write(str(included_indexes))
