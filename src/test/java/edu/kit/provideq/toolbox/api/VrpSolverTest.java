package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.vrp.clusterer.KmeansClusterer;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
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
import static edu.kit.provideq.toolbox.vrp.clusterer.ClusterVrpConfiguration.CLUSTER_VRP;
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

    @Autowired
    private ClusterAndSolveVrpSolver clusterer;

    @Autowired
    private KmeansClusterer kmeansSolver;

    private ProblemManager<String, String> problemManager;
    private List<String> problems;
    @Autowired
    private LkhVrpSolver lkhVrpSolver;

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

    @Test
    void testKmeansWithLkh() {
        for (String problem : problems) {
            //skip the small "test" problem because clustering small problems
            //can lead to errors
            if (problem.contains("NAME : test")) {
              continue;
            }

            //create VRP problem, has Clusterable VRP as subproblem
            var problemDto = ApiTestHelper.createProblem(client, clusterer, problem, VRP);
            assertEquals(ProblemState.SOLVING, problemDto.getState());

            //check if subproblem is set correctly
            List<SubProblemReferenceDto> vrpSubProblems = problemDto.getSubProblems();
            assertEquals(vrpSubProblems.size(), 1); //there should be exactly one subproblem, the vrp that needs to be clustered
            SubProblemReferenceDto vrpProblem = vrpSubProblems.get(0);
            List<String> clusterSubProblems = vrpProblem.getSubProblemIds();
            assertEquals(clusterSubProblems.size(), 1); //there should also only one subproblem Id
            assertEquals(vrpProblem.getSubRoutine().getTypeId(), CLUSTER_VRP.getId());

            //set k-means as CLUSTER_VRP solver:
            var clustererDto = ApiTestHelper.setProblemSolver(client, kmeansSolver, clusterSubProblems.get(0), CLUSTER_VRP.getId());

            //solve sub-problems (clusters):
            var vrpClusters = clustererDto.getSubProblems();
            for (var cluster : vrpClusters) {
                assertEquals(cluster.getSubRoutine().getTypeId(), VRP.getId()); //check if subproblem is VRP again
                for (String problemId : cluster.getSubProblemIds()) {
                    //set lkh-3 as solver:
                    var vrpClusterProblem = ApiTestHelper.setProblemSolver(client, lkhVrpSolver, problemId, VRP.getId());
                    assertNotNull(vrpClusterProblem.getInput());
                    assertNotNull(vrpClusterProblem.getSolution());
                    assertEquals(ProblemState.SOLVED, vrpClusterProblem.getState());
                    System.out.println("Solved some LKH :)");
                }
            }

            //solve the problem:
            problemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), VRP);

            System.out.println("Solved Original Problem:");
            System.out.println(problemDto.getSolution());

            assertNotNull(problemDto.getSolution());
            assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
            assertEquals(ProblemState.SOLVED, problemDto.getState());
        }
    }
}
