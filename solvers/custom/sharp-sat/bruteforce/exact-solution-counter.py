import argparse

parser = argparse.ArgumentParser(
    prog="Bruteforce SharpSat solver",
    description="A CLI Program to count number of solutions given a SAT Formula. It uses naive bruteforce approach"
)

parser.add_argument("input_file")
parser.add_argument("--output-file")
args = parser.parse_args()


def parse_dimacs(filename):
    clauses = []
    num_vars = 0
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if line == '' or line.startswith('c'):
                continue
            if line.startswith('p'):
                parts = line.split()
                num_vars = int(parts[2])
            else:
                literals = [int(x) for x in line.split() if int(x) != 0]
                clauses.append(literals)
    return num_vars, clauses


def clause_satisfied(assignment, clause):
    """
    Check if a single clause is satisfied by the given assignment.
    `assignment` is a list (or dict) of boolean values indexed from 1..n,
    `clause` is a list of integers. A positive literal i means x_i must be True.
    A negative literal -i means x_i must be False.
    """
    for lit in clause:
        if lit > 0 and assignment[lit]:
            return True
        if lit < 0 and not assignment[-lit]:
            return True
    return False


def formula_satisfied(assignment, clauses):
    """
    Check if the entire formula is satisfied.
    The formula is satisfied if all clauses are satisfied.
    """
    for clause in clauses:
        if not clause_satisfied(assignment, clause):
            return False
    return True


def count_solutions(num_vars, clauses):
    """
    Count the number of satisfying assignments by brute-forcing all possibilities.
    """
    count = 0
    # We'll represent each assignment with a bit pattern from 0 to 2^num_vars - 1
    # Where the ith variable's truth value is given by checking the ith bit.

    # assignment[var] = True means variable `var` is True, and vice versa.
    # (variable indices go 1..num_vars, so we ignore index 0).

    for bits in range(1 << num_vars):
        # Build the assignment for this bit pattern
        assignment = {}
        for var in range(1, num_vars + 1):
            # Check if the (var-1)-th bit is set
            # (var-1) because bit positions start at 0 but variables start at 1
            assignment[var] = bool(bits & (1 << (var - 1)))

        # Check if this assignment satisfies the formula
        if formula_satisfied(assignment, clauses):
            count += 1

    return count

def write_output(solution_count, output_file=None):
    if output_file:
        with open(output_file, 'w') as f:
            f.write(str(solution_count))
    else:
        print(solution_count)

num_vars, clauses = parse_dimacs(args.input_file)
solution_count = count_solutions(num_vars, clauses)
write_output(solution_count, args.output_file)
