package edu.kit.provideq.toolbox.vrp.clusterer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;

@Component
public class NoClusteringClusterer extends VrpClusterer {
    private final ApplicationContext context;
    
    @Autowired
    public NoClusteringClusterer(
        ApplicationContext context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return "No Clustering VRP Clusterer";
    }

    @Override
    public boolean canSolve(Problem<String> problem) {
        return problem.type() == ProblemType.CLUSTERABLE_VRP;
    }

    @Override
    public void solve(Problem<String> problem, Solution<String[]> solution, SubRoutinePool subRoutinePool) {
        solution.setSolutionData(new String[] {problem.problemData()});
        solution.complete();
    }

}
