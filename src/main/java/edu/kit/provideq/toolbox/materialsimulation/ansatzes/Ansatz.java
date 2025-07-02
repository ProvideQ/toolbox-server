package edu.kit.provideq.toolbox.materialsimulation.ansatzes;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * An abstract class representing an Ansatz for material simulation problems.
 */
public abstract class Ansatz implements ProblemSolver<String, String> {

  @Override
  public ProblemType<String, String> getProblemType() {
    return AnsatzConfiguration.MATERIAL_SIMULATION_ANSATZ;
  }

}
