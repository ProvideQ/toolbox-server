from __future__ import annotations

import importlib

import pandas as pd

from gamspy_qubo._utils import triu
from gamspy_qubo.backend import baseBackend


class DwaveBackend(baseBackend):
    """
    DWave Backend based on `dimod.BinaryQuadraticModel` and `SimulatedAnnealingSampler`.
    """

    def __init__(self, q_matrix, q_variables, q_constant, sense):
        super().__init__(q_matrix, q_variables, q_constant, sense)
        self.check_dependencies(
            "DWAVE", {"dwave-ocean-sdk": "dwave.samplers", "dimod": "dimod"}
        )

    def solve(self, *args, **kwargs) -> pd.DataFrame:
        """
        Solve the model using Dwave `SimulatedAnnealingSampler` and map the solution back
        to original problem.

        Users can provide keyword arguments as expected by `SimulatedAnnealingSample.sample()`

        Note: Follows the general return structure defined in `baseBackend.solve`
        """
        print("\n--- Starting D-Wave (Ocean) Solve ---")
        multiplier = -1 if self.sense.value == "MAX" else 1
        ut_mat = triu(self.q_matrix)
        ut_mat *= multiplier
        matrix_dict = {}
        rows, cols = ut_mat.nonzero()
        for i, j in zip(rows, cols, strict=True):
            matrix_dict[(self.q_variables[i], self.q_variables[j])] = ut_mat[i, j]

        dimod = importlib.import_module("dimod")
        dwave_sampler = importlib.import_module("dwave.samplers")
        bqm = dimod.BinaryQuadraticModel.from_qubo(
            matrix_dict, offset=float(self.q_constant)
        )
        sampler = dwave_sampler.SimulatedAnnealingSampler()
        kwargs.setdefault("num_reads", 100)
        response = sampler.sample(bqm, **kwargs)
        best_sample = response.first.sample
        sol = pd.DataFrame(best_sample.items(), columns=["i", "level"])

        return sol
