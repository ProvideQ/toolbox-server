package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

import java.util.Optional;

public record ExecutionResult(Optional<String> resultString, Optional<String> circuit) {
  public boolean hasResult() {
    return resultString.isPresent();
  }

  public boolean hasCircuit() {
    return circuit.isPresent();
  }
}
