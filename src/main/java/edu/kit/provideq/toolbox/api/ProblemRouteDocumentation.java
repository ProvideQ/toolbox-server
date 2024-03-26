package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.api.ProblemRouter.PROBLEM_ID_PARAM_NAME;
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.exampleobject.Builder.exampleOjectBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
        .response(buildProblemResponse(manager));
  }

  static void configureReadDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "read"))
        .tag(type.getId())
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(buildProblemResponse(manager));
  }

  public static void configureListDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "list"))
        .tag(type.getId())
        .response(buildProblemListResponse(manager));
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
        .response(buildProblemResponse(manager));
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
        .example(exampleOjectBuilder()
            .name(problemType.getId())
            .value(requestString))
        .schema(schemaBuilder()
            .implementation(ProblemDto.class))
        .mediaType(APPLICATION_JSON_VALUE);
  }

  private static org.springdoc.core.fn.builders.apiresponse.Builder buildProblemListResponse(
      ProblemManager<?, ?> manager) {
    var problemDtos = manager.getExampleInstances().stream()
        .map(ProblemDto::fromProblem)
        .toList();

    String problemDtosJson;
    try {
      problemDtosJson = new ObjectMapper().writeValueAsString(problemDtos);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Could not serialize example problems!", e);
    }

    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder()
            .array(arraySchemaBuilder()
                .schema(schemaBuilder().implementation(ProblemDto.class))
            )
            .example(exampleOjectBuilder()
                .value(problemDtosJson)
            )
        );
  }


  private static org.springdoc.core.fn.builders.apiresponse.Builder buildProblemResponse(
      ProblemManager<?, ?> problemManager) {
    var contentBuilder = contentBuilder()
        .mediaType(APPLICATION_JSON_VALUE)
        .schema(schemaBuilder().implementation(ProblemDto.class));

    // getExampleSolved, getExampleInvalid
    int exampleProblemIndex = 1;
    for (var exampleProblem : problemManager.getExampleInstances()) {
      contentBuilder = contentBuilder.example(
          buildExample(exampleProblem, "Example " + exampleProblemIndex)
      );
    }

    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder);
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder buildExample(
      Problem<?, ?> exampleProblem, String name) {
    String problemDtoJson;
    try {
      var problemDto = ProblemDto.fromProblem(exampleProblem);
      problemDtoJson = new ObjectMapper().writeValueAsString(problemDto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Example problem could not be parsed!", e);
    }

    return exampleOjectBuilder()
        .name(name)
        .value(problemDtoJson);
  }

  private static String getOperationId(ProblemType<?, ?> type, String operationName) {
    return "/%s/%s/%s".formatted(ENDPOINT_NAME, type.getId(), operationName);
  }
}
