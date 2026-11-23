package edu.kit.provideq.formats.sat.converters;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnf;
import edu.kit.provideq.formats.sat.formats.dimacs.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converts a logical expression in CNF form into a DIMACS CNF structure.
 */
public class ExpressionToDimacsCnf {
  private int nextVariable = 1;
  private final HashMap<String, Variable> parsedVariables = new HashMap<>();

  public DimacsCnf parse(Expression<String> cnfExpression) throws ParseException {
    parsedVariables.clear();
    nextVariable = 1;

    var clauses = new ArrayList<List<Variable>>();

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

    var variables = List.copyOf(parsedVariables.values());

    return new DimacsCnf(clauses, variables);
  }

  ArrayList<Variable> parseClause(
      Expression<String> expression) throws ParseException {
    var variables = new ArrayList<Variable>();
    addVariables(variables, expression);

    return variables;
  }

  void addVariables(
      List<Variable> variables,
      Expression<String> e) throws ParseException {
    // Expression can be a (negated) variable or an OR of multiple variables
    // Add parsed variables to list

    switch (e.getExprType()) {
      case Or.EXPR_TYPE -> {
        for (Expression<String> child : e.getChildren()) {
          addVariables(variables, child);
        }
      }
      case Not.EXPR_TYPE -> {
        var name = ((com.bpodgursky.jbool_expressions.Variable<String>)
            e.getChildren().getFirst()).getValue();
        variables.add(getVariable(name, true));
      }
      case com.bpodgursky.jbool_expressions.Variable.EXPR_TYPE -> {
        var name = ((com.bpodgursky.jbool_expressions.Variable<String>) e).getValue();
        variables.add(getVariable(name, false));
      }
      default -> throw new ParseException("Unexpected expression of type " + e.getExprType()
          + " when adding variables " + e);
    }
  }

  Variable getVariable(String name, boolean isNegated) {
    // Try to get variable value from parsed variables
    Variable variable = parsedVariables.get(name);
    if (variable != null) {
      return new Variable(
          variable.number(), name, isNegated);
    }

    // If variable is not parsed yet, create new variable
    variable = new Variable(nextVariable, name, isNegated);
    nextVariable++;

    // Add variable to parsed variables - the negation state doesn't matter here
    parsedVariables.put(name, variable);

    // Return the new variable
    return variable;
  }
}
