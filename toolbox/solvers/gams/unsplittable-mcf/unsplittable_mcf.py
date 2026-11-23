import json
import logging
import os

import gamspy as gp
import numpy as np
import pandas as pd


class UnsplittableMCF:
    DEFAULT_CONFIG = {
        "N_PRODUCTS": 1,
        "N_TIME_STEPS": 4,
        "N_ORDERS": 1,
        "N_FACTORIES": 1,
        "N_SORT": 1,
        "N_DIEBANK": 1,
        "N_ASSEMBLY": 1,
        "N_TEST": 1,
        "N_DISTRIBUTION": 1,
        "N_DEMAND": 1,
        "MIN_CYCLE_TIME": 0,
        "MAX_CYCLE_TIME": 1,
        "MIN_CAPACITY": 10,
        "MAX_CAPACITY": 15,
        "MIN_LOAD_FACTOR": 1,
        "MAX_LOAD_FACTOR": 2,
        "PENALTY_DELAY": 5,
        "PENALTY_SLACK": 40,
        "PENALTY_CONSTANT": 50,
    }

    def __init__(self, config_file="config.txt", instance_file=None):
        self.m = gp.Container()
        self.instance_data = {}

        if instance_file and os.path.exists(instance_file):
            with open(instance_file, "r") as f:
                self.instance_data = json.load(f)

            self.config = self.DEFAULT_CONFIG.copy()
            self.config.update(self.instance_data.get("config", {}))

            self._build_base_symbols()
            self._populate_from_data()

        elif config_file and os.path.exists(config_file):
            self.config = self._read_config(config_file)
            self._build_base_symbols()
            self._generate_random_data()

        else:
            raise FileNotFoundError(
                "Must provide either a valid config_file or instance_file."
            )

        self._derive_network_parameters()

    def _read_config(self, filename):
        cfg = self.DEFAULT_CONFIG.copy()

        with open(filename, "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    clean_key = key.strip()

                    if clean_key not in self.DEFAULT_CONFIG:
                        print(f"Warning: Ignoring unknown config key '{clean_key}'")
                        continue

                    try:
                        cfg[clean_key] = int(val.strip())
                    except ValueError:
                        cfg[clean_key] = val.strip()
        return cfg

    def _build_base_symbols(self):
        m = self.m
        c = self.config

        self.p = gp.Set(m, "p", records=[f"p{i}" for i in range(c["N_PRODUCTS"])])
        self.t = gp.Set(m, "t", records=[f"t{i}" for i in range(c["N_TIME_STEPS"])])
        self.o = gp.Set(m, "o", records=[f"o{i}" for i in range(c["N_ORDERS"])])

        self.v = gp.Set(
            m,
            "v",
            records=(
                    ["v_source", "v_magic"]
                    + [f"v_fac{i}" for i in range(c["N_FACTORIES"])]
                    + [f"v_sort{i}" for i in range(c["N_SORT"])]
                    + [f"v_db{i}" for i in range(c["N_DIEBANK"])]
                    + [f"v_asm{i}" for i in range(c["N_ASSEMBLY"])]
                    + [f"v_test{i}" for i in range(c["N_TEST"])]
                    + [f"v_dc{i}" for i in range(c["N_DISTRIBUTION"])]
            ),
            description="nodes",
        )

        v, t, o, p = self.v, self.t, self.o, self.p

        # Alias
        self.v1 = gp.Alias(m, "v1", alias_with=v)
        self.v2 = gp.Alias(m, "v2", alias_with=v)
        self.t1 = gp.Alias(m, "t1", alias_with=t)
        self.t2 = gp.Alias(m, "t2", alias_with=t)

        # simple network nodes
        self.v_source = gp.Set(m, "v_source", domain=v, records=["v_source"])
        self.v_magic = gp.Set(m, "v_magic", domain=v, records=["v_magic"])

        self.v_factory = gp.Set(
            m,
            "v_factory",
            domain=v,
            records=[f"v_fac{i}" for i in range(c["N_FACTORIES"])],
        )
        self.v_sort = gp.Set(
            m,
            "v_sort",
            domain=v,
            records=[f"v_sort{i}" for i in range(c["N_SORT"])],
        )
        self.v_diebank = gp.Set(
            m,
            "v_diebank",
            domain=v,
            records=[f"v_db{i}" for i in range(c["N_DIEBANK"])],
        )
        self.v_assembly = gp.Set(
            m,
            "v_assembly",
            domain=v,
            records=[f"v_asm{i}" for i in range(c["N_ASSEMBLY"])],
        )
        self.v_test = gp.Set(
            m,
            "v_test",
            domain=v,
            records=[f"v_test{i}" for i in range(c["N_TEST"])],
        )
        self.v_distribution = gp.Set(
            m,
            "v_distribution",
            domain=v,
            records=[f"v_dc{i}" for i in range(c["N_DISTRIBUTION"])],
        )

        # edges
        self.e = gp.Set(m, "e", domain=[v, t, v, t], description="directed TEN arcs")

        # edges with corresponding orders
        self.eo = gp.Set(
            m,
            "eo",
            domain=[v, t, v, t, o],
            description="directed TEN arcs that can be used for order o",
        )
        self.eo_delay = gp.Set(
            m,
            "eo_delay",
            domain=[v, t, v, t, o],
            description="directed TEN arcs for delayed delivery for order o",
        )
        self.eo_slack = gp.Set(
            m,
            "eo_slack",
            domain=[v, t, v, t, o],
            description="directed TEN arcs for unsatisfied demand for order o",
        )

        # mappings
        self.o_map = gp.Set(
            m,
            "o_map",
            domain=[o, p, t, self.v_distribution],
            description="order-product-time-distribution_center mapping",
        )
        self.o_map2 = gp.Set(
            m,
            "o_map2",
            domain=[o, t, self.v_distribution],
            description="order-time-distribution_center mapping",
        )
        self.o_v_distribution = gp.Set(
            m,
            "o_v_distribution",
            domain=[o, self.v_distribution],
            description="order to distribution center mapping",
        )
        self.o_t = gp.Set(
            m,
            "o_t",
            domain=[o, t],
            description="order to time step mapping",
        )
        self.o_p = gp.Set(
            m,
            "o_p",
            domain=[o, p],
            description="order to product mapping",
        )

        # Parameters
        self.demand = gp.Parameter(m, "demand", domain=[o, p, t, v])
        self.demand_o = gp.Parameter(
            m, "demand_o", domain=[o, t, v], description="demand of order o"
        )
        self.total_demand = gp.Parameter(
            m, "total_demand", domain=o, description="total demand of order o"
        )

        self.cycle_time = gp.Parameter(
            m, "cycle_time", domain=v, description="cycle time at node v"
        )
        self.capacity = gp.Parameter(
            m, "capacity", domain=v, description="capacity of node v"
        )
        self.load_factor = gp.Parameter(
            m,
            "load_factor",
            domain=[v, p],
            description="capacity load factor of product p at node v",
        )

        self.cost_delay = gp.Parameter(
            m,
            "cost_delay",
            domain=[v, t, v, t, o],
            description="cost for a delayed order",
        )
        self.cost_slack = gp.Parameter(
            m,
            "cost_slack",
            domain=[v, t, v, t, o],
            description="cost for unsatisfied orders",
        )

    def _generate_random_data(self):
        """Generates ONLY the core base data that will be saved in the JSON."""
        np.random.seed(42)
        c = self.config
        v, t, o, p, e = self.v, self.t, self.o, self.p, self.e

        (
            v_source,
            v_factory,
            v_sort,
            v_diebank,
            v_assembly,
            v_test,
            v_distribution,
        ) = (
            self.v_source,
            self.v_factory,
            self.v_sort,
            self.v_diebank,
            self.v_assembly,
            self.v_test,
            self.v_distribution,
        )
        v1, v2, t1, t2 = self.v1, self.v2, self.t1, self.t2

        p_list = p.records["uni"].tolist()
        t_list = t.records["uni"].tolist()
        v_dist_list = v_distribution.records["v"].tolist()

        demand_records = []
        for rec_o in o.records["uni"]:
            p_choice = np.random.choice(p_list)
            t_choice = np.random.choice(t_list)
            v_choice = np.random.choice(v_dist_list)
            val = np.random.randint(1, 5) * c["N_DEMAND"]
            demand_records.append((rec_o, p_choice, t_choice, v_choice, val))

        self.demand.setRecords(demand_records)

        self.cycle_time[v_factory] = np.random.randint(
            c["MIN_CYCLE_TIME"], c["MAX_CYCLE_TIME"] + 1
        )
        self.cycle_time[v_sort] = np.random.randint(
            c["MIN_CYCLE_TIME"], c["MAX_CYCLE_TIME"] + 1
        )
        self.cycle_time[v_diebank] = 0
        self.cycle_time[v_assembly] = np.random.randint(
            c["MIN_CYCLE_TIME"], c["MAX_CYCLE_TIME"] + 1
        )
        self.cycle_time[v_test] = np.random.randint(
            c["MIN_CYCLE_TIME"], c["MAX_CYCLE_TIME"] + 1
        )
        self.cycle_time[v_distribution] = 0

        self.capacity[v_factory] = np.random.randint(
            c["MIN_CAPACITY"], c["MAX_CAPACITY"] + 1
        )
        self.capacity[v_sort] = np.random.randint(
            c["MIN_CAPACITY"], c["MAX_CAPACITY"] + 1
        )
        self.capacity[v_diebank] = gp.SpecialValues.POSINF
        self.capacity[v_assembly] = np.random.randint(
            c["MIN_CAPACITY"], c["MAX_CAPACITY"] + 1
        )
        self.capacity[v_test] = np.random.randint(
            c["MIN_CAPACITY"], c["MAX_CAPACITY"] + 1
        )
        self.capacity[v_distribution] = gp.SpecialValues.POSINF

        self.load_factor[v_factory, p] = np.random.randint(
            c["MIN_LOAD_FACTOR"], c["MAX_LOAD_FACTOR"] + 1
        )
        self.load_factor[v_sort, p] = np.random.randint(
            c["MIN_LOAD_FACTOR"], c["MAX_LOAD_FACTOR"] + 1
        )
        self.load_factor[v_diebank, p] = 0
        self.load_factor[v_assembly, p] = np.random.randint(
            c["MIN_LOAD_FACTOR"], c["MAX_LOAD_FACTOR"] + 1
        )
        self.load_factor[v_test, p] = np.random.randint(
            c["MIN_LOAD_FACTOR"], c["MAX_LOAD_FACTOR"] + 1
        )
        self.load_factor[v_distribution, p] = 0

        # graph structure is not really random, but will bes saved in json
        e[v_source, t1, v_factory, t2].where[t1.first] = True
        e[v_factory, t1, v_sort, t2].where[
            gp.Ord(t1) + self.cycle_time[v_factory] == gp.Ord(t2)
            ] = True
        e[v_sort, t1, v_diebank, t2].where[
            (gp.Ord(t1) + self.cycle_time[v_sort] == gp.Ord(t2))
            & gp.Sum(e[v, t, v_sort, t1], 1)
            ] = True
        e[v_diebank, t1, v_diebank, t2].where[
            (gp.Ord(t1) + 1 == gp.Ord(t2)) & gp.Sum(e[v, t, v_diebank, t1], 1)
            ] = True
        e[v_diebank, t1, v_assembly, t2].where[
            (gp.Ord(t1) + self.cycle_time[v_diebank] == gp.Ord(t2))
            & gp.Sum(e[v, t, v_diebank, t1], 1)
            ] = True
        e[v_assembly, t1, v_test, t2].where[
            (gp.Ord(t1) + self.cycle_time[v_assembly] == gp.Ord(t2))
            & gp.Sum(e[v, t, v_assembly, t1], 1)
            ] = True
        e[v_test, t1, v_distribution, t2].where[
            (gp.Ord(t1) + self.cycle_time[v_test] == gp.Ord(t2))
            & gp.Sum(e[v, t, v_test, t1], 1)
            ] = True
        e[v_distribution, t1, v_distribution, t2].where[
            (gp.Ord(t1) + 1 == gp.Ord(t2))
        ] = True

        self.eo[v1, t1, v2, t2, o].where[e[v1, t1, v2, t2]] = True

        print("Random base data successfully generated.")

    def _populate_from_data(self):
        symbols_to_load = [
            "cycle_time",
            "capacity",
            "load_factor",
            "demand",
            "eo",
        ]

        for sym in symbols_to_load:
            if sym in self.instance_data:
                records_df = pd.DataFrame(self.instance_data[sym])
                records_df = records_df.replace(1e100, gp.SpecialValues.POSINF)
                gams_obj = getattr(self, sym)
                gams_obj.setRecords(records_df)

        print("Base data successfully populated from JSON.")

    def _derive_network_parameters(self):
        c = self.config
        t, o, p = self.t, self.o, self.p
        v1, v2, t1, t2 = self.v1, self.v2, self.t1, self.t2
        v_distribution, v_magic = self.v_distribution, self.v_magic

        self.o_map[o, p, t, v_distribution].where[
            self.demand[o, p, t, v_distribution]
        ] = True

        self.o_map2[o, t, v_distribution].where[
            gp.Sum(p, self.o_map[o, p, t, v_distribution])
        ] = True
        self.o_v_distribution[o, v_distribution].where[
            gp.Sum((p, t), self.o_map[o, p, t, v_distribution])
        ] = True
        self.o_t[o, t].where[
            gp.Sum((p, v_distribution), self.o_map[o, p, t, v_distribution])
        ] = True
        self.o_p[o, p].where[
            gp.Sum((t, v_distribution), self.o_map[o, p, t, v_distribution])
        ] = True

        self.e[v1, t1, v2, t2].where[gp.Sum(o, self.eo[v1, t1, v2, t2, o])] = True

        self.demand_o[o, t, v_distribution].where[self.o_map2[o, t, v_distribution]] = (
            gp.Sum(p, self.demand[o, p, t, v_distribution])
        )
        self.total_demand[o] = gp.Sum(
            (p, t, v_distribution), self.demand[o, p, t, v_distribution]
        )

        self.eo_delay[v_distribution, t1, v_distribution, t2, o].where[
            self.o_t[o, t2]
            & self.o_v_distribution[o, v_distribution]
            & (gp.Ord(t1) > gp.Ord(t2))
            ] = True

        self.eo_slack[v_magic, t, v_distribution, t, o].where[
            self.o_t[o, t] & self.o_v_distribution[o, v_distribution]
            ] = True

        self.cost_delay[v1, t1, v2, t2, o].where[self.eo_delay[v1, t1, v2, t2, o]] = (
                                                                                             gp.Ord(t1) - gp.Ord(t2)
                                                                                     ) * c["PENALTY_DELAY"]
        self.cost_slack[v1, t1, v2, t2, o].where[self.eo_slack[v1, t1, v2, t2, o]] = c[
            "PENALTY_SLACK"
        ]

    def build_equations_and_model(self):
        m = self.m
        v, t, o, p, e = self.v, self.t, self.o, self.p, self.e
        o_p = self.o_p
        demand_o, total_demand = self.demand_o, self.total_demand
        capacity, load_factor = (
            self.capacity,
            self.load_factor,
        )

        v_source = self.v_source
        eo_delay, eo_slack = (
            self.eo_delay,
            self.eo_slack,
        )
        cost_delay, cost_slack = self.cost_delay, self.cost_slack

        v1, v2, t1, t2 = self.v1, self.v2, self.t1, self.t2

        # time expanded network nodes for all node and time steps that are in the graph
        self.vt = gp.Set(m, "vt", domain=[v, t])
        self.vt[v, t].where[
            gp.Sum(self.e[v, t, v2, t2], 1) + gp.Sum(self.e[v1, t1, v, t], 1) > 0
            ] = True

        vt = self.vt

        # Variable
        self.Y = gp.Variable(
            m,
            "Y",
            type="binary",
            domain=[v1, t1, v2, t2, o],
            description="binary variable that decides if a arc is used for order o",
        )
        Y = self.Y

        # Equations
        eq_flow_balance = gp.Equation(
            m,
            "eq_flow_balance",
            domain=[v, t, o],
            description="flow conservation (includes demand satisfaction)",
        )
        eq_capacity = gp.Equation(
            m,
            "eq_capacity",
            domain=[v, t],
            description="capacity constraint to restrict inflow",
        )

        obj = gp.Sum(
            eo_delay[v1, t1, v2, t2, o],
            Y[eo_delay] * total_demand[o] * cost_delay[eo_delay],
            ) + gp.Sum(
            eo_slack[v1, t1, v2, t2, o],
            Y[eo_slack] * total_demand[o] * cost_slack[eo_slack],
            )

        # all in coming and out going flows need to be equal to the demand at that node
        # demand is only not zero for the distribution centers
        eq_flow_balance[v1, t1, o].where[vt[v1, t1] & ~v_source[v1]] = demand_o[
                                                                           o, t1, v1
                                                                       ] == gp.Sum(e[v2, t2, v1, t1], Y[e, o] * total_demand[o]) - gp.Sum(
            e[v1, t1, v2, t2], Y[e, o] * total_demand[o]
        ) + gp.Sum(eo_delay[v2, t2, v1, t1, o], Y[eo_delay] * total_demand[o]) - gp.Sum(
            eo_delay[v1, t1, v2, t2, o], Y[eo_delay] * total_demand[o]
        ) + gp.Sum(eo_slack[v2, t2, v1, t1, o], Y[eo_slack] * total_demand[o]) - gp.Sum(
            eo_slack[v1, t1, v2, t2, o], Y[eo_slack] * total_demand[o]
        )

        eq_capacity[v1, t1].where[vt[v1, t1] & ~v_source[v1]] = (
                gp.Sum(
                    gp.Domain(e[v2, t2, v1, t1], o_p[o, p]),
                    Y[e, o] * total_demand[o] * load_factor[v1, p],
                    )
                <= capacity[v1]
        )

        self.mcf = gp.Model(
            m,
            "mcf",
            problem="MIP",
            equations=m.getEquations(),
            sense=gp.Sense.MIN,
            objective=obj,
        )

        print("GAMSPy Model successfully built.")

    def save_to_json(self, filename="instance_export.json"):
        export_data = {"config": self.config}

        symbols_to_save = [
            "cycle_time",
            "capacity",
            "load_factor",
            "demand",
            "eo",
        ]

        for sym in symbols_to_save:
            gams_obj = getattr(self, sym)
            if gams_obj.records is not None:
                df = gams_obj.records.copy()

                if isinstance(gams_obj, gp.Set) and "element_text" in df.columns:
                    df = df.drop(columns=["element_text"])

                # Map GAMSPy's infinity to 1e100 for the JSON export
                df = df.replace([float("inf"), np.inf], 1e100)

                export_data[sym] = df.values.tolist()

        with open(filename, "w") as f:
            json.dump(export_data, f, indent=4)
        print(f"Instance saved to {filename}")

    def export_lp(self, filename="unsplittable_qubo.lp"):
        import os

        from gamspy_qubo import Qubo

        if self.mcf is None:
            raise ValueError("Model not built. Call build_model() before exporting.")

        print("\n--- Reformulating MIP to QUBO ---")
        penalty = self.config["PENALTY_CONSTANT"]

        mcf_qubo = Qubo(self.mcf, name="unsplittable_qubo", penalty=penalty)

        print(f"--- Exporting QUBO to LP format: {filename} ---")

        abs_lp_path = os.path.abspath(filename)

        # Export the reformulated model using CPLEX.
        # We expect a ResourceInterrupt Warning from the logger here, since the time limit is set to 0
        logger = logging.getLogger("MODEL")
        logger.disabled = True
        mcf_qubo.solve(
            solver="CPLEX",
            solver_options={
                "writelp": abs_lp_path,
                "ResLim ": 0,
            },
            options=gp.Options(generate_name_dict=False),
        )
        logger.disabled = False


        if os.path.exists(abs_lp_path):
            print(
                f"QUBO successfully exported to {abs_lp_path}. Ready for the external solver!"
            )
        else:
            print(f"Error: LP file was not found at {abs_lp_path}.")

    def solve_classical(self, solver=None, output_html="classical_routing.html"):
        """Solves the standard MIP without QUBO reformulation and plots the solution."""
        from plot_unsplittable import plot_mcf
        import pandas as pd

        if not hasattr(self, "mcf") or self.mcf is None:
            raise ValueError("Model not built. Call build_model() before solving.")

        print("\n--- Solving Classical MIP ---")
        self.mcf.solve(solver=solver)

        print("\n--- Classical Solve Results ---")
        print(f"Model Status: {self.mcf.status}")

        if self.mcf.objective_value is not None:
            print(f"Objective Value: {self.mcf.objective_value}")

            # Extract native solution
            classical_flows = (
                self.Y.records.copy() if self.Y.records is not None else pd.DataFrame()
            )

            print("\n--- Generating Classical Plot ---")
            plot_mcf(
                filename=output_html,
                flow_input=classical_flows,
                demand_records=self.demand.records,
            )
            print(f"Done! Open '{output_html}' in your browser.")
        else:
            print("No solution found.")

    def compare_and_plot(
            self, json_path="solution.json", output_html="unsplittable_comparison.html"
    ):
        """Solves classically, injects the QUBO JSON solution, and plots the comparison."""
        import pandas as pd
        from gamspy_qubo import Qubo

        from plot_unsplittable import plot_comparison

        if not hasattr(self, "mcf") or self.mcf is None:
            raise ValueError("Model not built. Call build_model() before comparing.")

        print("\n--- 1. Solving Classical MIP ---")
        self.mcf.solve(solver="CPLEX")
        print(f"Classical Objective Value: {self.mcf.objective_value}")

        classical_flows = (
            self.Y.records.copy() if self.Y.records is not None else pd.DataFrame()
        )

        print("\n--- 2. Reformulating into QUBO ---")
        penalty = self.config["PENALTY_CONSTANT"]
        mcf_qubo = Qubo(self.mcf, name="unsplittable_qubo", penalty=penalty)

        print("\n--- 3. Injecting JSON Solution ---")
        mcf_qubo.solve(solver="json", json_path=json_path)

        qubo_flows = (
            self.Y.records.copy() if self.Y.records is not None else pd.DataFrame()
        )

        print("\n--- 4. Generating Comparison Plot ---")
        plot_comparison(
            filename=output_html,
            classical_input=classical_flows,
            qubo_input=qubo_flows,
            demand_records=self.demand.records,
        )
        print(f"Done! Open '{output_html}' in your browser.")

    def plot_base_network(self, output_html="base_network.html"):
        """Plots all possible permitted arcs in the generated Time-Expanded Network before solving."""
        from plot_unsplittable import plot_mcf

        if not hasattr(self, "eo") or self.eo.records is None:
            raise ValueError(
                "Network not generated. Initialize the class with data first."
            )

        print("\n--- Generating Base Network Plot ---")
        # self.eo.records contains all allowed arcs [v, t, v, t, o]
        plot_mcf(
            filename=output_html,
            flow_input=self.eo.records,
            demand_records=self.demand.records,
        )
        print(f"Done! Open '{output_html}' in your browser.")

    def plot_json_solution(
            self, json_path="solution.json", output_html="json_only_solution.html"
    ):
        """Loads the QUBO JSON solution and plots only the quantum/external routing."""
        import pandas as pd
        from gamspy_qubo import Qubo

        from plot_unsplittable import plot_mcf

        if not hasattr(self, "mcf") or self.mcf is None:
            raise ValueError("Model not built. Call build_model() before plotting.")

        print("\n--- Reformulating into QUBO ---")
        penalty = self.config["PENALTY_CONSTANT"]
        mcf_qubo = Qubo(self.mcf, name="unsplittable_qubo", penalty=penalty)

        print(f"\n--- Injecting JSON Solution from {json_path} ---")
        mcf_qubo.solve(solver="json", json_path=json_path)

        qubo_flows = (
            self.Y.records.copy() if self.Y.records is not None else pd.DataFrame()
        )

        print("\n--- Generating JSON-Only Plot ---")
        plot_mcf(
            filename=output_html,
            flow_input=qubo_flows,
            demand_records=self.demand.records,
        )
        print(f"Done! Open '{output_html}' in your browser.")
