package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.Collections;
import java.util.List;

/**
 * Data transfer object for {@link Problem problems}, used in REST API request bodies and responses.
 */
public class ProblemDto {
  private String id;
  private String typeId;
  private String input;
  private String inputFormat;
  private SolutionDto solution;
  private ProblemState state;
  private BoundComparisonDto boundWithComparison;
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
  public static ProblemDto fromProblem(Problem<?, ?> problem) {
    var dto = new ProblemDto();

    dto.id = problem.getId().toString();
    dto.typeId = problem.getType().getId();
    if (problem.getInput().isPresent()) {
      var formattedInput = problem.getInput().get();
      dto.input = formattedInput.toString();
      dto.inputFormat = formattedInput.getFormat().getName();
    }
    dto.solution = problem.getSolution().map(SolutionDto::fromSolution).orElse(null);
    dto.boundWithComparison = problem.getBoundWithComparison().orElse(null);
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

  public String getInput() {
    return input;
  }

  public String getInputFormat() {
    return inputFormat;
  }

  public SolutionDto getSolution() {
    return solution;
  }

  public BoundComparisonDto getBoundWithComparison() {
    return boundWithComparison;
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
    if (subProblems == null) {
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
        + ", inputFormat=" + inputFormat
        + ", solution=" + solution
        + ", boundWithComparison=" + boundWithComparison
        + ", solverSettings=" + solverSettings
        + ", subProblems=" + subProblems
        + '}';
  }
}
