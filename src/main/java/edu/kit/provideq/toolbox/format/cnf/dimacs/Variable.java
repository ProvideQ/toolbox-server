package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.Objects;

public record Variable(int number, String name, boolean isNegated) {
  public Variable(Variable other) {
    this(other.number, other.name, other.isNegated);
  }

  @Override
  public String toString() {
    if (isNegated) {
      return DimacsCnf.NEGATION_PREFIX + String.valueOf(number);
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
