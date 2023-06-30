package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ProblemControllerProvider {
  private final Map<ProblemType, ProblemController> problemControllers;

  @Autowired
  public ProblemControllerProvider(
      ApplicationContext context) {
    problemControllers =
        context.getBeansOfType(ProblemController.class)
            .values()
            .stream()
            .collect(Collectors.toMap(ProblemController::getProblemType, Function.identity()));
  }

  public ProblemController getProblemController(ProblemType problemType) {
    return problemControllers.get(problemType);
  }
}
