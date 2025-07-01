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

import com.google.common.collect.Streams;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
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
 * This router handles requests regarding {@link Problem} instance attributes.
 */
@Configuration
@EnableWebFlux
public class ProblemAttributesRouter {
  public static final String PROBLEM_ID_PARAM_NAME = "problemId";
  public static final String ATTRIBUTE_ID_PARAM_NAME = "attributeId";
  private ProblemManagerProvider managerProvider;

  @Bean
  RouterFunction<ServerResponse> getAttributeRoutes() {
    var managers = managerProvider.getProblemManagers();
    return Streams.concat(
            managers.stream().map(this::defineGetRoute),
            managers.stream().map(this::defineListRoute)
        ).reduce(RouterFunction::and)
        .orElse(null);
  }

  /**
   * Estimate Operation: GET /problems/TYPE/{problemId}/attributes/{attributeId}.
   */
  private RouterFunction<ServerResponse> defineGetRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getAttributeRouteForProblemType(manager.getType()),
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
    var attributeId = req.pathVariable(ATTRIBUTE_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);

    try {
      var attributeGetter = manager.getType().getAttributes().get(attributeId);
      String attribute = attributeGetter.apply(problem.getInput().orElseThrow());
      return ok().body(Mono.just(attribute), new ParameterizedTypeReference<>() {
      });
    } catch (IllegalStateException | NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private void handleGetDocumentation(
      ProblemManager<?, ?> manager,
      Builder ops
  ) {
    ProblemType<?, ?> problemType = manager.getType();
    ops
        .operationId(getAttributeRouteForProblemType(problemType))
        .tag(problemType.getId())
        .description("Calculates the attribute for the given problem id and attribute id.")
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(ATTRIBUTE_ID_PARAM_NAME))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent())
        );
  }

  /**
   * Estimate Operation: GET /problems/TYPE/{problemId}/attributes.
   */
  private RouterFunction<ServerResponse> defineListRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getAttributesRouteForProblemType(manager.getType()),
        accept(APPLICATION_JSON),
        req -> handleList(manager, req),
        ops -> handleListDocumentation(manager, ops)
    ).build();
  }

  private <InputT, ResultT> Mono<ServerResponse> handleList(
      ProblemManager<InputT, ResultT> manager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);

    try {
      var attributeValues = manager.getType().getAttributes()
          .entrySet().stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  entry -> entry.getValue().apply(problem.getInput().orElseThrow())
              )
          );
      return ok().body(Mono.just(attributeValues), new ParameterizedTypeReference<>() {
      });
    } catch (IllegalStateException | NoSuchElementException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private void handleListDocumentation(
      ProblemManager<?, ?> manager,
      Builder ops
  ) {
    ProblemType<?, ?> problemType = manager.getType();
    ops
        .operationId(getAttributesRouteForProblemType(problemType))
        .tag(problemType.getId())
        .description("Calculates all attributes for the given problem id.")
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent())
        );
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent() {
    return contentBuilder()
        .mediaType(APPLICATION_JSON_VALUE)
        .schema(schemaBuilder().implementation(String.class));
  }

  private String getAttributeRouteForProblemType(ProblemType<?, ?> type) {
    return "/problems/%s/{%s}/attributes/{%s}".formatted(
        type.getId(),
        PROBLEM_ID_PARAM_NAME,
        ATTRIBUTE_ID_PARAM_NAME);
  }

  private String getAttributesRouteForProblemType(ProblemType<?, ?> type) {
    return "/problems/%s/{%s}/attributes".formatted(
        type.getId(),
        PROBLEM_ID_PARAM_NAME);
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }

}
