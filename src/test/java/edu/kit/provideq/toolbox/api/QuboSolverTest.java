package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
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
class QuboSolverTest {
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

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideArguments() {
    var problemManager = problemManagerProvider.findProblemManagerForType(QUBO).get();

    return ApiTestHelper.getAllArgumentCombinations(problemManager)
        .map(list -> Arguments.of(list.get(0), list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testQuboSolvers(ProblemSolver<String, String> solver, String input) {
    System.out.println("Testing Solver: " + solver.getName());
    var problemDto = ApiTestHelper.createProblem(client, solver, input, QUBO);
    assertEquals(ProblemState.SOLVED, problemDto.getState(),
            ApiTestHelper.getDebugText(problemDto));
    assertNotNull(problemDto.getSolution(), ApiTestHelper.getDebugText(problemDto));
    assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus(),
            ApiTestHelper.getDebugText(problemDto));
  }
}
