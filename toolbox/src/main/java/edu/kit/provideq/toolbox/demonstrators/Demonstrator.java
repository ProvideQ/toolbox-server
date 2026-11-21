package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A demonstrator is a broad group of processes that demonstrate certain aspects of quantum
 * computing.
 */
public interface Demonstrator extends ProblemSolver<DemonstratorFamily, DemonstratorReportFamily> {
  @Override
  default ProblemType<DemonstratorFamily, DemonstratorReportFamily> getProblemType() {
    return DemonstratorConfiguration.DEMONSTRATOR;
  }
}
