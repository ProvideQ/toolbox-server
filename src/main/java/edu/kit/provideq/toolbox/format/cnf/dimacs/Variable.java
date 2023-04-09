package edu.kit.provideq.toolbox.format.cnf.dimacs;

import java.util.Objects;

public class Variable {
    private final int number;
    private final String name;

    Variable(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable variable)) return false;

        return number == variable.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}
