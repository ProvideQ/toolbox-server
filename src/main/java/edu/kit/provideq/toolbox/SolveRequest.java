package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.TypedProblemType;
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

  @Nullable // TODO key type
  public Map<?, SolveRequest<?>> requestedSubSolveRequests;
}
