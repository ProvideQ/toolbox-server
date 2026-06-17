package edu.kit.provideq.toolbox.circuit.processing.results;

public class ExecutionResultVisitor implements ResultVisitor<String> {
  @Override
  public String visit(StringResult stringResult) {
    return stringResult.value();
  }

  @Override
  public String visit(ExecutionResult executionResult) {
    return executionResult.getHighestResult().orElse("");
  }
}
