package edu.kit.provideq.toolbox;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.function.Function;

/**
 * A solution holds all information concerning a specific
 * {@link edu.kit.provideq.toolbox.meta.Problem} solving process triggered by a
 * {@link edu.kit.provideq.toolbox.meta.ProblemSolver}. This includes metadata, debug data,
 * the current status of the process, as well as the eventually generated solution data
 *
 * @param <S> the type of the generated solution data
 */
public class Solution<S> implements SolutionHandle {
  private final long id;
  private SolutionStatus status = SolutionStatus.COMPUTING;
  private String metaData = "";
  private S solutionData;
  private String debugData;
  private String solverName;
  private long executionMilliseconds;

  /**
   * Internal constructor, used for de-serialization.
   */
  private Solution() {
    this.id = Long.MIN_VALUE;
  }

  public Solution(long id) {
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public SolutionStatus getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(SolutionStatus newStatus) {
    this.status = newStatus;
  }

  @Override
  public Solution<String> toStringSolution() {
    return toStringSolution(Object::toString);
  }

  /**
   * Converts this {@link Solution}{@code <T>} to a {@link Solution}{@code <String>} by applying a
   * given transformation function.
   *
   * @param stringSelector the function that transforms the {@link Solution#solutionData} of type T
   *                       to a String.
   * @return the solution with the stringified solution data.
   */
  public Solution<String> toStringSolution(@NotNull Function<S, String> stringSelector) {
    Objects.requireNonNull(stringSelector, "Missing String selector!");

    var stringSolution = new Solution<String>(getId());
    stringSolution.status = status;
    stringSolution.metaData = metaData;
    stringSolution.solutionData =
        (solutionData == null) ? null : stringSelector.apply(solutionData);
    stringSolution.debugData = debugData;
    stringSolution.solverName = solverName;
    stringSolution.executionMilliseconds = executionMilliseconds;
    return stringSolution;
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

  public S getSolutionData() {
    return this.solutionData;
  }

  public void setSolutionData(S solutionData) {
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
    var that = (Solution<S>) obj;
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
        + "solutionData" + solutionData + ']';
  }
}
