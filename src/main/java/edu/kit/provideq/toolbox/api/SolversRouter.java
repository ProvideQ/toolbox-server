package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ProblemManagerProvider;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import java.util.List;
import org.springdoc.core.fn.builders.operation.Builder;
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
  private final ProblemManagerProvider problemManagerProvider;

  @Autowired
  public SolversRouter(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }

  @Bean
  RouterFunction<ServerResponse> getSolversRoutes() {
    return problemManagerProvider.getProblemManagers().stream()
        .map(this::defineSolversRouteForProblemManager)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSolversRouteForProblemManager(
      ProblemManager<?, ?> problemManager) {
    var problemType = problemManager.getProblemType();
    return route().GET(
        getSolversRouteForProblemType(problemType),
        req -> handleSolversRouteForProblemManager(problemManager),
        ops -> handleSolversRouteDocumentation(ops, problemManager)
    ).build();
  }

  private Mono<ServerResponse> handleSolversRouteForProblemManager(
      ProblemManager<?, ?> problemManager
  ) {
    var solvers = getAllSolverInfos(problemManager);

    return ok().body(Mono.just(solvers), new ParameterizedTypeReference<>() {
    });
  }

  private static List<ProblemSolverInfo> getAllSolverInfos(ProblemManager<?, ?> problemManager) {
    return problemManager.getProblemSolvers().stream()
            .map(solver -> new ProblemSolverInfo(solver.getId(), solver.getName()))
            .toList();
  }

  private void handleSolversRouteDocumentation(Builder ops, ProblemManager<?, ?> problemManager) {
    ops
        .operationId(getSolversRouteForProblemType(problemManager.getProblemType()))
        .tag(problemManager.getProblemType().getId())
        .description("Returns a list of solvers available to solve the "
            + problemManager.getProblemType().getId() + " problem type.")
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent(problemManager))
        );
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent(
          ProblemManager<?, ?> problemManager) {
    var allSolvers = getAllSolverInfos(problemManager);
    String example;
    try {
      example = new ObjectMapper().writeValueAsString(allSolvers);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("solvers could not be parsed", e);
    }

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(problemManager.getProblemType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                    schemaBuilder().implementation(ProblemSolverInfo.class)));
  }

  private String getSolversRouteForProblemType(TypedProblemType<?, ?> type) {
    return "/solvers/" + type.getId();
  }
}
