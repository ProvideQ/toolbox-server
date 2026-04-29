package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutionSolver;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionSolverTest {
  private static final String CIRCUIT = """
      OPENQASM 2.0;
      include "qelib1.inc";
      qreg q[2];
      creg c[2];
      h q[0];
      cx q[0],q[1];
      measure q[0] -> c[0];
      measure q[1] -> c[1];""";

  @Autowired
  private WebTestClient client;

  @Autowired
  private ExecutionSolver executionSolver;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(60))
        .build();
  }

  @Test
  void testExecutionSolver() {
    var problem = ApiTestHelper.createProblem(client, executionSolver, CIRCUIT, EXECUTOR_CONFIG);
    ApiTestHelper.testSolution(problem);
    Solution<?> solution = problem.getSolution();
    assertTrue(solution.getSolutionData().toString().contains("OPENQASM"));
  }
}
