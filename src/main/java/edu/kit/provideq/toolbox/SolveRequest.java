package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Solve request containing some request content
 * CANNOT be abstract, otherwise internal springboot initializer throws
 */
public class SolveRequest<RequestType> {
  @Nullable
  public RequestType requestContent;

  @Nullable
  public String requestedSolverId;

  @Nullable
  public Map<ProblemType, SolveRequest> requestedSubSolveRequests;

  public <T> SolveRequest<T> replaceContent(T otherContent) {
    var request = new SolveRequest<T>();
    request.requestContent = otherContent;
    request.requestedSolverId = requestedSolverId;
    request.requestedSubSolveRequests = requestedSubSolveRequests;

    return request;
  }
}
