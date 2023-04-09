package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.HashMap;
import java.util.Map;

public class StringToDimacsCNFSolution {
    public Map<Integer, Boolean> parse(String solutionString) {
        var variableMap = new HashMap<Integer, Boolean>();

        solutionString
                .lines()
                .map(line -> line.split(String.valueOf(DimacsCNF.SEPARATOR)))
                .forEach(lineSegment -> {
                    if (lineSegment[0].charAt(0) == DimacsCNFSolution.VARIABLE_DECLARATION) {
                        // Parse variable declaration
                        var number = Integer.parseInt(lineSegment[1]);

                        variableMap.put(Math.abs(number), number >= 0);
                    }
                });

        return variableMap;
    }
}
