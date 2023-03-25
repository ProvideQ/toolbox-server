package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.ArrayList;
import java.util.HashMap;

class StringToDimacsCNF {
    public DimacsCNF parse(String dimacsCNFString) {
        var variableMap = new HashMap<Integer, String>();
        var clauses = new ArrayList<ArrayList<Variable>>();

        dimacsCNFString
                .lines()
                .map(line -> line.split(String.valueOf(DimacsCNF.SEPARATOR)))
                .forEach(lineSegment -> {
                    switch (lineSegment[0].charAt(0)) {
                        case DimacsCNF.COMMENT_START -> {
                            // Parse comment
                            String name = lineSegment[1];
                            Integer number = Integer.parseInt(lineSegment[2]);
                            variableMap.put(number, name);
                        }
                        case DimacsCNF.PREAMBLE_START -> {
                            // Parse preamble
                        }
                        default -> {
                            // Parse clause
                            var clause = new ArrayList<Variable>();
                            for (int i = 0; i < lineSegment.length - 1; i++) {
                                var number = Math.abs(Integer.parseInt(lineSegment[i]));
                                var name = variableMap.get(number);

                                clause.add(lineSegment[i].charAt(0) == DimacsCNF.NEGATION_PREFIX
                                        ? new NegatedVariable(number, name)
                                        : new Variable(number, name));
                            }
                            clauses.add(clause);
                        }
                    }
                });

        return new DimacsCNF(clauses);
    }
}
