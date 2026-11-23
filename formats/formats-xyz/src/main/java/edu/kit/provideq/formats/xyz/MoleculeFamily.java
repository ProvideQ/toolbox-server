package edu.kit.provideq.formats.xyz;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.xyz.formats.XYZMoleculeFormat;

import java.util.Set;

/**
 * Format family for molecule specification inputs.
 */
public final class MoleculeFamily implements FormatFamily {
  public static final MoleculeFamily INSTANCE = new MoleculeFamily();

  private MoleculeFamily() {
  }

  @Override
  public String getName() {
    return "Molecule";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return XYZMoleculeFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(XYZMoleculeFormat.INSTANCE);
  }
}
