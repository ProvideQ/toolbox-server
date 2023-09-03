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

  Stream<Arguments> provideAnomalySolverIds() {
    return Stream.concat(
            voidMetaSolver.getAllSolvers()
                    .stream()
                    .map(x -> Arguments.of(x.getClass().getName(), ProblemType.FEATURE_MODEL_ANOMALY_VOID, SOLVED)),
            deadFeatureMetaSolver.getAllSolvers()
                    .stream()
                    .map(x -> Arguments.of(x.getClass().getName(), ProblemType.FEATURE_MODEL_ANOMALY_DEAD, SOLVED)));
  }

  @ParameterizedTest
  @MethodSource("provideAnomalySolverIds")
  void testFeatureModelAnomalySolver(
      Class<? extends ProblemSolver<String, String>> solver,
      ProblemType anomalyType,
      SolutionStatus expectedStatus) {
    var req = new SolveFeatureModelRequest();
    req.requestedSolverId = solver.getName();
    req.requestContent = """
        namespace Sandwich
                        
        features
            Sandwich {extended__}   \s
                mandatory
                    Bread   \s
                        alternative
                            "Full Grain" {Calories 203, Price 1.99, Organic true}
                            Flatbread {Calories 90, Price 0.79, Organic true}
                            Toast {Calories 250, Price 0.99, Organic false}
                optional
                    Cheese   \s
                        optional
                            Gouda   \s
                                alternative
                                    Sprinkled {Fat {value 35, unit "g"}}
                                    Slice {Fat {value 35, unit "g"}}
                            Cheddar
                            "Cream Cheese"
                    Meat   \s
                        or
                            "Salami" {Producer "Farmer Bob"}
                            Ham {Producer "Farmer Sam"}
                            "Chicken Breast" {Producer "Farmer Sam"}
                    Vegetables   \s
                        optional
                            "Cucumber"
                            Tomatoes
                            Lettuce
        """;

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
