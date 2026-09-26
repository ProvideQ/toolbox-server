package edu.kit.provideq.toolbox.meta.guard;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.BooleanSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

public final class Guard {
  public static final String SETTING_NAME = "Guarded";
  public static final String RETRIES_SETTING_NAME = "Guard Retries";
  private static final int MAX_RETRIES = 100;

  private Guard() {
  }

  public record Configuration(boolean enabled, int retries) {
    public static final Configuration DISABLED = new Configuration(false, 0);
  }

  public record Attempt<ResultT>(ResultT result, String error, List<Solution<?>> sources) {
    public static <ResultT> Attempt<ResultT> of(ResultT result, Solution<?>... sources) {
      return new Attempt<>(result, null, List.of(sources));
    }

    public static <ResultT> Attempt<ResultT> failed(String error, Solution<?>... sources) {
      return new Attempt<>(null, error, List.of(sources));
    }

    private Optional<String> findFailure() {
      if (error != null) {
        return Optional.of(error);
      }
      if (result == null) {
        return Optional.of("The solver produced no result.");
      }
      return Optional.empty();
    }
  }

  public static List<SolverSetting> createSettings(
      String description,
      boolean defaultEnabled,
      int defaultRetries
  ) {
    return List.of(
        new BooleanSetting(SETTING_NAME, description, defaultEnabled),
        new IntegerSetting(
            RETRIES_SETTING_NAME,
            "How often the sub-routines are started again when the guard rejects a result",
            0,
            MAX_RETRIES,
            defaultRetries)
    );
  }

  public static Configuration readConfiguration(
      SolvingProperties properties,
      boolean defaultEnabled,
      int defaultRetries
  ) {
    var enabled = properties.<BooleanSetting>getSetting(SETTING_NAME)
        .map(BooleanSetting::getState)
        .orElse(defaultEnabled);
    var retries = properties.<IntegerSetting>getSetting(RETRIES_SETTING_NAME)
        .map(IntegerSetting::getValue)
        .orElse(defaultRetries);
    return new Configuration(enabled, retries);
  }

  public static <ProblemT, ResultT> Mono<Solution<ResultT>> solve(
      Solution<ResultT> solution,
      ProblemT problem,
      SolutionGuard<ProblemT, ResultT> guard,
      Configuration configuration,
      Supplier<Mono<Attempt<ResultT>>> attempt
  ) {
    if (!configuration.enabled()) {
      return attempt.get().map(result -> complete(solution, result));
    }

    return retry(solution, problem, guard, configuration.retries(), attempt,
        0, new StringBuilder());
  }

  private static <ResultT> Solution<ResultT> complete(
      Solution<ResultT> solution,
      Attempt<ResultT> attempt
  ) {
    var failure = attempt.findFailure();
    if (failure.isPresent()) {
      solution.setDebugData(failure.get());
      solution.abort();
    } else {
      solution.setSolutionData(attempt.result());
      solution.complete();
    }
    return solution;
  }

  private static <ProblemT, ResultT> Mono<Solution<ResultT>> retry(
      Solution<ResultT> solution,
      ProblemT problem,
      SolutionGuard<ProblemT, ResultT> guard,
      int retries,
      Supplier<Mono<Attempt<ResultT>>> attempt,
      int attemptIndex,
      StringBuilder rejections
  ) {
    return attempt.get().flatMap(result -> {
      Optional<String> violation = result.findFailure()
          .or(() -> guard.findViolation(problem, result.result()));

      if (violation.isEmpty()) {
        solution.setSolutionData(result.result());
        if (!rejections.isEmpty()) {
          solution.setDebugData(rejections.toString());
        }
        solution.complete();
        return Mono.just(solution);
      }

      result.sources().forEach(source -> source.rejectByGuard(violation.get()));
      rejections.append("Guard rejected attempt ").append(attemptIndex + 1).append(": ")
          .append(violation.get()).append('\n');

      if (attemptIndex >= retries) {
        rejections.append("No valid solution found after ").append(attemptIndex + 1)
            .append(" attempt(s).");
        solution.setDebugData(rejections.toString());
        solution.abort();
        return Mono.just(solution);
      }

      return retry(solution, problem, guard, retries, attempt, attemptIndex + 1, rejections);
    });
  }
}
