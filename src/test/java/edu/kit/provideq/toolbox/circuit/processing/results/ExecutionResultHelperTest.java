package edu.kit.provideq.toolbox.circuit.processing.results;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import edu.kit.provideq.toolbox.util.Pair;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExecutionResultHelperTest {
  private static final double EPSILON = 1e-12;

  @Test
  void createExecutionResult_parsesMultipleMeasurementsAndKeepsOriginalOptionals() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(1, 1, 0, 0): 159, (0, 0, 0, 0): 133, (0, 0, 1, 0): 101})"),
        Optional.of("test-circuit")
    );

    assertThat(result.resultString())
        .contains("Counter({(1, 1, 0, 0): 159, (0, 0, 0, 0): 133, (0, 0, 1, 0): 101})");
    assertThat(result.circuit()).contains("test-circuit");

    assertThat(result.sortedMeasurements()).isPresent();
    assertThat(result.sortedMeasurements().orElseThrow())
        .extracting(Pair::first, Pair::second)
        .containsExactly(
            tuple("1100", 159),
            tuple("0000", 133),
            tuple("0010", 101)
        );
  }

  @Test
  void createExecutionResult_calculatesProbabilitiesCorrectly() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(1, 1, 0, 0): 159, (0, 0, 0, 0): 133, (0, 0, 1, 0): 101})"),
        Optional.empty()
    );

    List<Pair<String, Double>> probabilities = result.sortedProbabilities().orElseThrow();

    assertThat(probabilities)
        .extracting(Pair::first)
        .containsExactly("1100", "0000", "0010");

    int total = 159 + 133 + 101;

    assertThat(probabilities.get(0).second()).isCloseTo(159.0 / total, within(EPSILON));
    assertThat(probabilities.get(1).second()).isCloseTo(133.0 / total, within(EPSILON));
    assertThat(probabilities.get(2).second()).isCloseTo(101.0 / total, within(EPSILON));
  }

  @Test
  void createExecutionResult_sortsDescendingIndependentOfInputOrder() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(0, 0): 5, (1, 1): 20, (1, 0): 10, (0, 1): 15})"),
        Optional.empty()
    );

    assertThat(result.sortedMeasurements().orElseThrow())
        .extracting(Pair::first, Pair::second)
        .containsExactly(
            tuple("11", 20),
            tuple("01", 15),
            tuple("10", 10),
            tuple("00", 5)
        );

    assertThat(result.sortedProbabilities().orElseThrow())
        .extracting(Pair::first)
        .containsExactly("11", "01", "10", "00");
  }

  @Test
  void createExecutionResult_handlesSingleMeasurement() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(1, 0, 1): 42})"),
        Optional.empty()
    );

    assertThat(result.sortedMeasurements().orElseThrow())
        .extracting(Pair::first, Pair::second)
        .containsExactly(tuple("101", 42));

    assertThat(result.sortedProbabilities().orElseThrow())
        .extracting(Pair::first, Pair::second)
        .containsExactly(tuple("101", 1.0));
  }

  @Test
  void createExecutionResult_returnsEmptyListsForEmptyCounter_V1() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter()"),
        Optional.empty()
    );

    assertThat(result.sortedMeasurements()).contains(List.of());
    assertThat(result.sortedProbabilities()).contains(List.of());
  }

  @Test
  void createExecutionResult_returnsEmptyListsForEmptyCounter_V2() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({})"),
        Optional.empty()
    );

    assertThat(result.sortedMeasurements()).contains(List.of());
    assertThat(result.sortedProbabilities()).contains(List.of());
  }

  @Test
  void createExecutionResult_keepsAllMeasurementsWhenCountsAreTied() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(1, 0): 7, (0, 1): 7, (1, 1): 3})"),
        Optional.empty()
    );

    assertThat(result.sortedMeasurements().orElseThrow())
        .extracting(Pair::first, Pair::second)
        .containsExactlyInAnyOrder(
            tuple("10", 7),
            tuple("01", 7),
            tuple("11", 3)
        );

    assertThat(result.sortedMeasurements().orElseThrow())
        .extracting(Pair::second)
        .containsExactly(7, 7, 3);
  }

  @Test
  void createExecutionResult_probabilitiesSumToOne() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.of("Counter({(0, 0): 5, (0, 1): 15, (1, 0): 30})"),
        Optional.empty()
    );

    double sum = result.sortedProbabilities().orElseThrow().stream()
        .mapToDouble(Pair::second)
        .sum();

    assertThat(sum).isCloseTo(1.0, within(EPSILON));
  }

  @Test
  void createExecutionResult_returnsEmptyMeasurementAndProbabilityOptionalsWhenResultStringIsEmpty() {
    ExecutionResult result = ExecutionResultHelper.createExecutionResult(
        Optional.empty(),
        Optional.of("circuit")
    );

    assertThat(result.resultString()).isEmpty();
    assertThat(result.circuit()).contains("circuit");
    assertThat(result.sortedMeasurements()).isEmpty();
    assertThat(result.sortedProbabilities()).isEmpty();
  }
}
