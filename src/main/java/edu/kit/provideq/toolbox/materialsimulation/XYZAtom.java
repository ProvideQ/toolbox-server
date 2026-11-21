package edu.kit.provideq.toolbox.materialsimulation;

/**
 * An atom of a molecule specification.
 *
 * @param element the chemical element symbol, e.g. {@code H}
 * @param x       x coordinate
 * @param y       y coordinate
 * @param z       z coordinate
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record XYZAtom(String element, double x, double y, double z) {
}
