package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.HashMap;
import java.util.Map;

class StringToDimacsCnfSolution {
  public static Map<Integer, Boolean> parse(String solutionString) {
    var variableMap = new HashMap<Integer, Boolean>();

    solutionString
        .lines()
        .map(line -> line.split(String.valueOf(DimacsCnf.SEPARATOR)))
        .forEach(lineSegment -> {
          if (lineSegment[0].charAt(0) == DimacsCnfSolution.VARIABLE_DECLARATION) {
            // Parse variable declaration
            var number = Integer.parseInt(lineSegment[1]);

            variableMap.put(Math.abs(number), number >= 0);
          }
        });

    return variableMap;
  }
}
