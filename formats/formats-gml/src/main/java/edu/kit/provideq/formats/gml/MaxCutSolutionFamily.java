package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.gml.formats.GmlSolutionFormat;
import java.util.Set;

/** Format family for MaxCut solutions. */
public final class MaxCutSolutionFamily implements FormatFamily {
  public static final MaxCutSolutionFamily INSTANCE = new MaxCutSolutionFamily();

  private MaxCutSolutionFamily() {
  }

  @Override
  public String getName() {
    return "MaxCut-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return GmlSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(GmlSolutionFormat.INSTANCE);
  }
}
