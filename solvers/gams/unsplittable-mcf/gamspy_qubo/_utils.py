from __future__ import annotations

import re

import numpy as np
import pandas as pd
from gamspy import SolveStatus
from gamspy.exceptions import GamspyException, ValidationError


def validate_value(value: int, allowed_values: list[int], param_name: str) -> int:
    if value not in allowed_values:
        raise ValueError(f"{param_name} must be one of {allowed_values}")
    return value


def check_row_entries(df: pd.DataFrame) -> pd.DataFrame:
    """
    Helper function to filter DataFrame having either 0 or 1 entries in each row.
    Args:
        df: A Pandas DataFrame.

    Returns:
        Filtered DataFrame with rows having either 0 or 1
    """
    row_contains_only_0s_or_1s = df.isin([0, 1]).all(axis=1)
    return df[row_contains_only_0s_or_1s]


def fetch_quadratic_coeff(raw_df: pd.DataFrame, bin_vars: list) -> np.ndarray:
    """
    Helper function to convert the original Q matrix of the problem to a symmetric matrix

    Args:
        raw_df: Original problem Q data in a pd.DataFrame

    Returns:
        Numpy Q matrix
    """
    raw_df["value"] /= 2
    mask = raw_df["j_1"].astype(str) == raw_df["j_2"].astype(str)
    filtered_quad = raw_df.loc[mask, :].copy()
    raw_df = raw_df.loc[~mask].copy()
    diag_quad = filtered_quad.reset_index(drop=True)
    quad = raw_df.copy(deep=True)
    quad["j_1"], quad["j_2"] = raw_df["j_2"], raw_df["j_1"]
    quad = pd.concat([raw_df, quad, diag_quad], axis=0)
    quad = quad.pivot(index="j_1", columns="j_2", values="value").fillna(0)
    quad = quad.reindex(labels=bin_vars, axis="index")
    quad = quad.reindex(labels=bin_vars, axis="columns")
    return quad.to_numpy()


def var_contribution(
    A: pd.DataFrame, vars: dict, cons: list | None = None
) -> np.ndarray:
    """
    Helper function to calculate the contribution of given variables
    in a constraint or set of constraints

    Args:
        A:      df of coefficients
        vars:   contributing variables
        cons:   participating constraints

    Returns:
        np.ndarray of Total contribution of all variables for that constraint
    """
    cons = slice(None) if cons is None else cons  # type: ignore
    coeffs_of_vars_in_constraint = A.loc[cons, vars.keys()].to_numpy()  # type: ignore
    lb_var_levels = np.array(list(vars.values())).reshape((len(vars), 1))
    if coeffs_of_vars_in_constraint.size > 0:
        return coeffs_of_vars_in_constraint @ lb_var_levels

    return np.array([0])


def modify_matrix(
    b_vec: np.ndarray,
    rhs: float,
    slacks: np.ndarray,
    A_coeff: pd.DataFrame,
    ele: pd.Series,
    nslacks: int,
) -> tuple[np.ndarray, pd.DataFrame, int]:
    """
    Helper function to update the original "A" matrix of coeffs

    Args:
        b_vec: The n*1 vector
        rhs : The Right hand side of a constraint
        slacks: result of gen_slacks()
        A_coeff: "A" matrix
        ele: constraint
        nslacks: number of slacks

    Returns:
        updated b_vec, A_coeff and number of slacks
    """
    con_index = ele.i
    slack_names = [
        f"slack_{con_index}_{i}" for i in range(nslacks + 1, nslacks + len(slacks) + 1)
    ]
    new_cols = pd.DataFrame(
        0, index=A_coeff.index, columns=slack_names, dtype=slacks.dtype
    )
    new_cols.loc[con_index, slack_names] = slacks
    A_coeff = pd.concat([A_coeff, new_cols], axis=1)
    nslacks += len(slacks)

    return np.append(b_vec, [rhs]), A_coeff, nslacks


def gen_slacks(var_range: float) -> np.ndarray:
    """
    Helper function to generate slacks depending on the range of variables or rhs.

    Note: `var_range` cannot be more than 1e4. This hard limit prevents the creation of
        excessive auxiliary binary variables and maintains model performance
        during the binarization of slacks or integer variables.

    Args:
        var_range: upper bound of variable

    Returns:
        Numpy array containing slack coefficients

    example:
        if var_range=5, then gen_slacks(5) returns [1, 2, 2]
    """
    if var_range >= 1e4:
        raise ValidationError(
            "The Upper bound is greater than or equal to 1e+4, Quitting!"
        )

    power = int(np.log2(var_range)) if var_range > 0 else 0
    bounded_coef = var_range - (2**power - 1)
    D_val = [2**i for i in range(power)] + [bounded_coef]
    return np.array(D_val)


def get_lhs_bounds(ele: pd.DataFrame) -> tuple[float, float]:
    """
    Helper function to find the bounds of a constraint

    Args:
        ele: The coefficients of the constraint

    Returns:
        lower_bound, upper_bound
    """
    return float(ele[ele < 0].sum()), float(ele[ele > 0].sum())


def check_convexity(Q: np.ndarray) -> str:
    eigenvalues = np.linalg.eigvals(Q)

    if np.all(eigenvalues > 0):
        return "Function is strictly convex."
    elif np.all(eigenvalues >= 0):
        return "Function is convex."
    else:
        return "Function is not convex."


def qubo_to_ising(Q: dict, offset: float = 0.0) -> tuple[dict, dict, float]:
    h = {}  # type: ignore
    J = {}
    linear_offset = 0.0
    quadratic_offset = 0.0

    for (u, v), bias in Q.items():
        if u == v:
            if u in h:
                h[u] += 0.5 * bias
            else:
                h[u] = 0.5 * bias
            linear_offset += bias
        else:
            if bias != 0.0:
                J[(u, v)] = 0.25 * bias

            if u in h:
                h[u] += 0.25 * bias
            else:
                h[u] = 0.25 * bias

            if v in h:
                h[v] += 0.25 * bias
            else:
                h[v] = 0.25 * bias

            quadratic_offset += bias

    offset += 0.5 * linear_offset + 0.25 * quadratic_offset

    return h, J, offset


def qubo_to_maxcut(Q: np.ndarray) -> np.ndarray:
    return -1 * np.sum(Q, axis=1)


def triu(Q_matrix: np.ndarray):
    upper_mask = np.triu(np.ones_like(Q_matrix), k=1)  # 1 above diagonal, 0 elsewhere
    scaled_upper = Q_matrix * (1 + upper_mask)  # Double above diagonal
    return np.triu(scaled_upper)


def check_classical_solve(solveStatus: SolveStatus | None):
    if solveStatus is None:
        raise GamspyException("Solver status is None. Solve the model first.")

    if solveStatus in [
        SolveStatus.IterationInterrupt,
        SolveStatus.ResourceInterrupt,
        SolveStatus.UserInterrupt,
    ]:
        # Continue mapping incumbent solution if solve_status is one of
        # [UserInterrupt, ResourceInterrupt, IterationInterrupt].
        pass

    elif solveStatus != SolveStatus.NormalCompletion:
        raise GamspyException(
            f"Solver did not yield NormalCompletion. Solver status = {solveStatus}"
        )


def parse_examiner_output(text):
    if "Primal constraints satisfied" in text:
        return {"feasible": True, "message": "Solution is feasible."}

    match = re.search(
        r"Primal infeasible.*?Max violation:\s+(.*?):\s+(.*)", text, re.DOTALL
    )

    if match:
        constraint_name = match.group(1).strip()
        violation_details = match.group(2).split("\n")[0].strip()
        return {
            "feasible": False,
            "message": f"Infeasible: {constraint_name} violates bounds ({violation_details})",
        }

    return {
        "feasible": False,
        "message": "Examiner failed or returned unexpected format.",
    }
