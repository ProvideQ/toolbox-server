package edu.kit.provideq.toolbox.format.cnf.dimacs;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ExpressionToDimacsCnf {
  private int nextVariable = 1;
  private final HashMap<String, Variable> parsedVariables = new HashMap<>();

  public DimacsCnf parse(Expression<String> cnfExpression) {
    parsedVariables.clear();
    nextVariable = 1;

    var clauses = new ArrayList<ArrayList<Variable>>();

    if (cnfExpression.getExprType().equals(And.EXPR_TYPE)) {
      // Parse AND of multiple expressions
      for (Expression<String> child : cnfExpression.getChildren()) {
        // Parse single expression
        clauses.add(parseClause(child));
      }
    } else {
      // Parse single expression
      clauses.add(parseClause(cnfExpression));
    }

    var variables = parsedVariables.values().stream().toList();

    return new DimacsCnf(clauses, variables);
  }

  ArrayList<Variable> parseClause(Expression<String> expression) {
    var variables = new ArrayList<Variable>();
    addVariables(variables, expression);

    return variables;
  }

  void addVariables(List<Variable> variables, Expression<String> e) {
    // Expression can be a (negated) variable or an OR of multiple variables
    // Add parsed variables to list

    switch (e.getExprType()) {
      case Or.EXPR_TYPE -> {
        for (Expression<String> child : e.getChildren()) {
          addVariables(variables, child);
        }
      }
      case Not.EXPR_TYPE -> {
        var name =
            ((com.bpodgursky.jbool_expressions.Variable<String>) e.getChildren().get(0)).getValue();
        variables.add(getVariable(name, true));
      }
      case com.bpodgursky.jbool_expressions.Variable.EXPR_TYPE -> {
        var name = ((com.bpodgursky.jbool_expressions.Variable<String>) e).getValue();
        variables.add(getVariable(name, false));
      }
    }
  }

  Variable getVariable(String name, boolean isNegated) {
    // Try to get variable from parsed variables
    Variable var = parsedVariables.get(name);
    if (var == null) {
      // If variable is not parsed yet, create new variable
      var = new Variable(nextVariable, name, isNegated);
      nextVariable++;

      // Add variable to parsed variables - the negation state doesn't matter here
      parsedVariables.put(name, var);

      // Return the new variable
      return var;
    }

    // Otherwise, return variable with correct negation state
    return new Variable(var.number(), name, isNegated);
  }
}
