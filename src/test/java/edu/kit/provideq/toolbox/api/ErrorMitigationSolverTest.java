package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.circuit.processing.solvers.mitigation.ErrorMitigationConfiguration.MITIGATION_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.toolbox.circuit.processing.solvers.mitigation.ErrorMitigationSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorMitigationSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ErrorMitigationSolver errorMitigationSolver;

  private ProblemManager<String, String> problemManager;
  private List<String> problems;

  @BeforeEach
  void beforeEach() {
    problemManager = problemManagerProvider.findProblemManagerForType(MITIGATION_CONFIG).get();
    problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Test
  void testErrorMitigationSolver() {
    var circuit = problems.get(0);
    var problem = ApiTestHelper.createProblem(client, errorMitigationSolver, circuit, MITIGATION_CONFIG);
    ApiTestHelper.testSolution(problem);
    assertEquals(circuit, problem.getSolution().getSolutionData());
  }
}
