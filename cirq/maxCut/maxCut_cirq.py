# This QAOA MaxCut implementation is adapted from Google Quantum:
# https://quantumai.google/cirq/experiments/qaoa/qaoa_maxcut

import sys

import cirq
import networkx as nx
import pandas as pd

# Step 1: Define Graph
import sympy
import numpy as np

input_path = sys.argv[1]
output_path = sys.argv[2]

# Read in working graph from problem file
working_graph: nx.Graph = nx.read_gml(input_path, None)
nx.set_edge_attributes(
    working_graph,
    {edge: {"weight": 1} for edge in working_graph.edges}  # TODO: use existing weight
)

# Step 2: Construct QAOA circuit

# Symbols for the rotation angles in the QAOA circuit.
alpha = sympy.Symbol("alpha")
beta = sympy.Symbol("beta")

qubits = cirq.LineQubit.range(working_graph.number_of_nodes())
qaoa_circuit = cirq.Circuit(
    # Prepare uniform superposition on working_qubits == working_graph.nodes
    cirq.H.on_each(qubits),
    # Do ZZ operations between neighbors u, v in the graph. Here, u is a qubit,
    # v is its neighboring qubit, and w is the weight between these qubits.
    (
        cirq.ZZ(qubits[list(working_graph).index(u)], qubits[list(working_graph).index(v)]) ** (alpha * w["weight"])
        for (u, v, w) in working_graph.edges(data=True)
    ),
    # Apply X operations along all nodes of the graph. Again working_graph's
    # nodes are the working_qubits. Note here we use a moment
    # which will force all the gates into the same line.
    cirq.Moment(cirq.X(qubit) ** beta for qubit in qubits),
    # All relevant things can be computed in the computational basis.
    (cirq.measure(qubit) for qubit in qubits),
)

# Step 3: Cost estimation


def estimate_cost(graph: nx.Graph, samples: pd.DataFrame) -> float:
    """Estimate the cost function of the QAOA on the given graph using the
    provided computational basis bitstrings."""
    cost_value = 0.0

    # Loop over edge pairs and compute contribution.
    for u, v, w in graph.edges(data=True):
        u_samples = samples[str(qubits[list(graph).index(u)])]
        v_samples = samples[str(qubits[list(graph).index(v)])]

        # Determine if it was a +1 or -1 eigenvalue.
        u_signs = (-1) ** u_samples
        v_signs = (-1) ** v_samples
        term_signs = u_signs * v_signs

        # Add scaled term to total cost.
        term_val = np.mean(term_signs) * w["weight"]
        cost_value += term_val

    return -cost_value


# Step 4: Classical optimization loop
# Set the grid size = number of points in the interval [0, 2π).
grid_size = 5

alpha_sweep = cirq.Linspace(alpha, 0, 2 * np.pi, grid_size)
beta_sweep = cirq.Linspace(beta, 0, 2 * np.pi, grid_size)

sim = cirq.Simulator()
samples = sim.run_sweep(
    qaoa_circuit, params=alpha_sweep * beta_sweep, repetitions=20000
)

exp_values = np.reshape(samples, (-1, grid_size)).tolist()
estimate = np.vectorize(lambda s: estimate_cost(working_graph, s.data))
exp_values = estimate(exp_values)
par_tuples = [tuple(y[1] for y in param_tuple) for param_tuple in (alpha_sweep * beta_sweep).param_tuples()]
par_values = np.reshape(par_tuples, (-1, grid_size, 2))

# Step 5: Cut comparison
best_exp_index = np.unravel_index(np.argmax(exp_values), exp_values.shape)
best_parameters = par_values[best_exp_index]
print(f"Best control parameters: {best_parameters}")

# Number of candidate cuts to sample.
num_cuts = 100
candidate_cuts = sim.sample(
    qaoa_circuit,
    params={alpha: best_parameters[0], beta: best_parameters[1]},
    repetitions=num_cuts,
)

# Variables to store best cut partitions and cut size.
best_qaoa_S_partition = set()
best_qaoa_T_partition = set()
best_qaoa_cut_size = -np.inf

# Analyze each candidate cut.
for i in range(num_cuts):
    candidate = candidate_cuts.iloc[i]
    one_qubits = set(candidate[candidate == 1].index)
    S_partition = set()
    T_partition = set()
    for node in working_graph.nodes:
        if str(node) in one_qubits:
            # If a one was measured add node to S partition.
            S_partition.add(node)
        else:
            # Otherwise a zero was measured so add to T partition.
            T_partition.add(node)

    cut_size = nx.cut_size(working_graph, S_partition, T_partition, weight="weight")

    # If you found a better cut update best_qaoa_cut variables.
    if cut_size > best_qaoa_cut_size:
        best_qaoa_cut_size = cut_size
        best_qaoa_S_partition = S_partition
        best_qaoa_T_partition = T_partition

print("-----QAOA-----")
working_graph.graph.update({
    "cut_value": best_qaoa_cut_size
})
nx.set_node_attributes(
    working_graph,
    {node: {"Partition": 1 if node in best_qaoa_S_partition else 2} for node in working_graph.nodes}
)

nx.write_gml(working_graph, output_path)
