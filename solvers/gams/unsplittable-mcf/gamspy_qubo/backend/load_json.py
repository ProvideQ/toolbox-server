import json

import pandas as pd

from gamspy_qubo.backend import baseBackend


class JsonBackend(baseBackend):
    """
    A custom backend that reads a pre-solved QUBO solution from a JSON file.
    """

    def __init__(self, q_matrix, q_variables, q_constant, sense):
        super().__init__(q_matrix, q_variables, q_constant, sense)

    def solve(self, json_path="solution.json", *args, **kwargs) -> pd.DataFrame:
        with open(json_path, "r") as f:
            sol_dict = json.load(f)

        results = []
        # q_variables holds the QUBO binary indices (e.g., 'b1', 'b2')
        for var in self.q_variables:
            # Match the variable name to the JSON keys, defaulting to 0.0 if not found
            json_key = var.replace("b", "x", 1)
            val = sol_dict.get(json_key, 0.0)
            results.append({"i": var, "level": float(val)})

        df = pd.DataFrame(results)

        # Provide safe default bounds for the binary variables
        df["marginal"] = 0.0
        df["lower"] = 0.0
        df["upper"] = 1.0
        df["scale"] = 1.0

        return df
