package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * This router handles getting the status of an existing solve request to
 * the GET {@code /solution/{problemIndex}} endpoints.
 * Requests are validated and relayed to the corresponding {@link MetaSolver}.
 */
@Configuration
@EnableWebFlux
public class SolutionRouter {
  private final MetaSolverProvider metaSolverProvider;

  public SolutionRouter(MetaSolverProvider metaSolverProvider) {
    this.metaSolverProvider = metaSolverProvider;
  }

  @Bean
  RouterFunction<ServerResponse> getSolutionRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineSolutionRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow(); // we should always have at least one route or the toolbox is useless
  }

  private RouterFunction<ServerResponse> defineSolutionRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver) {
    var problemType = metaSolver.getProblemType();
    return route().GET(
        getSolutionRouteForProblemType(problemType),
        accept(APPLICATION_JSON),
        req -> handleSolutionRouteForMetaSolver(metaSolver, req),
        ops -> handleSolversRouteDocumentation(ops, problemType)
    ).build();
  }

  private void handleSolversRouteDocumentation(Builder ops, ProblemType problemType) {
    ops
        .operationId(getSolutionRouteForProblemType(problemType))
        .tag(problemType.getId())
        .parameter(parameterBuilder().in(ParameterIn.QUERY).name("id"))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .implementation(Solution.class))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.NOT_FOUND.value())));
  }

  private Mono<ServerResponse> handleSolutionRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver, ServerRequest req) {
    var solution = req.queryParam("id")
        .map(Long::parseLong)
        .map(solutionId -> metaSolver.getSolutionManager().getSolution(solutionId))
        .map(Solution::toStringSolution)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solution for this problem with this solution id!"));

    return ok().body(Mono.just(solution), new ParameterizedTypeReference<>() {
    });
  }

  private String getSolutionRouteForProblemType(ProblemType type) {
    return "/solution/" + type.getId();
  }
}
