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

@Component
public class MetaSolverProvider {
  private final Map<ProblemType, MetaSolver<?, ?, ?>> metaSolvers;

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

  public MetaSolver<?, ?, ?> getMetaSolver(ProblemType problemType) {
    return metaSolvers.get(problemType);
  }

  public Collection<MetaSolver<?, ?, ?>> getMetaSolvers() {
    return Collections.unmodifiableCollection(metaSolvers.values());
  }
}
