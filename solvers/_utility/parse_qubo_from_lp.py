def parse_qubo_from_lp(model) -> tuple[list[str], dict[tuple[int, int], float], dict[int, float], float]:
    vars = [x.VarName for x in model.getVars() if x.VType == "B"]

    obj = model.getObjective()

    quadratic_terms = {
        (vars.index(term[1].VarName), vars.index(term[2].VarName)): term[0]
        for term in obj.quadTerms()
    }

    linear_terms = {
        vars.index(var.VarName): coeff
        for coeff, var in obj.linTerms()
        if var.VType == "B"
    }

    # Detect constant terms from non-binary variables
    # A variable is considered a constant if:
    # 1. Its LB equals its UB (fixed variable), OR
    # 2. Its name can be parsed as an integer
    constant_offset = obj.getLinExpr().getConstant()

    # Extract constant contributions
    constant_contributions = [
        coeff * lb if lb == ub else (
            coeff * int(var.VarName) if var.VarName.isdigit() else 0
        )
        for coeff, var in obj.getLinExpr().linTerms()
        for lb, ub in [(var.getAttr('LB'), var.getAttr('UB'))]
    ]

    constant_offset += sum(constant_contributions)

    return vars, quadratic_terms, linear_terms, constant_offset
