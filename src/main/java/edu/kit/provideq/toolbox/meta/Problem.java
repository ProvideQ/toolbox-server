package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import java.time.Duration;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * A problem encapsulates an input for a given problem type.
 * The problem can be solved using a matching ProblemSolver.
 *
 * @param <InputT> the data type of the problem's input.
 * @param <ResultT> the data type of the problem's solution.
 */
public class Problem<InputT, ResultT> {
  private final UUID id;
  private final ProblemType<InputT, ResultT> type;

  private InputT input;
  private Solution<ResultT> solution;
  private ProblemState state;
  private ProblemSolver<InputT, ResultT> solver;

  /**
   * Creates a new problem of a given {@link ProblemType}.
   *
   * @param type the kind of problem.
   */
  public Problem(ProblemType<InputT, ResultT> type,
      SubRoutinePool subRoutinePool /* TODO remove */) {
    this.id = UUID.randomUUID();
    this.type = type;

    this.setState(ProblemState.NEEDS_CONFIGURATION);
  }

  /**
   * Starts the solution of this problem.
   * Once the problem is solved, the solution can be obtained using {@link #getSolution()}.
   */
  public Mono<Solution<ResultT>> solve(Solution<ResultT> solution /* TODO remove */) {
    if (!this.isConfigured()) {
      throw new IllegalStateException("The problem is not fully configured!");
    }

    this.setState(ProblemState.SOLVING);

    long start = System.currentTimeMillis();
    return getSolver().solve(getInput(), null /* TODO sub routines! */)
        .repeatWhen(flux -> flux.delayElements(Duration.ofSeconds(5)))
        .takeUntil(sol -> sol.getStatus().isCompleted())
        .last()
        .doOnNext(sol -> {
          long finish = System.currentTimeMillis();
          sol.setExecutionMilliseconds(finish - start);
          this.solution = sol;
          this.setState(ProblemState.SOLVED);
        });
  }

  private boolean isConfigured() {
    return this.input != null
        && this.solver != null;
  }

  public UUID getId() {
    return id;
  }

  public ProblemType<InputT, ResultT> getType() {
    return type;
  }

  public InputT getInput() {
    return this.input;
  }

  public void setInput(InputT input) {
    this.input = input;
  }

  public Solution<ResultT> getSolution() {
    return this.solution;
  }

  public ProblemSolver<InputT, ResultT> getSolver() {
    return this.solver;
  }

  public void setSolver(ProblemSolver<InputT, ResultT> solver) {
    this.solver = solver;
  }

  public void setProblemSolver(ProblemSolver<InputT, ResultT> solver) {
    this.solver = solver;
  }

  public ProblemState getState() {
    return this.state;
  }

  private void setState(ProblemState newState) {
    this.state = newState;
  }
}
