package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration;
import edu.kit.provideq.toolbox.circuit.processing.results.ExecutionResultVisitor;
import edu.kit.provideq.toolbox.circuit.processing.results.Result;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class QuantumTreeGeneratorSolver extends KnapsackSolver {
  private static final SubRoutineDefinition<String, Result> CIRCUIT_PROCESSING_SUBROUTINE =
      new SubRoutineDefinition<>(
          CircuitProcessingConfiguration.CIRCUIT_PROCESSING,
          "Use circuit processing",
          true
      );


  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QuantumTreeGeneratorSolver(
      @Value("${path.qiskit.knapsack_quantum_tree_generator}") String scriptPath,
      @Value("${venv.qiskit.knapsack_quantum_tree_generator}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Knapsack Quantum Tree Generator";
  }

  @Override
  public String getDescription() {
    return "Solve Knapsack using the Quantum Tree Generator solver.";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    String openQasm = processResult.output().orElseThrow();
    return subRoutineResolver.runSubRoutine(CIRCUIT_PROCESSING_SUBROUTINE, openQasm)
        .map(resultSolution -> {
          Solution<String> s = new Solution<>(this);
          SolutionStatus status = resultSolution.getStatus();
          if (status == SolutionStatus.ERROR) {
            s.fail();
            s.setDebugData(resultSolution.getDebugData());
            return s;
          }

          s.complete();
          s.setSolutionData(resultSolution.getSolutionData().accept(new ExecutionResultVisitor()));
          return s;
        });
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(CIRCUIT_PROCESSING_SUBROUTINE);
  }
}
