package edu.kit.provideq.toolbox.meta;

/**
 * The type of problem to solve.
 */
public class ProblemType<InputT, ResultT> {
  private final String id;
  private final Class<InputT> inputClass;
  private final Class<ResultT> resultClass;

  /**
   * Defines a new problem type.
   *
   * @param id a unique string identifier for this type of problem.
   * @param inputClass the Java class object corresponding to the {@link InputT} type parameter.
   * @param resultClass the Java class object corresponding to the {@link ResultT} type parameter.
   */
  public ProblemType(
      String id,
      Class<InputT> inputClass,
      Class<ResultT> resultClass
  ) {
    this.id = id;
    this.inputClass = inputClass;
    this.resultClass = resultClass;
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

  @Override
  public String toString() {
    return "ProblemType{"
        + "id='%s'".formatted(id)
        + ", inputClass=%s".formatted(inputClass)
        + ", resultClass=%s".formatted(resultClass)
        + '}';
  }
}
