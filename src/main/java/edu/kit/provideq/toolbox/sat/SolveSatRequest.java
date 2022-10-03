package edu.kit.provideq.toolbox.sat;

import javax.validation.constraints.NotNull;

/**
 * POST Requests to /solve/sat should have a response body of this form.
 * @param formula the SAT formula to solve in the DIMACS SAT format.
 */
public record SolveSatRequest(
    @NotNull String formula
) {
}
