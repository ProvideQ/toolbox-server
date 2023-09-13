package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.SolutionStatus.SOLVED;
import static org.hamcrest.Matchers.is;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.MetaSolverHelper;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import edu.kit.provideq.toolbox.sat.SolveSatRequest;
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
    MetaSolverSat.class,
    GamsSatSolver.class,
    SubRoutinePool.class,
    GamsProcessRunner.class,
    ResourceProvider.class
})
class SatSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private MetaSolverSat metaSolverSat;

  Stream<Arguments> provideArguments() {
    return MetaSolverHelper.getAllArgumentCombinations(metaSolverSat)
            .map(list -> Arguments.of(list.get(0), list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testSatSolver(String solverId, String content) {
    var req = new SolveSatRequest();
    req.requestedSolverId = solverId;
    req.requestContent = content;

    var response = client.post()
        .uri("/solve/sat")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .exchange();

    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {
        })
        .value(Solution::getStatus, is(SOLVED));
  }
}
