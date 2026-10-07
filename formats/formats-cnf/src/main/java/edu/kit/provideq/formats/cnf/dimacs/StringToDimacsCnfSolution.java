package edu.kit.provideq.formats.cnf.dimacs;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for strings in DIMACS CNF solution format.
 */
class StringToDimacsCnfSolution {
  private StringToDimacsCnfSolution() {
  }

  static Map<Integer, Boolean> parse(String solutionString) {
    Map<Integer, Boolean> variableMap = new HashMap<>();

    for (String line : solutionString.split("\r\n|\n")) {
      if (line.isBlank()) {
        continue;
      }

      var lineSegment = line.split(String.valueOf(DimacsCnf.SEPARATOR));
      if (lineSegment[0].charAt(0) == DimacsCnfSolution.VARIABLE_DECLARATION) {
        // Parse variable declaration
        var number = Integer.parseInt(lineSegment[1]);

        variableMap.put(Math.abs(number), number >= 0);
      }
    }

    return variableMap;
  }
}
