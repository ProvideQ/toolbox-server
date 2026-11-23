package edu.kit.provideq.formats.qasm;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.qasm.formats.QasmFormat;

/**
 * Bundles all QASM module formats for registration.
 */
public final class QasmFormats {
  private QasmFormats() {
  }

  /**
   * Registers all QASM module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(QasmFormat.INSTANCE);
  }
}
