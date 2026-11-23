package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead.DeadFeatureConfiguration.FEATURE_MODEL_ANOMALY_DEAD;
import static edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID;
import static edu.kit.provideq.toolbox.solvers.sat.SatConfiguration.SAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.sat.SatFamily;
import edu.kit.provideq.formats.sat.SatSolutionFamily;
import edu.kit.provideq.formats.uvl.FeatureModelFamily;
import edu.kit.provideq.toolbox.api.dto.SubProblemReferenceDto;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead.formats.DeadFeatureSolutionFamily;
import edu.kit.provideq.toolbox.solvers.sat.solvers.QrispExactGroverSolver;
import edu.kit.provideq.toolbox.solvers.sat.solvers.QrispGroverSolver;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class FeatureModelAnomalySolversTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ConversionRegistry conversionRegistry;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
              .responseTimeout(Duration.ofSeconds(300))
              .build();
  }

  Stream<Arguments> provideArguments() {
    // Return combined stream
    return ApiTestHelper.concatAll(
              getArguments(FEATURE_MODEL_ANOMALY_VOID),
              getArguments(FEATURE_MODEL_ANOMALY_DEAD));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  <InT extends FormatFamily, OutT extends FormatFamily> Stream<Arguments> getArguments(
          ProblemType<InT, OutT> problemType
  ) {
    var featureModelManager = problemManagerProvider.findProblemManagerForType(problemType).get();
    var satManager = problemManagerProvider.findProblemManagerForType(SAT).get();

    var satSolver = satManager.getSolvers().stream()
        .filter(solver -> !(solver instanceof QrispGroverSolver))
        .filter(solver -> !(solver instanceof QrispExactGroverSolver))
        .toList();

    return ApiTestHelper.getAllArgumentCombinations(featureModelManager, satSolver)
            .map(list -> Arguments.of(
                    list.getFirst(),
                    problemType,
                    list.get(1),
                    list.get(2)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testFeatureModelAnomalySolver(
        ProblemSolver<FeatureModelFamily, DeadFeatureSolutionFamily> featureModelSolver,
        ProblemType<FeatureModelFamily, DeadFeatureSolutionFamily> problemType,
        FormattedValue<?> input,
        ProblemSolver<SatFamily, SatSolutionFamily> satSolver) {

    var problem = ApiTestHelper.createProblem(client, featureModelSolver, input, problemType);
    assertEquals(ProblemState.SOLVING, problem.getState());
    problem = ApiTestHelper.waitForSubProblems(30, client, problem, problemType);

    // Set solver for sat sub problem
    for (SubProblemReferenceDto subProblem : problem.getSubProblems()) {
      var subProblemTypeId = subProblem.getSubRoutine().getTypeId();
      for (String subProblemId : subProblem.getSubProblemIds()) {
        ApiTestHelper.setProblemSolver(client, satSolver, subProblemId, subProblemTypeId);
      }
    }

    problem = ApiTestHelper.trySolveFor(120, client, problem.getId(), problemType);
    ApiTestHelper.testSolution(problem, problemManagerProvider, conversionRegistry);
  }
}
