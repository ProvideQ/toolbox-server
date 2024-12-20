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
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.List;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
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
  private ProblemManagerProvider problemManagerProvider;

  @Bean
  RouterFunction<ServerResponse> getSolversRoutes() {
    return problemManagerProvider.getProblemManagers().stream()
        .map(this::defineSolversRouteForManager)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSolversRouteForManager(
      ProblemManager<?, ?> manager) {
    var problemType = manager.getType();
    return route().GET(
        getSolversRouteForProblemType(problemType),
        req -> handleSolversRouteForManager(manager),
        ops -> handleSolversRouteDocumentation(ops, manager)
    ).build();
  }

  private Mono<ServerResponse> handleSolversRouteForManager(ProblemManager<?, ?> manager) {
    var solvers = getAllSolverInfos(manager);

    return ok().body(Mono.just(solvers), new ParameterizedTypeReference<>() {
    });
  }

  private static List<ProblemSolverInfo> getAllSolverInfos(ProblemManager<?, ?> manager) {
    return manager.getSolvers().stream()
            .map(solver -> new ProblemSolverInfo(solver.getId(), solver.getName(),
                solver.getDescription()))
            .toList();
  }

  private void handleSolversRouteDocumentation(Builder ops, ProblemManager<?, ?> manager) {
    ops
        .operationId(getSolversRouteForProblemType(manager.getType()))
        .tag(manager.getType().getId())
        .description("Returns a list of solvers available to solve the "
            + manager.getType().getId() + " problem type.")
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent(manager))
        );
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent(
          ProblemManager<?, ?> manager) {
    var allSolvers = getAllSolverInfos(manager);
    String example;
    try {
      example = new ObjectMapper().writeValueAsString(allSolvers);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(manager.getType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                    schemaBuilder().implementation(ProblemSolverInfo.class)));
  }

  private String getSolversRouteForProblemType(ProblemType<?, ?> type) {
    return "/solvers/" + type.getId();
  }

  @Autowired
  public void setProblemManagerProvider(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }
}
