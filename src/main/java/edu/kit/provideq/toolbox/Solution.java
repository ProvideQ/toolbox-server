package edu.kit.provideq.toolbox;

import java.util.Objects;
import java.util.function.Function;

/**
 * A solution holds all information concerning a specific
 * {@link edu.kit.provideq.toolbox.meta.Problem} solving process triggered by a
 * {@link edu.kit.provideq.toolbox.meta.ProblemSolver}. This includes meta data, debug data,
 * the current status of the process, as well as the eventually generated solution data
 * @param <S> the type of the generated solution data
 */
public class Solution<S> implements SolutionHandle {
  private final long ID;
  private SolutionStatus status = SolutionStatus.COMPUTING;
  private String metaData = "";
  private S solutionData;
  private String debugData;
  private String solverName;
  private long executionMilliseconds;

  public Solution(long ID) {
    this.ID = ID;
  }

  public long getId() {
    return this.ID;
  }

  public SolutionStatus getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(SolutionStatus newStatus) {
    this.status = newStatus;
  }

  @Override
  public SolutionHandle toStringSolution() {
    return toStringSolution(Object::toString);
  }

  public SolutionHandle toStringSolution(Function<S, String> stringSelector) {
    var stringSolution = new Solution<String>(getId());
    stringSolution.status = status;
    stringSolution.metaData = metaData;
    stringSolution.solutionData = stringSelector.apply(solutionData);
    stringSolution.debugData = debugData;
    stringSolution.solverName = solverName;
    stringSolution.executionMilliseconds = executionMilliseconds;
    return stringSolution;
  }

  /**
   * sets the status to 'invalid'. irreversible
   */
  public void abort() {
    if (!this.status.isCompleted()) this.status = SolutionStatus.INVALID;
  }

  /**
   * Sets the status to 'solved'. irreversible
   */
  public void complete() {
    if (!this.status.isCompleted()) this.status = SolutionStatus.SOLVED;
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
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (Solution<S>) obj;
    return this.ID == that.ID &&
        Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, status);
  }

  @Override
  public String toString() {
    return "Solution[" +
        "id=" + ID + ", " +
        "status=" + status + ", " +
        "metaData=" + metaData + ", " +
        "solutionData" + solutionData + ']';
  }
}
