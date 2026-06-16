package edu.kit.provideq.toolbox.circuit.processing.results;

public interface Result {
  <R> R accept(ResultVisitor<R> resultVisitor);
}
