package edu.kit.provideq.formats.sat.converters;

import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnf;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfSolution;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for strings in DIMACS CNF solution format.
 */
public class StringToDimacsCnfSolution {
  private StringToDimacsCnfSolution() {
  }

  public static Map<Integer, Boolean> parse(String solutionString) {
    Map<Integer, Boolean> variableMap = new HashMap<>();

    for (String line : solutionString.split("\r\n|\n")) {
      if (line.isBlank()) {
        continue;
      }

      var lineSegment = line.split(String.valueOf(DimacsCnf.SEPARATOR));
      if (lineSegment[0].charAt(0) == DimacsCnfSolution.VARIABLE_DECLARATION) {
        // Parse variable declarations
        // a v-line may carry multiple literals and ends with the terminating 0
        for (int i = 1; i < lineSegment.length; i++) {
          var segment = lineSegment[i].trim();
          if (segment.isEmpty() || segment.equals("0")) {
            continue;
          }
          var number = Integer.parseInt(segment);

          variableMap.put(Math.abs(number), number >= 0);
        }
      }
    }

    return variableMap;
  }
}
