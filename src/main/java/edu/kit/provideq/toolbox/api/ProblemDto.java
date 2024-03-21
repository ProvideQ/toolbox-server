package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemState;

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
}
