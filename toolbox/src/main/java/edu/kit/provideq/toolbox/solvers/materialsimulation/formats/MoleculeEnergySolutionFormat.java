package edu.kit.provideq.toolbox.solvers.materialsimulation.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The molecule energy result format.
 *
 * <p>The representation is the energy report emitted by the quantum chemistry backend
 * (e.g. the Qiskit QEOM result). The report structure is declared so the data can be
 * addressed deterministically; the {@code electronic_energy} and {@code total_energy}
 * attribute readers extract the reported energy values.
 *
 * <p>Example instance:
 * <pre>
 * === GROUND STATE ENERGY ===
 * Electronic energy = -1.857275030146
 * Total energy = -1.137306035697
 * </pre>
 */
public record MoleculeEnergySolutionFormat() implements Format<String> {
  /**
   * Singleton instance of the molecule energy solution format.
   */
  public static final MoleculeEnergySolutionFormat INSTANCE = new MoleculeEnergySolutionFormat();

  private static final Pattern ELECTRONIC_ENERGY_PATTERN =
      Pattern.compile("Electronic energy = ([-+0-9.eE]+)");
  private static final Pattern TOTAL_ENERGY_PATTERN =
      Pattern.compile("Total energy = ([-+0-9.eE]+)");

  @Override
  public Map<String, AttributeReader<String, ?>> getAttributeReaders() {
    return Map.of(
        "electronic_energy", AttributeReader.of(
            String.class, report -> findEnergy(report, ELECTRONIC_ENERGY_PATTERN)),
        "total_energy", AttributeReader.of(
            String.class, report -> findEnergy(report, TOTAL_ENERGY_PATTERN)));
  }

  private static String findEnergy(String report, Pattern pattern) {
    var matcher = pattern.matcher(report);
    return matcher.find() ? matcher.group(1) : "";
  }

  @Override
  public String getName() {
    return "molecule-energy-result";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("energy-result");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<String> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      return FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read molecule energy result", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize molecule energy result", e);
    }
  }
}