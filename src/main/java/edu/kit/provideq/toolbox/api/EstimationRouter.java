package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.beans.factory.annotation.Autowired;
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
 * This router handles requests regarding {@link Problem} instance solution bound estimations.
 * The /bound endpoint is only available for problem types that have an estimator.
 */
@Configuration
@EnableWebFlux
public class EstimationRouter {
  public static final String PROBLEM_ID_PARAM_NAME = "problemId";
  private ProblemManagerProvider managerProvider;

  @Bean
  RouterFunction<ServerResponse> getEstimationRoutes() {
    return managerProvider.getProblemManagers().stream()
            .filter(manager -> manager.getType().getEstimator().isPresent())
            .map(this::defineGetRoute)
            .reduce(RouterFunction::and)
            .orElse(null);
  }

  /**
   * Estimate Operation: GET /problems/TYPE/{problemId}/bound.
   */
  private RouterFunction<ServerResponse> defineGetRoute(ProblemManager<?, ?> manager) {
    return route().GET(
            getEstimationRouteForProblemType(manager.getType()),
            accept(APPLICATION_JSON),
            req -> handleGet(manager, req),
            ops -> handleGetDocumentation(manager, ops)
    ).build();
  }

  private <InputT, ResultT> Mono<ServerResponse> handleGet(
          ProblemManager<InputT, ResultT> manager,
          ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);

    Mono<BoundDto> bound;
    try {
      problem.estimateBound();
      bound = Mono.just(new BoundDto(problem.getBound().orElseThrow()));
    } catch (IllegalStateException | NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    return ok().body(bound, new ParameterizedTypeReference<>() {
    });
  }

  private void handleGetDocumentation(
          ProblemManager<?, ?> manager,
          Builder ops
  ) {
    ProblemType<?, ?> problemType = manager.getType();
    ops
        .operationId(getEstimationRouteForProblemType(problemType))
        .tag(problemType.getId())
        .description("Estimates the solution bound for the problem with the given ID.")
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(responseBuilder()
              .responseCode(String.valueOf(HttpStatus.OK.value()))
              .content(getOkResponseContent())
        );
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent() {
    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .schema(schemaBuilder().implementation(BoundDto.class));
  }

  private String getEstimationRouteForProblemType(ProblemType<?, ?> type) {
    return "/problems/%s/{%s}/bound".formatted(type.getId(), PROBLEM_ID_PARAM_NAME);
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }

}
