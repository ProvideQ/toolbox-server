package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the MaxCut graph problem.
 */
@Configuration
public class MaxCutConfiguration {
  /**
   * An optimization problem:
   * For a given graph, find the optimal separation of vertices that maximises the cut crossing edge
   * weight sum.
   */
  public static final ProblemType<String, String> MAX_CUT = new ProblemType<>(
      "max-cut",
      String.class,
      String.class,
      SolveMaxCutRequest.class
  );
}
