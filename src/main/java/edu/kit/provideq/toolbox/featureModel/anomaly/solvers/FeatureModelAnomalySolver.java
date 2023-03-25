package edu.kit.provideq.toolbox.featureModel.anomaly.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCNF;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.solvers.GamsSATSolver;
import edu.kit.provideq.toolbox.sat.solvers.SATSolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeatureModelAnomalySolver extends FeatureModelSolver {
    private final SATSolver satSolver;

    @Autowired
    public FeatureModelAnomalySolver(
            GamsSATSolver satSolver) {
        this.satSolver = satSolver;
    }

    @Override
    public String getName() {
        return "Feature Model Anomaly via GAMS SAT";
    }

    @Override
    public boolean canSolve(Problem<String> problem) {
        //TODO: assess problemData
        return problem.type() == ProblemType.MAX_CUT;
    }

    @Override
    public float getSuitability(Problem<String> problem) {
        //TODO: implement algorithm for suitability calculation
        return 1;
    }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution) {
        // Convert uvl to cnf
        String cnf;
        try {
            cnf = UvlToDimacsCNF.convert(problem.problemData());
        } catch (ConversionException e) {
            solution.setDebugData("Conversion error: " + e.getMessage());
            return;
        }

        // todo Add a real solution collection which handles part solutions and still saves solvers and meta data for each
        var solutionBuilder = new StringBuilder();

        // Check if the FM is a Void Feature Model
        var voidSolution = new Solution<String>(-1);
        satSolver.solve(new Problem<>(cnf, ProblemType.SAT), voidSolution);
        if (voidSolution.getStatus() == SolutionStatus.SOLVED) {
            solutionBuilder.append(voidSolution.getSolutionData());
        } else {
            solutionBuilder.append(voidSolution.getDebugData());
        }

        // Check if there are any Dead Features
        var deadSolution = new Solution<String>(-1);
        satSolver.solve(new Problem<>(cnf, ProblemType.SAT), deadSolution);

        solution.setSolutionData(solutionBuilder.toString());
    }
}
