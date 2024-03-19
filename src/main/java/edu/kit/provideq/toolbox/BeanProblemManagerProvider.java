package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.TypedProblemType;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanProblemManagerProvider implements ProblemManagerProvider {
  private ApplicationContext context;

  @Override
  public Set<ProblemManager<?, ?>> getProblemManagers() {
    return context
        .getBeansOfType(ProblemManager.class)
        .values()
        .stream()
        .map(problemManager -> (ProblemManager<?, ?>) problemManager)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public <InputT, ResultT> Optional<ProblemManager<InputT, ResultT>> getProblemManagerForType(
      TypedProblemType<InputT, ResultT> problemType
  ) {
    // suppress warnings for an explicitly checked cast below
    //noinspection unchecked
    return getProblemManagers()
        .stream()
        .filter(problemManager -> problemManager.getProblemType() == problemType)
        // this cast is checked as the problem manager's types match the problem type's types
        .map(problemManager -> (ProblemManager<InputT, ResultT>) problemManager)
        .findAny();
  }

  @Autowired
  public void setContext(ApplicationContext context) {
    this.context = context;
  }
}
