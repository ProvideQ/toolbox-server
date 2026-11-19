package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.formats.TSPLIBFormat;
import java.util.Set;

/** Format family for TSP inputs. */
public final class TspFamily implements FormatFamily {
  public static final TspFamily INSTANCE = new TspFamily();

  private TspFamily() {
  }

  @Override
  public String getName() {
    return "TSP";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return TSPLIBFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(TSPLIBFormat.INSTANCE);
  }

}
