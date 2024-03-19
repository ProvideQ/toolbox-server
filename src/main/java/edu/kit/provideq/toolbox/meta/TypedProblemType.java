package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.test.Problem;

/**
 * A problem type represents the definition of an abstract problem.
 *
 * @param <InputT>  type of the input data of the problem.
 * @param <ResultT> type of the result / output data of the problem.
 *
 * @see Problem for problem instances.
 */
public final class TypedProblemType<InputT, ResultT> {
  private final String id;
  private final Class<InputT> inputClass;
  private final Class<ResultT> resultClass;

  public TypedProblemType(String id, Class<InputT> inputClass, Class<ResultT> resultClass) {
    this.id = id;
    this.inputClass = inputClass;
    this.resultClass = resultClass;
  }

  /**
   * Returns a unique identifier for this problem type.
   */
  public String getId() {
    return id;
  }

  public Class<InputT> getInputClass() {
    return inputClass;
  }

  public Class<ResultT> getResultClass() {
    return resultClass;
  }
}
