package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * The type of problem to solve.
 */
public class ProblemType<InputT, ResultT> {
  private final String id;
  private final Class<InputT> inputClass;
  private final Class<ResultT> resultClass;

  @Deprecated
  private final Class<? extends SolveRequest<?>> requestType;

  /**
   * Defines a new problem type.
   *
   * @param id a unique string identifier for this type of problem.
   * @param inputClass the Java class object corresponding to the {@link InputT} type parameter.
   * @param resultClass the Java class object corresponding to the {@link ResultT} type parameter.
   * @param requestType the Java subclass of {@link SolveRequest} used for REST API calls of this
   *     problem type.
   */
  public ProblemType(
      String id,
      Class<InputT> inputClass,
      Class<ResultT> resultClass,
      Class<? extends SolveRequest<?>> requestType
  ) {
    this.id = id;
    this.inputClass = inputClass;
    this.resultClass = resultClass;
    this.requestType = requestType;
  }

  /**
   * Returns the unique identifier for this problem type.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the Java class corresponding to the {@link InputT} type parameter.
   */
  public Class<InputT> getInputClass() {
    return inputClass;
  }

  /**
   * Returns the Java class corresponding to the {@link ResultT} type parameter.
   */
  public Class<ResultT> getResultClass() {
    return resultClass;
  }

  /**
   * Returns the java class representing the body of a REST request to solve a problem of this type.
   */
  @Deprecated
  public Class<? extends SolveRequest<?>> getRequestType() {
    return requestType;
  }
}
