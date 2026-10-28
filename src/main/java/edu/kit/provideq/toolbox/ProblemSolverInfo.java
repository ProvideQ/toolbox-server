package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.SolverCharacteristics;

public record ProblemSolverInfo(
    String id,
    String name,
    String description,
    SolverCharacteristics characteristics
) {

}
