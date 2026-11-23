package edu.kit.provideq.toolbox.api.dto;

import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolutionStatus;

/**
 * Data transfer object for {@link Solution solutions}, used in REST API responses.
 *
 * <p>The solution data is transported as text, serialized using the format the solver
 * expressed it in.
 */
public class SolutionDto {
  private SolutionStatus status;
  private String metaData;
  private String solutionData;
  private String solutionFormat;
  private String debugData;
  private String solverName;
  private long executionMilliseconds;

  /**
   * Use {@link #fromSolution(Solution)} instead.
   */
  private SolutionDto() {}

  /**
   * Creates a data transfer object for a given solution.
   */
  public static SolutionDto fromSolution(Solution<?> solution) {
    var dto = new SolutionDto();

    dto.status = solution.getStatus();
    dto.metaData = solution.getMetaData();
    dto.solutionData = solution.getSolutionDataAsText();
    if (solution.getSolutionData() != null) {
      dto.solutionFormat = solution.getSolutionData().getFormat().getName();
    }
    dto.debugData = solution.getDebugData();
    dto.solverName = solution.getSolverName();
    dto.executionMilliseconds = solution.getExecutionMilliseconds();

    return dto;
  }

  public SolutionStatus getStatus() {
    return status;
  }

  public String getMetaData() {
    return metaData;
  }

  public String getSolutionData() {
    return solutionData;
  }

  public String getSolutionFormat() {
    return solutionFormat;
  }

  public String getDebugData() {
    return debugData;
  }

  public String getSolverName() {
    return solverName;
  }

  public long getExecutionMilliseconds() {
    return executionMilliseconds;
  }

  @Override
  public String toString() {
    return "SolutionDto{"
        + "status=" + status
        + ", metaData=" + metaData
        + ", solutionData=" + solutionData
        + ", solutionFormat=" + solutionFormat
        + ", debugData=" + debugData
        + ", solverName=" + solverName
        + ", executionMilliseconds=" + executionMilliseconds
        + '}';
  }
}
