import re

from qiskit_optimization import QuadraticProgram

def parse_lp_file(file_path):
    # Read the LP file
    with open(file_path, 'r') as file:
        lines = file.readlines()

    # Initialize components for parsing
    objective_sense = 'minimize'  # default
    objective = ''
    constraints = []
    variable_types = {}
    in_objective_section = False
    in_constraints_section = False
    in_binary_section = False

    # Parsing logic
    for line in lines:
        line = line.strip()

        # Objective function
        if line.lower().startswith('minimize') or line.lower().startswith('maximize'):
            in_objective_section = True
            objective_sense = line.split()[0].lower()
            continue

        if in_objective_section and (line.lower().startswith('subject to') or line.lower().startswith('binary')):
            in_objective_section = False
            continue

        if in_objective_section:
            objective += line
            continue

        # Constraints (no constraints in this example, but you may extend it later)
        if line.lower().startswith('subject to'):
            in_constraints_section = True
            continue
        elif in_constraints_section and line.lower().startswith('binary'):
            in_constraints_section = False

        # Binary variables
        if line.lower().startswith('binary'):
            in_binary_section = True
            continue

        if in_binary_section:
            variables = line.split()
            for var in variables:
                variable_types[var] = 'binary'

    return objective_sense, objective, constraints, variable_types


def parse_objective(objective_str):
    """ Parse the objective function to extract linear and quadratic coefficients """
    # Remove the 'obj:' prefix
    objective_str = re.sub(r'^obj:\s*', '', objective_str)

    # Extract and remove the division factor (e.g., "/ 2", "/ 3", "/ 10")
    division_factor_match = re.search(r'/\s*([\d\.]+)', objective_str)
    division_factor = float(division_factor_match.group(1)) if division_factor_match else 1.0
    objective_str = re.sub(r'/\s*[\d\.]+', '', objective_str)

    # Remove square brackets
    objective_str = objective_str.replace('[', '').replace(']', '')

    # Extract terms using regular expression
    term_pattern = re.compile(r'([+-]?\s*\d*\.?\d+(?:e[+-]?\d+)?)\s*\*?\s*([\w\d]+)\s*(?:\*\s*([\w\d]+))?')
    terms = term_pattern.findall(objective_str)
    linear = {}
    quadratic = {}

    # Parse each term
    for coeff_str, var1, var2 in terms:
        coeff = float(coeff_str.replace(' ', ''))  # Remove any spaces in coefficient
        
        if var2:  # Quadratic term
            if (var1, var2) in quadratic:
                quadratic[(var1, var2)] += coeff
            else:
                quadratic[(var1, var2)] = coeff
        else:  # Linear term
            if var1 in linear:
                linear[var1] += coeff
            else:
                linear[var1] = coeff

    # Apply division factor
    if division_factor != 1.0 and division_factor != 0:
        for key in linear:
            linear[key] /= division_factor
        for key in quadratic:
            quadratic[key] /= division_factor

    return linear, quadratic


def convert_to_quadratic_program(lp_file_path):
    # Parse the LP file
    objective_sense, objective, constraints, variable_types = parse_lp_file(lp_file_path)

    # Parse objective components
    linear, quadratic = parse_objective(objective)

    # Create an instance of QuadraticProgram
    qp = QuadraticProgram()

    # Add variables
    for var in variable_types:
        if variable_types[var] == 'binary':
            qp.binary_var(var)

    # Set the objective sense
    if objective_sense == 'minimize':
        qp.minimize(linear=linear, quadratic=quadratic)
    else:
        qp.maximize(linear=linear, quadratic=quadratic)

    # Add constraints (empty in this example)
    # You can extend this logic to handle any constraints

    return qp