package edu.kit.provideq.toolbox.circuit.processing.results;

import edu.kit.provideq.toolbox.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExecutionResultHelper {
  private ExecutionResultHelper() {
    throw new IllegalStateException("Utility class");
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static ExecutionResult createExecutionResult(
      Optional<String> resultStringOptional,
      Optional<String> circuitOptional
  ) {
    if (resultStringOptional.isEmpty()) {
      return new ExecutionResult(
          Optional.empty(),
          Optional.empty(),
          resultStringOptional,
          circuitOptional
      );
    }

    var resultString = resultStringOptional.get();
    var counts = parseCounter(resultString);
    var sortedMeasurements = createSortedMeasurements(counts);
    var sortedProbabilities = createSortedProbabilities(counts);

    return new ExecutionResult(
        Optional.of(sortedMeasurements),
        Optional.of(sortedProbabilities),
        resultStringOptional,
        circuitOptional
    );
  }

  private static Map<String, Integer> parseCounter(String counterString) {
    Pattern pattern = Pattern.compile("\\(([01](?:\\s*,\\s*[01])*)\\)\\s*:\\s*(\\d+)");

    Matcher matcher = pattern.matcher(counterString);

    Map<String, Integer> counts = new HashMap<>();

    while (matcher.find()) {
      String bitString = matcher.group(1).replaceAll("\\s*,\\s*", "");
      int count = Integer.parseInt(matcher.group(2));

      counts.put(bitString, count);
    }

    return counts;
  }

  private static List<Pair<String, Integer>> createSortedMeasurements(
      Map<String, Integer> counts
  ) {
    return counts.entrySet()
        .stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
        .toList();
  }

  private static List<Pair<String, Double>> createSortedProbabilities(
      Map<String, Integer> counts
  ) {
    int totalShots = counts.values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    return counts.entrySet()
        .stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .map(entry -> new Pair<>(
            entry.getKey(),
            (double) entry.getValue() / totalShots
        ))
        .toList();
  }
}
