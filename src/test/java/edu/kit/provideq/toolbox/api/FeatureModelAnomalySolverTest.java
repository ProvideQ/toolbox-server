package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.featuremodel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.featuremodel.anomaly.MetaSolverFeatureModelAnomaly;
import edu.kit.provideq.toolbox.featuremodel.anomaly.solvers.FeatureModelAnomalySolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Stream;

import static edu.kit.provideq.toolbox.SolutionStatus.SOLVED;
import static org.hamcrest.Matchers.is;

@WebFluxTest
@Import(value = {
        SolveRouter.class,
        MetaSolverProvider.class,
        MetaSolverFeatureModelAnomaly.class,
        FeatureModelAnomalySolver.class,
        SubRoutinePool.class
})
public class FeatureModelAnomalySolverTest {
  @Autowired
  private WebTestClient client;

  public static Stream<Arguments> provideAnomalySolverIds() {
    String solverId = FeatureModelAnomalySolver.class.getName();
    return Stream.of(
        Arguments.of(solverId, "void", SOLVED),
        Arguments.of(solverId, "dead", SOLVED),

        // not implemented yet, change to SOLVED when they have been implemented!
        Arguments.of(solverId, "false-optional", SolutionStatus.INVALID),
        Arguments.of(solverId, "redundant-constraints", SolutionStatus.INVALID)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAnomalySolverIds")
  void testFeatureModelAnomalySolver(String solverId, String anomalyType,
                                     SolutionStatus expectedStatus) {
    var req = new SolveFeatureModelRequest();
    req.requestedSolverId = solverId;
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
            .uri("/solve/feature-model-anomaly/" + anomalyType) // FIXME type of anomaly?
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .exchange();

    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {})
            .value(Solution::getStatus, is(expectedStatus));
  }
}
