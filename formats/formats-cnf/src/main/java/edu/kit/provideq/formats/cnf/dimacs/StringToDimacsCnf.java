package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for strings in DIMACS CNF format.
 */
class StringToDimacsCnf {
  private StringToDimacsCnf() {
  }

  static DimacsCnf parse(String dimacsCnfString) throws ParseException {
    Map<Integer, String> variableMap = new HashMap<>();
    var clauses = new ArrayList<List<Variable>>();

    int variableCount = -1;
    int clauseCount = -1;

    for (String rawLine : dimacsCnfString.split("\r\n|\n")) {
      if (rawLine.isBlank()) {
        continue;
      }

      var line = rawLine;
      if (line.charAt(line.length() - 1) == '\r') {
        line = line.substring(0, line.length() - 1);
      }

      var lineSegment = line.split(String.valueOf(DimacsCnf.SEPARATOR));
      switch (lineSegment[0].charAt(0)) {
        case DimacsCnf.COMMENT_START -> {
          if (lineSegment.length < 3) {
            break;
          }

          // Parse comment
          Integer number = Integer.parseInt(lineSegment[1]);
          String name =
              String.join(" ", Arrays.copyOfRange(lineSegment, 2, lineSegment.length));
          variableMap.put(number, name);
        }
        case DimacsCnf.PREAMBLE_START -> {
          // Parse preamble
          if (!DimacsCnf.CNF_IDENTIFIER.equals(lineSegment[1])) {
            throw new ParseException(
                "Expected DIMACS CNF identifier %s in header, but found %s".formatted(
                    DimacsCnf.CNF_IDENTIFIER, lineSegment[1]));
          }

          variableCount = Integer.parseInt(lineSegment[2]);
          clauseCount = Integer.parseInt(lineSegment[3]);
        }
        default -> {
          // Parse clause; the last token is the terminating 0
          var clause = new ArrayList<Variable>();
          for (int i = 0; i < lineSegment.length - 1; i++) {
            var rawNumber = Integer.parseInt(lineSegment[i]);
            var number = Math.abs(rawNumber);
            var name = variableMap.get(number);

            var isNegated = rawNumber < 0;
            clause.add(new Variable(number, name, isNegated));
          }
          clauses.add(clause);
        }
      }
    }

    if (variableCount != variableMap.size()) {
      throw new ParseException(
          "Count of variables in the header is %d, but the actual count is %d".formatted(
              variableCount, variableMap.size()));
    }

    if (clauseCount != clauses.size()) {
      throw new ParseException(
          "Count of clauses in the header is %d, but the actual count is %d".formatted(
              clauseCount, clauses.size()));
    }

    return new DimacsCnf(clauses);
  }
}
