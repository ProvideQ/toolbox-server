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
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.boot.json.JsonParseException;
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
        .description("Creates a problem of type '" + type.getId() + "'. "
            + "Optionally, the caller can submit a request body with this request that acts like "
            + "an update / patch request applied immediately after creating the problem. "
            + "This endpoint will respond with the created problem.")
        .tag(type.getId())
        .requestBody(requestBodyBuilder()
            .content(getRequestContent(manager))
            .required(true))
        .response(buildProblemResponse(manager));
  }

  static void configureReadDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "read"))
        .description("This endpoint can be used to get / read problems of problem type '"
            + type.getId() + "'.")
        .tag(type.getId())
        .parameter(parameterBuilder().in(ParameterIn.PATH).name(PROBLEM_ID_PARAM_NAME))
        .response(buildProblemResponse(manager));
  }

  public static void configureListDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "list"))
        .description("Responds with a list of all problems of type '" + type.getId() + "'. "
            + "This includes all problems created either through the create / post endpoint of "
            + "this problem type or as sub-problems of other problems during the solution process.")
        .tag(type.getId())
        .response(buildProblemListResponse(manager));
  }

  static void configureUpdateDocs(ProblemManager<?, ?> manager, Builder ops) {
    var type = manager.getType();
    ops
        .operationId(getOperationId(type, "update"))
        .description("Updates the problem of type '" + type.getId() + "' with the given problem "
            + "ID."
            + "Only the 'input', 'solverId', 'solverSettings', and 'state' fields can be updated; "
            + "all other fields will be ignored. "
            + "Changes to the input or solver will reset the problem state to 'READY_TO_SOLVE'. "
            + "If the problem is fully configured, changing the state to 'SOLVING' will start the "
            + "solution process. "
            + "Setting the state to another value is not allowed. "
            + "The endpoint will respond with the updated problem.")
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
        .orElseThrow(() -> new MissingExampleException(manager.getType()));


    var request = ProblemDto.fromProblem(exampleProblem);

    String requestString;
    try {
      requestString = new ObjectMapper().writeValueAsString(request);
    } catch (JsonProcessingException exception) {
      throw new MissingExampleException(manager.getType(), exception);
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
      throw new JsonParseException(e);
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
          buildExample(problemManager, exampleProblem, "Example " + exampleProblemIndex)
      );
    }

    return responseBuilder()
        .responseCode(String.valueOf(HttpStatus.OK.value()))
        .content(contentBuilder);
  }

  private static org.springdoc.core.fn.builders.exampleobject.Builder buildExample(
      ProblemManager<?, ?> manager,
      Problem<?, ?> exampleProblem,
      String name) {
    String problemDtoJson;
    try {
      var problemDto = ProblemDto.fromProblem(exampleProblem);
      problemDtoJson = new ObjectMapper().writeValueAsString(problemDto);
    } catch (JsonProcessingException e) {
      throw new MissingExampleException(manager.getType(), e);
    }

    return exampleOjectBuilder()
        .name(name)
        .value(problemDtoJson);
  }

  private static String getOperationId(ProblemType<?, ?> type, String operationName) {
    return "/%s/%s/%s".formatted(ENDPOINT_NAME, type.getId(), operationName);
  }
}
