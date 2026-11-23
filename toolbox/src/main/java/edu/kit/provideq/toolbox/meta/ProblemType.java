package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.meta.bound.Bound;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The type of problem to solve.
 *
 * <p>Problem types declare the format families of their input and output.
 * The input family defines which formats may be submitted for this problem type.
 * The output family defines which formats solvers may produce solutions in.
 *
 * @param <InT>  input format family of this problem type
 * @param <OutT> output format family of this problem type
 */
public class ProblemType<InT extends FormatFamily, OutT extends FormatFamily> {
  private final String id;
  private final String description;
  private final InT inputFamily;
  private final OutT resultFamily;
  private final Function<FormattedValue<?>, Bound> estimator;
  private final String solutionPattern;

  /**
   * Defines a new problem type with declared input and output format families.
   *
   * @param id           a unique string identifier for this type of problem.
   * @param description  a description of the problem type.
   * @param inputFamily  the format family of the problem input.
   * @param resultFamily the format family of the problem solution.
   */
  public ProblemType(
      String id,
      String description,
      InT inputFamily,
      OutT resultFamily
  ) {
    this(id, description, inputFamily, resultFamily, null, null);
  }

  /**
   * Defines a new problem type with declared input and output format families
   * and estimation support.
   *
   * @param id              a unique string identifier for this type of problem.
   * @param description     a description of the problem type.
   * @param inputFamily     the format family of the problem input.
   * @param resultFamily    the format family of the problem solution.
   * @param estimator       the bound estimator for this problem type.
   * @param solutionPattern a regex pattern describing the format of valid solutions.
   */
  public ProblemType(
      String id,
      String description,
      InT inputFamily,
      OutT resultFamily,
      Function<FormattedValue<?>, Bound> estimator,
      String solutionPattern
  ) {
    this.id = id;
    this.description = description;
    this.inputFamily = inputFamily;
    this.resultFamily = resultFamily;
    this.estimator = estimator;
    this.solutionPattern = solutionPattern;
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
   * Returns the input format family of this problem type.
   * Defines which formats may be submitted for this problem type.
   */
  public InT getInputFamily() {
    return inputFamily;
  }

  /**
   * Returns the output format family of this problem type.
   * Defines which formats solvers may produce solutions in.
   */
  public OutT getResultFamily() {
    return resultFamily;
  }

  /**
   * Returns the bound estimator for this problem type.
   */
  public Optional<Function<FormattedValue<?>, Bound>> getEstimator() {
    return Optional.ofNullable(estimator);
  }

  public String getSolutionPattern() {
    return solutionPattern;
  }

  /**
   * Returns names of attributes supported by the input format family.
   */
  public Set<String> getAttributeNames() {
    return inputFamily.getAttributeNames();
  }

  /**
   * Reads an attribute as its string representation using the current format when possible,
   * otherwise converting to the nearest format in the conversion graph that defines the
   * attribute reader, through the caller-owned conversion service.
   */
  public <T> String readAttribute(
      String attributeName,
      FormattedValue<T> input,
      AttributeService attributeService) throws FormatException {
    if (attributeService == null) {
      var reader = input.getFormat().getAttributeReaders().get(attributeName);
      if (reader == null) {
        throw new IllegalStateException(
            "Missing reader to read attribute " + attributeName);
      }
      return String.valueOf(reader.read(input.getValue()));
    }

    return attributeService.readAsString(
        input.getValue(),
        input.getFormat(),
        attributeName);
  }

  @Override
  public String toString() {
    return "ProblemType{"
        + "id='%s'".formatted(id)
        + ", inputFamily=%s".formatted(inputFamily)
        + ", resultFamily=%s".formatted(resultFamily)
        + ", estimator?=%s".formatted(estimator != null)
        + ", description='%s'".formatted(description)
        + ", attributes=%s".formatted(getAttributeNames())
        + '}';
  }
}