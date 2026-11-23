package edu.kit.provideq.formats.lp;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.lp.formats.LPFormat;
import java.util.Set;

/** Format family for QUBO inputs. */
public final class QuboFamily implements FormatFamily {
  public static final QuboFamily INSTANCE = new QuboFamily();

  private QuboFamily() {
  }

  @Override
  public String getName() {
    return "QUBO";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return LPFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(LPFormat.INSTANCE);
  }
}
