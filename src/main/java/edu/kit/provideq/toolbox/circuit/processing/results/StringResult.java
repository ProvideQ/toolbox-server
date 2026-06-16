package edu.kit.provideq.toolbox.circuit.processing.results;

public record StringResult(
    String value
) implements Result {
  @Override
  public <R> R accept(ResultVisitor<R> resultVisitor) {
    return resultVisitor.visit(this);
  }
}
