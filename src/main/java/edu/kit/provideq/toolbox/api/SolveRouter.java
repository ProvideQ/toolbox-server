package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.SolveRequest;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.featuremodel.anomaly.MetaSolverFeatureModelAnomaly;
import edu.kit.provideq.toolbox.maxcut.MetaSolverMaxCut;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.arrayschema.Builder;
import org.springframework.context.ApplicationContext;
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
import java.util.Set;

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
    private final Set<MetaSolver<?, ?, ?>> metaSolvers;
    private final ApplicationContext context;
    private final Validator validator;

    public SolveRouter(MetaSolverSat sat, MetaSolverMaxCut maxCut, MetaSolverFeatureModelAnomaly featureModelAnomaly, ApplicationContext context, Validator validator) {
        this.metaSolvers = Set.of(sat, maxCut, featureModelAnomaly);
        this.context = context;
        this.validator = validator;
    }

    @Bean
    RouterFunction<ServerResponse> getSolveRoutes() {
        return metaSolvers.stream()
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
        var x = req
                .bodyToMono(new ParameterizedTypeReference<SolveRequest<ProblemT>>() {})
                .doOnNext(this::validate)
                .map(request -> solve(metaSolver, request))
                .map(Solution::toStringSolution);
        return ok().body(x, new ParameterizedTypeReference<>() {});
    }
    private <ProblemT> void validate(SolveRequest<ProblemT> request) {
        Errors errors = new BeanPropertyBindingResult(request, "request");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }

    private <ProblemT, SolutionT, SolverT extends ProblemSolver<ProblemT, SolutionT>> Solution<SolutionT>
            solve(MetaSolver<ProblemT, SolutionT, SolverT> metaSolver, SolveRequest<ProblemT> request) {
        Solution<SolutionT> solution = metaSolver.getSolutionManager().createSolution();
        Problem<ProblemT> problem = new Problem<>(request.requestContent, metaSolver.getProblemType());

        SolverT solver = metaSolver
                .getSolver(request.requestedSolverId)
                .orElseGet(() -> metaSolver.findSolver(problem, request.requestedMetaSolverSettings));

        solution.setSolverName(solver.getName());

        SubRoutinePool subRoutinePool =
                request.requestedSubSolveRequests == null
                        ? context.getBean(SubRoutinePool.class)
                        : context.getBean(SubRoutinePool.class, request.requestedSubSolveRequests);

        long start = System.currentTimeMillis();
        solver.solve(problem, solution, subRoutinePool);
        long finish = System.currentTimeMillis();

        solution.setExecutionMilliseconds(finish - start);

        return solution;
    }

    @Bean
    RouterFunction<ServerResponse> getSubRoutineRoutes() {
        return metaSolvers.stream()
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

        return ok().body(subroutines, new ParameterizedTypeReference<List<SubRoutineDefinition>>() {});
    }
}
