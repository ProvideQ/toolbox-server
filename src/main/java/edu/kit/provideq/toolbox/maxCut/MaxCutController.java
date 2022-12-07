package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.maxCut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaxCutController extends ProblemController<String, String, MaxCutSolver> {

  private final MetaSolver<MaxCutSolver> metaSolver = new MetaSolverMaxCut();

  @PostMapping("/solve/maxCut")
  public SolutionHandle solveMaxCut(@RequestBody @Valid SolveMaxCutRequest request) {
    return super.solve(request, metaSolver);
  }

  @GetMapping("/solve/maxCut")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    return super.getSolution(id);
  }
}
