package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.maxcut.solvers.CirqMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for MaxCut problems.
 */
@Component
public class MetaSolverMaxCut extends MetaSolver<String, String, MaxCutSolver> {
  private final String examplesDirectoryPath;
  private final ResourceProvider resourceProvider;

  @Autowired
  public MetaSolverMaxCut(
          @Value("${examples.directory.max-cut}") String examplesDirectoryPath,
          ResourceProvider resourceProvider,
          QiskitMaxCutSolver qiskitMaxCutSolver,
          GamsMaxCutSolver gamsMaxCutSolver,
          CirqMaxCutSolver cirqMaxCutSolver) {
    super(ProblemType.MAX_CUT, qiskitMaxCutSolver, gamsMaxCutSolver, cirqMaxCutSolver);
    this.examplesDirectoryPath = examplesDirectoryPath;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public MaxCutSolver findSolver(
          Problem<String> problem,
          List<MetaSolverSetting> metaSolverSettings) {
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      return resourceProvider.getExampleProblems(examplesDirectoryPath);
    } catch (Exception e) {
      return List.of();
    }
  }
}
