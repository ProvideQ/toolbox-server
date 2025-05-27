package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Bound;
import java.util.Optional;
import java.util.function.Function;

/**
 * The type of problem to solve.
 */
public class ProblemType<InputT, ResultT> {
  private final String id;
  private final String description;
  private final Class<InputT> inputClass;
  private final Class<ResultT> resultClass;
  private final Function<InputT, Bound> estimator;
  private final String solutionPattern;

  /**
   * Defines a new problem type.
   *
   * @param id          a unique string identifier for this type of problem.
   * @param description a description of the problem type.
   * @param inputClass  the Java class object corresponding to the {@link InputT} type parameter.
   * @param resultClass the Java class object corresponding to the {@link ResultT} type parameter.
   * @param estimator   the bound estimator for this problem type.
   *                    null if estimation is not supported.
   */
  public ProblemType(
      String id,
      String description,
      Class<InputT> inputClass,
      Class<ResultT> resultClass,
      Function<InputT, Bound> estimator,
      String solutionPattern
  ) {
    this.id = id;
    this.description = description;
    this.inputClass = inputClass;
    this.resultClass = resultClass;
    this.estimator = estimator;
    this.solutionPattern = solutionPattern;
  }

  /**
   * Defines a new problem type without estimation support.
   *
   * @param id          a unique string identifier for this type of problem.
   * @param description a description of the problem type.
   * @param inputClass  the Java class object corresponding to the {@link InputT} type parameter.
   * @param resultClass the Java class object corresponding to the {@link ResultT} type parameter.
   */
  public ProblemType(
      String id,
      String description,
      Class<InputT> inputClass,
      Class<ResultT> resultClass
  ) {
    this(id, description, inputClass, resultClass, null, null);
  }

  /**
   * Returns the unique identifier for this problem type.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns a description of this problem type.
   */
  public String getDescription() {
    return description;
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
   * Returns the bound estimator for this problem type.
   */
  public Optional<Function<InputT, Bound>> getEstimator() {
    return Optional.ofNullable(estimator);
  }

  public String getSolutionPattern() {
    return solutionPattern;
  }

  @Override
  public String toString() {
    return "ProblemType{"
        + "id='%s'".formatted(id)
        + ", inputClass=%s".formatted(inputClass)
        + ", resultClass=%s".formatted(resultClass)
        + ", estimator?=%s".formatted(estimator != null)
        + ", description='%s'".formatted(description)
        + '}';
  }
}
