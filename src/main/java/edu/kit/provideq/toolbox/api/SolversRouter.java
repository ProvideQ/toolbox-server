package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * This router handles solver discovery requests to the GET {@code /solvers/{problemType}}
 * endpoints.
 * Responses are generated from the solvers reported by the meta-solver registered for the given
 * problem type.
 */
@Configuration
@EnableWebFlux
public class SolversRouter {
  private final MetaSolverProvider metaSolverProvider;

  @Autowired
  public SolversRouter(MetaSolverProvider metaSolverProvider) {
    this.metaSolverProvider = metaSolverProvider;
  }

  @Bean
  RouterFunction<ServerResponse> getSolversRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineSolversRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSolversRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver) {
    String problemId = metaSolver.getProblemType().getId();
    return route().GET(
        "/solvers/" + problemId,
        req -> handleSolversRouteForMetaSolver(metaSolver),
        ops -> ops
            .operationId("/solvers/" + problemId)
            .tag(problemId)
            .response(responseBuilder()
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .content(contentBuilder()
                    .mediaType(APPLICATION_JSON_VALUE)
                    .array(arraySchemaBuilder().schema(
                        schemaBuilder().implementation(ProblemSolverInfo.class)))
                )
            )
    ).build();
  }

  private Mono<ServerResponse> handleSolversRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
    var solvers = metaSolver.getAllSolvers().stream()
        .map(solver -> new ProblemSolverInfo(solver.getId(), solver.getName()))
        .toList();

    return ok().body(Mono.just(solvers), new ParameterizedTypeReference<>() {
    });
  }
}
