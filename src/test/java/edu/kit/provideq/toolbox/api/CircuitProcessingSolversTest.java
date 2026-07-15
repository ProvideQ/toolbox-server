package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
import static edu.kit.provideq.toolbox.circuit.processing.solvers.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solvers.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solvers.optimization.OptimizationConfiguration.OPTIMIZATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.circuit.processing.solvers.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.circuit.processing.solvers.MoveToMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solvers.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solvers.executor.ExecutionSolver;
import edu.kit.provideq.toolbox.circuit.processing.solvers.mitigation.ErrorMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solvers.optimization.OptimizationSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
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
class CircuitProcessingSolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

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

  private ProblemManager<String, String> problemManager;
  private List<String> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300))
        .build();
    problemManager = problemManagerProvider.findProblemManagerForType(CIRCUIT_PROCESSING).get();
    problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Test
  void testMoveToMitigationSolver() {
    var circuit = problems.get(0);
    var problemDto = ApiTestHelper.createProblem(client, moveToMitigationSolver,
        circuit, CIRCUIT_PROCESSING);
    var subProblemId = problemDto.getSubProblems().get(0)
        .getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, subProblemId,
        MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertEquals(circuit, solvedDto.getSolution().getSolutionData());
  }

  @Test
  void testMoveToExecutionSolver() {
    var circuit = problems.get(0);
    var problemDto = ApiTestHelper.createProblem(client, moveToExecutionSolver,
        circuit, CIRCUIT_PROCESSING);
    var subProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, executionSolver, subProblemId, EXECUTOR_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client,
        problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertFalse(solvedDto.getSolution().getSolutionData().isBlank());
  }

  @Test
  void testMoveToOptimizationSolver() {
    var circuit = problems.get(0);
    var problemDto = ApiTestHelper.createProblem(client, moveToOptimizationSolver,
        circuit, CIRCUIT_PROCESSING);
    var optSubProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var optDto = ApiTestHelper.setProblemSolver(
        client, optimizationSolver, optSubProblemId, OPTIMIZATION_CONFIG.getId());
    var circuitSubProblemId = optDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var mitigationEntryDto = ApiTestHelper.setProblemSolver(
        client, moveToMitigationSolver, circuitSubProblemId, CIRCUIT_PROCESSING.getId());
    var mitigationId = mitigationEntryDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, mitigationId,
        MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto);
    assertTrue(solvedDto.getSolution().getSolutionData().contains("OPENQASM"));
  }
}
