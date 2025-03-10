package edu.kit.provideq.toolbox.lp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OrToolsMipCbc extends LpSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public OrToolsMipCbc(
      @Value("${ortools.script.ormip}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(OR-Tools) Cbc Solver for MIP";
  }

  @Override
  public String getDescription() {
    return "This solver uses OR-Tools CBC Solver to solve MIP problems";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "problem.lp")
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    // Return if process failed
    return Mono.just(processResult.applyTo(solution));
  }
}
