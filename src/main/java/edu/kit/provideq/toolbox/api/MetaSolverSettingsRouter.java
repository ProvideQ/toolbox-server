//package edu.kit.provideq.toolbox.api;
//
//import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
//import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
//import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
//import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
//import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
//import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
//import static org.springframework.web.reactive.function.server.ServerResponse.ok;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import edu.kit.provideq.toolbox.ProblemManager;
//import edu.kit.provideq.toolbox.ProblemManagerProvider;
//import edu.kit.provideq.toolbox.meta.ProblemType;
//import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
//import org.springdoc.core.fn.builders.operation.Builder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.reactive.config.EnableWebFlux;
//import org.springframework.web.reactive.function.server.RouterFunction;
//import org.springframework.web.reactive.function.server.ServerResponse;
//import reactor.core.publisher.Mono;
//
///**
// * This router handles requests to the GET {@code /meta-solver/{problemType}/settings} endpoints.
// * Responses are generated from the settings reported by the available meta-solvers
// * (see {@link ProblemManager#getSettings()}).
// */
//@Configuration
//@EnableWebFlux
//public class MetaSolverSettingsRouter {
//  private final ProblemManagerProvider problemManagerProvider;
//
//  @Autowired
//  public MetaSolverSettingsRouter(ProblemManagerProvider problemManagerProvider) {
//    this.problemManagerProvider = problemManagerProvider;
//  }
//
//  @Bean
//  RouterFunction<ServerResponse> getMetaSolverSettingsRoutes() {
//    return problemManagerProvider.getProblemManagers().stream()
//        .map(this::defineMetaSolverSettingsRouteForProblemManager)
//        .reduce(RouterFunction::and)
//        .orElseThrow();
//  }
//
//  private RouterFunction<ServerResponse> defineMetaSolverSettingsRouteForProblemManager(
//      ProblemManager<?, ?> problemManager) {
//    var problemType = problemManager.getProblemType();
//    return route().GET(
//        getRouteForProblemType(problemType),
//        req -> handleMetaSolverSettingsRouteForProblemManager(problemManager),
//        ops -> handleMetaSolverSettingsRouteDocumentation(ops, problemManager)
//    ).build();
//  }
//
//  private void handleMetaSolverSettingsRouteDocumentation(
//          Builder ops, ProblemManager<?, ?> problemManager) {
//    var problemType = problemManager.getProblemType();
//    ops
//            .operationId(getRouteForProblemType(problemType))
//            .tag(problemType.getId())
//            .description(("Returns the selection of settings available for of the "
//            + problemType.getId() + " meta-solver. Settings can be used to configure"
//            + " what the meat solver considers to choose the best solver."))
//            .response(getResponseOk(problemManager));
//  }
//
//  private static org.springdoc.core.fn.builders.apiresponse.Builder getResponseOk(
//          ProblemManager<?, ?> problemManager) {
//    String example;
//    try {
//      example = new ObjectMapper().writeValueAsString(problemManager.getSettings());
//    } catch (JsonProcessingException e) {
//      throw new RuntimeException("example could not be parsed", e);
//    }
//
//    return responseBuilder()
//            .responseCode(String.valueOf(HttpStatus.OK.value()))
//            .content(contentBuilder()
//                    .mediaType(APPLICATION_JSON_VALUE)
//                    .example(org.springdoc.core.fn.builders.exampleobject.Builder
//                            .exampleOjectBuilder()
//                            .name(problemManager.getProblemType().getId())
//                            .value(example))
//                    .array(arraySchemaBuilder().schema(
//                            schemaBuilder().implementation(MetaSolverSetting.class))
//                    )
//            );
//  }
//
//  private Mono<ServerResponse> handleMetaSolverSettingsRouteForProblemManager(
//      ProblemManager<?, ?> problemManager) {
//    return ok().body(Mono.just(problemManager.getSettings()), new ParameterizedTypeReference<>() {
//    });
//  }
//
//  private String getRouteForProblemType(ProblemType type) {
//    return "/meta-solver/settings/" + type.getId();
//  }
//}
