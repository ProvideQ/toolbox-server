package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.sat.SatConfiguration.SAT;
import static edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration.SHARPSAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.solvers.QrispExactGroverSolver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(20))
        .build();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideArguments() {
    var problemManager = problemManagerProvider.findProblemManagerForType(SAT).get();
    return ApiTestHelper.getAllArgumentCombinations(problemManager)
        .map(list -> Arguments.of(list.get(0), list.get(1)));
  }

  /**
   * Tests standard SAT solvers (excluding Qrisp) on all example inputs.
   */
  @ParameterizedTest
  @MethodSource("provideArguments")
  void testSatSolver(ProblemSolver<String, DimacsCnfSolution> solver, String input) {
    Assumptions.assumeFalse(solver instanceof QrispExactGroverSolver,
        "Skipping QrispExactGroverSolver in testSatSolver.");
    var problem = ApiTestHelper.createProblem(client, solver, input, SAT);
    ApiTestHelper.testSolution(problem);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideQrispExactGroverArguments() {
    var satManager = problemManagerProvider.findProblemManagerForType(SAT).get();

    var qrispSolvers = satManager.getSolvers().stream()
        .filter(s -> s instanceof QrispExactGroverSolver)
        .toList();

    var exampleInputs = satManager.getExampleInstances().stream()
        .flatMap(e -> e.getInput().stream())
        .toList();

    var sharpSatManager = problemManagerProvider.findProblemManagerForType(SHARPSAT).get();
    var sharpSatSolvers = sharpSatManager.getSolvers();

    List<Arguments> result = new ArrayList<>();
    for (var qrisp : qrispSolvers) {
      for (String input : exampleInputs) {
        for (var subSolver : sharpSatSolvers) {
          result.add(Arguments.of(qrisp, SAT, input, subSolver));
        }
      }
    }
    return result.stream();
  }
  @ParameterizedTest
  @MethodSource("provideQrispExactGroverArguments")
  void testQrispExactGroverSolver(
      ProblemSolver<String, DimacsCnfSolution> qrispSolver,
      ProblemType<String, DimacsCnfSolution> problemType,
      String problemInput,
      ProblemSolver<String, Integer> sharpSatSolver
  ) {
    var problemDto = ApiTestHelper.createProblem(client, qrispSolver, problemInput, problemType);
    assertEquals(ProblemState.SOLVING, problemDto.getState(),
        "Qrisp problem must be SOLVING.");

    var subProblems = problemDto.getSubProblems();
    assertFalse(subProblems.isEmpty(),
        "QrispExactGroverSolver must produce at least one SharpSAT sub-problem.");

    for (SubProblemReferenceDto subRef : subProblems) {
      assertEquals(SHARPSAT.getId(), subRef.getSubRoutine().getTypeId(),
          "Sub-problem should be assigned to SharpSAT routine.");

      for (String subProblemId : subRef.getSubProblemIds()) {
        var assignedSubProblem = ApiTestHelper.setProblemSolver(
            client,
            sharpSatSolver,
            subProblemId,
            SHARPSAT.getId()
        );
        ApiTestHelper.testSolution(assignedSubProblem);
      }
    }
    var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), problemType);
    ApiTestHelper.testSolution(solvedProblemDto);
  }
}
