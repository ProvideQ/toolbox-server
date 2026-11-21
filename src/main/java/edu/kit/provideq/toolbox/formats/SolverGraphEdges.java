package edu.kit.provideq.toolbox.formats;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;

import edu.kit.provideq.formats.core.converters.graph.LabeledEdge;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds {@link LabeledEdge} entries representing the toolbox's
 * available solvers: each solver is a directed edge from its problem type's input family
 * to its result family.
 */
@Component
public class SolverGraphEdges {
  private ProblemManagerProvider managerProvider;

  /**
   * Returns one edge per registered solver, labeled with the solver's name.
   */
  public List<LabeledEdge> getEdges() {
    return managerProvider.getProblemManagers().stream()
        .filter(manager -> !manager.getType().equals(DEMONSTRATOR))
        .flatMap(manager -> solverEdges(manager).stream())
        .toList();
  }

  /**
   * Returns a list of LabeledEdge objects representing the solvers for the given problem manager.
   */
  private static List<LabeledEdge> solverEdges(ProblemManager<?, ?> manager) {
    var inputFamily = manager.getType().getInputFamily();
    var resultFamily = manager.getType().getResultFamily();

    return manager.getSolvers().stream()
        .map(solver -> new LabeledEdge(
            inputFamily, resultFamily, solver.getName()))
        .toList();
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }
}
