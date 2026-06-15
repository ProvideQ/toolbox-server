package edu.kit.provideq.toolbox.circuit.processing.solvers.executor;

import java.util.Optional;

public record ExecutionResult(Optional<String> resultString, Optional<String> circuit) {
  public boolean hasResult() {
    return resultString.isPresent();
  }

  public boolean hasCircuit() {
    return circuit.isPresent();
  }

  @Override
  public String toString() {
    var stringBuilder = new StringBuilder();
    resultString.ifPresent(r -> stringBuilder.append("Result:\n").append(r));
    circuit.ifPresent(c -> {
      if (!stringBuilder.isEmpty()) {
        stringBuilder.append("\n\n");
      }
      stringBuilder.append("Circuit:\n").append(c);
    });
    return stringBuilder.toString();
  }
}
