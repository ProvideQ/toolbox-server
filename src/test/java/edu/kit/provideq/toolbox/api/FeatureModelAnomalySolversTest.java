package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.featuremodel.anomaly.dead.DeadFeatureConfiguration.FEATURE_MODEL_ANOMALY_DEAD;
import static edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel.VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID;
import static edu.kit.provideq.toolbox.sat.SatConfiguration.SAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.solvers.QrispExactGroverSolver;
import edu.kit.provideq.toolbox.sat.solvers.QrispGroverSolver;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
class FeatureModelAnomalySolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
              .responseTimeout(Duration.ofSeconds(60))
              .build();
  }

  Stream<Arguments> provideArguments() {
    // Return combined stream
    return ApiTestHelper.concatAll(
              getArguments(FEATURE_MODEL_ANOMALY_VOID),
              getArguments(FEATURE_MODEL_ANOMALY_DEAD));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  <InputT, ResultT> Stream<Arguments> getArguments(
          ProblemType<InputT, ResultT> problemType
  ) {
    var featureModelManager = problemManagerProvider.findProblemManagerForType(problemType).get();
    var satManager = problemManagerProvider.findProblemManagerForType(SAT).get();

    var satSolver = satManager.getSolvers().stream()
        .filter(solver -> !(solver instanceof QrispGroverSolver))
        .filter(solver -> !(solver instanceof QrispExactGroverSolver))
        .toList();

    return ApiTestHelper.getAllArgumentCombinations(featureModelManager, satSolver)
            .map(list -> Arguments.of(
                    list.get(0),
                    problemType,
                    list.get(1),
                    list.get(2)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testFeatureModelAnomalySolver(
        ProblemSolver<String, String> featureModelSolver,
        ProblemType<String, String> problemType,
        String input,
        ProblemSolver<String, DimacsCnfSolution> satSolver) {

    var problem = ApiTestHelper.createProblem(client, featureModelSolver, input, problemType);
    assertEquals(ProblemState.SOLVING, problem.getState());

    // Set solver for sat sub problem
    for (SubProblemReferenceDto subProblem : problem.getSubProblems()) {
      var subProblemTypeId = subProblem.getSubRoutine().getTypeId();
      for (String subProblemId : subProblem.getSubProblemIds()) {
        ApiTestHelper.setProblemSolver(client, satSolver, subProblemId, subProblemTypeId);
      }
    }

    problem = ApiTestHelper.trySolveFor(15, client, problem.getId(), problemType);
    ApiTestHelper.testSolution(problem);
  }
}
