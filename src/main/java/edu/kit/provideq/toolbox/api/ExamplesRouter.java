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
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
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
 * This router provides example problems for each problem type.
 */
@Configuration
@EnableWebFlux
public class ExamplesRouter {
  private ProblemManagerProvider problemManagerProvider;
  private static final Logger logger = Logger.getLogger(ExamplesRouter.class.getName());

  @Bean
  RouterFunction<ServerResponse> getExamplesRoutes() {
    return problemManagerProvider.getProblemManagers().stream()
        .map(this::defineExamplesRouteForManager)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  private RouterFunction<ServerResponse> defineExamplesRouteForManager(
      ProblemManager<?, ?> manager) {
    var problemType = manager.getType();
    return route().GET(
        getExamplesRouteForProblemType(problemType),
        req -> handleExamplesRouteForManager(manager),
        ops -> handleExamplesRouteDocumentation(ops, manager)
    ).build();
  }

  private Mono<ServerResponse> handleExamplesRouteForManager(ProblemManager<?, ?> manager) {
    logger.warning("Handling read for manager: " + manager.getType().getId());
    var exampleProblems = List.of("example1", "example2", "example3");
    logger.warning("Example problems: " + exampleProblems);

    return ok().body(Mono.just(exampleProblems), new ParameterizedTypeReference<>() {})
        .doOnError(error -> logger.severe("Error handling read: " + error.getMessage()));
  }

  private void handleExamplesRouteDocumentation(Builder ops, ProblemManager<?, ?> manager) {
    ops
        .operationId(getExamplesRouteForProblemType(manager.getType()))
        .tag(manager.getType().getId())
        .description("This endpoint can be used to view example problems for '"
            + manager.getType().getId() + "'.")
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(getOkResponseContent(manager))
        );
  }

  private static <InputT, ResultT> List<InputT> getExampleInput(
      ProblemManager<InputT, ResultT> manager
  ) {
    return manager.getExampleInstances()
        .stream()
        .map(Problem::getInput)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private static org.springdoc.core.fn.builders.content.Builder getOkResponseContent(
      ProblemManager<?, ?> manager) {
    var allExamples = getExampleInput(manager);
    String example;
    try {
      example = new ObjectMapper().writeValueAsString(allExamples);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }

    return contentBuilder()
        .mediaType(APPLICATION_JSON_VALUE)
        .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
            .name(manager.getType().getId())
            .value(example))
        .array(arraySchemaBuilder()
            .schema(schemaBuilder()
                .implementation(String.class)));
  }

  private String getExamplesRouteForProblemType(ProblemType<?, ?> type) {
    return "/examples/" + type.getId();
  }

  @Autowired
  public void setProblemManagerProvider(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }
}
