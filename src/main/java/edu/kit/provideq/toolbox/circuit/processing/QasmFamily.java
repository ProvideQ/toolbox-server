package edu.kit.provideq.toolbox.circuit.processing;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.formats.QasmFormat;
import java.util.Set;

/**
 * Format family for OpenQASM quantum circuits, used for problem inputs and circuit outputs.
 */
public final class QasmFamily implements FormatFamily {
  public static final QasmFamily INSTANCE = new QasmFamily();

  private QasmFamily() {
  }

  @Override
  public String getName() {
    return "QASM";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return QasmFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(QasmFormat.INSTANCE);
  }
}