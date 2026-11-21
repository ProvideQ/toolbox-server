package edu.kit.provideq.toolbox.materialsimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.xyz.XYZAtom;
import edu.kit.provideq.formats.xyz.XYZMolecule;
import edu.kit.provideq.formats.xyz.XYZMoleculeFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("AbbreviationAsWordInName")
class XYZXYZMoleculeFormatTest {
  @Test
  void roundTripsMolecule() throws Exception {
    var molecule = new XYZMolecule(List.of(
        new XYZAtom("H", 0, 0, 0),
        new XYZAtom("H", 0, 0, 0.735)
    ));
    var text = "H 0.0 0.0 0.0; H 0.0 0.0 0.735";
    var formatted = FormattedValue.of(
        Document.of(molecule), XYZMoleculeFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(molecule, FormattedValue.parse(text, XYZMoleculeFormat.INSTANCE)
        .getValue().parse());
  }

  @Test
  void exposesAtomCount() throws Exception {
    var formatted = FormattedValue.parse("H 0 0 0; H 0 0 0.735", XYZMoleculeFormat.INSTANCE);

    assertEquals(2, formatted.getFormat().getAttributeReaders()
        .get("atom_count").read(formatted.getValue()));
  }
}
