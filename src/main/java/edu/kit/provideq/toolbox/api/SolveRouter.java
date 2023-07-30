package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.featuremodel.anomaly.MetaSolverFeatureModelAnomaly;
import edu.kit.provideq.toolbox.maxcut.MetaSolverMaxCut;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.sat.MetaSolverSat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Set;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
        String problemId = metaSolver.getClass().getSimpleName(); // FIXME
        return route().POST(
                "/solve/" + problemId,
                accept(APPLICATION_JSON),
                req -> ok().build(),
                ops -> ops.operationId("solve-" + problemId)
        ).build();
    }
}
