package edu.kit.provideq.toolbox.meta;

public interface ProblemSolver {
  boolean canSolve(Problem problem);
  Float getSuitability(Problem problem);
}
