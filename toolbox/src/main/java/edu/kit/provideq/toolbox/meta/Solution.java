package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.families.FormatFamily;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * A solution holds all information concerning a specific problem-solving process triggered by a
 * {@link ProblemSolver}. This includes metadata, debug data, the current status of the process,
 * as well as the eventually generated solution data.
 *
 * @param <OutT> output format family of the solution data
 */
public class Solution<OutT extends FormatFamily> {
  private final UUID id;
  private SolutionStatus status = SolutionStatus.COMPUTING;
  private String metaData = "";
  private FormattedValue<?> solutionData;
  private String debugData;
  private String solverName;
  private long executionMilliseconds;

  /**
   * Internal constructor, used for de-serialization.
   */
  private Solution() {
    this.id = UUID.randomUUID();
  }

  private Solution(UUID id) {
    this.id = id;
  }

  public <I extends FormatFamily> Solution(ProblemSolver<I, OutT> problemSolver) {
    this.id = UUID.randomUUID();
    this.solverName = problemSolver.getName();
  }

  public static <InT extends FormatFamily, OutT extends FormatFamily> Solution<OutT> failed(
      ProblemSolver<InT, OutT> problemSolver,
      String text) {
    var solution = new Solution<>(problemSolver);
    solution.fail();
    solution.setDebugData(text);
    return solution;
  }

  /**
   * Creates a solution with status 'invalid' and the given debug data, mirroring
   * {@link #failed(ProblemSolver, String)} for inputs that cannot be processed.
   *
   * @param problemSolver the solver that produced this solution
   * @param text          debug data describing why the input is invalid
   * @param <InT>         input format family of the solver
   * @param <OutT>        output format family of the solution
   * @return the aborted solution
   */
  public static <InT extends FormatFamily, OutT extends FormatFamily> Solution<OutT> aborted(
      ProblemSolver<InT, OutT> problemSolver,
      String text) {
    var solution = new Solution<>(problemSolver);
    solution.abort();
    solution.setDebugData(text);
    return solution;
  }

  public static <S extends FormatFamily, T extends FormatFamily> Solution<T> from(
      Solution<S> source,
      Function<FormattedValue<?>, FormattedValue<?>> mapper) {
    var result = new Solution<T>();
    result.solverName = source.solverName;
    result.status = source.status;
    result.metaData = source.metaData;
    result.debugData = source.debugData;
    result.executionMilliseconds = source.executionMilliseconds;
    result.solutionData = source.solutionData == null ? null : mapper.apply(source.solutionData);
    return result;
  }

  public UUID getId() {
    return this.id;
  }

  public SolutionStatus getStatus() {
    return this.status;
  }

  public void setStatus(SolutionStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * sets the status to 'invalid'. irreversible
   */
  public void abort() {
    if (!this.status.isCompleted()) {
      this.status = SolutionStatus.INVALID;
    }
  }

  /**
   * sets the status to 'error'. irreversible
   */
  public void fail() {
    if (!this.status.isCompleted()) {
      this.status = SolutionStatus.ERROR;
    }
  }

  /**
   * Sets the status to 'solved'. irreversible
   */
  public void complete() {
    if (!this.status.isCompleted()) {
      this.status = SolutionStatus.SOLVED;
    }
  }

  public String getMetaData() {
    return this.metaData;
  }

  public void setMetaData(String metaData) {
    this.metaData = metaData;
  }

  /**
   * Returns the solution data, or null if the solver has not set any.
   */
  public FormattedValue<?> getSolutionData() {
    return this.solutionData;
  }

  /**
   * Returns the solution data serialized to text using its current format,
   * or null if no solution data is set.
   *
   * @return textual representation of the solution data
   */
  public String getSolutionDataAsText() {
    if (solutionData == null) {
      return null;
    }

    return solutionData.toString();
  }

  /**
   * Sets the solution data expressed in the given format.
   *
   * @param value solution value in the format's representation
   * @param format format the solution data is expressed in
   * @param <T> representation type of the solution data
   */
  public <T> void setSolutionData(T value, Format<T> format) {
    this.solutionData = FormattedValue.of(value, format);
  }

  /**
   * Sets the solution data as a formatted value.
   *
   * @param solutionData the formatted solution data
   */
  public void setSolutionData(FormattedValue<?> solutionData) {
    this.solutionData = solutionData;
  }

  public String getDebugData() {
    return this.debugData;
  }

  public void setDebugData(String debugData) {
    this.debugData = debugData;
  }

  public String getSolverName() {
    return solverName;
  }

  public void setSolverName(String solverName) {
    this.solverName = solverName;
  }

  public long getExecutionMilliseconds() {
    return executionMilliseconds;
  }

  public void setExecutionMilliseconds(long executionMilliseconds) {
    this.executionMilliseconds = executionMilliseconds;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Solution<?>) obj;
    return this.id == that.id
        && Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status);
  }

  @Override
  public String toString() {
    return "Solution["
        + "id=" + id + ", "
        + "status=" + status + ", "
        + "metaData=" + metaData + ", "
        + "debugData=" + debugData + ", "
        + "solutionData=" + solutionData + ']';
  }
}