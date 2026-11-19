package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.qubo.QuboFamily;
import edu.kit.provideq.toolbox.qubo.QuboSolutionFamily;
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
class QuboSolverTest {
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

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  Stream<Arguments> provideArguments() {
    var problemManager = problemManagerProvider.findProblemManagerForType(QUBO).get();

    return ApiTestHelper.getAllArgumentCombinations(problemManager)
        .map(list -> Arguments.of(list.getFirst(), list.get(1)));
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testQuboSolvers(
      ProblemSolver<QuboFamily, QuboSolutionFamily> solver, FormattedValue<?> input) {
    var problemDto = ApiTestHelper.createProblem(client, solver, input, QUBO);
    ApiTestHelper.testSolution(problemDto, problemManagerProvider, conversionRegistry);
  }
}