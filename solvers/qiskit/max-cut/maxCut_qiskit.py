# Hippity hoppity your code is now my property!
# https://qiskit.org/documentation/optimization/tutorials/06_examples_max_cut_and_tsp.html
# https://pypi.org/project/pygmlparser/


# useful additional packages
import numpy as np
import networkx as nx
import sys

import pygmlparser as pygmlparser

# gml parsing
from pygmlparser.Parser import Parser
from pygmlparser.Graph import Graph

# Qiskit
from qiskit.circuit.library import TwoLocal
from qiskit.primitives import Sampler
from qiskit_optimization.applications import Maxcut
from qiskit_algorithms import SamplingVQE
from qiskit_algorithms.optimizers import SPSA


#if len(sys.argv) != 3:
#    raise TypeError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

# To include the weight information, we'll need to map the weight to the label
# so the parser can read it. Therefore, remove all existing "label" lines and
# replace all "weight" lines with "label" lines.
with open(input_path, 'r+') as file:
    # Read all lines from the file
    lines = file.readlines()

    # Move the file cursor to the beginning
    file.seek(0)

    # Iterate through the lines, removing lines with "label" and replacing "weight" with "label"
    for line in lines:
        if "label" not in line:
            modified_line = line.replace("weight", "label")
            file.write(modified_line)

    # Truncate the remaining content in case the new content is shorter than the old content
    file.truncate()

# Instantiate a parser, load a file, and parse it!
parser: Parser = Parser()
parser.loadGML(input_path)
parser.parse()

# Retrieve the graph nodes
nodes: Graph.Nodes = parser.graph.graphNodes  # a map of id -> Node objects

# Retrieve the graph edges
edges: Graph.Edges = parser.graph.graphEdges  # list of Edge objects

# Generating a graph
n = len(nodes)  # Number of nodes in graph
G = nx.Graph()
G.add_nodes_from(np.arange(0, n, 1))
elist = []

for e in edges:
    elist.append((e.source_node.id, e.target_node.id, e.label))

# tuple is (i,j,weight) where (i,j) is the edge
G.add_weighted_edges_from(elist)

# Computing the weight matrix from the graph
w = np.zeros([n, n])
for i in range(n):
    for j in range(n):
        temp = G.get_edge_data(i, j, default=0)
        if temp != 0:
            weight = temp["weight"]
            w[i, j] = 1 if weight is None else weight

max_cut = Maxcut(w)
qp = max_cut.to_quadratic_program()
qubitOp, offset = qp.to_ising()

# construct VQE
optimizer=SPSA(maxiter=300)
ry = TwoLocal(qubitOp.num_qubits, "ry", "cz", reps=5, entanglement="linear")
vqe = SamplingVQE(sampler=Sampler(), ansatz=ry, optimizer=optimizer)

# run VQE
result = vqe.compute_minimum_eigenvalue(qubitOp)

# save results
x = max_cut.sample_most_likely(result.eigenstate)
f = open(output_path, 'w')
f.write("energy:" + str(result.eigenvalue.real) + "\n")
f.write("time:" + str(result.optimizer_time) + "\n")
f.write("max-cut objective:" + str(result.eigenvalue.real + offset) + "\n")
f.write("solution:" + str(x) + "\n")
f.write("solution objective:" + str(qp.objective.evaluate(x)) + "\n")
f.close()
