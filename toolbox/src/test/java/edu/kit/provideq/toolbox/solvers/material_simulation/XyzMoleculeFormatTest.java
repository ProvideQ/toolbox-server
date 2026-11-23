package edu.kit.provideq.toolbox.solvers.material_simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.xyz.formats.XYZAtom;
import edu.kit.provideq.formats.xyz.formats.XYZMolecule;
import edu.kit.provideq.formats.xyz.formats.XyzMoleculeFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("AbbreviationAsWordInName")
class XyzMoleculeFormatTest {
  @Test
  void roundTripsMolecule() throws Exception {
    var molecule = new XYZMolecule(List.of(
        new XYZAtom("H", 0, 0, 0),
        new XYZAtom("H", 0, 0, 0.735)
    ));
    var text = "H 0.0 0.0 0.0; H 0.0 0.0 0.735";
    var formatted = FormattedValue.of(
        Document.of(molecule), XyzMoleculeFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(molecule, FormattedValue.parse(text, XyzMoleculeFormat.INSTANCE)
        .getValue().parse());
  }

  @Test
  void exposesAtomCount() throws Exception {
    var formatted = FormattedValue.parse("H 0 0 0; H 0 0 0.735", XyzMoleculeFormat.INSTANCE);

    assertEquals(2, formatted.getFormat().getAttributeReaders()
        .get("atom_count").read(formatted.getValue()));
  }
}
