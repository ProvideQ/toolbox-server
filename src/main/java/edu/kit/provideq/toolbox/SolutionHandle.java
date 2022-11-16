package edu.kit.provideq.toolbox;

import java.util.Objects;

/**
 * Used to refer to the solution (process) of a submitted problem.
 */
public final class SolutionHandle {
    private final long id;
    private SolutionStatus status;

    /**
     * @param id     the unique identifier for this solution (process).
     * @param status the current status of the solution process.
     */
    public SolutionHandle(long id, SolutionStatus status) {
        this.id = id;
        this.status = status;
    }

    public long id() {
        return id;
    }

    public SolutionStatus status() {
        return status;
    }

    public void setStatus(SolutionStatus newStatus) {
        status = newStatus;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SolutionHandle) obj;
        return this.id == that.id &&
                Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }

    @Override
    public String toString() {
        return "SolutionHandle[" +
                "id=" + id + ", " +
                "status=" + status + ']';
    }
}
