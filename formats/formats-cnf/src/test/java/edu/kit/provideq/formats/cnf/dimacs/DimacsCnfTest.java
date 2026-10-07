package edu.kit.provideq.formats.cnf.dimacs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.ConversionRegistry;
import edu.kit.provideq.formats.core.FormatException;
import edu.kit.provideq.formats.core.FormatsToolbox;
import edu.kit.provideq.formats.core.ParseException;
import edu.kit.provideq.formats.core.PropertyNotFoundException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DIMACS CNF module: parsing, conversion and properties.
 */
class DimacsCnfTest {
  @Test
  void parsesCnfFromString() throws FormatException {
    var cnf = DimacsCnf.fromDimacsCnfString("c 1 apple\nc 2 banana\np cnf 2 1\n1 -2 0\n");

    assertEquals(2, cnf.getVariables().size());
    assertEquals(1, cnf.getOrClauses().size());
  }

  @Test
  void convertsLogicalExpressionToCnf() throws FormatException {
    var cnf = DimacsCnf.fromLogicalExpressionString("apple and banana");

    assertEquals(2, cnf.getVariables().size());
    assertEquals(2, cnf.getOrClauses().size());

    var serialized = cnf.toString();
    var reparsed = DimacsCnf.fromDimacsCnfString(serialized);

    assertEquals(cnf.getOrClauses().size(), reparsed.getOrClauses().size());
    assertEquals(cnf.getVariables().size(), reparsed.getVariables().size());
  }

  @Test
  void roundTripThroughRegistry() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();

    var toolbox = new FormatsToolbox(registry);
    DimacsCnf cnf = toolbox.convert("apple and banana", "logical-expression", "dimacs-cnf");

    assertEquals(2, cnf.getVariables().size());
  }

  @Test
  void readsProperties() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var toolbox = new FormatsToolbox(registry);
    CnfFormats.registerProperties(toolbox);

    var cnf = DimacsCnf.fromLogicalExpressionString("apple and banana or pear");

    assertEquals(3, toolbox.readProperty(cnf, DimacsCnfFormat.INSTANCE,
        "variableCount", Integer.class));
    assertEquals(2, toolbox.readProperty(cnf, DimacsCnfFormat.INSTANCE,
        "clauseCount", Integer.class));
  }

  @Test
  void propertyFallbackFromExpressionFormat() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var toolbox = new FormatsToolbox(registry);
    CnfFormats.registerProperties(toolbox);

    // The expression format itself has no accessors; the fallback should convert
    // to DIMACS CNF and read there.
    var expression = "apple and banana";

    var variableCount = toolbox.getPropertyService().read(
        expression, ExpressionFormat.INSTANCE, "variableCount", Integer.class);

    assertEquals(2, variableCount);
  }

  @Test
  void unknownPropertyThrows() {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var toolbox = new FormatsToolbox(registry);
    CnfFormats.registerProperties(toolbox);

    assertThrows(PropertyNotFoundException.class,
        () -> toolbox.readProperty("not a cnf", ExpressionFormat.INSTANCE,
            "unknown", Integer.class));
  }

  @Test
  void invalidCnfThrowsParseException() {
    assertThrows(ParseException.class, () -> DimacsCnf.fromDimacsCnfString("p cnf 5 2\n1 0\n"));
  }
}
