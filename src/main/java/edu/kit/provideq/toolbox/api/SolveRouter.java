package edu.kit.provideq.toolbox.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolveRequest;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * This router handles problem-solving requests to the GET and POST {@code /solve/{problemType}}
 * endpoints.
 * Requests are validated and relayed to the corresponding {@link MetaSolver}.
 */
@Configuration
@EnableWebFlux
public class SolveRouter {
  private final MetaSolverProvider metaSolverProvider;
  private final Validator validator;

  public SolveRouter(MetaSolverProvider metaSolverProvider, Validator validator) {
    this.metaSolverProvider = metaSolverProvider;
    this.validator = validator;
  }

  @Bean
  RouterFunction<ServerResponse> getSolveRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow(); // we should always have at least one route or the toolbox is useless
  }

  private RouterFunction<ServerResponse> defineRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
    return route().POST(
        getSolveRouteForProblemType(metaSolver.getProblemType()),
        accept(APPLICATION_JSON),
        req -> handleRouteForMetaSolver(metaSolver, req),
        ops -> handleRouteDocumentation(metaSolver, ops)
    ).build();
  }

  private <ProblemT, SolutionT> Mono<ServerResponse> handleRouteForMetaSolver(
      MetaSolver<ProblemT, SolutionT, ?> metaSolver, ServerRequest req) {
    var solutionMono = req
        .bodyToMono(new ParameterizedTypeReference<SolveRequest<ProblemT>>() {
        })
        .doOnNext(this::validate)
        .map(metaSolver::solve)
        .map(Solution::toStringSolution);
    return ok().body(solutionMono, new ParameterizedTypeReference<>() {
    });
  }

  private <ProblemT> void validate(SolveRequest<ProblemT> request) {
    Errors errors = new BeanPropertyBindingResult(request, "request");
    validator.validate(request, errors);
    if (errors.hasErrors()) {
      throw new ServerWebInputException(errors.toString());
    }
  }

  @Bean
  RouterFunction<ServerResponse> getSolutionRoutes() {
    return metaSolverProvider.getMetaSolvers().stream()
        .map(this::defineSolutionRouteForMetaSolver)
        .reduce(RouterFunction::and)
        .orElseThrow(); // we should always have at least one route or the toolbox is useless
  }

  private RouterFunction<ServerResponse> defineSolutionRouteForMetaSolver(
      MetaSolver<?, ?, ?> metaSolver) {
    var problemType = metaSolver.getProblemType();
    return route().GET(
        // FIXME this is intentionally SOLVE instead of SOLUTION to avoid breaking things
        //  but maybe we should switch the name at some point
        getSolveRouteForProblemType(problemType),
        accept(APPLICATION_JSON),
        req -> handleSolutionRouteForMetaSolver(metaSolver, req),
        ops -> ops
            .operationId(getSolutionRouteForProblemType(problemType))
            .tag(problemType.getId())
            .parameter(parameterBuilder().in(ParameterIn.QUERY).name("id"))
            .response(responseBuilder()
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .implementation(Solution.class)
            )
    ).build();
  }

  private Mono<ServerResponse> handleSolutionRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver,
                                                                ServerRequest req) {
    var solution = req.queryParam("id")
        .map(Long::parseLong)
        .map(solutionId -> metaSolver.getSolutionManager().getSolution(solutionId))
        .map(Solution::toStringSolution)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a solution for this problem with this solution id!"));

    return ok().body(Mono.just(solution), new ParameterizedTypeReference<>() {
    });
  }

  private void handleRouteDocumentation(MetaSolver<?, ?, ?> metaSolver, Builder ops) {
    var problemType = metaSolver.getProblemType();
    ops
        .operationId(getSolveRouteForProblemType(problemType))
        .tag(problemType.getId())
        .description("Solves a " + problemType.getId() + " problem. To solve the problem, "
            + "either the meta-solver will choose the best available solver,"
            + "or a specific solver selected in the request will be used.")
        .requestBody(requestBodyBuilder()
                .content(getRequestContent(metaSolver))
                .required(true))
        .response(getResponseOk(metaSolver));
  }

  private static org.springdoc.core.fn.builders.apiresponse.Builder getResponseOk(
          MetaSolver<?, ?, ?> metaSolver) {
    return responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .content(contentBuilder()
                    .mediaType(APPLICATION_JSON_VALUE)
                    .example(getExampleSolved(metaSolver))
                    .example(getExampleInvalid(metaSolver))
                    .array(arraySchemaBuilder().schema(
                            schemaBuilder().implementation(Solution.class))));
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleSolved(
          MetaSolver<?, ?, ?> metaSolver) {
    return getExampleOk(
            "Solved",
            metaSolver,
            solution -> {
              solution.setSolutionData("Solution data to solve the problem");
              solution.complete();
            });
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleInvalid(
          MetaSolver<?, ?, ?> metaSolver) {
    return getExampleOk(
            "Error",
            metaSolver,
            solution -> {
              solution.setDebugData("Some error occurred");
              solution.abort();
            });
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleOk(
          String exampleName,
          MetaSolver<?, ?, ?> metaSolver,
          Consumer<Solution<String>> solutionModifier) {
    // Prepare a solved solution with some example data
    var solvedSolution = new Solution<String>(42);
    solvedSolution.setExecutionMilliseconds(42);
    metaSolver.getAllSolvers().stream()
            .findFirst()
            .ifPresent(solver -> solvedSolution.setSolverName(solver.getName()));
    solutionModifier.accept(solvedSolution);

    // Convert the solution to a string
    String solvedSolutionString;
    try {
      solvedSolutionString = new ObjectMapper().writeValueAsString(solvedSolution);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("example could not be parsed", e);
    }

    // Build the example
    return org.springdoc.core.fn.builders.exampleobject.Builder
            .exampleOjectBuilder()
            .name(exampleName)
            .description("The problem was solved successfully.")
            .value(solvedSolutionString);
  }

  private org.springdoc.core.fn.builders.content.Builder getRequestContent(
          MetaSolver<?, ?, ?> metaSolver) {
    String content = metaSolver.getExampleProblems()
            .stream().findFirst()
            .map(e -> {
              if (e instanceof String) {
                return (String) e;
              }

              try {
                return new ObjectMapper().writeValueAsString(e);
              } catch (JsonProcessingException exception) {
                throw new RuntimeException("example could not be parsed", exception);
              }
            })
            .orElseThrow(() -> new RuntimeException("no example available"));

    var request = new SolveRequest<String>();
    request.requestContent = content;
    request.requestedMetaSolverSettings = metaSolver.getSettings();

    metaSolver.getAllSolvers().stream()
            .findFirst()
            .ifPresentOrElse(solver -> {
              request.requestedSolverId = solver.getId();
              request.requestedSubSolveRequests = solver.getSubRoutines().stream()
                      .collect(Collectors.toMap(
                              SubRoutineDefinition::type,
                              subRoutine -> {
                                var subSolveRequest = new SolveRequest<>();

                                subSolveRequest.requestedSolverId = metaSolverProvider
                                        .getMetaSolver(subRoutine.type())
                                        .getAllSolvers().stream()
                                        .findFirst()
                                        .map(ProblemSolver::getId)
                                        .orElse("");
                                return subSolveRequest;
                              }));
            }, () -> {
              throw new RuntimeException("no solver found");
            });

    String requestString;
    try {
      requestString = new ObjectMapper().writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      throw new RuntimeException("no example available", exception);
    }

    var problemType = metaSolver.getProblemType();
    return contentBuilder()
            .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
                    .name(problemType.getId())
                    .value(requestString))
            .schema(schemaBuilder().implementation(
                    problemType.getRequestType()))
            .mediaType(APPLICATION_JSON_VALUE);
  }

  private String getSolveRouteForProblemType(ProblemType type) {
    return "/solve/" + type.getId();
  }

  private String getSolutionRouteForProblemType(ProblemType type) {
    return "/solution/" + type.getId();
  }
}
