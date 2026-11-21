package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.lp.LPSolutionFormat;
import java.util.Set;

/** Format family for QUBO solutions. */
public final class QuboSolutionFamily implements FormatFamily {
  public static final QuboSolutionFamily INSTANCE = new QuboSolutionFamily();

  private QuboSolutionFamily() {
  }

  @Override
  public String getName() {
    return "QUBO-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return LPSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(
        LPSolutionFormat.INSTANCE,
        JsonQuboSolutionFormat.INSTANCE,
        PositionalBitsSolutionFormat.INSTANCE);
  }
}
