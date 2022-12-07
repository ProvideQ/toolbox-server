package edu.kit.provideq.toolbox;

public abstract class SolveRequest<RequestType> {
  public final RequestType requestContent;

  public SolveRequest(RequestType requestContent) {
    this.requestContent = requestContent;
  }
}
