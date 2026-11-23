package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.gml.formats.GmlFormat;
import java.util.Set;

/** Format family for MaxCut graph inputs. */
public final class MaxCutFamily implements FormatFamily {
  public static final MaxCutFamily INSTANCE = new MaxCutFamily();

  private MaxCutFamily() {
  }

  @Override
  public String getName() {
    return "MaxCut";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return GmlFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(GmlFormat.INSTANCE);
  }
}
