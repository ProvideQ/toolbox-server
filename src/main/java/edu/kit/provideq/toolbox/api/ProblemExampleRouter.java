package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.List;
import java.util.Optional;
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
 * This router provides example problems for each problem type.
 */
@Configuration
@EnableWebFlux
public class ProblemExampleRouter {
  private ProblemManagerProvider managerProvider;

  @Bean
  RouterFunction<ServerResponse> getProblemExampleRoutes() {
    var managers = this.managerProvider.getProblemManagers();
    return managers.stream()
        .map(this::defineReadRoute)
        .reduce(RouterFunction::and)
        .orElseThrow();
  }

  /**
   * GET /problems/TYPE/example.
   */
  private RouterFunction<ServerResponse> defineReadRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getPath(manager.getType()),
        req -> handleRead(manager),
        ops -> configureReadDocs(manager, ops)
    ).build();
  }

  private <InputT, ResultT> Mono<ServerResponse> handleRead(
      ProblemManager<InputT, ResultT> manager
  ) {
    // Add logging to capture request details
    System.out.println("Handling read for manager: " + manager.getType().getId());

    var exampleProblems = getExampleInput(manager);
    System.out.println("Example problems: " + exampleProblems);

    if (exampleProblems == null || exampleProblems.isEmpty()) {
      System.err.println("No example problems found for manager: " + manager.getType().getId());
      return ServerResponse.status(HttpStatus.BAD_REQUEST).bodyValue("No example problems found");
    }

    return ok().body(Mono.just(exampleProblems), new ParameterizedTypeReference<>() {
    }).doOnError(error -> {
      // Log the error details
      System.err.println("Error handling read: " + error.getMessage());
    });
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

  private static void configureReadDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();

    String requestString;
    try {
      requestString = new ObjectMapper().writeValueAsString(getExampleInput(manager));
    } catch (JsonProcessingException exception) {
      throw new MissingExampleException(manager.getType(), exception);
    }

    ops
        .operationId(getPath(manager.getType()))
        .description("This endpoint can be used to view example problems for '"
            + type.getId() + "'.")
        .tag(type.getId())
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(contentBuilder()
                .array(arraySchemaBuilder()
                    .schema(schemaBuilder().implementation(String.class))
                )
                .example(exampleOjectBuilder()
                    .value(requestString)
                )
            )
            .content(contentBuilder()));
  }

  private static String getPath(ProblemType<?, ?> type) {
    return "/problems/%s/examples".formatted(type.getId());
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }

}
