package edu.kit.provideq.formats.xyz.formats;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Object representation of a molecule specification.
 *
 * @param atoms the atoms of the molecule with their coordinates
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record XYZMolecule(List<XYZAtom> atoms) {
  public XYZMolecule {
    atoms = List.copyOf(atoms);
  }

  /**
   * Returns the molecule in the semicolon-separated wire notation, e.g.
   * {@code H 0 0 0; H 0 0 0.735}, as accepted by quantum chemistry drivers.
   */
  @Override
  public String toString() {
    return atoms.stream()
        .map(atom -> "%s %s %s %s".formatted(atom.element(), atom.x(), atom.y(), atom.z()))
        .collect(Collectors.joining("; "));
  }
}
