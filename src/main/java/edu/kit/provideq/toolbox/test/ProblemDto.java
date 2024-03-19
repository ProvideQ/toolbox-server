package edu.kit.provideq.toolbox.test;

import edu.kit.provideq.toolbox.Solution;
import java.util.List;

/**
 * Data transfer object for {@link Problem}s in API requests and responses.
 */
public class ProblemDto<InputT, ResultT> {
  private String id;
  private String typeId;
  private InputT input;
  private Solution<ResultT> solution;
  private ProblemState state;
  private String solverId;
  private List<String> subProblemIds;

  public static <InputT, ResultT> ProblemDto<InputT, ResultT> fromProblem(
      Problem<InputT, ResultT> problem
  ) {
    var dto = new ProblemDto<InputT, ResultT>();
    dto.id = problem.getId().toString();
    dto.typeId = problem.getType().getId();
    dto.input = problem.getInput();
    dto.solution = problem.getSolution();
    dto.state = problem.getState();
    dto.solverId = problem.getSolver() != null ? problem.getSolver().getId() : null;
    dto.subProblemIds = problem.getSubRoutines().stream()
        .map(subProblem -> subProblem.getId().toString())
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

  public ProblemState getState() {
    return state;
  }

  public String getSolverId() {
    return solverId;
  }

  public List<String> getSubProblemIds() {
    return subProblemIds;
  }
}
