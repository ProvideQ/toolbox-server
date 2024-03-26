package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemState;
import java.util.ArrayList;
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
  private ProblemState state;
  private String solverId;
  private List<SubProblemReferenceDto> subProblems;

  /**
   * Creates a data transfer object for a given problem.
   */
  public static <InputT, ResultT> ProblemDto<InputT, ResultT> fromProblem(
      Problem<InputT, ResultT> problem
  ) {
    var dto = new ProblemDto<InputT, ResultT>();

    dto.id = problem.getId().toString();
    dto.typeId = problem.getType().getId();
    dto.input = problem.getInput();
    dto.solution = problem.getSolution();
    dto.state = problem.getState();

    if (problem.getSolver() != null) {
      dto.solverId = problem.getSolver().getId();
    }

    dto.subProblems = new ArrayList<>(problem.getSubProblems().size());
    if (problem.getSolver() != null) {
      for (var subRoutine : problem.getSolver().getSubRoutines()) {
        dto.subProblems.add(SubProblemReferenceDto.forSubRoutine(problem, subRoutine));
      }
    }

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

  public ProblemState getState() {
    return state;
  }

  public String getSolverId() {
    return solverId;
  }

  public List<SubProblemReferenceDto> getSubProblems() {
    return Collections.unmodifiableList(subProblems);
  }
}
