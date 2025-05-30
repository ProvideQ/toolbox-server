package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;

import edu.kit.provideq.toolbox.demonstrators.CplexMipDemonstrator;
import edu.kit.provideq.toolbox.demonstrators.MoleculeEnergySimulator;
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
class DemonstratorTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private CplexMipDemonstrator cplexMipDemonstrator;

  @Autowired
  private MoleculeEnergySimulator moleculeEnergySimulator;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(20))
        .build();
  }

  @Test
  void testCplexMip() {
    var problem = ApiTestHelper.createProblem(client, cplexMipDemonstrator, "", DEMONSTRATOR);
    ApiTestHelper.testSolution(problem);
  }

  @Test
  void testMoleculeEnergy() {
    String atom = "H .0 .0 .0; H .0 .0 0.74279";
    var problem = ApiTestHelper.createProblem(client, moleculeEnergySimulator, atom, DEMONSTRATOR);
    ApiTestHelper.testSolution(problem);
  }
}
