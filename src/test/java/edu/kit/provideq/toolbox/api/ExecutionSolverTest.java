package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.solvers.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solvers.executor.ExecutionSolver;
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
  private ExecutionSolver executionSolver;

  private List<String> problems;

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
    var circuit = problems.get(0);
    var problem = ApiTestHelper.createProblem(client, executionSolver, circuit, EXECUTOR_CONFIG);
    ApiTestHelper.testSolution(problem);
    Solution<?> solution = problem.getSolution();
    assertTrue(solution.getSolutionData().toString().contains("OPENQASM"));
  }
}
