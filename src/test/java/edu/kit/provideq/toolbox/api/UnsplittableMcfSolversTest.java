package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.unsplittablemcf.UnsplittableMcfConfiguration.UNSPLITTABLE_MCF;

import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.qubo.solvers.GamsQuboSolver;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for Unsplittable Multi Commodity Flow solvers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
class UnsplittableMcfSolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300)) // Longer timeout for MCF solving
        .build();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideArguments() {
    var problemManager = problemManagerProvider.findProblemManagerForType(UNSPLITTABLE_MCF).get();
    var quboProblemManager = problemManagerProvider.findProblemManagerForType(QUBO).get();

    // TODO: For now just allow the Gams Qubo solver until the Qubo solution format is standardized
    var quboSolvers = quboProblemManager.getSolvers().stream()
        .filter(GamsQuboSolver.class::isInstance)
        .toList();

    return ApiTestHelper.getAllArgumentCombinations(problemManager, quboSolvers)
            .map(list -> Arguments.of(list.get(0), list.get(1), list.get(2)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testUnsplittableMcfSolver(ProblemSolver<String, String> solver,
                                 String input,
                                 ProblemSolver<String, String> quboSolver) {
    var problem = ApiTestHelper.createProblem(client, solver, input, UNSPLITTABLE_MCF);

    // Configure qubo solver for possible subroutines
    for (SubProblemReferenceDto subProblem : problem.getSubProblems()) {
      var subProblemTypeId = subProblem.getSubRoutine().getTypeId();
      for (String subProblemId : subProblem.getSubProblemIds()) {
        ApiTestHelper.setProblemSolver(client, quboSolver, subProblemId, subProblemTypeId);
      }
    }

    problem = ApiTestHelper.trySolveFor(120, client, problem.getId(), UNSPLITTABLE_MCF);
    ApiTestHelper.testSolution(problem);
  }
}
