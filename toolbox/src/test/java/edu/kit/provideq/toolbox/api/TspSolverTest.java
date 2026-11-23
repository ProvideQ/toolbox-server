package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.solvers.tsp.TspConfiguration.TSP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.QrispQuboSolver;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.QuboSolver;
import edu.kit.provideq.toolbox.solvers.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.solvers.tsp.solvers.QuboTspSolver;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class TspSolverTest {

  @Autowired
  private WebTestClient client;
  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ConversionRegistry conversionRegistry;
  @Autowired
  private LkhTspSolver lkhTspSolver;
  @Autowired
  private QuboTspSolver quboTspSolver;
  @Autowired
  private DwaveQuboSolver dwaveQuboSolver;
  @Autowired
  private QrispQuboSolver qrispQuboSolver;

  private ProblemManager<?, ?> problemManager;
  private List<? extends FormattedValue<?>> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300))
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
    for (FormattedValue<?> problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, lkhTspSolver, problem, TSP);
      ApiTestHelper.testSolution(problemDto, problemManagerProvider, conversionRegistry);
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
    FormattedValue<?> problem = null;
    for (var element : problems) {
      if (element.toString().contains(problemName)) {
        problem = element;
        break;
      }
    }
    assertTrue(problem != null);

    var problemDto = ApiTestHelper.createProblem(client, quboTspSolver, problem, TSP);
    assertEquals(ProblemState.SOLVING, problemDto.getState(), problemDto.toString());
    problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, TSP);

    //Set a QUBO solver:
    var quboSubProblem = problemDto.getSubProblems().getFirst().getSubProblemIds();
    ApiTestHelper.setProblemSolver(
        client,
        solver,
        quboSubProblem.getFirst(),
        QUBO.getId()
    );

    //solve problem:
    var solvedProblemDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), TSP);
    ApiTestHelper.testSolution(solvedProblemDto, problemManagerProvider, conversionRegistry);
  }
}