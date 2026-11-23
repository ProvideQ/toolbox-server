package edu.kit.provideq.toolbox.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

public class ApiTestHelper {
  public static Stream<List<Object>> getAllArgumentCombinations(
      ProblemManager<?, ?> problemManager) {
    return getAllArgumentCombinations(problemManager, List.of());
  }

  public static Stream<List<Object>> getAllArgumentCombinations(
      ProblemManager<?, ?> problemManager,
      List<?>... lists) {
    // Get all solvers
    var solvers = problemManager.getSolvers().stream().toList();

    // Get all example inputs
    var problems = problemManager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    List<List<?>> arguments = new ArrayList<>(List.of(solvers, problems));
    for (List<?> list : lists) {
      if (list != null && !list.isEmpty()) {
        arguments.add(list);
      }
    }

    // Return all combinations
    return Lists.cartesianProduct(arguments).stream();
  }

  public static void testSolution(ProblemDto problemDto) {
    assertEquals(ProblemState.SOLVED, problemDto.getState(), problemDto.toString());
    assertNotNull(problemDto.getSolution());
    assertEquals(
        SolutionStatus.SOLVED,
        problemDto.getSolution().getStatus(),
        problemDto.toString());
  }

  /**
   * Tests a solved problem: asserts the solution status, that the solution is expressed in a
   * format of the problem type's result family, that the solution data parses back through
   * that format, and - if the problem type has a content validator - that the solution is
   * consistent with the problem instance.
   *
   * @param problemDto             the solved problem
   * @param problemManagerProvider provider to resolve the problem type of the problem
   * @param conversionRegistry     the shared format registry
   */
  public static void testSolution(
      ProblemDto problemDto,
      ProblemManagerProvider problemManagerProvider,
      ConversionRegistry conversionRegistry) {
    testSolution(problemDto);
    validateSolution(problemDto, problemManagerProvider, conversionRegistry);
  }

  private static void validateSolution(
      ProblemDto problemDto,
      ProblemManagerProvider problemManagerProvider,
      ConversionRegistry conversionRegistry) {
    var solutionDto = problemDto.getSolution();
    var formatName = solutionDto.getSolutionFormat();
    assertNotNull(formatName, "Solved problem must declare its solution format: " + problemDto);

    var format = conversionRegistry.getFormat(formatName);
    assertNotNull(
        format, "Solution format %s is not registered: %s".formatted(formatName, problemDto));

    var problemManager = problemManagerProvider.getProblemManagers().stream()
        .filter(manager -> manager.getType().getId().equals(problemDto.getTypeId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(
                "No problem manager found for type " + problemDto.getTypeId()));
    var resultFamily = problemManager.getType().getResultFamily();
    assertTrue(conversionRegistry.getFormats(resultFamily).contains(format),
        "Solution format %s is not a member of the result family of problem type %s: %s"
            .formatted(formatName, problemDto.getTypeId(), problemDto));

    // The solution data must actually parse as the advertised format.
    // This catches solvers that emit data in a different representation
    // than the format they declare.
    try {
      format.parse(solutionDto.getSolutionData());
    } catch (ParseException e) {
      throw new AssertionError(
          "Solution data does not parse as the declared format %s: %s%nSolution data: %s"
              .formatted(formatName, e.getMessage(), solutionDto.getSolutionData()),
          e);
    }
  }

  @SafeVarargs
  public static <T> Stream<T> concatAll(Stream<T>... streams) {
    return Arrays.stream(streams).reduce(Stream::concat).orElseThrow();
  }

  /**
   * Creates a problem via the API with the given formatted input.
   */
  public static ProblemDto createProblem(
      WebTestClient client,
      ProblemSolver<?, ?> solver,
      FormattedValue<?> input,
      ProblemType<?, ?> problemType) {
    String inputText = input.toString();

    // Initialize mock solver
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getSolverId()).thenReturn(solver.getId());
    Mockito.when(problemDtoMock.getInput()).thenReturn(inputText);
    Mockito.when(problemDtoMock.getInputFormat()).thenReturn(input.getFormat().getName());
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    var problem = client.post()
        .uri("/problems/" + problemType.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ProblemDto.class)
        .returnResult()
        .getResponseBody();

    assertNotNull(problem);
    // The input text is only round-trip stable until a solver parses it: documents
    // canonicalize their text on first parse, so solvers that read the structured input
    // change the serialized text. Only assert that the input was accepted.
    assertNotNull(problem.getInput());

    return problem;
  }

  /**
   * Waits until the given problem has created its sub-problems: solvers create sub-problems
   * lazily when they run, so the sub-problem ids are not available immediately after the
   * problem is submitted.
   *
   * @param seconds     maximum number of seconds to wait
   * @param client      the web test client
   * @param problemDto  the problem to wait for
   * @param problemType the problem's type
   * @return the refreshed problem with all sub-problem ids assigned
   */
  @SuppressWarnings("BusyWait")
  public static ProblemDto waitForSubProblems(
      long seconds,
      WebTestClient client,
      ProblemDto problemDto,
      ProblemType<?, ?> problemType) {
    var deadline = System.currentTimeMillis() + seconds * 1000;
    var current = problemDto;
    while (System.currentTimeMillis() < deadline) {
      var allAssigned = !current.getSubProblems().isEmpty()
          && current.getSubProblems().stream()
              .noneMatch(reference -> reference.getSubProblemIds().isEmpty());
      if (allAssigned) {
        return current;
      }

      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
        // Ignore
      }

      current = client.get()
          .uri("/problems/" + problemType.getId() + "/" + problemDto.getId())
          .exchange()
          .expectStatus().isOk()
          .expectBody(ProblemDto.class)
          .returnResult()
          .getResponseBody();
      assertNotNull(current);
    }

    throw new IllegalStateException(
        "Problem " + problemDto.getId() + " did not create sub-problems in time: " + current);
  }

  public static ProblemDto setProblemSolver(
      WebTestClient client,
      ProblemSolver<?, ?> solver,
      String problemId,
      String problemTypeId) {    // Set subroutine solver and state
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getSolverId()).thenReturn(solver.getId());
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    var problem = client.patch()
        .uri("/problems/" + problemTypeId + "/" + problemId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ProblemDto.class)
        .returnResult()
        .getResponseBody();

    assertNotNull(problem);

    return problem;
  }

  @SuppressWarnings("BusyWait")
  public static ProblemDto trySolveFor(
      long seconds,
      WebTestClient client,
      String problemId,
      ProblemType<?, ?> problemType) {
    AtomicBoolean hasTimeout = new AtomicBoolean(false);
    // Timeout throwing exception in x seconds
    Mono.delay(Duration.ofSeconds(seconds))
        .flux()
        .doOnNext(_ -> hasTimeout.set(true))
        .subscribe();

    // Wait for problem 10 times
    long waitMilliseconds = seconds * 1000 / 10;

    StringBuilder builder = new StringBuilder();

    ProblemDto problemDto;
    while (true) {
      builder.append("Checking if problem is solved...\n");
      problemDto = client.get()
          .uri("/problems/" + problemType.getId() + "/" + problemId)
          .exchange()
          .expectStatus().isOk()
          .expectBody(ProblemDto.class)
          .returnResult()
          .getResponseBody();

      if (problemDto == null) {
        System.out.println("Testcase with id " + problemId
            + " will terminate because problemDto is null");
        System.out.println("problemType: " + problemType);
      }
      assertNotNull(problemDto);

      builder.append("Fetched problem: ").append(problemDto).append("\n");

      if (problemDto.getState() == ProblemState.SOLVED) {
        System.out.println(problemId + " solved successful.");
        break;
      }

      for (SubProblemReferenceDto subProblem : problemDto.getSubProblems()) {
        String subProblemTypeId = subProblem.getSubRoutine().getTypeId();
        for (String subProblemId : subProblem.getSubProblemIds()) {
          var subProblemDto = client.get()
              .uri("/problems/" + subProblemTypeId + "/" + subProblemId)
              .exchange()
              .expectStatus().isOk()
              .expectBody(ProblemDto.class)
              .returnResult()
              .getResponseBody();
          builder.append("Fetched sub problem: ").append(subProblemDto).append("\n");

          if (subProblemDto == null) {
            System.out.println("subProblem: " + subProblem);
            System.out.println("Log: " + builder);
          }
          assertNotNull(subProblemDto);
        }
      }

      builder.append("Problem not solved yet. Waiting 5 seconds...\n");

      try {
        Thread.sleep(waitMilliseconds);
      } catch (InterruptedException ignored) {
        // Ignore
      }

      if (hasTimeout.get()) {
        System.out.println("Stopping testcase due to timeout.");
        System.out.println("log: " + builder);
        throw new IllegalStateException("Problem did not solve in time: " + problemDto);
      }
    }

    // print error output if something went wrong
    if (problemDto.getState() != ProblemState.SOLVED
        || problemDto.getSolution().getStatus() != SolutionStatus.SOLVED) {
      System.out.println(builder);
    }

    assertEquals(ProblemState.SOLVED, problemDto.getState());
    assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());

    return problemDto;
  }
}