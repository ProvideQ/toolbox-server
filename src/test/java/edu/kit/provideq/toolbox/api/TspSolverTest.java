package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.tsp.TspConfiguration.TSP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QrispQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QuboSolver;
import edu.kit.provideq.toolbox.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
class TspSolverTest {

  @Autowired
  private WebTestClient client;
  @Autowired
  private ProblemManagerProvider problemManagerProvider;
  @Autowired
  private LkhTspSolver lkhTspSolver;
  @Autowired
  private QuboTspSolver quboTspSolver;
  @Autowired
  private DwaveQuboSolver dwaveQuboSolver;
  @Autowired
  private QrispQuboSolver qrispQuboSolver;

  private ProblemManager<String, String> problemManager;
  private List<String> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(60))
        .build();
    problemManager = problemManagerProvider.findProblemManagerForType(TSP).get();
    problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * Solves tsp problem with LKH-3.
   */
  @Test
  void testLkhTspSolver() {
    for (String problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, lkhTspSolver, problem, TSP);
      assertEquals(ProblemState.SOLVED, problemDto.getState(), ApiTestHelper.getDebugText(problemDto));
      assertNotNull(problemDto.getSolution(), ApiTestHelper.getDebugText(problemDto));
      assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus(), ApiTestHelper.getDebugText(problemDto));
    }
  }

  private Stream<Arguments> quboSolvers() {
    return Stream.of(
        Arguments.of(dwaveQuboSolver, "NAME : small sample"),
        Arguments.of(qrispQuboSolver, "NAME : VerySmallSample")
    );
  }

  /**
   * Transforms Tsp Problem to QUBO and then solves it with a Quantum Annealer solver.
   */
  @ParameterizedTest
  @MethodSource("quboSolvers")
  void testQuboTspSolver(QuboSolver solver, String problemName) {
    //get the small problem, cause quantum simulation is used:
    var problem = problems.stream().filter(element -> element.contains(problemName)).findFirst();
    assertTrue(problem.isPresent());

    var problemDto = ApiTestHelper.createProblem(client, quboTspSolver, problem.get(), TSP);
    assertEquals(ProblemState.SOLVING, problemDto.getState());

    //Set a QUBO solver:
    var quboSubProblem = problemDto.getSubProblems().get(0).getSubProblemIds();
    ApiTestHelper.setProblemSolver(
        client,
        solver,
        quboSubProblem.get(0),
        QUBO.getId()
    );

    //solve problem:
    var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), TSP);

    //validate result:
    assertEquals(ProblemState.SOLVED, solvedProblemDto.getState(), ApiTestHelper.getDebugText(problemDto));
    assertNotNull(solvedProblemDto.getSolution(), ApiTestHelper.getDebugText(problemDto));
    assertEquals(SolutionStatus.SOLVED, solvedProblemDto.getSolution().getStatus(),
            ApiTestHelper.getDebugText(problemDto));
  }
}
