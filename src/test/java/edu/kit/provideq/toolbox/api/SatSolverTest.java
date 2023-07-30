package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.*;
import edu.kit.provideq.toolbox.maxcut.MetaSolverMaxCut;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import edu.kit.provideq.toolbox.sat.SolveSatRequest;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import org.junit.jupiter.params.ParameterizedTest;
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
        MetaSolverSat.class,
        GamsSatSolver.class,
        SubRoutinePool.class,
        GamsProcessRunner.class,
        ResourceProvider.class
})
public class SatSolverTest {
  @Autowired
  private WebTestClient client;

  public static Stream<String> provideSatSolverIds() {
    return Stream.of(
        GamsSatSolver.class.getName()
    );
  }

  @ParameterizedTest
  @MethodSource("provideSatSolverIds")
  void testSatSolver(String solverId) {
    var req = new SolveSatRequest();
    req.requestedSolverId = solverId;
    req.requestContent = "a and b";

    var response = client.post()
            .uri("/solve/sat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .exchange();

    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {})
            .value(Solution::getStatus, is(SOLVED));
  }
}
