package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration.KNAPSACK;

import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
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
public class KnapsackSolversTest {
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
    var problemManager = problemManagerProvider.findProblemManagerForType(KNAPSACK).get();

    return ApiTestHelper.getAllArgumentCombinations(problemManager)
            .map(list -> Arguments.of(list.get(0), list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testKnapsackSolver(ProblemSolver<String, String> solver, String input) {
    var problem = ApiTestHelper.createProblem(client, solver, input, KNAPSACK);
    ApiTestHelper.testSolution(problem);
  }
}