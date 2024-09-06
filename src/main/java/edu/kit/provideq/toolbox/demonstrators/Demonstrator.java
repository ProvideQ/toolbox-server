package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public interface Demonstrator extends ProblemSolver<String, String> {
  @Override
  default ProblemType<String, String> getProblemType() {
    return DemonstratorConfiguration.DEMONSTRATOR;
  }
}
