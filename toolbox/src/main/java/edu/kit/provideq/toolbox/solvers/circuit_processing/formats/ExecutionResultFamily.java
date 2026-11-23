package edu.kit.provideq.toolbox.solvers.circuit_processing.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for quantum circuit execution results.
 */
public final class ExecutionResultFamily implements FormatFamily {
  public static final ExecutionResultFamily INSTANCE = new ExecutionResultFamily();

  private ExecutionResultFamily() {
  }

  @Override
  public String getName() {
    return "ExecutionResult";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return ExecutionResultFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(ExecutionResultFormat.INSTANCE);
  }
}