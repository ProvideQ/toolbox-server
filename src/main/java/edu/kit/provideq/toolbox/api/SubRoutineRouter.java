package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
 * This router handles sub-routine discovery requests to the GET {@code /sub-routines/{problemType}}
 * endpoints.
 * Responses are generated from the sub-routine data reported by the given solver itself
 * (see {@link ProblemSolver#getSubRoutines()}).
 */
@Configuration
@EnableWebFlux
public class SubRoutineRouter {
  private final MetaSolverProvider metaSolverProvider;

  @Autowired
  public SubRoutineRouter(MetaSolverProvider metaSolverProvider) {
    this.metaSolverProvider = metaSolverProvider;
  }

  @Bean
  RouterFunction<ServerResponse> getSubRoutineRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineSubRoutineRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSubRoutineRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver) {
    var problemType = metaSolver.getProblemType();
    return route().GET(
        getSubRoutinesRouteForProblemType(problemType),
        req -> handleSubRoutineRouteForMetaSolver(metaSolver, req),
        ops -> ops
            .operationId(getSubRoutinesRouteForProblemType(problemType))
            .parameter(parameterBuilder().in(ParameterIn.QUERY).name("id"))
            .tag(problemType.getId())
            .response(responseBuilder()
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .content(contentBuilder()
                    .mediaType(APPLICATION_JSON_VALUE)
                    .array(arraySchemaBuilder().schema(
                        schemaBuilder().implementation(SubRoutineDefinition.class)))
                )
            )
    ).build();
  }

  private Mono<ServerResponse> handleSubRoutineRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver,
                                                                  ServerRequest req) {
    var subroutines = req.queryParam("id")
        .flatMap(metaSolver::getSolver)
        .map(ProblemSolver::getSubRoutines)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solver for this problem with this solver id!"));

    return ok().body(Mono.just(subroutines), new ParameterizedTypeReference<>() {
    });
  }

  private String getSubRoutinesRouteForProblemType(ProblemType type) {
    return "/sub-routines/" + type.getId();
  }
}
