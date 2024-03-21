package edu.kit.provideq.toolbox.vrp.clusterer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;

@Component
public class NoClusteringClusterer extends VrpClusterer {
    
    @Autowired
    public NoClusteringClusterer(
        ApplicationContext context) {
        super("", "", context);
    }

    @Override
    public String getName() {
        return "No Clustering VRP Clusterer (Classical)";
    }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution, SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
        var vrpSolver = subRoutinePool.<String, String>getSubRoutine(ProblemType.VRP);

        var vrpSolution = vrpSolver.apply(problem.problemData());

        solution.setSolutionData(vrpSolution.getSolutionData());
        solution.complete();
    }

}
