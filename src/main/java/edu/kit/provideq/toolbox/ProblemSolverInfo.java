package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.SolverCharacteristic;
import java.util.List;

public record ProblemSolverInfo(
    String id,
    String name,
    String description,
    List<SolverCharacteristic> characteristics
) {

}
