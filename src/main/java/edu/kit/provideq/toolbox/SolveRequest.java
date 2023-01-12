package edu.kit.provideq.toolbox;

/**
 * Abstract solve request containing some request content
 */
public abstract class SolveRequest<RequestType> {
  public final RequestType requestContent;

  public SolveRequest(RequestType requestContent) {
    this.requestContent = requestContent;
  }
}
