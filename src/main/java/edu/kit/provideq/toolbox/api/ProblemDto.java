package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.Collections;
import java.util.List;

/**
 * Data transfer object for {@link Problem problems}, used in REST API request bodies and responses.
 */
public class ProblemDto<InputT, ResultT> {
  private String id;
  private String typeId;
  private InputT input;
  private Solution<ResultT> solution;
  private Bound bound;
  private ProblemState state;
  private String solverId;
  private List<SolverSetting> solverSettings;
  private List<SubProblemReferenceDto> subProblems;

  /**
   * Use {@link #fromProblem(Problem)} instead.
   */
  private ProblemDto() {}

  /**
   * Creates a data transfer object for a given problem.
   */
  public static <InputT, ResultT> ProblemDto<InputT, ResultT> fromProblem(
      Problem<InputT, ResultT> problem
  ) {
    var dto = new ProblemDto<InputT, ResultT>();

    dto.id = problem.getId().toString();
    dto.typeId = problem.getType().getId();
    dto.input = problem.getInput().orElse(null);
    dto.solution = problem.getSolution();
    dto.bound = problem.getBound();
    dto.state = problem.getState();
    dto.solverId = problem.getSolver()
        .map(ProblemSolver::getId)
        .orElse(null);
    dto.solverSettings = problem.getSolverSettings().stream().toList();
    dto.subProblems = problem.getSolver().stream()
        .flatMap(solver -> solver.getSubRoutines().stream())
        .map(subRoutine -> SubProblemReferenceDto.forSubRoutine(problem, subRoutine))
        .toList();

    return dto;
  }

  public String getId() {
    return id;
  }

  public String getTypeId() {
    return typeId;
  }

  public InputT getInput() {
    return input;
  }

  public Solution<ResultT> getSolution() {
    return solution;
  }

  public Bound getBound() {
    return bound;
  }

  public ProblemState getState() {
    return state;
  }

  public String getSolverId() {
    return solverId;
  }

  public List<SolverSetting> getSolverSettings() {
    if (solverSettings == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(solverSettings);
  }

  public List<SubProblemReferenceDto> getSubProblems() {
    if (solverSettings == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(subProblems);
  }

  @Override
  public String toString() {
    return "ProblemDto{"
        + "typeId=" + typeId
        + ", id=" + id
        + ", state=" + state
        + ", solverId=" + solverId
        + ", input=" + input
        + ", solution=" + solution
        + ", bound=" + bound
        + ", solverSettings=" + solverSettings
        + ", subProblems=" + subProblems
        + '}';
  }
}
