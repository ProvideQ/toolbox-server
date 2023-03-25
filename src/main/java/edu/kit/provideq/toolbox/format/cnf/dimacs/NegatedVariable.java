package edu.kit.provideq.toolbox.format.cnf.dimacs;

class NegatedVariable extends Variable {
    NegatedVariable(int number, String name) {
        super(number, name);
    }

    @Override
    public String toString() {
        return DimacsCNF.NEGATION_PREFIX + super.toString();
    }
}
