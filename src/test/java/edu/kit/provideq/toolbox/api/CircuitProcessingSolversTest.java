package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
import static edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solver.optimization.OptimizationConfiguration.OPTIMIZATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutionSolver;
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
class CircuitProcessingSolversTest {
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
  private MoveToMitigationSolver moveToMitigationSolver;

  @Autowired
  private MoveToExecutionSolver moveToExecutionSolver;

  @Autowired
  private MoveToOptimizationSolver moveToOptimizationSolver;

  @Autowired
  private ErrorMitigationSolver errorMitigationSolver;

  @Autowired
  private ExecutionSolver executionSolver;

  @Autowired
  private OptimizationSolver optimizationSolver;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(60))
        .build();
  }

  @Test
  void testMoveToMitigationSolver() {
    var problemDto = ApiTestHelper.createProblem(client, moveToMitigationSolver, CIRCUIT, CIRCUIT_PROCESSING);
    var subProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, subProblemId, MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertEquals(CIRCUIT, solvedDto.getSolution().getSolutionData());
  }

  @Test
  void testMoveToExecutionSolver() {
    var problemDto = ApiTestHelper.createProblem(client, moveToExecutionSolver, CIRCUIT, CIRCUIT_PROCESSING);
    var subProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, executionSolver, subProblemId, EXECUTOR_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertFalse(solvedDto.getSolution().getSolutionData().isBlank());
  }

  @Test
  void testMoveToOptimizationSolver() {
    var problemDto = ApiTestHelper.createProblem(client, moveToOptimizationSolver, CIRCUIT, CIRCUIT_PROCESSING);
    var optSubProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var optDto = ApiTestHelper.setProblemSolver(
        client, optimizationSolver, optSubProblemId, OPTIMIZATION_CONFIG.getId());
    var circuitSubProblemId = optDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var mitigationEntryDto = ApiTestHelper.setProblemSolver(
        client, moveToMitigationSolver, circuitSubProblemId, CIRCUIT_PROCESSING.getId());
    var mitigationId = mitigationEntryDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, mitigationId, MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertTrue(solvedDto.getSolution().getSolutionData().contains("OPENQASM"));
  }
}
