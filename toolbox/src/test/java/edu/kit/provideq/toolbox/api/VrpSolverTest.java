package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.solvers.qubo.QuboConfiguration.QUBO;
import static edu.kit.provideq.toolbox.solvers.tsp.TspConfiguration.TSP;
import static edu.kit.provideq.toolbox.solvers.vrp.VrpConfiguration.VRP;
import static edu.kit.provideq.toolbox.solvers.vrp.clusterer.VrpClustererConfiguration.CLUSTER_VRP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.api.dto.SubProblemReferenceDto;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.solvers.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.solvers.tsp.solvers.QuboTspSolver;
import edu.kit.provideq.toolbox.solvers.vrp.clusterer.KmeansClusterer;
import edu.kit.provideq.toolbox.solvers.vrp.clusterer.TwoPhaseClusterer;
import edu.kit.provideq.toolbox.solvers.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.solvers.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.solvers.vrp.solvers.QrispVrpSolver;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureWebTestClient
class VrpSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Autowired
  private ConversionRegistry conversionRegistry;

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

  @Autowired
  private LkhVrpSolver lkhVrpSolver;

  @Autowired
  private QrispVrpSolver qrispVrpSolver;

  private ProblemManager<?, ?> problemManager;
  private List<? extends FormattedValue<?>> problems;

  @BeforeEach
  void beforeEach() {
    this.client = this.client.mutate()
        .responseTimeout(Duration.ofSeconds(300))
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
    for (FormattedValue<?> problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, lkh3vrpSolver, problem, VRP);
      ApiTestHelper.testSolution(problemDto, problemManagerProvider, conversionRegistry);
    }
  }

  /**
   * Tests the Grover-based Qrisp VRP solver on the smallest example, which is sized for
   * quantum simulation.
   */
  @Test
  void testQrispVrpSolver() {
    var smallProblem = problems.stream()
        .filter(problem -> problem.toString().contains("NAME : test"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing VerySmallSampleForGrover example"));

    var problemDto = ApiTestHelper.createProblem(client, qrispVrpSolver, smallProblem, VRP);
    problemDto = ApiTestHelper.trySolveFor(300, client, problemDto.getId(), VRP);
    ApiTestHelper.testSolution(problemDto, problemManagerProvider, conversionRegistry);
  }

  /**
   * test LKH-3 solver in combination with k-means = 3.
   */
  @Test
  void testKmeansWithLkhForVrp() throws Exception {
    for (FormattedValue<?> problem : problems) {
      //skip the small "test" problem because clustering small problems
      //can lead to errors
      if (problem.toString().contains("DIMENSION : 3")) {
        continue;
      }

      //create VRP problem, has Clusterable VRP as subproblem
      var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem, VRP);
      assertEquals(ProblemState.SOLVING, problemDto.getState(), problemDto.toString());
      problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, VRP);

      //check if subproblem is set correctly
      List<SubProblemReferenceDto> vrpSubProblems = problemDto.getSubProblems();
      //there should be exactly one subproblem, the vrp that needs to be clustered:
      assertEquals(1, vrpSubProblems.size(), vrpSubProblems.toString());
      SubProblemReferenceDto vrpProblem = vrpSubProblems.getFirst();
      List<String> clusterSubProblems = vrpProblem.getSubProblemIds();
      //there should also only one subproblem Id
      assertEquals(1, clusterSubProblems.size(), clusterSubProblems.toString());
      assertEquals(vrpProblem.getSubRoutine().getTypeId(), CLUSTER_VRP.getId());

      //set k-means as CLUSTER_VRP solver:
      var clustererDto =
          ApiTestHelper.setProblemSolver(client, kmeansClusterer, clusterSubProblems.getFirst(),
              CLUSTER_VRP.getId());
      clustererDto = ApiTestHelper.waitForSubProblems(30, client, clustererDto, CLUSTER_VRP);

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
          ApiTestHelper.testSolution(vrpClusterProblem, problemManagerProvider, conversionRegistry);
        }
      }

      //solve the problem:
      problemDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), VRP);
      ApiTestHelper.testSolution(problemDto, problemManagerProvider, conversionRegistry);
    }
  }

  /**
   * Tests the two phase clusterer in combination with Lkh-3 tsp solver.
   */
  @Test
  void testTwoPhaseWithLkhForTsp() {
    for (FormattedValue<?> problem : problems) {
      var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem, VRP);
      assertEquals(ProblemState.SOLVING, problemDto.getState(), problemDto.toString());
      problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, VRP);

      //set two-phase as CLUSTER_VRP solver:
      var clusterSubProblems = problemDto.getSubProblems().getFirst().getSubProblemIds();
      var clustererDto = ApiTestHelper.setProblemSolver(
          client,
          twoPhaseClusterer,
          clusterSubProblems.getFirst(),
          CLUSTER_VRP.getId());
      clustererDto = ApiTestHelper.waitForSubProblems(30, client, clustererDto, CLUSTER_VRP);

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
          ApiTestHelper.testSolution(tspClusterProblem, problemManagerProvider, conversionRegistry);
        }
      }

      //solve the problem:
      var solvedProblemDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), VRP);
      ApiTestHelper.testSolution(solvedProblemDto, problemManagerProvider, conversionRegistry);
    }
  }

  /**
   * Tests the two phase clusterer in combination with a qubo transformation and quantum annealer.
   */
  @Test
  void testTwoPhaseWithAnnealer() {
    //get the small problem, cause quantum simulation is used:
    FormattedValue<?> problem = null;
    for (var element : problems) {
      if (element.toString().contains("NAME : small sample")) {
        problem = element;
        break;
      }
    }
    assertTrue(problem != null);

    var problemDto = ApiTestHelper.createProblem(client, abstractClusterer, problem, VRP);
    assertEquals(ProblemState.SOLVING, problemDto.getState(), problemDto.toString());
    problemDto = ApiTestHelper.waitForSubProblems(30, client, problemDto, VRP);

    //set two-phase as CLUSTER_VRP solver:
    var clusterSubProblems = problemDto.getSubProblems().getFirst().getSubProblemIds();
    var clustererDto = ApiTestHelper.setProblemSolver(
        client,
        twoPhaseClusterer,
        clusterSubProblems.getFirst(),
        CLUSTER_VRP.getId());
    clustererDto = ApiTestHelper.waitForSubProblems(30, client, clustererDto, CLUSTER_VRP);

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
        tspClusterProblem = ApiTestHelper.waitForSubProblems(30, client, tspClusterProblem, TSP);

        //set d-wave annealer as qubo solver:
        assertEquals(1, tspClusterProblem.getSubProblems().size(), tspClusterProblem.toString());
        var quboProblem = tspClusterProblem.getSubProblems().getFirst();
        assertEquals(1, quboProblem.getSubProblemIds().size(), quboProblem.toString());
        assertEquals(quboProblem.getSubRoutine().getTypeId(), QUBO.getId(), quboProblem.toString());
        ApiTestHelper.setProblemSolver(client, dwaveQuboSolver,
            quboProblem.getSubProblemIds().getFirst(), QUBO.getId());
      }
    }

    //solve the problem:
    var solvedProblemDto = ApiTestHelper.trySolveFor(120, client, problemDto.getId(), VRP);
    ApiTestHelper.testSolution(solvedProblemDto, problemManagerProvider, conversionRegistry);
  }
}