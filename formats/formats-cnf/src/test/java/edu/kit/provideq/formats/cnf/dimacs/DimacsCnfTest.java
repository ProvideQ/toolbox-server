package edu.kit.provideq.formats.cnf.dimacs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.attributes.AttributeNotFoundException;
import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
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

    var conversionService = new ConversionService(registry);
    Document<DimacsCnf> cnf = conversionService.convert(
        "apple and banana", "logical-expression", "dimacs-cnf");

    assertEquals(2, cnf.parse().getVariables().size());
  }

  @Test
  void convertsCnfBackToLogicalExpression() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();

    var conversionService = new ConversionService(registry);
    var expression = conversionService.convert(
        Document.of(
            DimacsCnf.fromDimacsCnfString("c 1 apple\nc 2 banana\np cnf 2 1\n1 -2 0\n")),
        "dimacs-cnf",
        "logical-expression");

    assertEquals("(apple or not banana)", expression);
  }

  @Test
  void readsAttributes() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));

    var cnf = Document.of(
        DimacsCnf.fromLogicalExpressionString("apple and banana or pear"));

    assertEquals(3, attributeService.read(cnf, DimacsCnfFormat.INSTANCE,
        "variable_count", Integer.class));
    assertEquals(2, attributeService.read(cnf, DimacsCnfFormat.INSTANCE,
        "clause_count", Integer.class));
  }

  @Test
  void attributeFallbackFromExpressionFormat() throws FormatException {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));

    // The expression format itself has no attribute readers; the fallback should convert
    // to DIMACS CNF and read there.
    var expression = "apple and banana";

    var variableCount = attributeService.read(
        expression, LogicalExpressionFormat.INSTANCE, "variable_count", Integer.class);

    assertEquals(2, variableCount);
  }

  @Test
  void unknownAttributeThrows() {
    var registry = new ConversionRegistry();
    CnfFormats.register(registry);
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));

    assertThrows(AttributeNotFoundException.class,
        () -> attributeService.read("not a cnf", LogicalExpressionFormat.INSTANCE,
            "unknown", Integer.class));
  }

  @Test
  void invalidCnfThrowsParseException() {
    assertThrows(ParseException.class,
        () -> DimacsCnf.fromDimacsCnfString("p cnf 5 2\n1 0\n"));
  }

  @Test
  void documentToStringReturnsSourceWithoutParsing() {
    var cnfString = "c 1 apple\np cnf 1 1\n1 0\n";
    var document = Document.of(cnfString, DimacsCnf::fromDimacsCnfString);

    assertEquals(cnfString, document.toString());
  }

  @Test
  void documentParsingIsDeferredUntilParse() {
    var document = Document.of("this is not a cnf", DimacsCnf::fromDimacsCnfString);

    assertThrows(ParseException.class, document::parse);
  }
}