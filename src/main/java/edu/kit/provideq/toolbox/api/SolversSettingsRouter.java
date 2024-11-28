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
import edu.kit.provideq.toolbox.exception.MissingSolverException;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.content.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
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
 * This router handles solver settings discovery requests to the
 * GET {@code /solvers/{problemType}/{solverId}/settings} endpoints.
 *  * Responses are generated from the settings reported by the given solver itself
 * (see {@link ProblemSolver#getSolverSettings()}).
 */
@Configuration
@EnableWebFlux
public class SolversSettingsRouter {
  private ProblemManagerProvider problemManagerProvider;
  private static final String SOLVER_ID_PARAM_NAME = "solverId";

  @Bean
  RouterFunction<ServerResponse> getSolverSettingsRoutes() {
    return problemManagerProvider.getProblemManagers().stream()
        .map(this::defineSolverSettingsRouteForManager)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineSolverSettingsRouteForManager(
      ProblemManager<?, ?> manager) {
    var problemType = manager.getType();
    return route().GET(
        getSolverSettingsRouteForProblemType(problemType),
        req -> handleSolverSettingsRouteForManager(manager, req),
        ops -> handleSolverSettingsRouteDocumentation(manager, ops)
    ).build();
  }

  private Mono<ServerResponse> handleSolverSettingsRouteForManager(
      ProblemManager<?, ?> manager,
      ServerRequest req
  ) {
    var solverId = req.pathVariable(SOLVER_ID_PARAM_NAME);
    var solver = manager.findSolverById(solverId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solver for this problem with this solver id!"));

    var solverSettings = solver.getSolverSettings();

    return ok().body(Mono.just(solverSettings), new ParameterizedTypeReference<>() {});
  }

  private void handleSolverSettingsRouteDocumentation(
      ProblemManager<?, ?> manager,
      org.springdoc.core.fn.builders.operation.Builder ops
  ) {
    ProblemType<?, ?> problemType = manager.getType();
    ops.operationId(getSolverSettingsRouteForProblemType(problemType))
            .parameter(getParameterBuilder(manager))
            .tag(problemType.getId())
            .description("Returns the solver settings available for the problem type "
                    + problemType.getId() + ". "
                    + "Solver settings can be used to configure a solver to solve a problem"
                    + " in a specific way. Pass solver settings via the /problems endpoint.")
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.OK.value()))
                    .content(getOkResponseContent(manager)))
            .response(responseBuilder()
                    .responseCode(String.valueOf(HttpStatus.NOT_FOUND.value())));
  }

  private static org.springdoc.core.fn.builders.parameter.Builder
      getParameterBuilder(ProblemManager<?, ?> manager) {
    return parameterBuilder()
        .in(ParameterIn.PATH)
        .name(SOLVER_ID_PARAM_NAME)
            .description("The id of the solver to get the settings from."
                    + " Use the endpoint GET /solvers/" + manager.getType().getId()
                    + " to get a list of available solver ids.")
            .required(true)
            .example(manager
                    .getSolvers().stream()
                    .findFirst()
                    .map(ProblemSolver::getId)
                    .orElseThrow(() -> new MissingExampleException(manager.getType())));
  }

  private static Builder getOkResponseContent(ProblemManager<?, ?> manager) {
    String example = manager
            .getSolvers().stream()
            .findFirst()
            .map(solver -> {
              var solverSettingsDtos = solver.getSolverSettings();
              try {
                return new ObjectMapper().writeValueAsString(solverSettingsDtos);
              } catch (JsonProcessingException e) {
                throw new JsonParseException(e);
              }
            })
            .orElseThrow(() -> new MissingSolverException(manager.getType()));

    return contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(manager.getType().getId())
                    .value(example))
            .array(arraySchemaBuilder().schema(
                     schemaBuilder().implementation(SolverSetting.class)));
  }

  private String getSolverSettingsRouteForProblemType(ProblemType<?, ?> type) {
    return "/solvers/" + type.getId() + "/{" + SOLVER_ID_PARAM_NAME + "}/settings";
  }

  @Autowired
  public void setProblemManagerProvider(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }
}
