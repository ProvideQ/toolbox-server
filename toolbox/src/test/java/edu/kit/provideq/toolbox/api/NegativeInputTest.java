package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration.KNAPSACK;
import static edu.kit.provideq.toolbox.sat.SatConfiguration.SAT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.ProblemState;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests that the API rejects invalid problem inputs with a client error instead of
 * accepting them and failing during solving.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class NegativeInputTest {
  @Autowired
  private WebTestClient client;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(60))
        .build();
  }

  /**
   * Tests that unparseable input does not produce a successful solution: the knapsack format
   * parses lazily, so the problem is accepted but the solution must end up failed or invalid.
   */
  @Test
  void failsOnUnparseableInput() throws Exception {
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getSolverId()).thenReturn("PythonKnapsackSolver");
    Mockito.when(problemDtoMock.getInput()).thenReturn("this is not a knapsack instance !!!");
    Mockito.when(problemDtoMock.getInputFormat()).thenReturn("knapsack");
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    var problem = client.post()
        .uri("/problems/" + KNAPSACK.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ProblemDto.class)
        .returnResult()
        .getResponseBody();
    assertNotNull(problem);

    // wait for the solving process to finish
    ProblemDto solved = null;
    for (int i = 0; i < 30; i++) {
      var current = client.get()
          .uri("/problems/" + KNAPSACK.getId() + "/" + problem.getId())
          .exchange()
          .expectStatus().isOk()
          .expectBody(ProblemDto.class)
          .returnResult()
          .getResponseBody();
      assertNotNull(current);
      if (current.getState() == ProblemState.SOLVED) {
        solved = current;
        break;
      }
      Thread.sleep(1000);
    }

    assertNotNull(solved, "Problem did not finish solving in time");
    assertNotNull(solved.getSolution());
    assertTrue(solved.getSolution().getStatus() != SolutionStatus.SOLVED,
        "Unparseable input must not produce a solved solution: " + solved);
  }

  @Test
  void rejectsUnknownInputFormat() {
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getInput()).thenReturn("(a and b) or c");
    Mockito.when(problemDtoMock.getInputFormat()).thenReturn("not-a-format");
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    client.post()
        .uri("/problems/" + SAT.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void rejectsMissingInputFormat() {
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getInput()).thenReturn("(a and b) or c");
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    client.post()
        .uri("/problems/" + SAT.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void rejectsFormatOutsideInputFamily() {
    var problemDtoMock = Mockito.mock(ProblemDto.class);
    Mockito.when(problemDtoMock.getInput()).thenReturn("(a and b) or c");
    Mockito.when(problemDtoMock.getInputFormat()).thenReturn("dimacs-cnf-solution");
    Mockito.when(problemDtoMock.getState()).thenReturn(ProblemState.SOLVING);

    client.post()
        .uri("/problems/" + KNAPSACK.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(problemDtoMock)
        .exchange()
        .expectStatus().isBadRequest();
  }
}
