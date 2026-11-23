package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorFamily;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorInputFormat;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorReportFamily;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class DemonstratorTest {
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
    var problemManager = problemManagerProvider.findProblemManagerForType(DEMONSTRATOR).get();

    return problemManager
        .getSolvers()
        .stream()
        .map(Arguments::of);
  }

  /**
   * Tests both demonstrators with empty input.
   *
   * <p>Empty input is the expected contract here: both demonstrators (CPLEX MIP scaling
   * benchmark and molecule-energy simulation) generate their own workload from solver
   * settings and ignore the submitted problem input.
   */
  @ParameterizedTest
  @MethodSource("provideArguments")
  void testDemonstratorsWithEmptyInput(
      ProblemSolver<DemonstratorFamily, DemonstratorReportFamily> solver) {
    var problem = ApiTestHelper.createProblem(client, solver,
        FormattedValue.of("", DemonstratorInputFormat.INSTANCE), DEMONSTRATOR);
    ApiTestHelper.testSolution(problem, problemManagerProvider, conversionRegistry);
  }
}
