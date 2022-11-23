package edu.kit.provideq.toolbox;

/**
 * Used to refer to the solution (process) of a submitted problem.
 */
public interface SolutionHandle {
    long id();

    SolutionStatus status();

    void setStatus(SolutionStatus newStatus);
}
