package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.maxCut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import javax.validation.Valid;
import java.util.Set;

import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.web.bind.annotation.*;

@RestController
public class MaxCutController extends ProblemController<String, String, MaxCutSolver> {

  private final MetaSolver<MaxCutSolver> metaSolver;

  public MaxCutController(MetaSolver<MaxCutSolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.MAX_CUT;
  }

  @Override
  public MetaSolver<MaxCutSolver> getMetaSolver() {
    return metaSolver;
  }

  @CrossOrigin
  @PostMapping("/solve/maxCut")
  public SolutionHandle solveMaxCut(@RequestBody @Valid SolveMaxCutRequest request) {
    return super.solve(request);
  }

  @GetMapping("/solve/maxCut")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    return super.getSolution(id);
  }

  @CrossOrigin
  @GetMapping("/solvers/maxCut")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
