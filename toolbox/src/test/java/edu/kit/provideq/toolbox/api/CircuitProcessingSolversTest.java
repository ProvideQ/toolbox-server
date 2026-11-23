package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.circuit_processing.CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
import static edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.ExecutorConfiguration.EXECUTOR_CONFIG;
import static edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.optimization.OptimizationConfiguration.OPTIMIZATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.qasm.QasmFamily;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.ExecutionResultFamily;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.ExecutionSolver;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.optimization.OptimizationSolver;
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
  private ConversionRegistry conversionRegistry;

  @Autowired
  private MoveToExecutionSolver moveToExecutionSolver;

  @Autowired
  private MoveToOptimizationSolver moveToOptimizationSolver;

  @Autowired
  private ExecutionSolver executionSolver;

  @Autowired
  private OptimizationSolver optimizationSolver;

  private ProblemManager<QasmFamily, ExecutionResultFamily> problemManager;
  private List<? extends FormattedValue<?>> problems;

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
  void testMoveToExecutionSolver() {
    var circuit = problems.getFirst();
    var problemDto = ApiTestHelper.createProblem(client, moveToExecutionSolver,
        circuit, CIRCUIT_PROCESSING);
    problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, CIRCUIT_PROCESSING);
    var subProblemId = problemDto.getSubProblems().getFirst().getSubProblemIds().getFirst();
    ApiTestHelper.setProblemSolver(client, executionSolver, subProblemId, EXECUTOR_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client,
        problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto, problemManagerProvider, conversionRegistry);
    assertFalse(solvedDto.getSolution().getSolutionData().isBlank());
  }

  @Test
  void testMoveToOptimizationSolver() {
    var circuit = problems.getFirst();
    var problemDto = ApiTestHelper.createProblem(client, moveToOptimizationSolver,
        circuit, CIRCUIT_PROCESSING);
    problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, CIRCUIT_PROCESSING);
    var optSubProblemId = problemDto.getSubProblems().getFirst().getSubProblemIds().getFirst();
    var optDto = ApiTestHelper.setProblemSolver(
        client, optimizationSolver, optSubProblemId, OPTIMIZATION_CONFIG.getId());
    optDto = ApiTestHelper.waitForSubProblems(30, client, optDto, OPTIMIZATION_CONFIG);
    var circuitSubProblemId = optDto.getSubProblems().getFirst().getSubProblemIds().getFirst();
    var executionEntryDto = ApiTestHelper.setProblemSolver(
        client, moveToExecutionSolver, circuitSubProblemId, CIRCUIT_PROCESSING.getId());
    executionEntryDto = ApiTestHelper.waitForSubProblems(
        30, client, executionEntryDto, CIRCUIT_PROCESSING);
    var executionId = executionEntryDto.getSubProblems().getFirst().getSubProblemIds().getFirst();
    ApiTestHelper.setProblemSolver(client, executionSolver, executionId,
        EXECUTOR_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), CIRCUIT_PROCESSING);
    ApiTestHelper.testSolution(solvedDto, problemManagerProvider, conversionRegistry);
    assertTrue(solvedDto.getSolution().getSolutionData().contains("OPENQASM"));
  }
}