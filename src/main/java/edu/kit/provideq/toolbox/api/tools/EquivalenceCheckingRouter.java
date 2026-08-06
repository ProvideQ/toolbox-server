package edu.kit.provideq.toolbox.api.tools;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.databind.JsonNode;
import edu.kit.provideq.toolbox.tools.equivalencechecking.EquivalenceChecking;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Routes requests to synchronous toolbox utilities. */
@Configuration
@EnableWebFlux
public class EquivalenceCheckingRouter {
  static final String EQUIVALENCE_CHECKING_PATH = "/tools/equivalencechecking";

  private final EquivalenceChecking equivalenceChecking;

  public EquivalenceCheckingRouter(EquivalenceChecking equivalenceChecking) {
    this.equivalenceChecking = equivalenceChecking;
  }

  /** Registers the equivalence-checking endpoint. */
  @Bean
  public RouterFunction<ServerResponse> getEquivalenceCheckingRoutes() {
    return route().POST(
        EQUIVALENCE_CHECKING_PATH,
        contentType(APPLICATION_JSON).and(accept(APPLICATION_JSON)),
        this::handleEquivalenceChecking,
        ops -> ops
            .operationId("equivalenceChecking")
            .tag("tools")
            .description("Checks whether two quantum circuits are equivalent.")
    ).build();
  }

  private Mono<ServerResponse> handleEquivalenceChecking(ServerRequest request) {
    return request.bodyToMono(JsonNode.class)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "A JSON request body is required.")))
        .flatMap(input -> Mono.fromCallable(() -> equivalenceChecking.check(input))
            .subscribeOn(Schedulers.boundedElastic()))
        .onErrorMap(
            IllegalStateException.class,
            exception -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                exception))
        .flatMap(output -> ok()
            .contentType(APPLICATION_JSON)
            .bodyValue(output));
  }
}
