package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.formats.VRPSolutionFormat;
import java.util.Set;

/** Format family for VRP solutions. */
public final class VrpSolutionFamily implements FormatFamily {
  public static final VrpSolutionFamily INSTANCE = new VrpSolutionFamily();

  private VrpSolutionFamily() {
  }

  @Override
  public String getName() {
    return "VRP-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return VRPSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(VRPSolutionFormat.INSTANCE);
  }
}
