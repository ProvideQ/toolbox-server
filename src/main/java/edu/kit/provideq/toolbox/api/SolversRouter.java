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
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
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
    var problemType = metaSolver.getProblemType();
    return route().GET(
        getSolversRouteForProblemType(problemType),
        req -> handleSolversRouteForMetaSolver(metaSolver),
        ops -> handleSolversRouteDocumentation(ops, metaSolver)
    ).build();
  }

  private Mono<ServerResponse> handleSolversRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
    var solvers = getAllSolverInfos(metaSolver);

    return ok().body(Mono.just(solvers), new ParameterizedTypeReference<>() {
    });
  }

  private static List<ProblemSolverInfo> getAllSolverInfos(MetaSolver<?, ?, ?> metaSolver) {
    return metaSolver.getAllSolvers().stream()
            .map(solver -> new ProblemSolverInfo(solver.getId(), solver.getName()))
            .toList();
  }

  private void handleSolversRouteDocumentation(Builder ops, MetaSolver<?, ?, ?> metaSolver) {
    ops
        .operationId(getSolversRouteForProblemType(metaSolver.getProblemType()))
        .tag(metaSolver.getProblemType().getId())
        .description("Returns a list of solvers available to solve the "
            + metaSolver.getProblemType().getId() + " problem type.")
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent(metaSolver))
        );
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent(
          MetaSolver<?, ?, ?> metaSolver) {
    var allSolvers = getAllSolverInfos(metaSolver);
    String example;
    try {
      example = new ObjectMapper().writeValueAsString(allSolvers);
    } catch (JsonProcessingException e) {
      example = "Error: solvers could not be parsed";
    }

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(metaSolver.getProblemType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                    schemaBuilder().implementation(ProblemSolverInfo.class)));
  }

  private String getSolversRouteForProblemType(ProblemType type) {
    return "/solvers/" + type.getId();
  }
}
