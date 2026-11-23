package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.api.dto.SolutionDto;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.solvers.ExecutionSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class ExecutionSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ConversionRegistry conversionRegistry;

  @Autowired
  private ExecutionSolver executionSolver;

  private List<? extends FormattedValue<?>> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300))
        .build();
    problems = problemManagerProvider.findProblemManagerForType(EXECUTOR_CONFIG).get()
        .getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Test
  void testExecutionSolver() {
    var circuit = problems.getFirst();
    var problem = ApiTestHelper.createProblem(client, executionSolver, circuit, EXECUTOR_CONFIG);
    ApiTestHelper.testSolution(problem, problemManagerProvider, conversionRegistry);
    SolutionDto solution = problem.getSolution();
    assertTrue(solution.getSolutionData().contains("OPENQASM"));
  }
}