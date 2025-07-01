package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
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
 * This router handles requests regarding {@link ProblemType} instances.
 */
@Configuration
@EnableWebFlux
public class ProblemTypeRouter {
  private static final String PROBLEM_TYPES_BASE_PATH = "/problem-types";
  private ProblemManagerProvider managerProvider;

  @Bean
  RouterFunction<ServerResponse> getProblemTypeRoutes() {
    var managers = this.managerProvider.getProblemManagers();
    return Streams.concat(
            managers.stream().map(this::defineReadRoute)
        )
        .reduce(RouterFunction::and)
        .orElseThrow()
        .and(defineListTypesRoute());
  }

  /**
   * Create operation: GET /problem-types.
   */
  private RouterFunction<ServerResponse> defineListTypesRoute() {
    return route().GET(
        PROBLEM_TYPES_BASE_PATH,
        accept(APPLICATION_JSON),
        req -> handleListTypes(),
        ops -> ops
            .operationId("%s/%s".formatted(PROBLEM_TYPES_BASE_PATH, "listType"))
            .description(
                "Responds with a list of all problem types that are supported by the server.")
            .tag("_")
            .response(buildProblemTypeListResponse())
    ).build();
  }

  private Mono<ServerResponse> handleListTypes() {
    var problemTypes = managerProvider.getProblemManagers().stream()
        .filter(p -> !p.getType().equals(DEMONSTRATOR))
        .map(ProblemManager::getType)
        .map(ProblemTypeDto::fromProblemType)
        .toList();

    return ok().body(Mono.just(problemTypes), new ParameterizedTypeReference<>() {
    });
  }

  private org.springdoc.core.fn.builders.apiresponse.Builder buildProblemTypeListResponse() {
    var problemTypes = managerProvider.getProblemManagers().stream()
        .filter(p -> !p.getType().equals(DEMONSTRATOR))
        .map(ProblemManager::getType)
        .map(ProblemTypeDto::fromProblemType)
        .toList();

    String problemTypeDtosJson;
    try {
      problemTypeDtosJson = new ObjectMapper().writeValueAsString(problemTypes);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }

    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder()
            .array(arraySchemaBuilder()
                .schema(schemaBuilder().implementation(ProblemTypeDto.class))
            )
            .example(exampleOjectBuilder()
                .value(problemTypeDtosJson)
            )
        );
  }

  /**
   * List operation: GET /problem-types/TYPE.
   */
  private RouterFunction<ServerResponse> defineReadRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        "%s/%s".formatted(PROBLEM_TYPES_BASE_PATH, manager.getType().getId()),
        accept(APPLICATION_JSON),
        req -> handleList(manager),
        ops -> configureReadDocs(manager, ops)
    ).build();
  }

  private <InputT, ResultT> Mono<ServerResponse> handleList(
      ProblemManager<InputT, ResultT> manager
  ) {
    var problemTypeDto = ProblemTypeDto.fromProblemType(manager.getType());
    return ok().body(Mono.just(problemTypeDto), new ParameterizedTypeReference<>() {
    });
  }

  public static void configureReadDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId("%s/%s/%s".formatted(PROBLEM_TYPES_BASE_PATH, type.getId(), "read"))
        .description("This endpoint can be used to get / read the problem type '"
            + type.getId() + "'.")
        .tag(type.getId())
        .response(buildProblemResponse(manager));
  }

  private static org.springdoc.core.fn.builders.apiresponse.Builder buildProblemResponse(
      ProblemManager<?, ?> problemManager) {
    var problemTypeDto = ProblemTypeDto.fromProblemType(problemManager.getType());

    String problemTypeDtoJson;
    try {
      problemTypeDtoJson = new ObjectMapper().writeValueAsString(problemTypeDto);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }

    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder()
            .schema(schemaBuilder()
                .implementation(ProblemTypeDto.class))
            .example(exampleOjectBuilder()
                .value(problemTypeDtoJson)
            )
        );
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }
}
