package edu.kit.provideq.formats.tsplib;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBFormat;

import java.util.Set;

/** Format family for TSP inputs. */
public final class TspLibFamily implements FormatFamily {
  public static final TspLibFamily INSTANCE = new TspLibFamily();

  private TspLibFamily() {
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
