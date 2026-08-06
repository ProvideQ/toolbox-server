package edu.kit.provideq.toolbox.api.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.tools.equivalencechecking.EquivalenceChecking;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class EquivalenceCheckingRouterTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void equivalenceCheckingReturnsJsonObject() throws Exception {
    var equivalenceChecking = mock(EquivalenceChecking.class);
    JsonNode toolOutput = objectMapper.readTree("""
        {
          "strategy": "pyzx",
          "status": "equivalent",
          "globalPhaseIgnored": true
        }
        """);
    when(equivalenceChecking.check(any(JsonNode.class))).thenReturn(toolOutput);

    var router = new EquivalenceCheckingRouter(equivalenceChecking).getEquivalenceCheckingRoutes();
    var client = WebTestClient.bindToRouterFunction(router).build();

    client.post()
        .uri(EquivalenceCheckingRouter.EQUIVALENCE_CHECKING_PATH)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .bodyValue("""
            {
              "strategy": "pyzx",
              "qasmA": "OPENQASM 2.0;",
              "qasmB": "OPENQASM 2.0;"
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType("application/json")
        .expectBody()
        .jsonPath("$.strategy").isEqualTo("pyzx")
        .jsonPath("$.status").isEqualTo("equivalent")
        .jsonPath("$.globalPhaseIgnored").isEqualTo(true);
  }

  @Test
  void equivalenceCheckingRejectsMissingBody() {
    var router = new EquivalenceCheckingRouter(
        mock(EquivalenceChecking.class)).getEquivalenceCheckingRoutes();
    var client = WebTestClient.bindToRouterFunction(router).build();

    client.post()
        .uri(EquivalenceCheckingRouter.EQUIVALENCE_CHECKING_PATH)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isBadRequest();
  }
}
