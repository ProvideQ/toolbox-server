package edu.kit.provideq.toolbox;

import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * Abstract solve request containing some request content
 */
public class SolveRequest<RequestType> {
  public RequestType requestContent;

  @Nullable
  public UUID requestedSolverId;
}
