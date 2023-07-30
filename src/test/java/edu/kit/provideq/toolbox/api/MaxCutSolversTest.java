package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.SolutionStatus.SOLVED;
import static org.hamcrest.Matchers.is;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.PythonProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.maxcut.MetaSolverMaxCut;
import edu.kit.provideq.toolbox.maxcut.SolveMaxCutRequest;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import(value = {
    SolveRouter.class,
    MetaSolverProvider.class,
    MetaSolverMaxCut.class,
    QiskitMaxCutSolver.class,
    GamsMaxCutSolver.class,
    QiskitMaxCutSolver.class,
    SubRoutinePool.class,
    GamsProcessRunner.class,
    PythonProcessRunner.class,
    ResourceProvider.class
})
class MaxCutSolversTest {
  @Autowired
  private WebTestClient client;

  public static Stream<String> provideMaxCutSolverIds() {
    return Stream.of(
        GamsMaxCutSolver.class.getName(),
        QiskitMaxCutSolver.class.getName()
    );
  }

  @ParameterizedTest
  @MethodSource("provideMaxCutSolverIds")
  void testMaxCutSolver(String solverId) {
    var req = new SolveMaxCutRequest();
    req.requestedSolverId = solverId;
    req.requestContent = """
        graph [
            id 42
            node [
                id 1
                label "1"
            ]
            node [
                id 2
                label "2"
            ]
            node [
                id 3
                label "3"
            ]
            edge [
                source 1
                target 2
            ]
            edge [
                source 2
                target 3
            ]
            edge [
                source 3
                target 1
            ]
        ]""";

    var response = client.post()
        .uri("/solve/max-cut")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .exchange();

    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {
        })
        .value(Solution::getStatus, is(SOLVED));
  }
}
