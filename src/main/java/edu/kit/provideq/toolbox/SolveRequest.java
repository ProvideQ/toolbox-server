package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.authentication.Authentication;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Solve request containing some request content.
 * CANNOT be abstract, otherwise internal springboot initializer throws.
 */
public class SolveRequest<RequestT> {
  @Nullable
  public RequestT requestContent;

  @Nullable
  public String requestedSolverId;

  @Nullable
  public List<MetaSolverSetting> requestedMetaSolverSettings;

  @Nullable
  public Map<ProblemType, SolveRequest<?>> requestedSubSolveRequests;

  @Nullable
  public Authentication authentication;

  public <T> SolveRequest<T> replaceContent(T otherContent) {
    var request = new SolveRequest<T>();
    request.requestContent = otherContent;
    request.requestedSolverId = requestedSolverId;
    request.requestedMetaSolverSettings = requestedMetaSolverSettings;
    request.requestedSubSolveRequests = requestedSubSolveRequests;

    return request;
  }
}
