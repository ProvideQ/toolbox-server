package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.SolutionStatus.SOLVED;
import static org.hamcrest.Matchers.is;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.featuremodel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.featuremodel.anomaly.dead.DeadFeatureMetaSolver;
import edu.kit.provideq.toolbox.featuremodel.anomaly.dead.SatBasedDeadFeatureSolver;
import edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel.SatBasedVoidFeatureSolver;
import edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel.VoidFeatureMetaSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.MetaSolverHelper;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebFluxTest
@Import(value = {
    SolveRouter.class,
    MetaSolverProvider.class,
    DeadFeatureMetaSolver.class,
    SatBasedDeadFeatureSolver.class,
    VoidFeatureMetaSolver.class,
    SatBasedVoidFeatureSolver.class,
    SubRoutinePool.class,
    MetaSolverSat.class,
    GamsSatSolver.class,
    GamsProcessRunner.class,
    ResourceProvider.class,
})
class FeatureModelAnomalySolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private VoidFeatureMetaSolver voidMetaSolver;

  @Autowired
  private DeadFeatureMetaSolver deadFeatureMetaSolver;

  Stream<Arguments> provideArguments() {
    // Return combined stream
    return Stream.concat(
            getArguments(voidMetaSolver, ProblemType.FEATURE_MODEL_ANOMALY_VOID),
            getArguments(deadFeatureMetaSolver, ProblemType.FEATURE_MODEL_ANOMALY_DEAD));
  }

  static Stream<Arguments> getArguments(MetaSolver<?, ?, ?> metaSolver, ProblemType problemType) {
    return MetaSolverHelper.getAllArgumentCombinations(metaSolver)
            .map(list -> Arguments.of(
                    list.get(0),
                    problemType,
                    SOLVED,
                    list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testFeatureModelAnomalySolver(
      Class<? extends ProblemSolver<String, String>> solver,
      ProblemType anomalyType,
      SolutionStatus expectedStatus,
      String content) {
    var req = new SolveFeatureModelRequest();
    req.requestedSolverId = solver.getName();
    req.requestContent = content;

    var response = client.post()
        .uri("/solve/" + anomalyType.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .exchange();

    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {
        })
        .value(Solution::getStatus, is(expectedStatus));
  }
}
