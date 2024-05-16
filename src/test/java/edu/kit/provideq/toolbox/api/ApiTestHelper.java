package edu.kit.provideq.toolbox.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
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
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
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

  @SafeVarargs
  public static <T> Stream<T> concatAll(Stream<T>... streams) {
    return Arrays.stream(streams).reduce(Stream::concat).orElseThrow();
  }

  public static <InputT, ResultT> ProblemDto<InputT, ResultT> createProblem(
          WebTestClient client,
          ProblemSolver<InputT, ResultT> solver,
          InputT input,
          ProblemType<InputT, ResultT> problemType) {
    // Initialize mock solver
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getSolverId()).thenReturn(solver.getId());
    Mockito.when(problemDtoMock.getInput()).thenReturn(input);
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    var problem = client.post()
            .uri("/problems/" + problemType.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(problemDtoMock)
            .exchange()
            .expectStatus().isOk()
            .expectBody(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {
            })
            .returnResult()
            .getResponseBody();

    assertNotNull(problem);
    assertEquals(input, problem.getInput());

    return problem;
  }

  public static <InputT, ResultT> ProblemDto<InputT, ResultT> setProblemSolver(
          WebTestClient client,
          ProblemSolver<InputT, ResultT> solver,
          String problemId,
          String problemTypeId) {
    // Set subroutine solver and state
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getSolverId()).thenReturn(solver.getId());
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    var problem = client.patch()
            .uri("/problems/" + problemTypeId + "/" + problemId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(problemDtoMock)
            .exchange()
            .expectStatus().isOk()
            .expectBody(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {
            })
            .returnResult()
            .getResponseBody();

    assertNotNull(problem);

    return problem;
  }

  @SuppressWarnings("BusyWait")
  public static <InputT, ResultT> ProblemDto<InputT, ResultT> trySolveFor(
          long seconds,
          WebTestClient client,
          String problemId,
          ProblemType<InputT, ResultT> problemType) {
    AtomicBoolean hasTimeout = new AtomicBoolean(false);
    // Timeout throwing exception in x seconds
    Mono.delay(Duration.ofSeconds(seconds))
            .flux()
            .doOnNext(x -> {
              hasTimeout.set(true);
            })
            .subscribe();

    // Wait for problem 10 times
    long waitMilliseconds = seconds * 1000 / 10;

    ProblemDto<InputT, ResultT> problemDto;
    while (true) {
      System.out.println("Checking if problem is solved...");
      problemDto = client.get()
              .uri("/problems/" + problemType.getId() + "/" + problemId)
              .exchange()
              .expectStatus().isOk()
              .expectBody(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {
              })
              .returnResult()
              .getResponseBody();

      assertNotNull(problemDto);

      if (problemDto.getState() == ProblemState.SOLVED) {
        break;
      }

      for (SubProblemReferenceDto subProblem : problemDto.getSubProblems()) {
        String subProblemTypeId = subProblem.getSubRoutine().getTypeId();
        for (String subProblemId : subProblem.getSubProblemIds()) {
          var subProblemDto = client.get()
                  .uri("/problems/" + subProblemTypeId + "/" + subProblemId)
                  .exchange()
                  .expectStatus().isOk()
                  .expectBody(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {
                  })
                  .returnResult()
                  .getResponseBody();

          assertNotNull(subProblemDto);
        }
      }

      System.out.println("Problem not solved yet. Waiting 5 seconds...");

      try {
        Thread.sleep(waitMilliseconds);
      } catch (InterruptedException ignored) {
        // Ignore
      }

      if (hasTimeout.get()) {
        throw new IllegalStateException("Problem did not solve in time: " + problemDto);
      }
    }

    System.out.println(problemDto.getSolution());
    assertEquals(ProblemState.SOLVED, problemDto.getState());
    Assertions.assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());

    return problemDto;
  }
}
