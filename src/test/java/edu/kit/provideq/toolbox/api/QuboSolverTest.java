package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.stream.Stream;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
class QuboSolverTest {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ProblemManagerProvider problemManagerProvider;

    void beforeEach() {
        this.client = this.client.mutate()
                .responseTimeout(Duration.ofSeconds(60))
                .build();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    Stream<Arguments> provideArguments() {
        var problemManager = problemManagerProvider.findProblemManagerForType(QUBO).get();

        return ApiTestHelper.getAllArgumentCombinations(problemManager)
                .map(list -> Arguments.of(list.get(0), list.get(1)));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void testQuboSolvers(ProblemSolver<String, String> solver, String input) {
        var problemDto = ApiTestHelper.createProblem(client, solver, input, QUBO);

        System.out.println("Solver: " + solver);
        System.out.println("Solution: " + problemDto.getSolution());
        System.out.println("Debug Data: " + problemDto.getSolution().getDebugData());

        assertEquals(ProblemState.SOLVED, problemDto.getState());
        assertNotNull(problemDto.getSolution());
        assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
    }
}
