package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * POST Requests to /solve/qubo should have a response body of this form.
 * The needed formula the qubo formula to solve in the
 * <a href="https://www.gurobi.com/documentation/current/refman/lp_format.html">LP format</a>.
 */
public class SolveQuboRequest extends SolveRequest<String> {
}
