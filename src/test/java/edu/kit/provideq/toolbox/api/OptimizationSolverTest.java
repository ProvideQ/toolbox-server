package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
import static edu.kit.provideq.toolbox.circuit.processing.solvers.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static edu.kit.provideq.toolbox.circuit.processing.solvers.optimization.OptimizationConfiguration.OPTIMIZATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.circuit.processing.solvers.MoveToMitigationSolver;
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
class OptimizationSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private OptimizationSolver optimizationSolver;

  @Autowired
  private MoveToMitigationSolver moveToMitigationSolver;

  @Autowired
  private ErrorMitigationSolver errorMitigationSolver;

  private ProblemManager<String, String> problemManager;
  private List<String> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300))
        .build();
    problemManager = problemManagerProvider.findProblemManagerForType(OPTIMIZATION_CONFIG).get();
    problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Test
  void testOptimizationSolver() {
    var circuit = problems.get(0);
    var problemDto = ApiTestHelper.createProblem(client, optimizationSolver,
        circuit, OPTIMIZATION_CONFIG);
    var circuitSubProblemId = problemDto.getSubProblems().get(0).getSubProblemIds().get(0);
    var mitigationEntryDto = ApiTestHelper.setProblemSolver(
        client, moveToMitigationSolver, circuitSubProblemId, CIRCUIT_PROCESSING.getId());
    var mitigationId = mitigationEntryDto.getSubProblems().get(0).getSubProblemIds().get(0);
    ApiTestHelper.setProblemSolver(client, errorMitigationSolver, mitigationId,
        MITIGATION_CONFIG.getId());
    var solvedDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), OPTIMIZATION_CONFIG);
    ApiTestHelper.testSolution(solvedDto);
    assertTrue(solvedDto.getSolution().getSolutionData().contains("OPENQASM"));
  }
}
