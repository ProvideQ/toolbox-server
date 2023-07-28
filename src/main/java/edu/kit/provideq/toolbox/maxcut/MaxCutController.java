package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.format.gml.Gml;
import edu.kit.provideq.toolbox.maxcut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaxCutController extends ProblemController<String, String, MaxCutSolver> {

  private final MetaSolver<String, String, MaxCutSolver> metaSolver;

  public MaxCutController(MetaSolver<String, String, MaxCutSolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.MAX_CUT;
  }

  @Override
  public MetaSolver<String, String, MaxCutSolver> getMetaSolver() {
    return metaSolver;
  }

  @CrossOrigin
  @PostMapping("/solve/max-cut")
  public SolutionHandle solveMaxCut(@RequestBody @Valid SolveMaxCutRequest request) {
    return super.solve(request);
  }

  @CrossOrigin
  @GetMapping("/solve/max-cut")
  public SolutionHandle getSolution(@RequestParam(name = "id") long id) {
    return super.findSolution(id);
  }

  @CrossOrigin
  @GetMapping("/sub-routines/max-cut")
  public List<SubRoutineDefinition> getSubRoutines(@RequestParam(name = "id") String solverId) {
    return super.getSubRoutines(solverId);
  }

  @CrossOrigin
  @GetMapping("/meta-solver/settings/max-cut")
  public List<MetaSolverSetting> getMetaSolverSettings() {
    return metaSolver.getSettings();
  }

  @CrossOrigin
  @GetMapping("/solvers/max-cut")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
