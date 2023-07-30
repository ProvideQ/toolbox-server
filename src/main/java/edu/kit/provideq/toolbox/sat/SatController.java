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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SatController extends ProblemController<String, DimacsCnfSolution, SatSolver> {

  private final MetaSolver<String, DimacsCnfSolution, SatSolver> metaSolver;

  public SatController(MetaSolver<String, DimacsCnfSolution, SatSolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.SAT;
  }

  @Override
  public MetaSolver<String, DimacsCnfSolution, SatSolver> getMetaSolver() {
    return metaSolver;
  }

  @CrossOrigin
  @GetMapping("/solve/sat")
  public SolutionHandle getSolution(@RequestParam(name = "id") long id) {
    return super.findSolution(id).toStringSolution(DimacsCnfSolution::toHumanReadableString);
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
