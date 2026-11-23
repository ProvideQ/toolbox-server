# Format Conversion Graph

Formats: 28, Conversions: 10, Solvers: 30

Boxes group the formats of a format family. Solid arrows are registered converters between formats,  where some additionally require the problem instance as context (labeled with the context type); dashed arrows are solvers, leading from a problem's input family to its result family.

```mermaid
graph LR
    subgraph DeadFeatureSolution ["DeadFeatureSolution"]
        dead_feature_list["dead-feature-list"]
    end
    subgraph Demonstrator ["Demonstrator"]
        demonstrator_input["demonstrator-input"]
    end
    subgraph DemonstratorReport ["DemonstratorReport"]
        demonstrator_report["demonstrator-report"]
    end
    subgraph ExecutionResult ["ExecutionResult"]
        execution_result["execution-result"]
    end
    subgraph FeatureModel ["FeatureModel"]
        uvl["uvl"]
    end
    subgraph Knapsack ["Knapsack"]
        knapsack["knapsack"]
    end
    subgraph Knapsack_Solution ["Knapsack-Solution"]
        knapsack_solution["knapsack-solution"]
    end
    subgraph MaxCut ["MaxCut"]
        gml["gml"]
    end
    subgraph MaxCut_Solution ["MaxCut-Solution"]
        gml_solution["gml-solution"]
        qaoa_maxcut_report["qaoa-maxcut-report"]
    end
    subgraph Molecule ["Molecule"]
        xyz_molecule["xyz-molecule"]
    end
    subgraph MoleculeEnergySolution ["MoleculeEnergySolution"]
        molecule_energy_result["molecule-energy-result"]
    end
    subgraph QASM ["QASM"]
        qasm["qasm"]
    end
    subgraph QUBO ["QUBO"]
        lp["lp"]
    end
    subgraph QUBO_Solution ["QUBO-Solution"]
        json_qubo_solution["json-qubo-solution"]
        lp_solution["lp-solution"]
        qubo_positional_solution["qubo-positional-solution"]
    end
    subgraph SAT ["SAT"]
        dimacs_cnf["dimacs-cnf"]
        logical_expression["logical-expression"]
    end
    subgraph SAT_Solution ["SAT-Solution"]
        dimacs_cnf_solution["dimacs-cnf-solution"]
    end
    subgraph SolutionCount ["SolutionCount"]
        solution_count["solution-count"]
    end
    subgraph TSP ["TSP"]
        tsplib["tsplib"]
    end
    subgraph TSP_Solution ["TSP-Solution"]
        tsplib_tour["tsplib-tour"]
    end
    subgraph UMCF ["UMCF"]
        umcf["umcf"]
    end
    subgraph UMCF_Solution ["UMCF-Solution"]
        umcf_result_report["umcf-result-report"]
    end
    subgraph VRP ["VRP"]
        vrp["vrp"]
    end
    subgraph VRP_Solution ["VRP-Solution"]
        vrp_tour["vrp-tour"]
    end
    subgraph VoidFeatureSolution ["VoidFeatureSolution"]
        void_feature_solution["void-feature-solution"]
    end
    dimacs_cnf --> logical_expression
    gml_solution --> qaoa_maxcut_report
    json_qubo_solution --> lp_solution
    logical_expression --> dimacs_cnf
    lp_solution --> json_qubo_solution
    uvl --> dimacs_cnf
    vrp --> tsplib
    lp_solution -->|"context: String"| qubo_positional_solution
    qaoa_maxcut_report -->|"context: Gml"| gml_solution
    qubo_positional_solution -->|"context: String"| lp_solution
    FeatureModel -.->|"SAT-based Dead Feature Solver"| DeadFeatureSolution
    FeatureModel -.->|"SAT-based Void Feature Model Solver"| VoidFeatureSolution
    Knapsack -.->|"Horowitz-Sahni Knapsack"| Knapsack_Solution
    Knapsack -.->|"Qiskit Knapsack"| Knapsack_Solution
    MaxCut -.->|"Cirq MaxCut"| MaxCut_Solution
    MaxCut -.->|"GAMS MaxCut"| MaxCut_Solution
    MaxCut -.->|"Qiskit MaxCut"| MaxCut_Solution
    Molecule -.->|"Qiskit Material Simulation"| MoleculeEnergySolution
    QASM -.->|"Apply Tket Optimization Pass"| ExecutionResult
    QASM -.->|"Execute OpenQASM circuit"| ExecutionResult
    QASM -.->|"Execute QASM Code"| ExecutionResult
    QASM -.->|"Optimize QASM Code"| ExecutionResult
    QUBO -.->|"(D-Wave) Annealing QUBO Solver"| QUBO_Solution
    QUBO -.->|"(GAMS) SCIP Solver for QUBOs"| QUBO_Solution
    QUBO -.->|"(Qiskit) QAOA Solver for QUBOs"| QUBO_Solution
    QUBO -.->|"(Qrisp) QAOA Solver for QUBOs"| QUBO_Solution
    SAT -.->|"Exact QRISP SAT"| SAT_Solution
    SAT -.->|"GAMS SAT"| SAT_Solution
    SAT -.->|"Grover-Search (Qrisp)"| SAT_Solution
    SAT -.->|"Bruteforce Exact SharpSAT solver"| SolutionCount
    SAT -.->|"Ganak SharpSAT solution counter"| SolutionCount
    TSP -.->|"LKH-3 TSP Solver"| TSP_Solution
    TSP -.->|"TSP to QUBO Transformation"| TSP_Solution
    UMCF -.->|"(GAMS) SCIP Classical MCF Solver"| UMCF_Solution
    UMCF -.->|"U-MCF to QUBO Transformation"| UMCF_Solution
    VRP -.->|"Cluster before Solving"| VRP_Solution
    VRP -.->|"Grover-based VRP Solver (Qrisp)"| VRP_Solution
    VRP -.->|"K-means Clustering (VRP -> Set of VRP)"| VRP_Solution
    VRP -.->|"LKH-3 VRP Solver"| VRP_Solution
    VRP -.->|"Two Phase Clustering (VRP -> Set of TSP)"| VRP_Solution
```
