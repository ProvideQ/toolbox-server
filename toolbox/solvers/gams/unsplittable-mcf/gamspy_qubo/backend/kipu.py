from __future__ import annotations

import importlib

import pandas as pd

from gamspy_qubo._utils import triu
from gamspy_qubo.backend import baseBackend


class KipuBackend(baseBackend):
    """
    Kipu Backend based on `Miray Advanced` (simulator) quantum optimizer.
    """

    def __init__(self, q_matrix, q_variables, q_constant, sense):
        super().__init__(q_matrix, q_variables, q_constant, sense)
        self.check_dependencies("KIPU", {"planqk-service-sdk": "planqk.service.client"})

    def _miray(self):
        pass

    def _illay(self):
        pass

    def solve(self, *args, **kwargs) -> pd.DataFrame:
        """
        Solve the model using Kipu's `Miray` quantum optimizer and map the solution back
        to original problem.

        ### Authorization
        To authorize the client, you must provide the following as keyword arguments
        (retrieved from your application credentials):
        * `access_key_id`: Your unique application access key.
        * `secret_access_key`: Your private application secret key.

        ### Configuration (kwargs)
        Users can provide additional parameters to tune the solver request.
        Supported arguments include:
        * `shots` (int): The number of samples to collect. Default is 1000.
        * `num_iterations` (int): Number of optimization iterations. Default is 3.
        * `num_greedy_passes` (int): Number of greedy refinement passes. Default is 0.

        Note: Follows the general return structure defined in `baseBackend.solve`
        """
        assert self.q_matrix.shape[0] <= 20, (
            f"Miray Optimizer only supports 20 qubits. This QUBO has {self.q_matrix.shape[0]} variables."
        )

        print("\n--- Starting Kipu Optimizer ---")
        planq_service_client = importlib.import_module("planqk.service.client")
        client = planq_service_client.PlanqkServiceClient(
            service_endpoint="https://gateway.hub.kipu-quantum.com/kipu-quantum/miray-advanced-quantum-optimizer---simulator/1.0.0",
            access_key_id=kwargs.get("access_key_id"),
            secret_access_key=kwargs.get("secret_access_key"),
            token_endpoint="https://gateway.hub.kipu-quantum.com/token",
        )

        multiplier = -1 if self.sense.value == "MAX" else 1
        ut_mat = triu(self.q_matrix)
        ut_mat *= multiplier
        problem = {}
        rows, cols = ut_mat.nonzero()
        for i, j in zip(rows, cols, strict=True):
            if i == j:
                problem[f"({i},)"] = ut_mat[i, j]  # diagonal terms
            else:
                problem[f"({i}, {j})"] = ut_mat[i, j]  # off-diagonal terms
        problem["()"] = self.q_constant  # constant

        request_defaults = {"shots": 1000, "num_iterations": 3, "num_greedy_passes": 0}
        filtered_kwargs = {
            key: kwargs.get(key, default_value)
            for key, default_value in request_defaults.items()
        }
        request = {
            "problem": problem,
            "problem_type": "binary",
            **filtered_kwargs,
        }
        service_exec = client.run(request)  # send job
        print(f"JOB ID: {service_exec.id} | Waiting...")

        result = service_exec.result()
        response = result.result
        _sol = {
            self.q_variables[int(k)]: v for k, v in response["mapped_solution"].items()
        }
        sol = pd.DataFrame(
            _sol.items(),
            columns=["i", "level"],
        )

        return sol
