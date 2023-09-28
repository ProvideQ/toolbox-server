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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("3-nodes-3-edges.txt"),
          "3-nodes-3-edges example for MaxCut is unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream));
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
