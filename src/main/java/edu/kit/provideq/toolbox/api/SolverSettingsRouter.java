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
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
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
 * This router handles settings requests to the GET {@code /settings/{problemType}}
 * endpoints.
 * Responses are generated from the settings data reported by the given solver itself
 */
@Configuration
@EnableWebFlux
public class SolverSettingsRouter {
  /*
  private final MetaSolverProvider metaSolverProvider;

  @Autowired
  public SolverSettingsRouter(MetaSolverProvider metaSolverProvider) {
    this.metaSolverProvider = metaSolverProvider;
  }

  @Bean
  RouterFunction<ServerResponse> getSettingsRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineSettingsRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSettingsRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver) {
    var problemType = metaSolver.getProblemType();
    return route().GET(
        getSettingsRouteForProblemType(problemType),
        req -> handleSettingsRouteForMetaSolver(metaSolver, req),
        ops -> handleSettingsRouteDocumentation(metaSolver, ops)
    ).build();
  }

  private Mono<ServerResponse> handleSettingsRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver,
                                                                  ServerRequest req) {
    var settings = req.queryParam("id")
        .flatMap(metaSolver::getSolver)
        .map(ProblemSolver::getSettings)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solver for this problem with this solver id!"));

    return ok().body(Mono.just(settings), new ParameterizedTypeReference<>() {
    });
  }

  private void handleSettingsRouteDocumentation(
          MetaSolver<?, ?, ?> metaSolver, org.springdoc.core.fn.builders.operation.Builder ops) {
    ProblemType problemType = metaSolver.getProblemType();
    ops.operationId(getSettingsRouteForProblemType(problemType))
            .parameter(getParameterBuilder(metaSolver))
            .tag(problemType.getId())
            .description("Returns the settings available for the given solver id of type "
                    + problemType.getId() + ". "
                    + "Settings are used to further define the solver.")
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.OK.value()))
                    .content(getOkResponseContent(metaSolver)))
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.NOT_FOUND.value())));
  }

  private static org.springdoc.core.fn.builders.parameter.Builder
      getParameterBuilder(MetaSolver<?, ?, ?> metaSolver) {
    return parameterBuilder()
            .in(ParameterIn.QUERY)
            .name("id")
            .description("The id of the solver to get the settings from."
                    + " Use the endpoint GET /solvers/" + metaSolver.getProblemType().getId()
                    + " to get a list of available solver ids.")
            .required(true)
            .example(metaSolver
                    .getAllSolvers().stream()
                    .findFirst()
                    .map(ProblemSolver::getId)
                    .orElseThrow(() -> new RuntimeException("No solver found")));
  }

  private static Builder getOkResponseContent(MetaSolver<?, ?, ?> metaSolver) {
    String example = metaSolver
            .getAllSolvers().stream()
            .findFirst()
            .map(solver -> {
              var settings = solver.getSettings();
              try {
                return new ObjectMapper().writeValueAsString(settings);
              } catch (JsonProcessingException e) {
                throw new MissingExampleException("example could not be parsed", e);
              }
            })
            .orElseThrow(() -> new RuntimeException("no solver found"));

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(metaSolver.getProblemType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                    schemaBuilder().implementation(MetaSolverSetting.class)));
  }

  private String getSettingsRouteForProblemType(ProblemType type) {
    return "/settings/" + type.getId();
  }
*/
}
