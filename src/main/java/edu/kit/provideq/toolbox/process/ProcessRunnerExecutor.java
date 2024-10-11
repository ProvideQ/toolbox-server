package edu.kit.provideq.toolbox.process;

import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.UUID;

public interface ProcessRunnerExecutor<T> {
  /**
   * Runs the process provided in the constructor.
   *
   * @param problemType The type of the problem that is run
   * @param solutionId  The id of the resulting solution
   * @return Returns the process result, which contains the solution data
   *         or an error as output depending on the success of the process.
   */
  ProcessResult<T> run(ProblemType<?, ?> problemType, UUID solutionId);
}
