import logging as log

import gamspy as gp
import numpy as np
import pandas as pd
from gamspy.exceptions import GamspyException, ValidationError

from gamspy_qubo import _utils
from gamspy_qubo.backend import DwaveBackend, JsonBackend, KipuBackend

LOG_LEVEL_DICT = {0: log.WARN, 1: log.INFO, 2: log.DEBUG}
SUPPORTED_QUANTUM_BACKENDS = [
    "dwave",
    "kipu",
    "json",
]  # append this list when adding new quantum backend


class Qubo(gp.Model):
    """
    QUBO Reformulation for Integer Programs (IPs) modelled in GAMSPy.

    Parameters
    ----------
    model: gp.Model
        GAMSPy generated IP Model.
    name: str | None
        Name for the reformulated model. By default it is "QUBO"
    penalty: int | None
        Set the appropriate penalty to be used to penalize the constraints. By default it is 1
    log_on: int | None
        Enable logging information.
        Options are {0 = WARN, 1 = INFO, 2 = DEBUG}.
        By default it is 0.

    Notes
    -----
    - Currently two quantum solvers, ("dwave", "kipu"), are supported.
    - The desired quantum solver can be set using the `solver` keyword argument in the `model.solve()` call. (default = "cplex")
    - We first use the `CONVERT` solver to generate a standardized version of the original problem.
    - One can also pass keyword arguments to the `CONVERT` solver, for. e.g., `options=gp.Options(hold_fixed_variables=True)`
    """

    def __init__(
        self,
        model: gp.Model,
        /,
        name: str = "QUBO",
        penalty: int = 1,
        log_on: int = 0,
        **kwargs,
    ):
        if not isinstance(model, gp.Model):
            raise ValidationError("Qubo() only accepts a >gamspy.Model< object.")

        self._og_model: gp.Model = model
        self._modelName: str = name
        self._sense: gp.Sense = model.sense
        self.penalty: int = penalty
        _work_dir = model.container.working_directory
        self._container: gp.Container = self._run_convert(workdir=_work_dir, **kwargs)
        self._q_container = gp.Container(working_directory=_work_dir)
        self.Q: np.ndarray = np.array([])
        self.Qconst: float = 0.0
        self._TRANSFORMATION_COMPLETE = False
        self._MAPPING_COMPLETE = False

        if (
            log_level := LOG_LEVEL_DICT.get(
                _utils.validate_value(
                    log_on, allowed_values=[0, 1, 2], param_name="log_on"
                ),
                log.WARN,
            )
        ) < log.WARN:
            log.basicConfig(
                filename=f"{self._og_model.name}_reformulation.log",
                filemode="w",
                format="%(message)s",
                level=log_level,
                force=True,
            )

    def __str__(self) -> str:
        return (
            f"Model {self._modelName}:\n  Problem Type: MIQCP\n  Sense:"
            f" {self._sense}\n  Equations: {self._modelName}_objective"
        )

    def _run_convert(self, workdir, **kwargs) -> gp.Container:
        try:
            self._og_model.solve(
                solver="CONVERT",
                solver_options={
                    "dumpgdx": f"{self._og_model.name}.gdx",
                    "GDXQuadratic": 1,
                    "GDXHessian": 1,
                },
                **kwargs,
            )
        except Exception as e:
            raise GamspyException("Error while running the >CONVERT< operation.") from e

        return gp.Container(
            load_from=f"{self._og_model.name}.gdx",
            working_directory=workdir,
        )

    def _check_transformation(self):
        if not self._TRANSFORMATION_COMPLETE:
            raise ValidationError("Run transform first to generate the Q Matrix.")

    def transform(
        self, penalty: int | None = None
    ) -> tuple[gp.Parameter, gp.Set, gp.Parameter]:
        """
        Method to perform the QUBO reformulation on the provided model.

        Arguments:
            penalty: different penalty can be provided again to generate a new QUBO.

        Returns:
            qd: gp.Parameter: the Q Matrix
            qi: gp.Set: binary variables participating in the QUBO
            qconst: gp.Parameter: the offset calculated based on the penalty provided.
        """
        if penalty is None:
            penalty = self.penalty

        self._fixed_vars_flag = False
        self._lower_bounded_vars_flag = False
        obj_eq_name: pd.DataFrame = self._container["iobj"].records

        if obj_eq_name is None:
            raise ValidationError(
                "The objective is not defined using a scalar equation. `iobj` in gdx is empty. Quitting."
            )

        obj_var: list = self._container["jobj"].records["j"].to_list()
        # fetch the objective variable name
        all_vars: pd.DataFrame = self._container[
            "j"
        ].records  # fetches all variable names
        raw_a: pd.DataFrame = self._container["A"].records  # A coefficients
        eq_data: pd.DataFrame = self._container["e"].records  # fetches equation data

        if (
            raw_a[-raw_a["i"].isin(obj_eq_name["i"].tolist())]["value"]
            .mod(1)
            .sum(axis=0)
            > 0
        ):  # floating point coeffs in objective function is accepted
            raise ValidationError(
                "Reformulation with Non-Integer Coefficients not possible. Quitting."
            )

        raw_a = raw_a.pivot(index="i", columns="j", values="value").fillna(
            0
        )  # arranging in a matrix

        if (
            eq_data["lower"].mod(1).sum(axis=0) > 0
            or eq_data["upper"].mod(1).sum(axis=0) > 0
        ):
            raise ValidationError(
                "Reformulation with Non-Integer RHS not possible. Quitting."
            )

        bin_vars = self._container[
            "jb"
        ].records  # fetches binary variable names, if any
        int_vars = self._container[
            "ji"
        ].records  # fetches integer variable names, if any
        bin_vars = (
            [] if bin_vars is None else bin_vars["j"].to_list()
        )  # check if any bin_vars are present
        int_vars = (
            [] if int_vars is None else int_vars["j"].to_list()
        )  # check if any int_vars are present
        self._int_vars_flag = False if len(int_vars) == 0 else True
        all_var_vals = (
            self._container["x"].records
        )  # get all variable values, viz., [level, marginal, lower, upper, scale]

        if (
            len(all_vars) - len(bin_vars) - len(int_vars) != 1
        ):  # Continuous variables are not allowed
            raise ValidationError("There are continuous variables. Quitting.")

        self._obj_eq_name = obj_eq_name["i"].to_list()

        check_quad = self._container["ANl"].records

        """
        Check if there are any fixed variables in the gdx, i.e., lb=ub=level of any variable.
        If such variables exist, separate them from the list of non-fixed variables and treat them as constants in the objective function.

        We also need to check if the level of variables are set and handle them separately
        """
        lower_mask = (all_var_vals["lower"] > 0) & (
            all_var_vals["lower"] != all_var_vals["upper"]
        )
        fixed_mask = (all_var_vals["level"] == all_var_vals["lower"]) & (
            all_var_vals["level"] == all_var_vals["upper"]
        )

        self._vars_with_lower_bounds = dict(
            all_var_vals[lower_mask][["j", "lower"]].values
        )
        fixed_vars = dict(all_var_vals[fixed_mask][["j", "level"]].values)

        fixed_and_lower_bounds = {**self._vars_with_lower_bounds, **fixed_vars}
        sum_fixed_obj_var_coeffs = 0.0

        if check_quad is not None:
            rawquad = self._container[
                "Q"
            ].records  # fetch quadratic terms from the original problem, if any.

            if self._int_vars_flag:
                raise ValidationError(
                    "Quadratic Program with integer variables are not supported."
                )

            if any(check_quad["j"].isin(fixed_and_lower_bounds.keys())):
                raise ValidationError(
                    "Quadratic terms with non-zero variable levels are not supported at the moment."
                )

        log.debug("Coefficient matrix: raw_a\n" + raw_a.to_string())
        log.debug("\nEquation Data: eq_data\n" + eq_data.to_string())
        log.debug("\nVariable Data: all_var_vals\n" + all_var_vals.to_string())

        if (
            fixed_and_lower_bounds
        ):  # adjust the rhs of equations when level of variables > 0
            log.info(
                f"\nList of variables with lower bounds:\n{self._vars_with_lower_bounds}"
            )
            self._lower_bounded_vars_flag = True
            contribution = _utils.var_contribution(raw_a, fixed_and_lower_bounds)
            eq_data.loc[:, ["lower", "upper"]] -= contribution
            if fixed_vars:
                fixed_set = set(fixed_vars.keys())
                self._fixed_vars_flag = True
                log.info(f"\nList of Fixed Variables:\n{fixed_vars}")
                # remove the fixed variables from computation
                bin_vars = [var for var in bin_vars if var not in fixed_set]
                int_vars = [var for var in int_vars if var not in fixed_set]
                sum_fixed_obj_var_coeffs += np.ndarray.item(
                    _utils.var_contribution(raw_a, fixed_vars, cons=self._obj_eq_name)
                )
                raw_a.drop(
                    labels=list(fixed_vars.keys()), axis=1, inplace=True
                )  # dropping columns from the coefficient matrix
                self._fixed_var_vals: pd.DataFrame = all_var_vals[
                    all_var_vals["j"].isin(fixed_vars)
                ].copy(deep=True)

            log.debug(
                "\nAfter removing fixed variables and adjusting for non-zero levels: raw_a\n"
                + raw_a.to_string()
            )
            log.debug(
                "\nAfter removing fixed variables and adjusting for non-zero levels: eq_data\n"
                + eq_data.to_string()
            )

        """
        Check if there exist a row in coefficient matrix with all zero values. This can happen if all vars in a constraint are fixed.
        Such row is irrelevant for QUBO and can be dropped out of the matrix and set of constraints.
        """
        redundant_cons = list(raw_a[raw_a.apply(abs).sum(axis=1) == 0].index)
        if len(redundant_cons) > 0:
            log.info(f"\nDropping these redundant constraint: \n{redundant_cons}")
            raw_a.drop(redundant_cons, axis=0, inplace=True)
            eq_data.drop(
                eq_data[eq_data["i"].isin(redundant_cons)].index, axis=0, inplace=True
            )

        """
        If integer variables exist, convert all integers to binary with '@' as a delimiter of variable names
        If Integer variables with lower bound exist, i.e., lb >=1 and lb!=ub, then convert binary variable for that range
        If these variables contribute to the objective function, their lower bounds are added as a constant
        """
        sum_lower_bound_of_int_vars = 0
        if self._int_vars_flag:
            if self._vars_with_lower_bounds:
                sum_lower_bound_of_int_vars += np.ndarray.item(
                    _utils.var_contribution(
                        raw_a, self._vars_with_lower_bounds, self._obj_eq_name
                    )
                )

            int_var_vals = all_var_vals[all_var_vals["j"].isin(int_vars)]
            int_to_bin_bounds = {
                row["j"]: _utils.gen_slacks(row["upper"] - row["lower"])
                for _, row in int_var_vals.iterrows()
            }  # generate coeffs for converted binary vars
            int_bin_vals = pd.DataFrame(columns=["intName", "binName", "value"])
            for var, bin_bounds in int_to_bin_bounds.items():
                for i in range(len(bin_bounds)):  # naming the converted binary vars
                    new_row = pd.DataFrame(
                        {
                            "intName": var,
                            "binName": f"{var}@_bin{i}",
                            "value": bin_bounds[i],
                        },
                        index=[0],
                    )
                    int_bin_vals = (
                        pd.concat([int_bin_vals, new_row], ignore_index=True)
                        if not int_bin_vals.empty
                        else new_row.copy()
                    )

            self._binName_list = int_bin_vals[
                "binName"
            ].to_list()  # list of all converted binary variable names
            log.info(
                "\nInteger to Binary Mapping: int_bin_vals\n" + int_bin_vals.to_string()
            )
            int_bin_vals = int_bin_vals.pivot(
                index="intName", columns="binName", values="value"
            ).fillna(0)  # mapping each binary var to its integer var component
            int_bin_vals = int_bin_vals.reindex(labels=int_vars, axis="index")
            int_bin_vals = int_bin_vals.reindex(
                labels=self._binName_list, axis="columns"
            )
            self._int_bin_vals = int_bin_vals

            raw_a_int = raw_a[int_vars]
            raw_a_int = raw_a_int.dot(
                int_bin_vals
            )  # updating the "A" coeff matrix with the new coeffs for converted binary vars
            raw_a_rest = raw_a[obj_var + bin_vars]
            raw_a = pd.concat(
                [raw_a_rest, raw_a_int], axis="columns"
            )  # new "A" coeff matrix
            log.info("\nInteger to Binary Mapping: raw_a\n" + raw_a.to_string())
            bin_vars += self._binName_list  # append the list of original binary variables with the list of converted binary variables

        cons = eq_data[-eq_data["i"].isin(self._obj_eq_name)].reset_index(
            drop=True
        )  # fetch only the constraints and not the objective equation
        nvars = len(bin_vars)
        nslacks = 0
        self._obj_var_direction = raw_a[obj_var].loc[self._obj_eq_name].to_numpy()
        obj_var_coeff = raw_a[bin_vars].loc[self._obj_eq_name].to_numpy()
        if self._obj_var_direction > 0:
            obj_var_coeff = -1 * obj_var_coeff
        obj = np.zeros((nvars, nvars))
        np.fill_diagonal(obj, obj_var_coeff)

        """
        Pre-processing in case special constraints exist
        Remove the constraint from the "A" matrix and include the special penalty directly in the objective
        Doing so, reduces the number of slack variables used in the final reformulation
        special penalty case 1: sum(x_i | 1 <= i <= n) <= 1 => P*sum(x_i*x_j | i < j)
        special penalty case 2: x_i  + x_j >= 1 => P*(1 - x_i - x_j + x_i*x_j)
        """

        # Case 1 implementation
        special_cons_case_1_label = [
            ele.i for _, ele in cons.iterrows() if ele.upper == 1 and ele.lower != 1
        ]
        if special_cons_case_1_label:
            case1_cons = raw_a[bin_vars].loc[special_cons_case_1_label]
            case1_cons = _utils.check_row_entries(case1_cons.copy())
            case1_cons_index_label = list(case1_cons.index)
            case1_penalty = case1_cons.to_numpy()
            if case1_penalty.size > 0:
                case1_penalty = (case1_penalty.T @ case1_penalty) / 2
                np.fill_diagonal(case1_penalty, np.zeros((1, len(bin_vars))))
            else:  # if there are no rows with only 0/1 entries
                case1_penalty = np.zeros((nvars, nvars))
            log.debug(f"\nSpecial constraint case 1:\n{special_cons_case_1_label}")
        else:
            case1_cons_index_label = []
            case1_penalty = np.zeros((nvars, nvars))

        # Case 2 implementation
        special_cons_case_2_label = [
            ele.i for _, ele in cons.iterrows() if ele.lower == 1 and ele.upper != 1
        ]
        if special_cons_case_2_label:
            case2_cons = raw_a[bin_vars].loc[special_cons_case_2_label]
            case2_cons = _utils.check_row_entries(case2_cons.copy())
            case2_cons = case2_cons[case2_cons.sum(axis=1) == 2]
            case2_cons_index_label = list(case2_cons.index)
            case2_penalty = case2_cons.to_numpy()
            if case2_penalty.size > 0:
                case2_penalty = (case2_penalty.T @ case2_penalty) / 2
                case2_diag = np.diag_indices_from(case2_penalty)
                case2_penalty[case2_diag] *= -2
            else:  # if there are no rows with two 1s in them
                case2_penalty = np.zeros((nvars, nvars))
            log.debug(f"\nSpecial constraint case 2:\n{special_cons_case_2_label}")

        else:
            case2_cons_index_label = []
            case2_penalty = np.zeros((nvars, nvars))

        final_special_cons = case1_cons_index_label + case2_cons_index_label
        final_special_penalty = case1_penalty + case2_penalty
        case2_penalty_offset_factor = len(case2_cons_index_label)

        is_max = True if self._sense == gp.Sense.MAX else False
        P = -1 * penalty if is_max else penalty  # penalty term for classic solvers
        obj += P * final_special_penalty

        cons.drop(cons[cons["i"].isin(final_special_cons)].index, axis=0, inplace=True)
        raw_a.drop(final_special_cons, axis=0, inplace=True)

        A_coeff = raw_a.loc[cons["i"], bin_vars]

        self._quad_val = np.array([])
        if (
            check_quad is not None
        ):  # check if quadratic terms are present in the original problem
            log.debug("\nRaw Q data from GDX: Q\n" + rawquad.to_string())
            rawquad_obj = rawquad[rawquad["i_0"].isin(self._obj_eq_name)].copy(
                deep=True
            )
            if (
                len(rawquad_obj.index) != 0
            ):  # check if quadratic terms exist in the objective function
                rawquad_obj.drop(["i_0"], axis=1, inplace=True)
                self._quad_val = _utils.fetch_quadratic_coeff(
                    raw_df=rawquad_obj, bin_vars=bin_vars
                )
                sum_fixed_obj_var_coeffs /= 2

            rawquad_cons = rawquad[
                -rawquad["i_0"].isin(self._obj_eq_name)
            ]  # non-linear constraints without objective equation
            if len(rawquad_cons.index) != 0:  # non-linear constraints exists
                raise ValidationError("There are non-linear constraints. Quitting.")
                ### Removed the support for quadratic constraints.

        if (
            self._quad_val.size
        ):  # add the old quadratic terms/matrix to the new objective
            log.debug(
                "\nUpdate Objective by adding Q: \n" + np.array2string(self._quad_val)
            )
            obj += (
                -1 * self._quad_val if self._obj_var_direction > 0 else self._quad_val
            )
            log.debug("\nNew Q: \n" + np.array2string(obj))

        b_vec = np.array([])
        log.info("\nFinal Cons: \n" + cons.to_string())
        for _, ele in cons.iterrows():
            if ele.upper == ele.lower:  # equal-to type constraint
                rhs = ele.lower
                lhs_min_lb, lhs_max_ub = _utils.get_lhs_bounds(A_coeff.loc[ele.i])
                if (rhs - lhs_min_lb) < 0 or (lhs_max_ub - rhs) < 0:
                    raise ValidationError(f"Constraint is infeasible: {ele.i}")
                else:
                    b_vec = np.append(b_vec, [rhs])
                    slacks: (
                        list | np.ndarray
                    ) = []  # do not introduce slacks for equality type constraints

            elif ele.upper == np.inf:  # greater than type constraint
                rhs = ele.lower
                _, lhs_max_ub = _utils.get_lhs_bounds(A_coeff.loc[ele.i])
                slacks_range = lhs_max_ub - rhs
                if slacks_range > 0:
                    slacks = -1 * _utils.gen_slacks(slacks_range)
                    b_vec, A_coeff, nslacks = _utils.modify_matrix(
                        b_vec, rhs, slacks, A_coeff, ele, nslacks
                    )
                elif slacks_range == 0:
                    b_vec = np.append(b_vec, [rhs])
                    slacks = []
                else:
                    raise ValidationError(f"Constraint is infeasible: {ele.i}")

            else:  # less-than type constraint
                rhs = ele.upper
                lhs_min_lb, _ = _utils.get_lhs_bounds(A_coeff.loc[ele.i])
                slacks_range = rhs - lhs_min_lb
                if slacks_range > 0:
                    slacks = _utils.gen_slacks(slacks_range)
                    b_vec, A_coeff, nslacks = _utils.modify_matrix(
                        b_vec, rhs, slacks, A_coeff, ele, nslacks
                    )
                elif slacks_range == 0:
                    b_vec = np.append(b_vec, [rhs])
                    slacks = []
                else:
                    raise ValidationError(f"Constraint is infeasible: {ele.i}")

        logging_a_mat = A_coeff.unstack().reset_index()
        logging_a_mat = logging_a_mat[logging_a_mat[0] != 0]
        log_data = {
            "final_coefficient_matrix": f"\n{logging_a_mat.to_string()}",
            "final_rhs": b_vec,
            "constant_rhs_term": b_vec.T @ b_vec,
            "case2_penalty_offset_factor": case2_penalty_offset_factor,
            "fixed_variable_contribution": sum_fixed_obj_var_coeffs,
            "integer_lower_bound_contribution": sum_lower_bound_of_int_vars,
        }
        # Single structured log entry
        log.debug(
            "Final optimization summary:\n"
            + "\n".join(
                f"{k.replace('_', ' ').title()}: {v}" for k, v in log_data.items()
            )
        )
        # A matrix and b_vec are available. Now, for penalization: $(A.X - B)^{2}$  = $(A.X - B)^{T} * (A.X - B)$
        a_mat = A_coeff.to_numpy()
        nvars += nslacks  # increment the total number of variables by total number of slack variable used
        X_diag = np.zeros((nvars, nvars))
        np.fill_diagonal(X_diag, -b_vec.T @ a_mat)
        new_x = a_mat.T @ a_mat + 2 * X_diag

        newobj = np.zeros((nvars, nvars))
        newobj[: len(bin_vars), : len(bin_vars)] = (
            obj  # define the new objective: Q for the qubo
        )

        self.Qconst = (
            P * b_vec.T @ b_vec
            + P * case2_penalty_offset_factor
            + sum_fixed_obj_var_coeffs
            + sum_lower_bound_of_int_vars
        )
        log.debug(f"\nPenalty: {P} | Total Offset: {self.Qconst}\n")
        self.Q = newobj + P * new_x
        Qdf_df: pd.DataFrame | pd.Series = pd.DataFrame(
            self.Q, columns=list(A_coeff.columns), index=list(A_coeff.columns)
        )
        Qdf_df = Qdf_df.unstack()
        Qdf_df = Qdf_df.reset_index()
        Qdf = list(Qdf_df.itertuples(index=False, name=None))

        qconst = gp.Parameter(
            self._q_container,
            "qconst",
            None,
            self.Qconst,
            description="Constant term to offset the objective value to original",
        )
        if Qdf:
            qi = gp.Set(
                self._q_container,
                "qi",
                records=A_coeff.columns,
                description="QUBO variables",
            )
            qd = gp.Parameter(
                self._q_container,
                "qd",
                [qi, qi],
                records=Qdf,
                description="Q matrix",
            )
        else:
            qi = gp.Set(
                self._q_container,
                "qi",
                records=["all_variables_fixed"],
                description="QUBO variables",
            )
            qd = gp.Parameter(
                self._q_container,
                "qd",
                [qi, qi],
                records=[("all_variables_fixed", "all_variables_fixed", 0)],
                description="Q matrix",
            )

        self._TRANSFORMATION_COMPLETE = True
        return qd, qi, qconst

    def write_gdx(self, gdxName: str | None = None):
        if gdxName:
            self._q_container.write(gdxName)
        else:
            self._q_container.write(f"qout_{self._modelName}.gdx")

    def write_qubowl(self) -> None:
        """
        Convenient method to produce QUBO in QUBOWL ready file format (.qs)

        Returns:
                A QMat_<modelName>.qs file accepted by QUBOWL.
        """
        self._check_transformation()

        non_zero_indices = np.tril_indices_from(self.Q)
        non_zero_values = self.Q[non_zero_indices]
        with open(f"QMat_{self._modelName}.qs", "w") as fp:
            fp.write(f"{self.Q.shape[0]} {len(non_zero_values)} {self.Qconst}\n")
            for i, j, value in zip(*non_zero_indices, non_zero_values, strict=True):
                if value != 0:
                    fp.write(f"{i + 1} {j + 1} {value}\n")

    def _model(self) -> None:
        try:
            qd, qi, qconst = self._q_container.getSymbols(["qd", "qi", "qconst"])
        except Exception as e:
            raise GamspyException(
                "Something went from while fetching the q symbols."
            ) from e

        i = gp.Alias(self._q_container, name="i", alias_with=qi)

        j = gp.Alias(self._q_container, name="j", alias_with=qi)

        x = gp.Variable(
            self._q_container,
            name="x",
            type="binary",
            domain=i,
            description="QUBO variables",
        )

        qubo_obj = gp.Sum(gp.Domain(i, j), x[i] * qd[i, j] * x[j]) + qconst
        super().__init__(
            container=self._q_container,
            name=self._modelName,
            problem="MIQCP",
            sense=self._sense,
            objective=qubo_obj,
        )

    def solve(self, *args, **kwargs) -> pd.DataFrame | None:
        kwargs.setdefault("solver", "cplex")
        self._backend = kwargs.get("solver")
        optimized_variable_values = None
        orig_obj_var = getattr(self._og_model, "_objective_variable", None)
        if orig_obj_var is not None:
            original_obj_sym = orig_obj_var.name
        else:
            original_obj_sym = f"{self._og_model.name}_objective_variable"

        if not self._TRANSFORMATION_COMPLETE:
            self.transform()

        if f"{self._modelName}_objective" not in self._q_container.data:
            self._model()

        if self._backend not in SUPPORTED_QUANTUM_BACKENDS:
            try:
                solved = super().solve(*args, **kwargs)
                _utils.check_classical_solve(solveStatus=super().solve_status)
            except Exception as e:
                raise GamspyException(
                    f"Something went wrong while solving using >{self._backend}< backend."
                ) from e
            optimized_variable_values = self._q_container["x"].records
        elif self._backend in SUPPORTED_QUANTUM_BACKENDS:
            q_vars: pd.Series = self._q_container["qi"].records["uni"]
            _initialize = {
                "dwave": DwaveBackend,
                "kipu": KipuBackend,
                "json": JsonBackend,
            }
            _backend: DwaveBackend | KipuBackend | JsonBackend = _initialize[
                self._backend
            ](
                q_matrix=self.Q,
                q_variables=q_vars,
                q_constant=self.Qconst,
                sense=self._sense,
            )  # type: ignore
            try:
                optimized_variable_values = _backend.solve(*args, **kwargs)
                assert isinstance(optimized_variable_values, pd.DataFrame), (
                    "backend.solve() must return a `pd.DataFrame`. "
                    "Refer to the return structue of `baseBackend.solve` for more details."
                )
                optimized_variable_values["i"] = pd.Categorical(
                    optimized_variable_values["i"],
                    categories=q_vars.cat.categories,
                    ordered=True,
                )  # NOTE: some solvers can shuffle the order of UELs, and
                # it is critical for them to be in order for mapping them back
                cols = ["marginal", "lower", "upper", "scale"]
                optimized_variable_values = optimized_variable_values.reindex(
                    columns=optimized_variable_values.columns.tolist() + cols
                )
            except Exception as e:
                raise GamspyException(
                    f"Something went wrong while solving using >{self._backend}< backend."
                ) from e
        else:
            raise GamspyException(f"Backend {self._backend} not supported.")

        solution = {
            "optimized_vals": optimized_variable_values.sort_values("i"),
            "obj_fn_sym": original_obj_sym,
        }
        self._map_solution(solution)

        return solved if self._backend not in SUPPORTED_QUANTUM_BACKENDS else None

    def _map_solution(self, solution) -> None:
        """
        This function maps the QUBO solution to the original Problem
        """
        all_vars = self._container["j"].records
        optimized_vals = solution["optimized_vals"]
        original_obj_sym = solution["obj_fn_sym"]

        if self._fixed_vars_flag:
            self._fixed_var_vals.rename({"j": "i"}, axis=1, inplace=True)
            optimized_vals = pd.concat(
                [optimized_vals, self._fixed_var_vals], ignore_index=True
            )

        if self._int_vars_flag:
            int_bin_map = self._int_bin_vals.unstack()
            int_bin_map = int_bin_map[int_bin_map != 0].reset_index()  # type: ignore

            opt_lookup = optimized_vals.set_index("i")["level"]
            int_bin_map["final_level"] = int_bin_map[0] * int_bin_map["binName"].map(
                opt_lookup
            )

            int_summary = int_bin_map.groupby("intName")["final_level"].sum()

            original_int_vals = self._container["x"].records
            original_int_vals = original_int_vals[
                original_int_vals["j"].isin(int_summary.index)
            ].copy()

            original_int_vals["level"] = original_int_vals["j"].map(int_summary)
            original_int_vals.rename(columns={"j": "i"}, inplace=True)

            cols = ["i", "level", "marginal", "lower", "upper", "scale"]
            original_int_vals = original_int_vals[cols]
            optimized_vals = optimized_vals[
                ~optimized_vals["i"].isin(self._binName_list)
            ]
            optimized_vals = pd.concat(
                [optimized_vals, original_int_vals], ignore_index=True
            )

            if self._vars_with_lower_bounds:
                optimized_vals.loc[
                    optimized_vals["i"].isin(self._vars_with_lower_bounds.keys()),
                    "level",
                ] += list(self._vars_with_lower_bounds.values())

        pattern = r"(?P<var_sym>.+)\((?P<domain>.*?)\)"
        extracted = all_vars["element_text"].str.extract(pattern)
        extracted["domain"] = extracted["domain"].str.replace(r"\'", "", regex=True)
        extracted["uni"] = all_vars["uni"]

        merged_vals = optimized_vals.merge(
            extracted, left_on="i", right_on="uni", how="inner"
        )

        for var_name, group in merged_vals.groupby("var_sym"):
            target_symbol = self._og_model.container[var_name]
            domain_names = [
                dom if isinstance(dom, str) else dom.name
                for dom in target_symbol.domain
            ]

            split_labels = group["domain"].str.split(",", expand=True)
            split_labels.columns = domain_names

            attrs = ["level", "marginal", "lower", "upper", "scale"]
            new_records = pd.concat(
                [split_labels, group[attrs]],
                axis=1,
            )
            dtype_map = {
                **{c: "category" for c in domain_names},
                **{c: "float" for c in attrs},
            }
            new_records = new_records.astype(dtype_map)
            target_symbol.records = new_records.reset_index(drop=True)

        """
        The code below is required to calculate the contribution of variables towards the objective using the new levels obtained from the QUBO solve.
        Since the QUBO solve returns a different level for the objective variable when the optimal solution is not returned, for example, it includes the penalty for every constraint not satisfied.
        """

        obj_var = self._container["jobj"].records["j"].values[0]
        rem_syms = all_vars[all_vars["uni"] != obj_var]["uni"].to_list()
        mask = optimized_vals["i"].isin(rem_syms)
        filtered_vals = optimized_vals[mask]

        x_l = filtered_vals["level"].to_numpy()
        orig_syms_w_new_levels = filtered_vals.set_index("i")["level"].to_dict()

        # at the moment `quad` only contains contribution from the objective row.
        quad_contribution = x_l.T @ self._quad_val @ x_l if self._quad_val.size else 0
        jacobian: pd.DataFrame = self._container["A"].records  # A coefficients
        jacobian = jacobian.pivot(index="i", columns="j", values="value").fillna(
            0
        )  # arranging in a matrix
        linear_contribution = _utils.var_contribution(
            jacobian, orig_syms_w_new_levels, cons=self._obj_eq_name
        ).flatten()[0]

        total_objective_contribution = linear_contribution + quad_contribution
        total_objective_contribution = (
            -1 * total_objective_contribution
            if self._obj_var_direction > 0
            else total_objective_contribution
        )

        obj_var_coeff: gp.Variable = self._q_container[
            f"{self._modelName}_objective_variable"
        ]
        obj_var_coeff.l = total_objective_contribution
        super().__setattr__(
            "_objective_value", total_objective_contribution
        )  # sets the value for the attribute of QUBO model instance
        self._og_model.container[
            original_obj_sym
        ].records = obj_var_coeff.records  # sets the value for original model instance
        self._MAPPING_COMPLETE = True

    @property
    def qubo(self):
        return self.Q

    @staticmethod
    def check_convexity(Q: np.ndarray):
        """
        Convenience method to check the convexity of the QUBO using eigenvalues
        """
        return _utils.check_convexity(Q)

    @staticmethod
    def qubo_to_ising(Q: dict, offset: float = 0.0):
        """
        This is the Qubo to Ising Reformulation. Here, the variable X in {-1,1}

        Args:
                Q: in a form of dict, {(i,j): val}
                offset: offset from the Qubo reformulation

        Returns:
                h: the bias vector \
                J: the coupling matrix\
                offset: adjusted offset for the Ising model
        """
        return _utils.qubo_to_ising(Q, offset)

    @staticmethod
    def qubo_to_maxcut(Q: np.ndarray):
        """
        This is the Qubo to Maxcut Reformulation. This can be used for SDP procedures.

        Args:
            Q: a n x n symmetric numpy matrix

        Returns:
            n x 1 vector associated with the extra variable required in max cut transformation
        """
        return _utils.qubo_to_maxcut(Q)

    @staticmethod
    def triu(Q: np.ndarray) -> np.ndarray:
        """
        Helper function to fetch the upper triangular matrix of a symmetric Q matrix

        Returns:
            An upper triangular matrix
        """
        return _utils.triu(Q)

    def check_feasibility(self):
        import io

        stream = io.StringIO()
        self._og_model.solve(
            solver="examiner",
            solver_options={"examineInitPoint": 1, "objvarAutoAdjust": 1},
            output=stream,
        )
        result = _utils.parse_examiner_output(text=stream.getvalue())

        if not self._MAPPING_COMPLETE:
            result["WARNING"] = (
                "Model must be solved before checking feasibility of the QUBO solution."
            )

        return result
