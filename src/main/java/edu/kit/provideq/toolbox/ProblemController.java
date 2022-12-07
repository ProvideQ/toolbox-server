package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class ProblemController<ProblemFormatType, SolutionFormatType, SolverType extends ProblemSolver<ProblemFormatType, SolutionFormatType>> {

  public SolutionHandle solve(SolveRequest<ProblemFormatType> request, MetaSolver<SolverType> metaSolver) {
    Solution<SolutionFormatType> solution = SolutionManager.createSolution();
    Problem<ProblemFormatType> problem = new Problem<>(request.requestContent, ProblemType.SAT);
    SolverType solver = metaSolver.findSolver(problem);


    solver.solve(problem, solution);
    return solution;
  }

  public SolutionHandle getSolution(long id) {
    var solution = SolutionManager.getSolution(id);
    if (solution == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("Unable to find solution process with id %d", id));
    }

    // for demonstration purposes, jobs are marked as solved once a request goes in here
    if (solution.status() == SolutionStatus.COMPUTING) solution.complete();

    return solution;
  }
}
