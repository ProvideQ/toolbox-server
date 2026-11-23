package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.solvers.unsplittablemcf.UnsplittableMcfConfiguration.UNSPLITTABLE_MCF;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.solvers.qubo.QuboFamily;
import edu.kit.provideq.toolbox.solvers.qubo.QuboSolutionFamily;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.UmcfFamily;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.UmcfSolutionFamily;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for Unsplittable Multi Commodity Flow solvers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class UnsplittableMcfSolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ConversionRegistry conversionRegistry;

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

    // all QUBO solvers can be used as sub-routines: they all emit the standardized
    // lp-solution format. The Qiskit/Qrisp QAOA solvers are excluded: the UMCF example
    // instance reformulates into a QUBO with 46 binary variables, which cannot be handled
    // by statevector-based QAOA simulation (2^46 amplitudes) and would never terminate.
    var quboSolvers = quboProblemManager.getSolvers().stream()
        .filter(solver -> !solver.getId().equals("QiskitQuboSolver")
            && !solver.getId().equals("QrispQuboSolver"))
        .toList();

    return ApiTestHelper.getAllArgumentCombinations(problemManager, quboSolvers)
            .map(list -> Arguments.of(list.getFirst(), list.get(1), list.get(2)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testUnsplittableMcfSolver(ProblemSolver<UmcfFamily, UmcfSolutionFamily> solver,
                                 FormattedValue<?> input,
                                 ProblemSolver<QuboFamily, QuboSolutionFamily> quboSolver) {
    var problem = ApiTestHelper.createProblem(client, solver, input, UNSPLITTABLE_MCF);

    if (solver.getSubRoutines().isEmpty()) {
      // solvers without sub-routines (e.g. the classical GAMS solver) solve directly and
      // never create sub-problems
      problem = ApiTestHelper.trySolveFor(120, client, problem.getId(), UNSPLITTABLE_MCF);
    } else {
      problem = ApiTestHelper.waitForSubProblems(30, client, problem, UNSPLITTABLE_MCF);

      // Configure qubo solver for possible subroutines
      for (SubProblemReferenceDto subProblem : problem.getSubProblems()) {
        var subProblemTypeId = subProblem.getSubRoutine().getTypeId();
        for (String subProblemId : subProblem.getSubProblemIds()) {
          ApiTestHelper.setProblemSolver(client, quboSolver, subProblemId, subProblemTypeId);
        }
      }

      problem = ApiTestHelper.trySolveFor(120, client, problem.getId(), UNSPLITTABLE_MCF);
    }

    ApiTestHelper.testSolution(problem, problemManagerProvider, conversionRegistry);
  }
}