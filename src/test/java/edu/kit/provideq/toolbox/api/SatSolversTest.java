package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.sat.SatConfiguration.SAT;
import static edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration.SHARPSAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.sat.solvers.QrispExactGroverSolver;
import edu.kit.provideq.toolbox.sharpsat.solvers.GanakSolver;
import edu.kit.provideq.toolbox.sharpsat.solvers.PythonBruteForceSolver;
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
class SatSolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;
  private ProblemManager<String, DimacsCnfSolution> problemManager;
  private List<String> problems;
  private QrispExactGroverSolver qrispExactGroverSolver;
  private PythonBruteForceSolver pythonBruteForceSolver;
  private GanakSolver ganakSolver;

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
            .responseTimeout(Duration.ofSeconds(20))
            .build();
    problemManager = problemManagerProvider.findProblemManagerForType(SAT).get();
    problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }



  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideArguments() {
    var problemManager = problemManagerProvider.findProblemManagerForType(SAT).get();
    var satSolver = problemManager.getSolvers().stream()
        .filter(solver -> !(solver instanceof QrispExactGroverSolver))
        .toList();

    return ApiTestHelper.getAllArgumentCombinations(problemManager, satSolver)
        .map(list -> Arguments.of(list.get(0), list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testSatSolver(ProblemSolver<String, DimacsCnfSolution> solver, String input) {
    var problem = ApiTestHelper.createProblem(client, solver, input, SAT);
    ApiTestHelper.testSolution(problem);
  }

  @Test
  void testQrispExactGroverSolverWithGanak() {
    for (String problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, qrispExactGroverSolver, problem, SAT);
      assertEquals(ProblemState.SOLVING, problemDto.getState(), "Initial state must be SOLVING.");

      // QrispExactGroverSolver has only 1 sub-problem - sharpSAT
      var subProblems = problemDto.getSubProblems();
      assertFalse(subProblems.isEmpty(), "QrispExactGroverSolver has a sub-problem.");

      for (SubProblemReferenceDto subProblemRef : subProblems) {
        assertEquals(SHARPSAT.getId(),
            subProblemRef.getSubRoutine().getTypeId(),
            "Sub-problem should be a SharpSAT routine.");

        // Should be exactly 1 sub-problem ID. Set ganak for it.
        var subProblemIds = subProblemRef.getSubProblemIds();
        assertEquals(1, subProblemIds.size(), "Exactly one SharpSAT sub-problem ID expected.");

        // Set Ganak as the sub-solver
        var assignedSubProblem = ApiTestHelper.setProblemSolver(
            client,
            ganakSolver,
            subProblemIds.get(0),
            SHARPSAT.getId()
        );

        ApiTestHelper.testSolution(assignedSubProblem);
      }
      var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), SAT);
      ApiTestHelper.testSolution(solvedProblemDto);
    }
  }

  @Test
  void testQrispExactGroverSolverWithBruteForce() {
    for (String problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, qrispExactGroverSolver, problem, SAT);
      assertEquals(ProblemState.SOLVING, problemDto.getState(), "Initial state must be SOLVING.");

      var subProblems = problemDto.getSubProblems();
      assertFalse(subProblems.isEmpty(), "QrispExactGroverSolver has a sub-problem.");

      for (SubProblemReferenceDto subProblemRef : subProblems) {
        assertEquals(SHARPSAT.getId(),
            subProblemRef.getSubRoutine().getTypeId(),
            "Sub-problem should be a SharpSAT routine.");

        var subProblemIds = subProblemRef.getSubProblemIds();
        assertEquals(1, subProblemIds.size(), "Exactly one SharpSAT sub-problem ID expected.");

        var assignedSubProblem = ApiTestHelper.setProblemSolver(
            client,
            pythonBruteForceSolver,
            subProblemIds.get(0),
            SHARPSAT.getId()
        );

        ApiTestHelper.testSolution(assignedSubProblem);
      }
      var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), SAT);
      ApiTestHelper.testSolution(solvedProblemDto);
    }
  }

}
