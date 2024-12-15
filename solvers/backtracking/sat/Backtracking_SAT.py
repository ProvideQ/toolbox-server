from qrisp import QuantumBool, mcx, QuantumArray, control, auto_uncompute
from qrisp.quantum_backtracking import QuantumBacktrackingTree as QBT

import argparse
import csv
import matplotlib.pyplot as plt

def dimacs_to_array(path_to_file):
    # Read the file into one line
    with open(path_to_file, 'r') as cnf_dimacs_file:
        in_data = cnf_dimacs_file.readlines()
        cnf_dimacs_file.close()

    for line in in_data:
        if line.startswith('p'):
            input_settings = line.split()

    # Convert the DIMACS to an array
    input_cnf = [[int(n) for n in line.split() if n != '0'] for line in in_data if line[0] not in ('c', 'p')]

    return input_cnf, int(input_settings[2])


def check_clauses(sat_formula, q_assignments, h):
    # Checks the clauses of a SAT assignment
    # Get a SAT formular as CNF: format [[1, -2], [-1, 2]]
    # Assignments for each variable
    # Height in the backtracking tree

    # Create a result array
    # Len := number of clauses
    # Initialise the QuantumArray with True
    results = QuantumArray(qtype=QuantumBool(), shape=len(sat_formula))
    for i in results:
        i.flip()
    clause_nr = 0

    # Iterate over each literal in each clause
    # Create a control state for an 'or' gate
    for clause in sat_formula:
        clause_qbls = []
        ctrl_state = ''
        for literal in clause:
            # Find assignment of the current literal
            qbl = q_assignments[abs(literal) - 1]

            # Set up control state
            # If the literal is negativ '1' else '0'
            # Used for the mcx gate - building an 'or' statement
            if literal < 0:
                ctrl_state += '1'
            else:
                ctrl_state += '0'

            # Add assignment of the current literal
            # To check one claus
            clause_qbls.append(qbl)

        # Get the smallest literal in the claus
        smallest_literal = min([abs(x) - 1 for x in clause])

        # Makes sure that the clause is checked only when all the containing literals have been assigned
        # (h <= the smallest literal)
        with control(h[smallest_literal]):
            # Multy control x gate over the assignment of the clause
            # Flip the gate of the result in position of the current claus
            mcx(clause_qbls, results[clause_nr], ctrl_state=ctrl_state)

        clause_nr += 1

    # return list with truth values
    return results


def check_sat_assignments(sat_formula, q_assignments, h):
    # Checks a SAT assignment
    # Get a SAT formular as CNF: format [[1, -2], [-1, 2]]
    # Assignments for each variable
    # Height in the backtracking tree

    # evaluate each clause
    comparison_qbls = check_clauses(sat_formula, q_assignments, h)

    # Quantum Bool - default False
    sudoku_valid = QuantumBool()

    # Compute the result
    # mcx over the clauses of the SAT-Formula
    # AND-Gate
    mcx(comparison_qbls, sudoku_valid)

    return sudoku_valid


class Backtracking:
    def __init__(self, sat_formula, number_of_literal, get_assignment, input_precision):
        self.__sat_formula = sat_formula
        self.__get_assignment = get_assignment
        self.__input_precision = input_precision
        self.mes_res = None
        self.__tree = QBT(max_depth = number_of_literal+1,
                          branch_qv = QuantumBool(),
                          accept = self.__accept,
                          reject = self.__reject,
                          subspace_optimization = True)

    def get_sat_formula(self):
        return self.__sat_formula

    def set_sat_formula(self, sat_formula):
        self.__sat_formula = sat_formula

    def get_get_assignment(self):
        return self.__get_assignment

    def set_get_assignment(self, get_assignment):
        self.__get_assignment = get_assignment

    def get_input_precision(self):
        return self.__input_precision

    def set_input_precision(self, input_precision):
        self.__input_precision = input_precision

    def show_statevector(self):
        self.__tree.visualize_statevector()
        plt.show()

    def get_statevector_graph(self, get_root):
        return self.__tree.statevector_graph(get_root)

    def get_depth(self):
        return self.__tree.qs.compile().depth()

    def get_cnot_count(self):
        return self.__tree.qs.compile().cnot_count()

    def get_quantum_circuit(self):
        return self.__tree.qs.compile()

    def get_num_qubits(self):
        return self.__tree.qs.compile().num_qubits()

    def get_mes_restult(self):
        return self.mes_res

    # Accept funktion used by Qrisp Backtracking-tree
    # A node is accepted if it has height 0 and none of its ancestor nodes were rejected
    @auto_uncompute
    def __accept(self, tree):
        return tree.h == 0


    # Reject funktion used by Qrisp Backtracking-tree
    @auto_uncompute
    def __reject(self, tree):
        # Cut off the assignment with height 0
        # since it is not relevant
        q_assignments = tree.branch_qa[1:]

        # Modify the height to reflect the cut-off
        modified_height = tree.h[1:]

        # Check the assignments for the SAT-Formula
        assignment_valid = check_sat_assignments(self.__sat_formula, q_assignments, modified_height)
        return assignment_valid.flip()


    def calculate(self):
        # Calculate the backtracking tree

        # Get an assignment for the given SAT-Formular
        # Get if there is an assignment for the given SAT-Formula
        if self.__get_assignment:
            # Get an assignment
            sol = self.__tree.find_solution(precision=self.__input_precision)
            return sol[::-1][1:]
        else:
            # Initialize root
            self.__tree.init_node([])

            # Perform QPE
            qpe_res = self.__tree.estimate_phase(precision=self.__input_precision)

            # Retrieve measurements
            self.mes_res = qpe_res.get_measurement()

            if self.mes_res[0] > 0.4:
                # 'Solution exists'
                return True
            elif self.mes_res[0] < 0.3:
                # 'No solution exists'
                return False
            else:
                # Precision variable needs to be increased
                return 'Insufficient precision'


def main():
    parser = argparse.ArgumentParser(description='An SAT solver')

    parser.add_argument('file_name', type=str, nargs=1, metavar='Path',
                        help="Path to dimacs file with SAT formula. path/to/file.dimacs")
    parser.add_argument('get_assignment', choices=['True', 'False'], nargs='?', default='False',
                        metavar='Get assignment',
                        help='If true get an assignment for the SAT formula, if there is one. [True/False]')
    parser.add_argument('input_precision', choices=range(2, 9), type=int, nargs='?', default=3, metavar='Precision',
                        help='Set the precision of the backtracking algorithm. Integer (2-8)')
    parser.add_argument('to_file', type=str, nargs='?', metavar='Print into file',
                        help='Path to result file. If file dose not exist create it.')

    args = parser.parse_args()

    sat_problem, literals = dimacs_to_array(args.file_name[0])

    assignment = False
    if args.get_assignment == 'True':
        assignment = True

    bt = Backtracking(sat_problem, literals, assignment, args.input_precision)

    if args.to_file:
        with open(args.to_file, mode='a') as result_file:
            result_write = csv.writer(result_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)

            result_write.writerow([args.file_name[0], bt.calculate(), bt.get_mes_restult(), bt.get_depth(), bt.get_cnot_count(), bt.get_num_qubits(), bt.get_input_precision()])
    else:
        print('File Name: %s' % args.file_name[0])
        print('SAT Formula: %s' % bt.get_sat_formula())
        print('SAT Answer: %s' % bt.calculate())
        print('Measurement: %s' % bt.get_mes_restult())
        print('Depth: %s' % bt.get_depth())
        print('cnot count: %s' % bt.get_cnot_count())
        print('Number of qubits: %s' % bt.get_num_qubits())


if __name__ == '__main__':
    main()



