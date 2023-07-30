package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.*;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

import java.util.List;

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
        String problemId = metaSolver.getProblemType().getId();
        return route().POST(
                "/solve/" + problemId,
                accept(APPLICATION_JSON),
                req -> handleRouteForMetaSolver(metaSolver, req),
                ops -> ops
                        .operationId("/solve/" + problemId)
                        .tag(problemId)
                        .requestBody(requestBodyBuilder()
                                .content(contentBuilder()
                                        .schema(schemaBuilder().implementation(metaSolver.getProblemType().getRequestType()))
                                        .mediaType(APPLICATION_JSON_VALUE)
                                )
                                .required(true)
                        )
                        .response(responseBuilder()
                                .responseCode(String.valueOf(HttpStatus.OK.value())).implementation(SolutionHandle.class)
                        )
        ).build();
    }

    private <ProblemT, SolutionT> Mono<ServerResponse> handleRouteForMetaSolver(MetaSolver<ProblemT, SolutionT, ?> metaSolver, ServerRequest req) {
        var solutionMono = req
                .bodyToMono(new ParameterizedTypeReference<SolveRequest<ProblemT>>() {})
                .doOnNext(this::validate)
                .map(metaSolver::solve)
                .map(Solution::toStringSolution);
        return ok().body(solutionMono, new ParameterizedTypeReference<>() {});
    }
    private <ProblemT> void validate(SolveRequest<ProblemT> request) {
        Errors errors = new BeanPropertyBindingResult(request, "request");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }

    @Bean
    RouterFunction<ServerResponse> getSubRoutineRoutes() {
        return metaSolverProvider.getMetaSolvers().stream()
                .map(this::defineSubRoutineRouteForMetaSolver)
                .reduce(RouterFunction::and)
                .orElseThrow();
    }

    private RouterFunction<ServerResponse> defineSubRoutineRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        String problemId = metaSolver.getProblemType().getId();
        return route().GET(
                "/sub-routines/" + problemId,
                req -> handleSubRoutineRouteForMetaSolver(metaSolver, req),
                ops -> ops
                        .operationId("/sub-routines/" + problemId)
                        .parameter(parameterBuilder().in(ParameterIn.QUERY).name("id"))
                        .tag(problemId)
                        .response(responseBuilder()
                                .responseCode(String.valueOf(HttpStatus.OK.value()))
                                .content(contentBuilder()
                                        .mediaType(APPLICATION_JSON_VALUE)
                                        .array(arraySchemaBuilder().schema(schemaBuilder().implementation(SubRoutineDefinition.class)))
                                )
                        )
        ).build();
    }

    private Mono<ServerResponse> handleSubRoutineRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver, ServerRequest req) {
        var subroutines = req.queryParam("id")
                .flatMap(metaSolver::getSolver)
                .map(ProblemSolver::getSubRoutines)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find a solver for this problem with this solver id!"));

        return ok().body(Mono.just(subroutines), new ParameterizedTypeReference<>() {});
    }

    @Bean
    RouterFunction<ServerResponse> getSolversRoutes() {
        return metaSolverProvider.getMetaSolvers().stream()
                .map(this::defineSolversRouteForMetaSolver)
                .reduce(RouterFunction::and)
                .orElseThrow();
    }

    private RouterFunction<ServerResponse> defineSolversRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        String problemId = metaSolver.getProblemType().getId();
        return route().GET(
                "/solvers/" + problemId,
                req -> handleSolversRouteForMetaSolver(metaSolver),
                ops -> ops
                        .operationId("/solvers/" + problemId)
                        .tag(problemId)
                        .response(responseBuilder()
                                .responseCode(String.valueOf(HttpStatus.OK.value()))
                                .content(contentBuilder()
                                        .mediaType(APPLICATION_JSON_VALUE)
                                        .array(arraySchemaBuilder().schema(schemaBuilder().implementation(ProblemSolverInfo.class)))
                                )
                        )
        ).build();
    }

    private Mono<ServerResponse> handleSolversRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        var solvers = metaSolver.getAllSolvers().stream()
                .map(solver -> new ProblemSolverInfo(solver.getId(), solver.getName()))
                .toList();

        return ok().body(Mono.just(solvers), new ParameterizedTypeReference<>() {});
    }

    @Bean
    RouterFunction<ServerResponse> getMetaSolverSettingsRoutes() {
        return metaSolverProvider.getMetaSolvers().stream()
                .map(this::defineMetaSolverSettingsRouteForMetaSolver)
                .reduce(RouterFunction::and)
                .orElseThrow();
    }

    private RouterFunction<ServerResponse> defineMetaSolverSettingsRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        String problemId = metaSolver.getProblemType().getId();
        return route().GET(
                "/meta-solver/settings/" + problemId,
                req -> handleMetaSolverSettingsRouteForMetaSolver(metaSolver),
                ops -> ops
                        .operationId("/meta-solver/settings/" + problemId)
                        .tag(problemId)
                        .response(responseBuilder()
                                .responseCode(String.valueOf(HttpStatus.OK.value()))
                                .content(contentBuilder()
                                        .mediaType(APPLICATION_JSON_VALUE)
                                        .array(arraySchemaBuilder().schema(schemaBuilder().implementation(MetaSolverSetting.class)))
                                )
                        )
        ).build();
    }

    private Mono<ServerResponse> handleMetaSolverSettingsRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        return ok().body(Mono.just(metaSolver.getSettings()), new ParameterizedTypeReference<>() {});
    }

    @Bean
    RouterFunction<ServerResponse> getSolutionRoutes() {
        return metaSolverProvider.getMetaSolvers().stream()
                .map(this::defineSolutionRouteForMetaSolver)
                .reduce(RouterFunction::and)
                .orElseThrow(); // we should always have at least one route or the toolbox is useless
    }

    private RouterFunction<ServerResponse> defineSolutionRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        String problemId = metaSolver.getProblemType().getId();
        return route().GET(
                "/solve/" + problemId,
                accept(APPLICATION_JSON),
                req -> handleSolutionRouteForMetaSolver(metaSolver, req),
                ops -> ops
                        .operationId("/solution/" + problemId)
                        .tag(problemId)
                        .parameter(parameterBuilder().in(ParameterIn.QUERY).name("id"))
                        .response(responseBuilder()
                                .responseCode(String.valueOf(HttpStatus.OK.value())).implementation(SolutionHandle.class)
                        )
        ).build();
    }

    private Mono<ServerResponse> handleSolutionRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver, ServerRequest req) {
        var solution = req.queryParam("id")
                .map(Long::parseLong)
                .map(solutionId -> metaSolver.getSolutionManager().getSolution(solutionId))
                .map(Solution::toStringSolution)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find a solution for this problem with this solution id!"));

        // yes, solution is of type `Solution<String>`. No idea why `toStringSolution` returns `SolutionHandle`
        return ok().body(Mono.just(solution), new ParameterizedTypeReference<Solution<String>>() {});
    }
}
