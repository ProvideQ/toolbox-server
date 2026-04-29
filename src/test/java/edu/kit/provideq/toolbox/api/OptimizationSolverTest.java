package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
import static edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solver.optimization.OptimizationConfiguration.OPTIMIZATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.optimization.OptimizationSolver;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class OptimizationSolverTest {
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
  private OptimizationSolver optimizationSolver;

  @Autowired
  private MoveToMitigationSolver moveToMitigationSolver;

  @Autowired
  private ErrorMitigationSolver errorMitigationSolver;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(60))
        .build();
  }

  @Test
  void testOptimizationSolver() {
    var problemDto = ApiTestHelper.createProblem(client, optimizationSolver, CIRCUIT, OPTIMIZATION_CONFIG);
    var circuitSubProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var mitigationEntryDto = ApiTestHelper.setProblemSolver(
        client, moveToMitigationSolver, circuitSubProblemId, CIRCUIT_PROCESSING.getId());
    var mitigationId = mitigationEntryDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, mitigationId, MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), OPTIMIZATION_CONFIG);
    ApiTestHelper.testSolution(solvedDto);
    assertTrue(solvedDto.getSolution().getSolutionData().contains("OPENQASM"));
  }
}
