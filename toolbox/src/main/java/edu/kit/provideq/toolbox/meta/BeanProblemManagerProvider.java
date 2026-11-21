package edu.kit.provideq.toolbox.meta;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This implementation provides all {@link ProblemManager problem managers} registered as beans in
 * the Spring application context.
 */
@Component
public class BeanProblemManagerProvider implements ProblemManagerProvider {
  private ApplicationContext context;

  @Override
  public Set<ProblemManager<?, ?>> getProblemManagers() {
    return context
        .getBeansOfType(ProblemManager.class)
        .values()
        .stream()
        .map(manager -> (ProblemManager<?, ?>) manager)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Dependency injection.
   */
  @Autowired
  private void setContext(ApplicationContext context) {
    this.context = context;
  }
}
