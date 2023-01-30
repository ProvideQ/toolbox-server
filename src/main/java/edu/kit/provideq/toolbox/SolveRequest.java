package edu.kit.provideq.toolbox;

import org.springframework.lang.Nullable;

/**
 * Abstract solve request containing some request content
 */
public class SolveRequest<RequestType> {
  public RequestType requestContent;

  @Nullable
  public String requestedSolverId;
}
