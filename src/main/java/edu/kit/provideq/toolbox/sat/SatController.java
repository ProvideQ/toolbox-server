package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.sat.solvers.SatSolver;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
public class SatController extends ProblemController<String, DimacsCnfSolution, SatSolver> {

  private final MetaSolver<SatSolver> metaSolver;

  public SatController(MetaSolver<SatSolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.SAT;
  }

  @Override
  public MetaSolver<SatSolver> getMetaSolver() {
    return metaSolver;
  }

  @CrossOrigin
  @PostMapping("/solve/sat")
  public SolutionHandle solveSat(@RequestBody @Valid SolveSatRequest request) {
    return super.solve(request).toStringSolution(DimacsCnfSolution::toHumanReadableString);
  }

  @CrossOrigin
  @GetMapping("/solve/sat")
  public SolutionHandle getSolution(@RequestParam(name = "id") long id) {
    return super.findSolution(id).toStringSolution(DimacsCnfSolution::toHumanReadableString);
  }

  @CrossOrigin
  @GetMapping("/sub-routines/sat")
  public List<SubRoutineDefinition> getSubRoutines(@RequestParam(name = "id") String solverId) {
    return super.getSubRoutines(solverId);
  }

  @CrossOrigin
  @GetMapping("/meta-solver/settings/sat")
  public List<MetaSolverSetting> getMetaSolverSettings() {
    return metaSolver.getSettings();
  }

  @CrossOrigin
  @GetMapping("/solvers/sat")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
