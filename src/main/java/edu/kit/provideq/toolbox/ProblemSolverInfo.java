package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.authentication.AuthenticationOptions;
import javax.annotation.Nullable;

/**
 * Information about a problem solver.
 * They are used to give clients information about the problem solvers that are available to them.
 *
 * @param id                    the id of the problem solver
 * @param name                  the name of the problem solver
 * @param authenticationOptions possible authentication options for the problem solver
 */
public record ProblemSolverInfo(
    String id,
    String name,
    @Nullable AuthenticationOptions authenticationOptions) {

}