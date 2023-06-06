package edu.kit.provideq.toolbox;

/**
 * Used to refer to the solution (process) of a submitted problem.
 */
public interface SolutionHandle {
    long getId();

    SolutionStatus getStatus();

    void setStatus(SolutionStatus newStatus);

    SolutionHandle toStringSolution();
}
