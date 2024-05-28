package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static edu.kit.provideq.toolbox.vrp.VrpConfiguration.VRP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public class VrpSolverTest {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ProblemManagerProvider problemManagerProvider;

    @Autowired
    private LkhVrpSolver lkh3Solver;

    private ProblemManager<String, String> problemManager;
    private List<String> problems;

    @BeforeEach
    void beforeEach() {
        this.client = this.client.mutate()
                .responseTimeout(Duration.ofSeconds(60))
                .build();
        problemManager = problemManagerProvider.findProblemManagerForType(VRP).get();
        problems = problemManager.getExampleInstances()
                .stream()
                .map(Problem::getInput)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Test
    void testLkh3SolverIsolated() {
        for (String problem : problems) {
            var problemDto = ApiTestHelper.createProblem(client, lkh3Solver, problem, VRP);
            assertEquals(ProblemState.SOLVED, problemDto.getState());
            assertNotNull(problemDto.getSolution());
            assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
        }
    }

    @Test
    void testQrispGroverIsolated() {
        //only run on very small example:
        for (var problem : problems) {
            if (problem.contains("DIMENSION : 3")) {
                var problemDto = ApiTestHelper.createProblem(client, lkh3Solver, problem, VRP);
                assertEquals(ProblemState.SOLVED, problemDto.getState());
                assertNotNull(problemDto.getSolution());
                assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
                break;
            }
        }
    }

}
