# Format Conversion Graph

Formats: 28, Conversions: 10, Solvers: 30

Boxes group the formats of a format family. Solid arrows are registered converters between formats,  where some additionally require the problem instance as context (labeled with the context type); dashed arrows are solvers, leading from a problem's input family to its result family.

```mermaid
graph LR
    subgraph DemonstratorReport ["DemonstratorReport"]
        demonstrator_report["demonstrator-report"]
    end
    subgraph Knapsack ["Knapsack"]
        knapsack["knapsack"]
    end
    subgraph VRP_Solution ["VRP-Solution"]
        vrp_tour["vrp-tour"]
    end
    subgraph Knapsack_Solution ["Knapsack-Solution"]
        knapsack_solution["knapsack-solution"]
    end
    subgraph MaxCut_Solution ["MaxCut-Solution"]
        gml_solution["gml-solution"]
        qaoa_maxcut_report["qaoa-maxcut-report"]
    end
    subgraph UMCF_Solution ["UMCF-Solution"]
        umcf_result_report["umcf-result-report"]
    end
    subgraph DeadFeatureSolution ["DeadFeatureSolution"]
        dead_feature_list["dead-feature-list"]
    end
    subgraph MoleculeEnergySolution ["MoleculeEnergySolution"]
        molecule_energy_result["molecule-energy-result"]
    end
    subgraph SAT ["SAT"]
        dimacs_cnf["dimacs-cnf"]
        logical_expression["logical-expression"]
    end
    subgraph TSP_Solution ["TSP-Solution"]
        tsplib_tour["tsplib-tour"]
    end
    subgraph ExecutionResult ["ExecutionResult"]
        execution_result["execution-result"]
    end
    subgraph Demonstrator ["Demonstrator"]
        demonstrator_input["demonstrator-input"]
    end
    subgraph FeatureModel ["FeatureModel"]
        uvl["uvl"]
    end
    subgraph MaxCut ["MaxCut"]
        gml["gml"]
    end
    subgraph SolutionCount ["SolutionCount"]
        solution_count["solution-count"]
    end
    subgraph QASM ["QASM"]
        qasm["qasm"]
    end
    subgraph TSP ["TSP"]
        tsplib["tsplib"]
    end
    subgraph VoidFeatureSolution ["VoidFeatureSolution"]
        void_feature_solution["void-feature-solution"]
    end
    subgraph Molecule ["Molecule"]
        xyz_molecule["xyz-molecule"]
    end
    subgraph SAT_Solution ["SAT-Solution"]
        dimacs_cnf_solution["dimacs-cnf-solution"]
    end
    subgraph QUBO_Solution ["QUBO-Solution"]
        lp_solution["lp-solution"]
        json_qubo_solution["json-qubo-solution"]
        qubo_positional_solution["qubo-positional-solution"]
    end
    subgraph VRP ["VRP"]
        vrp["vrp"]
    end
    subgraph QUBO ["QUBO"]
        lp["lp"]
    end
    subgraph UMCF ["UMCF"]
        umcf["umcf"]
    end
    dimacs_cnf --> logical_expression
    logical_expression --> dimacs_cnf
    gml_solution --> qaoa_maxcut_report
    lp_solution --> json_qubo_solution
    vrp --> tsplib
    uvl --> dimacs_cnf
    json_qubo_solution --> lp_solution
    qaoa_maxcut_report -->|"context: Gml"| gml_solution
    lp_solution -->|"context: String"| qubo_positional_solution
    qubo_positional_solution -->|"context: String"| lp_solution
    TSP -.->|"TSP to QUBO Transformation"| TSP_Solution
    TSP -.->|"LKH-3 TSP Solver"| TSP_Solution
    QASM -.->|"Optimize QASM Code"| ExecutionResult
    QASM -.->|"Execute QASM Code"| ExecutionResult
    FeatureModel -.->|"SAT-based Dead Feature Solver"| DeadFeatureSolution
    QASM -.->|"Apply Tket Optimization Pass"| ExecutionResult
    SAT -.->|"Bruteforce Exact SharpSAT solver"| SolutionCount
    SAT -.->|"Ganak SharpSAT solution counter"| SolutionCount
    SAT -.->|"GAMS SAT"| SAT_Solution
    SAT -.->|"Exact QRISP SAT"| SAT_Solution
    SAT -.->|"Grover-Search (Qrisp)"| SAT_Solution
    QUBO -.->|"(Qrisp) QAOA Solver for QUBOs"| QUBO_Solution
    QUBO -.->|"(D-Wave) Annealing QUBO Solver"| QUBO_Solution
    QUBO -.->|"(Qiskit) QAOA Solver for QUBOs"| QUBO_Solution
    QUBO -.->|"(GAMS) CPLEX Solver for QUBOs"| QUBO_Solution
    Molecule -.->|"Qiskit Material Simulation"| MoleculeEnergySolution
    Knapsack -.->|"Qiskit Knapsack"| Knapsack_Solution
    Knapsack -.->|"Horowitz-Sahni Knapsack"| Knapsack_Solution
    VRP -.->|"Cluster before Solving"| VRP_Solution
    VRP -.->|"LKH-3 VRP Solver"| VRP_Solution
    VRP -.->|"Grover-based VRP Solver (Qrisp)"| VRP_Solution
    FeatureModel -.->|"SAT-based Void Feature Model Solver"| VoidFeatureSolution
    UMCF -.->|"(GAMS) CPLEX Classical MCF Solver"| UMCF_Solution
    UMCF -.->|"U-MCF to QUBO Transformation"| UMCF_Solution
    MaxCut -.->|"GAMS MaxCut"| MaxCut_Solution
    MaxCut -.->|"Qiskit MaxCut"| MaxCut_Solution
    MaxCut -.->|"Cirq MaxCut"| MaxCut_Solution
    VRP -.->|"K-means Clustering (VRP -> Set of VRP)"| VRP_Solution
    VRP -.->|"Two Phase Clustering (VRP -> Set of TSP)"| VRP_Solution
    QASM -.->|"Execute OpenQASM circuit"| ExecutionResult
```
