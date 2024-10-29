import argparse

from qrisp.grover import grovers_alg, tag_state
from qrisp import *

parser = argparse.ArgumentParser(
    prog="Qrisp Grover SAT Solver",
    description="A CLI Program to solve SAT problems in DIMACS CNF format with Qrisp"
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

def sat_oracle(quantum_variables, sat_clauses):
    # Oracle compares the CNF literals with the quantum variables.
    # Use XNOR gate because it returns True when both quantum variable
    # and literal have the same value.
    #
    # Check if a clause is fulfilled by  using de morgan laws to calculate the logical OR of literals
    # using only conjunctions (AND) and negations (NOT) (a ∨ b ∨ c = ¬(¬a ∧ ¬b ∧ ¬c)):
    # - invert the assignments of all control variables
    # - use AND-Gate by using multi controlled X gate (mcx)
    # - flip the result.
    # This gives the OR of the original literals without OR Gate.
    #
    # After calculating if the clause is fulfilled with the given quantum_variables,
    # use AND-Gate on all clause_satisfied_flags to see if CNF is fulfilled with this assigment.
    # If yes, tag the state (assigment of quantum_variables) using tag_state().
    clause_satisfied_flags = []
    for clause in sat_clauses:
        clause_satisfied = QuantumBool()
        controls = []
        for literal in clause:
            quantum_variable_index = abs(literal) - 1
            corresponding_qubit = quantum_variables[quantum_variable_index]
            literal_qubit = QuantumBool()

            # set literal_qubit to represent literals value:
            # true for positive literals, false for negative
            if literal < 0:
                literal_qubit[:] = False
            else:
                literal_qubit[:] = True

            # Compare quantum variable and literal using XNOR gate
            # x | y | x XNOR y
            # 0 | 0 |    1
            # 0 | 1 |    0
            # 1 | 0 |    0
            # 1 | 1 |    1

            result = xnor_gate(corresponding_qubit, literal_qubit)
            controls.append(result)

            literal_qubit.uncompute()
            literal_qubit.delete()

        # calculate OR of literals by using de morgans (easy, as clauses are from cnf) and multi controlled x gate:
        # OR: a ∨ b ∨ c = ¬(¬a ∧ ¬b ∧ ¬c)
        for control in controls:
            x(control)

        # AND on flipped controls
        mcx(controls, clause_satisfied)
        x(clause_satisfied) # negate whole clause to get OR of the original literals
        clause_satisfied_flags.append(clause_satisfied)

        for control in controls:
            control.uncompute()
            control.delete()

    # tag state (apply multi Z gate) only if the current assignment of quantum variables fulfill CNF ==
    # if all clauses are satisfied
    # again, do this by multi controlled x gate (AND gate)
    all_clauses_satisfied = QuantumBool()
    mcx(clause_satisfied_flags, all_clauses_satisfied)
    with all_clauses_satisfied == True: # this is qrisps if for quantum variables
        target_state = {qvar: 1 for qvar in clause_satisfied_flags}
        tag_state(tag_specificator=target_state)

    all_clauses_satisfied.uncompute()
    all_clauses_satisfied.delete()
    for flag in clause_satisfied_flags:
        flag.uncompute()
        flag.delete()

def xnor_gate(x: QuantumBool, y: QuantumBool):
    # XNOR gate implementation using two CX gates
    # x | y | x XNOR y
    # 0 | 0 |    1
    # 0 | 1 |    0
    # 1 | 0 |    0
    # 1 | 1 |    1

    result = QuantumBool()
    result[:] = True
    cx(x, result)
    cx(y, result)
    return result

num_vars, clauses = parse_dimacs(args.input_file)
qvars = [QuantumBool() for _ in range(num_vars)]

grovers_alg(
    qvars,
    sat_oracle,
    kwargs={"sat_clauses": clauses},
)

result = [qv.get_measurement() for qv in qvars]
assignment = {i + 1: True if measurement[True] >= 0.5 else False for i, measurement in enumerate(result)}

if args.output_file:
    with open(args.output_file, 'w') as f:
        if assignment:
            f.write("s SATISFIABLE\n")
            f.write("v " + " ".join([f"{var if val else -var}" for var, val in assignment.items()]) + " 0\n")
        else:
            f.write("s UNSATISFIABLE\n")
else:
    if assignment:
        print("s SATISFIABLE")
        print("v " + " ".join([f"{var if val else -var}" for var, val in assignment.items()]) + " 0")
    else:
        print("s UNSATISFIABLE")

