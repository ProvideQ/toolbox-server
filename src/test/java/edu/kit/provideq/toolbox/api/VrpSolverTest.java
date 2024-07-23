package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.tsp.TspConfiguration.TSP;
import static edu.kit.provideq.toolbox.vrp.VrpConfiguration.VRP;
import static edu.kit.provideq.toolbox.vrp.clusterer.ClusterVrpConfiguration.CLUSTER_VRP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import edu.kit.provideq.toolbox.vrp.clusterer.KmeansClusterer;
import edu.kit.provideq.toolbox.vrp.clusterer.TwoPhaseClusterer;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public class VrpSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private LkhVrpSolver lkh3vrpSolver;

  @Autowired
  private LkhTspSolver lkh3tspSolver;

  @Autowired
  private ClusterAndSolveVrpSolver abstractClusterer;

  @Autowired
  private KmeansClusterer kmeansClusterer;

  @Autowired
  private TwoPhaseClusterer twoPhaseClusterer;

  @Autowired
  private QuboTspSolver quboTspSolver;

  @Autowired
  private DwaveQuboSolver dwaveQuboSolver;

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
      var problemDto = ApiTestHelper.createProblem(client, lkh3vrpSolver, problem, VRP);
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
        var problemDto = ApiTestHelper.createProblem(client, lkh3vrpSolver, problem, VRP);
        assertEquals(ProblemState.SOLVED, problemDto.getState());
        assertNotNull(problemDto.getSolution());
        assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
        break;
      }
    }
  }

  /**
   * test LKH-3 solver in combination with k-means = 3
   */
  @Test
  void testKmeansWithLkhForVrp() {
    for (String problem : problems) {
      //skip the small "test" problem because clustering small problems
      //can lead to errors
      if (problem.contains("NAME : test")) {
        continue;
      }

      //create VRP problem, has Clusterable VRP as subproblem
      var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem, VRP);
      assertEquals(ProblemState.SOLVING, problemDto.getState());

      //check if subproblem is set correctly
      List<SubProblemReferenceDto> vrpSubProblems = problemDto.getSubProblems();
      assertEquals(vrpSubProblems.size(),
          1); //there should be exactly one subproblem, the vrp that needs to be clustered
      SubProblemReferenceDto vrpProblem = vrpSubProblems.get(0);
      List<String> clusterSubProblems = vrpProblem.getSubProblemIds();
      assertEquals(clusterSubProblems.size(), 1); //there should also only one subproblem Id
      assertEquals(vrpProblem.getSubRoutine().getTypeId(), CLUSTER_VRP.getId());

      //set k-means as CLUSTER_VRP solver:
      var clustererDto =
          ApiTestHelper.setProblemSolver(client, kmeansClusterer, clusterSubProblems.get(0),
              CLUSTER_VRP.getId());

      //solve sub-problems (clusters):
      var vrpClusters = clustererDto.getSubProblems();
      for (var cluster : vrpClusters) {
        assertEquals(cluster.getSubRoutine().getTypeId(),
            VRP.getId()); //check if subproblem is VRP again
        for (String problemId : cluster.getSubProblemIds()) {
          //set lkh-3 as solver:
          var vrpClusterProblem =
              ApiTestHelper.setProblemSolver(client, lkhVrpSolver, problemId, VRP.getId());
          assertNotNull(vrpClusterProblem.getInput());
          assertNotNull(vrpClusterProblem.getSolution());
          assertEquals(ProblemState.SOLVED, vrpClusterProblem.getState());
        }
      }

      //solve the problem:
      problemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), VRP);
      assertNotNull(problemDto.getSolution());
      assertEquals(SolutionStatus.SOLVED, problemDto.getSolution().getStatus());
      assertEquals(ProblemState.SOLVED, problemDto.getState());
    }
  }


  /**
   * Tests the two phase clusterer in combination with Lkh-3 tsp solver.
   */
  @Test
  void testTwoPhaseWithLkhForTsp() {
    for (String problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem, VRP);
      assertEquals(ProblemState.SOLVING, problemDto.getState());

      //set two-phase as CLUSTER_VRP solver:
      var clusterSubProblems = problemDto.getSubProblems().get(0).getSubProblemIds();
      var clustererDto = ApiTestHelper.setProblemSolver(
          client,
          twoPhaseClusterer,
          clusterSubProblems.get(0),
          CLUSTER_VRP.getId());

      //solve sub-problems (clusters):
      var tspClusters = clustererDto.getSubProblems();
      for (var cluster : tspClusters) {
        //check if subproblem is TSP now
        assertEquals(cluster.getSubRoutine().getTypeId(), TSP.getId());
        for (String problemId : cluster.getSubProblemIds()) {
          //set lkh-3 as solver:
          var tspClusterProblem = ApiTestHelper.setProblemSolver(
              client,
              lkh3tspSolver,
              problemId,
              TSP.getId());

          assertNotNull(tspClusterProblem.getInput());
          assertNotNull(tspClusterProblem.getSolution());
          assertEquals(ProblemState.SOLVED, tspClusterProblem.getState());
        }
      }

      //solve the problem:
      var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), VRP);
      assertNotNull(solvedProblemDto.getSolution());
      assertEquals(SolutionStatus.SOLVED, solvedProblemDto.getSolution().getStatus());
      assertEquals(ProblemState.SOLVED, solvedProblemDto.getState());
    }
  }

  /**
   * Tests the two phase clusterer in combination with a qubo transformation and quantum annealer
   */
  @Test
  void testTwoPhaseWithAnnealer() {
    //get the small problem, cause quantum simulation is used:
    var problem = problems.stream().filter(element -> element.contains("NAME : small sample")).findFirst();
    assertTrue(problem.isPresent());

    var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem.get(), VRP);
    assertEquals(ProblemState.SOLVING, problemDto.getState());

    //set two-phase as CLUSTER_VRP solver:
    var clusterSubProblems = problemDto.getSubProblems().get(0).getSubProblemIds();
    var clustererDto = ApiTestHelper.setProblemSolver(
        client,
        twoPhaseClusterer,
        clusterSubProblems.get(0),
        CLUSTER_VRP.getId());

    //solve sub-problems (clusters):
    var tspClusters = clustererDto.getSubProblems();
    for (var cluster : tspClusters) {
      //check if subproblem is TSP now
      assertEquals(cluster.getSubRoutine().getTypeId(), TSP.getId());
      for (String problemId : cluster.getSubProblemIds()) {
        //set QUBO tsp as solver:
        var tspClusterProblem = ApiTestHelper.setProblemSolver(
            client,
            quboTspSolver,
            problemId,
            TSP.getId());

        //set d-wave annealer as qubo solver:
        assertEquals(tspClusterProblem.getSubProblems().size(), 1);
        var quboProblem = tspClusterProblem.getSubProblems().get(0);
        assertEquals(quboProblem.getSubProblemIds().size(), 1);
        assertEquals(quboProblem.getSubRoutine().getTypeId(), QUBO.getId());
        ApiTestHelper.setProblemSolver(client, dwaveQuboSolver, quboProblem.getSubProblemIds().get(0), QUBO.getId());
      }
    }

    //solve the problem:
    var solvedProblemDto = ApiTestHelper.trySolveFor(60, client, problemDto.getId(), VRP);
    assertNotNull(solvedProblemDto.getSolution());
    assertEquals(SolutionStatus.SOLVED, solvedProblemDto.getSolution().getStatus());
    assertEquals(ProblemState.SOLVED, solvedProblemDto.getState());
  }
}
