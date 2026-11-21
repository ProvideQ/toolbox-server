package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.formats.TSPLIBSolutionFormat;
import java.util.Set;

/** Format family for TSP solutions. */
public final class TspSolutionFamily implements FormatFamily {
  public static final TspSolutionFamily INSTANCE = new TspSolutionFamily();

  private TspSolutionFamily() {
  }

  @Override
  public String getName() {
    return "TSP-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return TSPLIBSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(TSPLIBSolutionFormat.INSTANCE);
  }
}
