package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.content.Builder;
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
  private ProblemManagerProvider problemManagerProvider;

  @Bean
  RouterFunction<ServerResponse> getSubRoutineRoutes() {
    return problemManagerProvider.getProblemManagers().stream()
        .map(this::defineSubRoutineRouteForManager)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSubRoutineRouteForManager(
      ProblemManager<?, ?> manager) {
    var problemType = manager.getType();
    return route().GET(
        getSubRoutinesRouteForProblemType(problemType),
        req -> handleSubRoutineRouteForMetaSolver(manager, req),
        ops -> handleSubRoutineRouteDocumentation(manager, ops)
    ).build();
  }

  private Mono<ServerResponse> handleSubRoutineRouteForMetaSolver(
      ProblemManager<?, ?> manager,
      ServerRequest req
  ) {
    var subroutines = req.queryParam("id")
        .flatMap(manager::findSolverById)
        .map(ProblemSolver::getSubRoutines)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solver for this problem with this solver id!"));

    return ok().body(Mono.just(subroutines), new ParameterizedTypeReference<>() {
    });
  }

  private void handleSubRoutineRouteDocumentation(
      ProblemManager<?, ?> manager,
      org.springdoc.core.fn.builders.operation.Builder ops
  ) {
    ProblemType<?, ?> problemType = manager.getType();
    ops.operationId(getSubRoutinesRouteForProblemType(problemType))
            .parameter(getParameterBuilder(manager))
            .tag(problemType.getId())
            .description("Returns the sub-routines available for the given solver id of type "
                    + problemType.getId() + ". "
                    + "Sub-routines are used in some solvers to solve sub-problems. "
                    + "Passing a sub-routine in a solve request will ensure that"
                    + " the desired sub-routine is used in the calculation.")
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.OK.value()))
                    .content(getOkResponseContent(manager)))
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.NOT_FOUND.value())));
  }

  private static org.springdoc.core.fn.builders.parameter.Builder
      getParameterBuilder(ProblemManager<?, ?> manager) {
    return parameterBuilder()
            .in(ParameterIn.QUERY)
            .name("id")
            .description("The id of the solver to get the sub-routines from."
                    + " Use the endpoint GET /solvers/" + manager.getType().getId()
                    + " to get a list of available solver ids.")
            .required(true)
            .example(manager
                    .getSolvers().stream()
                    .findFirst()
                    .map(ProblemSolver::getId)
                    .orElseThrow(() -> new RuntimeException("No solver found")));
  }

  private static Builder getOkResponseContent(ProblemManager<?, ?> manager) {
    String example = manager
            .getSolvers().stream()
            .findFirst()
            .map(solver -> {
              var subRoutines = solver.getSubRoutines();
              try {
                return new ObjectMapper().writeValueAsString(subRoutines);
              } catch (JsonProcessingException e) {
                throw new RuntimeException("example could not be parsed", e);
              }
            })
            .orElseThrow(() -> new RuntimeException("no solver found"));

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(manager.getType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                    schemaBuilder().implementation(SubRoutineDefinition.class)));
  }

  private String getSubRoutinesRouteForProblemType(ProblemType<?, ?> type) {
    return "/sub-routines/" + type.getId();
  }

  @Autowired
  public void setProblemManagerProvider(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }
}
