package edu.kit.provideq.toolbox.format.cnf.dimacs;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ExpressionToDimacsCNF {
    private int nextVariable = 1;
    private final HashMap<String, Variable> parsedVariables = new HashMap<>();

    public DimacsCNF parse(Expression<String> cnfExpression) {
        parsedVariables.clear();
        nextVariable = 1;

        var clauses = new ArrayList<ArrayList<Variable>>();

        if (cnfExpression.getExprType().equals(And.EXPR_TYPE)) {
            for (Expression<String> child : cnfExpression.getChildren()) {
                clauses.add(parseClause(child));
            }
        } else {
            clauses.add(parseClause(cnfExpression));
        }

        var variables = parsedVariables.values().stream().toList();

        return new DimacsCNF(clauses, variables);
    }

    ArrayList<Variable> parseClause(Expression<String> expression) {
        var variables = new ArrayList<Variable>();
        addVariables(variables, expression);

        return variables;
    }

    void addVariables(List<Variable> variables, Expression<String> e) {
        switch (e.getExprType()) {
            case Or.EXPR_TYPE -> {
                for (Expression<String> child : e.getChildren()) {
                    addVariables(variables, child);
                }
            }
            case Not.EXPR_TYPE -> {
                var name = ((com.bpodgursky.jbool_expressions.Variable<String>) e.getChildren().get(0)).getValue();
                variables.add(getNegatedVariable(name));
            }
            case com.bpodgursky.jbool_expressions.Variable.EXPR_TYPE -> {
                var name = ((com.bpodgursky.jbool_expressions.Variable<String>) e).getValue();
                variables.add(getVariable(name));
            }
        }
    }

    Variable getNegatedVariable(String name) {
        Variable var = parsedVariables.get(name);
        if (var == null) {
            var = new Variable(Math.abs(nextVariable), name, true);
            nextVariable++;

            parsedVariables.put(name, var);
        }

        return var;
    }

    Variable getVariable(String name) {
        Variable var = parsedVariables.get(name);
        if (var == null) {
            var = new Variable(nextVariable, name, false);
            nextVariable++;

            parsedVariables.put(name, var);
        }

        return var;
    }
}
