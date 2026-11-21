package edu.kit.provideq.formats.tsplib;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Converts a TSPLIB-CVRP document ({@link VRPFormat}) into a TSPLIB-TSP document
 * ({@link TSPLIBFormat}) by stripping the capacity constraints.
 *
 * <p>The conversion removes the {@code CAPACITY} header, the {@code DEMAND_SECTION} block and
 * rewrites the {@code TYPE} header from {@code CVRP} to {@code TSP}, yielding a plain traveling
 * salesman problem over the same nodes. All other sections (e.g. {@code NODE_COORD_SECTION},
 * {@code DEPOT_SECTION}) are kept as-is.
 *
 * <p>Example input (VRP):
 * <pre>{@code
 * NAME : test
 * TYPE : CVRP
 * DIMENSION : 3
 * EDGE_WEIGHT_TYPE : EUC_2D
 * CAPACITY : 2
 * NODE_COORD_SECTION
 * 1 0.00 0.00
 * 2 1.00 0.50
 * 3 0.50 -1.00
 * DEMAND_SECTION
 * 1 0
 * 2 1
 * 3 2
 * DEPOT_SECTION
 * 1
 * -1
 * }</pre>
 *
 * <p>Example output (TSPLIB):
 * <pre>{@code
 * NAME : test
 * TYPE : TSP
 * DIMENSION : 3
 * EDGE_WEIGHT_TYPE : EUC_2D
 * NODE_COORD_SECTION
 * 1 0.00 0.00
 * 2 1.00 0.50
 * 3 0.50 -1.00
 * DEPOT_SECTION
 * 1
 * -1
 * }</pre>
 */
public class VrpToTsplibConverter implements Converter<String, String> {
  /**
   * Singleton instance of the converter.
   */
  public static final VrpToTsplibConverter INSTANCE = new VrpToTsplibConverter();

  private static final String SECTION_SUFFIX = "SECTION";

  @Override
  public Format<String> getSource() {
    return VRPFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return TSPLIBFormat.INSTANCE;
  }

  @Override
  public String convert(String value) throws FormatException {
    var lines = new ArrayList<String>();
    var inDemandSection = false;

    for (var line : value.lines().toList()) {
      var upper = line.toUpperCase(Locale.ROOT);

      if (upper.startsWith("DEMAND_SECTION")) {
        inDemandSection = true;
        continue;
      }
      if (inDemandSection) {
        // The demand block ends where the next section (or EOF) begins
        if (upper.contains(SECTION_SUFFIX) || upper.startsWith("EOF")) {
          inDemandSection = false;
        } else {
          continue;
        }
      }

      if (upper.startsWith("CAPACITY")) {
        continue;
      }

      if (upper.startsWith("TYPE")) {
        line = rewriteType(line);
      }

      lines.add(line);
    }

    return String.join(System.lineSeparator(), lines);
  }

  private static String rewriteType(String line) {
    var separatorIndex = line.indexOf(':');
    if (separatorIndex < 0) {
      return line;
    }
    var type = line.substring(separatorIndex + 1).trim();
    if (type.equalsIgnoreCase("CVRP")) {
      return line.substring(0, separatorIndex) + ": TSP";
    }
    return line;
  }
}
