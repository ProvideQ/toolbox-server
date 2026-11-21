package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for demonstrator benchmark reports.
 */
public final class DemonstratorReportFamily implements FormatFamily {
  public static final DemonstratorReportFamily INSTANCE = new DemonstratorReportFamily();

  private DemonstratorReportFamily() {
  }

  @Override
  public String getName() {
    return "DemonstratorReport";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return DemonstratorReportFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(DemonstratorReportFormat.INSTANCE);
  }
}
