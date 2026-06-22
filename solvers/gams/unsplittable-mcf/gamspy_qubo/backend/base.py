from __future__ import annotations

import importlib
from abc import ABC, abstractmethod
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    import numpy as np
    import pandas as pd
    from gamspy import Sense


class baseBackend(ABC):
    """Abstract base class that provides a structure to different quantum backends."""

    def __init__(
        self,
        q_matrix: np.ndarray,
        q_variables: pd.Series,
        q_constant: float | int,
        sense: Sense,
    ):
        """
        :param q_matrix: QUBO Matrix
        :type q_matrix: np.ndarray
        :param q_variables: List of variables that are associated with the QUBO
        :type q_variables: list[str]
        :param q_constant: the offset penalty term
        :type q_constant: float | int
        :param sense: direction of optimization, i.e, Min. or Max.
        :type sense: Sense
        """
        self.q_matrix = q_matrix
        self.q_variables = q_variables
        self.q_constant = q_constant
        self.sense = sense

    @abstractmethod
    def solve(self, *args, **kwargs) -> pd.DataFrame:
        """Solve the problem using quantum backend

        Returns:
            A pandas.DataFrame that must have the following structure,
            `columns = ["i", "level"]`, where `i` contains variables and
            `level` is the corresponding value of the variable after solving.
        """
        raise NotImplementedError

    @staticmethod
    def check_dependencies(solver_name: str, packages: dict):
        for pkg_name, import_path in packages.items():
            try:
                importlib.import_module(import_path)
            except ImportError:
                raise ImportError(
                    f"The {solver_name} solver requires the '{pkg_name}' package. "
                    f"Install it with: pip install {pkg_name}"
                ) from None
