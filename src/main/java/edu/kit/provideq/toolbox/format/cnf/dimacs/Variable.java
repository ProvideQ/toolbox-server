package edu.kit.provideq.toolbox.format.cnf.dimacs;

class Variable {
    protected final int number;
    protected final String name;

    Variable(int number, String name) {
        this.number = number;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
