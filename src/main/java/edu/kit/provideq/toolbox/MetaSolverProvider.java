package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * The meta solver provide can be used to find all or specific {@link MetaSolver}s.
 */
@Component
public class MetaSolverProvider {
  private final Map<ProblemType, MetaSolver<?, ?, ?>> metaSolvers;

  /**
   * Initializes a meta solver provider bean.
   * This provider will fetch available meta solvers once through the given {@code context}.
   *
   * @param context used to find available meta solvers.
   */
  @Autowired
  public MetaSolverProvider(ApplicationContext context) {
    this.metaSolvers =
        context.getBeansOfType(MetaSolver.class)
            .values()
            .stream()
            .collect(Collectors.toMap(
                    MetaSolver::getProblemType,
                    metaSolver -> (MetaSolver<?, ?, ?>) metaSolver));
  }

  /**
   * Finds a meta solver for the given problem type.
   *
   * @param problemType the type of problem to find the meta solver of.
   * @return the meta solver that manages solvers of the given type.
   */
  public MetaSolver<?, ?, ?> getMetaSolver(ProblemType problemType) {
    return metaSolvers.get(problemType);
  }

  /**
   * Returns all registered meta solvers for all available problem types.
   */
  public Collection<MetaSolver<?, ?, ?>> getMetaSolvers() {
    return Collections.unmodifiableCollection(metaSolvers.values());
  }
}
