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

import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFlux
public class StrategyRouter {
  public static final String PROBLEM_ID_PARAM_NAME = "problemId";
  private ProblemManagerProvider managerProvider;

  @Bean
  RouterFunction<ServerResponse> getStrategyRoutes() {
    return managerProvider.getProblemManagers().stream()
        .map(this::defineGetRoute)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineGetRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getStrategyRouteForProblemType(manager.getType()),
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
    var strategyDto = StrategyDto.fromProblem(problem);

    return ok().body(Mono.just(strategyDto), new ParameterizedTypeReference<>() {
    });
  }

  private void handleGetDocumentation(ProblemManager<?, ?> manager, Builder ops) {
    ProblemType<?, ?> problemType = manager.getType();
    ops
        .operationId(getStrategyRouteForProblemType(problemType))
        .tag(problemType.getId())
        .description("Returns the meta-solver strategy configured for the given problem, meaning "
            + "the sequence of transformation rules composed by the problem and its sub-problems, "
            + "together with the correctness verdict that can be made about it.")
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(contentBuilder()
                .mediaType(APPLICATION_JSON_VALUE)
                .schema(schemaBuilder().implementation(StrategyDto.class)))
        );
  }

  private String getStrategyRouteForProblemType(ProblemType<?, ?> type) {
    return "/problems/%s/{%s}/strategy".formatted(type.getId(), PROBLEM_ID_PARAM_NAME);
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }
}
