package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.formats.VRPFormat;
import java.util.Set;

/** Format family for VRP inputs. */
public final class VrpFamily implements FormatFamily {
  public static final VrpFamily INSTANCE = new VrpFamily();

  private VrpFamily() {
  }

  @Override
  public String getName() {
    return "VRP";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return VRPFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(VRPFormat.INSTANCE);
  }

}
