package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.Objects;

/**
 * Represents a variable in a DIMACS CNF file.

 * @param number number of the variable
 * @param name name of the variable
 * @param isNegated true if the variable is negated
 */
public record Variable(int number, String name, boolean isNegated) {
  /**
   * Copy constructor.

   * @param other variable to copy
   */
  public Variable(Variable other) {
    this(other.number, other.name, other.isNegated);
  }

  @Override
  public String toString() {
    if (isNegated) {
        return String.valueOf(DimacsCnf.NEGATION_PREFIX) + number;
    }

    return String.valueOf(number);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Variable variable)) {
      return false;
    }

    return number == variable.number;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }
}
