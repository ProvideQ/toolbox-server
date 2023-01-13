package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.sat.solvers.SATSolver;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
public class SatController extends ProblemController<String, String, SATSolver> {

  private final MetaSolver<SATSolver> metaSolver = new MetaSolverSAT();

  @CrossOrigin
  @PostMapping("/solve/sat")
  public SolutionHandle solveSat(@RequestBody @Valid SolveSatRequest request) {
    return super.solve(request, metaSolver);
  }

  @GetMapping("/solve/sat")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    return super.getSolution(id);
  }
}
