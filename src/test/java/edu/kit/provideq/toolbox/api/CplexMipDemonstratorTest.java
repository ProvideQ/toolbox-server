package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.demonstrators.CplexMipDemonstrator;
import edu.kit.provideq.toolbox.meta.ProblemState;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
class CplexMipDemonstratorTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private CplexMipDemonstrator cplexMipDemonstrator;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(20))
        .build();
  }

  @Test
  void testCplexMipDemonstrator() {
    var problem = ApiTestHelper.createProblem(client, cplexMipDemonstrator, "", DEMONSTRATOR);
    ApiTestHelper.testSolution(problem);
  }
}
