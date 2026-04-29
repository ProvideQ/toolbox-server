package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationSolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorMitigationSolverTest {
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
  private ErrorMitigationSolver errorMitigationSolver;

  @Test
  void testErrorMitigationSolver() {
    var problem = ApiTestHelper.createProblem(client, errorMitigationSolver, CIRCUIT, MITIGATION_CONFIG);
    ApiTestHelper.testSolution(problem);
    assertEquals(CIRCUIT, problem.getSolution().getSolutionData());
  }
}
