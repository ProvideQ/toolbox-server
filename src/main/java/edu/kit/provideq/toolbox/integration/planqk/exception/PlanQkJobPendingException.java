package edu.kit.provideq.toolbox.integration.planqk.exception;

public class PlanQkJobPendingException extends Exception {
  public PlanQkJobPendingException() {
    super("PlanQK job is still pending");
  }
}
