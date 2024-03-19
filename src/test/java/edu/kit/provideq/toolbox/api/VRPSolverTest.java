package edu.kit.provideq.toolbox.api;

import static edu.kit.provideq.toolbox.SolutionStatus.SOLVED;
import static org.hamcrest.Matchers.is;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboMetaSolver;
import edu.kit.provideq.toolbox.qubo.SolveQuboRequest;
import edu.kit.provideq.toolbox.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QrispQuboSolver;
import edu.kit.provideq.toolbox.MetaSolverProvider;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.vrp.MetaSolverVrp;
import edu.kit.provideq.toolbox.vrp.SolveVrpRequest;
import edu.kit.provideq.toolbox.vrp.clusterer.ClusterVrpRequest;
import edu.kit.provideq.toolbox.vrp.clusterer.KmeansClusterer;
import edu.kit.provideq.toolbox.vrp.clusterer.MetaSolverClusterVrp;
import edu.kit.provideq.toolbox.vrp.clusterer.NoClusteringClusterer;
import edu.kit.provideq.toolbox.vrp.clusterer.TwoPhaseClusterer;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.QuboTspSolver;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebFluxTest
@Import(value = {
    SolveRouter.class,
    MetaSolverClusterVrp.class,
    MetaSolverProvider.class,
    MetaSolverVrp.class,
    ClusterAndSolveVrpSolver.class,
    BinaryProcessRunner.class,
    LkhVrpSolver.class,
    QuboTspSolver.class,
    SubRoutinePool.class,
    ResourceProvider.class,
    NoClusteringClusterer.class,
    TwoPhaseClusterer.class,
    KmeansClusterer.class,
    QuboMetaSolver.class,
    QiskitQuboSolver.class,
    QrispQuboSolver.class,
    DwaveQuboSolver.class
})
class VRPSolverTest {
  @Autowired
  private WebTestClient client;

  @Autowired
  private MetaSolverVrp metaSolverVrp;

  Stream<Arguments> provideArguments() {
    String exampleProblem = metaSolverVrp.getExampleProblems().get(0);

    var simpleLKHRequest = new SolveVrpRequest();

    simpleLKHRequest.requestedSolverId = LkhVrpSolver.class.getName();
    simpleLKHRequest.requestContent = exampleProblem;


    var clusterKmeansAndLKH = new SolveVrpRequest();

    clusterKmeansAndLKH.requestedSolverId = ClusterAndSolveVrpSolver.class.getName();
    clusterKmeansAndLKH.requestContent = exampleProblem;

    var clusterRequest = new ClusterVrpRequest();
    clusterRequest.requestedSolverId = KmeansClusterer.class.getName();

    var solveRequest = new SolveVrpRequest();
    solveRequest.requestedSolverId = LkhVrpSolver.class.getName();

    clusterRequest.requestedSubSolveRequests = Map.of(ProblemType.VRP, solveRequest);
    clusterKmeansAndLKH.requestedSubSolveRequests = Map.of(ProblemType.CLUSTERABLE_VRP, clusterRequest);



    String smallProblem = metaSolverVrp.getExampleProblems().get(1);

    var clusterTspToQuboAndAnneal = new SolveVrpRequest();
    clusterTspToQuboAndAnneal.requestedSolverId = ClusterAndSolveVrpSolver.class.getName();
    clusterTspToQuboAndAnneal.requestContent = smallProblem;

    var clusterTspRequest = new ClusterVrpRequest();
    clusterTspRequest.requestedSolverId = TwoPhaseClusterer.class.getName();

    var transformQuboRequest = new SolveVrpRequest();
    transformQuboRequest.requestedSolverId = QuboTspSolver.class.getName();

    var quboRequest = new SolveQuboRequest();
    quboRequest.requestedSolverId = DwaveQuboSolver.class.getName();

    transformQuboRequest.requestedSubSolveRequests = Map.of(ProblemType.QUBO, quboRequest);
    clusterTspRequest.requestedSubSolveRequests = Map.of(ProblemType.VRP, transformQuboRequest);
    clusterTspToQuboAndAnneal.requestedSubSolveRequests = Map.of(ProblemType.CLUSTERABLE_VRP, clusterTspRequest);

    
    return List.of(simpleLKHRequest, clusterKmeansAndLKH, clusterTspToQuboAndAnneal).stream().map(Arguments::of);
  }

  @BeforeEach
  void setUp() {  
    this.client = this.client
                             .mutate()
                             .responseTimeout(Duration.ofSeconds(60)) // Set to 60 seconds
                             .build();
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  void testVrpSolver(SolveVrpRequest request) {

    var response = client.post()
        .uri("/solve/vrp")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange();


    response.expectStatus().isOk();
    response.expectBody(new ParameterizedTypeReference<Solution<String>>() {
        })
        .value(Solution::getStatus, is(SOLVED));
  }
}
