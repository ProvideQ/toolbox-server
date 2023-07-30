package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.featuremodel.anomaly.MetaSolverFeatureModelAnomaly;
import edu.kit.provideq.toolbox.maxcut.MetaSolverMaxCut;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
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

    public SolveRouter(MetaSolverSat sat, MetaSolverMaxCut maxCut, MetaSolverFeatureModelAnomaly featureModelAnomaly) {
        this.metaSolvers = Set.of(sat, maxCut, featureModelAnomaly);
    }

    @Bean
    RouterFunction<ServerResponse> getRoutes() {
        return metaSolvers.stream()
                .map(this::defineRouteForMetaSolver)
                .reduce(RouterFunction::and)
                .orElseThrow(); // we should always have at least one route or the toolbox is useless
    }

    private RouterFunction<ServerResponse> defineRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        String problemId = metaSolver.getProblemType().getId();
        return route().POST(
                "/solve2/" + problemId,
                accept(APPLICATION_JSON),
                req -> handleRouteForMetaSolver(metaSolver),
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
                                .responseCode("200").implementation(SolutionHandle.class)
                        )
        ).build();
    }
    private Mono<ServerResponse> handleRouteForMetaSolver(MetaSolver<?, ?, ?> metaSolver) {
        return ok().build();
    }
}
