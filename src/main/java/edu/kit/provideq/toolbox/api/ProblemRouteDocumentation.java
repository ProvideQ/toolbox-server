package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.api.ProblemRouter.PROBLEM_ID_PARAM_NAME;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.function.Consumer;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;

/**
 * Helper class for configuring OpenAPI documentation on the {@link ProblemRouter} routes.
 */
final class ProblemRouteDocumentation {
  private static final String ENDPOINT_NAME = "problems";

  static void configureCreateDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "create"))
        .tag(type.getId())
        .description("Solves a " + type.getId() + " problem. To solve the problem, "
            + "either the meta-solver will choose the best available solver,"
            + "or a specific solver selected in the request will be used.")
        .requestBody(requestBodyBuilder()
            .content(getRequestContent(manager))
            .required(true))
        .response(getResponseOk(manager));
  }

  static void configureReadDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "read"))
        .tag(type.getId())
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .implementation(ProblemDto.class)
        );
  }

  public static void configureListDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "list"))
        .tag(type.getId())
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .implementationArray(ProblemDto.class)
        );
  }

  static void configureUpdateDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "update"))
        .tag(type.getId())
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .requestBody(requestBodyBuilder()
            .content(getRequestContent(manager))
            .required(true))
        .response(responseBuilder()
            .responseCode(String.valueOf(HttpStatus.OK.value()))
            .implementation(ProblemDto.class)
        );
    // TODO missing error codes and examples
  }

  private static <InputT, ResultT> org.springdoc.core.fn.builders.content.Builder getRequestContent(
      ProblemManager<InputT, ResultT> manager) {
    var exampleProblem = manager.getExampleInstances().stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException(
            "No example available for problem " + manager.getType() + "!"));


    var request = ProblemDto.fromProblem(exampleProblem);

    String requestString;
    try {
      requestString = new ObjectMapper().writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      throw new RuntimeException("no example available", exception);
    }

    var problemType = manager.getType();
    return contentBuilder()
        .example(org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder()
            .name(problemType.getId())
            .value(requestString))
        .schema(schemaBuilder()
            .implementation(ProblemDto.class))
        .mediaType(APPLICATION_JSON_VALUE);
  }

  private static org.springdoc.core.fn.builders.apiresponse.Builder getResponseOk(
      ProblemManager<?, ?> problemManager) {
    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder()
            .mediaType(APPLICATION_JSON_VALUE)
            .example(getExampleSolved(problemManager))
            .example(getExampleInvalid(problemManager))
            .array(arraySchemaBuilder().schema(
                schemaBuilder().implementation(Solution.class))));
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleOk(
      String exampleName,
      ProblemManager<?, ?> problemManager,
      Consumer<Solution<String>> solutionModifier
  ) {
    // Prepare a solved solution with some example data
    var solvedSolution = new Solution<String>();
    solvedSolution.setExecutionMilliseconds(42);
    problemManager.getSolvers().stream()
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

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleSolved(
      ProblemManager<?, ?> problemManager) {
    return getExampleOk(
        "Solved",
        problemManager,
        solution -> {
          solution.setSolutionData("Solution data to solve the problem");
          solution.complete();
        });
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder getExampleInvalid(
      ProblemManager<?, ?> problemManager) {
    return getExampleOk(
        "Error",
        problemManager,
        solution -> {
          solution.setDebugData("Some error occurred");
          solution.abort();
        });
  }

  private static String getOperationId(ProblemType<?, ?> type, String operationName) {
    return "/%s/%s/%s".formatted(ENDPOINT_NAME, type.getId(), operationName);
  }
}
