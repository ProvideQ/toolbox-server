package edu.kit.provideq.toolbox.circuit.processing.results;

import edu.kit.provideq.toolbox.util.Pair;
import java.util.List;
import java.util.Optional;

/**
 * A record representing the result of a circuit processing execution operation.
 * This includes information about measurement outcomes, probabilities,
 * the resultant circuit, and a generated result string.
 *
 * @param sortedMeasurements  An optional list of measurement results sorted by frequency in descending order.
 *                            Each entry consists of a string representing the measurement outcome
 *                            and an integer representing the count of that outcome.
 * @param sortedProbabilities An optional list of measurement probabilities sorted in descending order.
 *                            Each entry consists of a string representing the measurement outcome
 *                            and a double representing the probability of that outcome.
 * @param resultString        An optional string representation of the circuit processing result.
 * @param circuit             An optional string representing the processed circuit in a predefined format.
 */
public record ExecutionResult(
    Optional<List<Pair<String, Integer>>> sortedMeasurements,
    Optional<List<Pair<String, Double>>> sortedProbabilities,
    Optional<String> resultString,
    Optional<String> circuit
) implements Result {

  @Override
  public <R> R accept(ResultVisitor<R> resultVisitor) {
    return resultVisitor.visit(this);
  }

  public boolean hasResult() {
    return resultString.isPresent();
  }

  public boolean hasCircuit() {
    return circuit.isPresent();
  }

  public Optional<String> getHighestResult() {
    if (sortedMeasurements().isEmpty()) {
      return Optional.empty();
    }

    var sortedMeasurements = sortedMeasurements().get();
    if (sortedMeasurements.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(sortedMeasurements.get(0).first());
  }

  public Optional<Integer> getHighestResultMeasurement() {
    if (sortedMeasurements().isEmpty()) {
      return Optional.empty();
    }

    var sortedMeasurements = sortedMeasurements().get();
    if (sortedMeasurements.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(sortedMeasurements.get(0).second());
  }

  public Optional<Double> getHighestResultProbability() {
    if (sortedProbabilities().isEmpty()) {
      return Optional.empty();
    }

    var sortedProbabilities = sortedProbabilities().get();
    if (sortedProbabilities.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(sortedProbabilities.get(0).second());
  }
}
