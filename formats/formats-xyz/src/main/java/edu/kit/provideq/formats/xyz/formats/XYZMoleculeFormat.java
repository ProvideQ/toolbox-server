package edu.kit.provideq.formats.xyz.formats;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The molecule specification format.
 *
 * <p>The representation is a semicolon-separated list of atoms, each in
 * {@code <element> <x> <y> <z>} notation.
 *
 * <p>Example instance (hydrogen molecule, 0.735 bond length):
 * <pre>
 * H 0 0 0; H 0 0 0.735
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public final class XYZMoleculeFormat implements Format<Document<XYZMolecule>> {
  public static final XYZMoleculeFormat INSTANCE = new XYZMoleculeFormat();

  private XYZMoleculeFormat() {
  }

  @Override
  public Map<String, AttributeReader<Document<XYZMolecule>, ?>> getAttributeReaders() {
    return Map.of(
        "atom_count",
        AttributeReader.of(Integer.class, molecule -> molecule.parse().atoms().size()));
  }

  @Override
  public String getName() {
    return "xyz-molecule";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("xyz");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("xyz");
  }

  @Override
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<XYZMolecule> getRepresentationClass() {
    return XYZMolecule.class;
  }

  @Override
  public Document<XYZMolecule> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input.
      // It is parsed into atoms lazily on Document.parse().
      return Document.of(
          FormatReader.readAll(reader).trim(), XYZMoleculeFormat::parseMolecule);
    } catch (IOException e) {
      throw new ParseException("Could not read molecule specification", e);
    }
  }

  @Override
  public void serialize(Document<XYZMolecule> value, Writer writer)
      throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize molecule specification", e);
    }
  }

  static XYZMolecule parseMolecule(String source) throws ParseException {
    List<XYZAtom> atoms = new ArrayList<>();

    if (source.isEmpty()) {
      throw new ParseException("Molecule specification must contain at least one atom");
    }

    for (var atomText : source.split(";")) {
      var parts = atomText.trim().split("\\s+");
      if (parts.length != 4) {
        throw new ParseException(
            "Atoms must be in '<element> <x> <y> <z>' notation, but was: " + atomText.trim());
      }
      try {
        atoms.add(new XYZAtom(
            parts[0],
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        ));
      } catch (NumberFormatException e) {
        throw new ParseException("Invalid atom coordinates: " + atomText.trim(), e);
      }
    }

    return new XYZMolecule(atoms);
  }
}