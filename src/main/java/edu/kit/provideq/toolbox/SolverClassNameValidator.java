package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SolverClassNameValidator implements ApplicationListener<ContextRefreshedEvent> {
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    List<String> solverClassNames = event.getApplicationContext()
        .getBeansOfType(ProblemManager.class)
        .values()
        .stream()
        .map(manager -> (ProblemManager<?, ?>) manager)
        .flatMap(manager -> manager.getSolvers().stream())
        .map(solver -> solver.getClass().getSimpleName())
        .toList();

    var duplicates = solverClassNames
        .stream()
        .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .toList();

    if (!duplicates.isEmpty()) {
      throw new IllegalStateException(
          "Duplicate solver class names found: " + String.join(", ", duplicates)
      );
    }
  }
}
