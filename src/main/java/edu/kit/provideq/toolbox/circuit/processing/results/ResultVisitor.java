package edu.kit.provideq.toolbox.circuit.processing.results;

public interface ResultVisitor<R> {
  R visit(StringResult stringResult);

  R visit(ExecutionResult executionResult);
}
