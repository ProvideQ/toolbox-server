package edu.kit.provideq.toolbox.format.cnf.dimacs;

import edu.kit.provideq.toolbox.exception.ConversionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

class StringToDimacsCNF {
    public static DimacsCNF parse(String dimacsCNFString) throws ConversionException {
        var variableMap = new HashMap<Integer, String>();
        var clauses = new ArrayList<ArrayList<Variable>>();

        var variableCount = new AtomicInteger(-1);
        var clauseCount = new AtomicInteger(-1);

        dimacsCNFString
                .lines()
                .map(line -> line.split(String.valueOf(DimacsCNF.SEPARATOR)))
                .toList()
                .forEach(lineSegment -> {
                    switch (lineSegment[0].charAt(0)) {
                        case DimacsCNF.COMMENT_START -> {
                            if (lineSegment.length < 3) break;

                            // Parse comment
                            Integer number = Integer.parseInt(lineSegment[1]);
                            String name = String.join(" ", Arrays.copyOfRange(lineSegment, 2, lineSegment.length));
                            variableMap.put(number, name);
                        }
                        case DimacsCNF.PREAMBLE_START -> {
                            // Parse preamble
                            if (!DimacsCNF.CNF_IDENTIFIER.equals(lineSegment[1])) {
                                throw new RuntimeException("Excepted Dimacs CNF identifier %s in header, but found %s".formatted(DimacsCNF.CNF_IDENTIFIER, lineSegment[1]));
                            }

                            variableCount.set(Integer.parseInt(lineSegment[2]));
                            clauseCount.set(Integer.parseInt(lineSegment[3]));
                        }
                        default -> {
                            // Parse clause
                            var clause = new ArrayList<Variable>();
                            for (int i = 0; i < lineSegment.length - 1; i++) {
                                var number = Math.abs(Integer.parseInt(lineSegment[i]));
                                var name = variableMap.get(number);

                                var isNegated = lineSegment[i].charAt(0) == DimacsCNF.NEGATION_PREFIX;
                                clause.add(new Variable(number, name, isNegated));
                            }
                            clauses.add(clause);
                        }
                    }
                });

        if (variableCount.get() != variableMap.size()) {
            throw new ConversionException("Count of variables in the header is %d, but the actual count is %d".formatted(variableCount.get(), variableMap.size()));
        }

        if (clauseCount.get() != clauses.size()) {
            throw new ConversionException("Count of clauses in the header is %d, but the actual count is %d".formatted(clauseCount.get(), clauses.size()));
        }

        return new DimacsCNF(clauses);
    }
}
