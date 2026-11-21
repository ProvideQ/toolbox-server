package edu.kit.provideq.toolbox.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for format family declarations on {@link ProblemType}.
 */
class ProblemTypeTest {
  private static final FormatFamily INPUT_FAMILY = new TestFamily("in-family");
  private static final FormatFamily RESULT_FAMILY = new TestFamily("out-family");
  private static final AtomicInteger ATTRIBUTE_READS = new AtomicInteger();

  @Test
  void familiesAreExposedWhenDeclared() {
    var type = new ProblemType<>("id", "description", INPUT_FAMILY, RESULT_FAMILY);

    assertEquals(INPUT_FAMILY, type.getInputFamily());
    assertEquals(RESULT_FAMILY, type.getResultFamily());
  }

  @Test
  void attributesAreDelegatedToInputFamily() {
    var type = new ProblemType<>("id", "description", INPUT_FAMILY, RESULT_FAMILY);

    assertEquals(INPUT_FAMILY.getAttributeNames(), type.getAttributeNames());
  }

  @Test
  void attributeReadersRemainLazy() throws Exception {
    ATTRIBUTE_READS.set(0);
    var type = new ProblemType<>("id", "description", INPUT_FAMILY, RESULT_FAMILY);

    assertEquals(0, ATTRIBUTE_READS.get());
    type.readAttribute("attribute", FormattedValue.of("value", new TestFormat()), null);

    assertEquals(1, ATTRIBUTE_READS.get());
  }

  private record TestFamily(String name) implements FormatFamily {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Format<?> getCanonicalFormat() {
      return new TestFormat();
    }
  }

  private record TestFormat() implements Format<String> {
    @Override
    public String getName() {
      return "test";
    }

    @Override
    public Set<String> getAliases() {
      return Set.of();
    }

    @Override
    public Set<String> getFileExtensions() {
      return Set.of();
    }

    @Override
    public FormatType getType() {
      return FormatType.PROBLEM;
    }

    @Override
    public Class<String> getRepresentationClass() {
      return String.class;
    }

    @Override
    public java.util.Map<String, AttributeReader<String, ?>> getAttributeReaders() {
      return java.util.Map.of(
          "attribute", AttributeReader.of(String.class, value -> {
            ATTRIBUTE_READS.incrementAndGet();
            return value;
          }));
    }

    @Override
    public String parse(Reader reader) {
      return "";
    }

    @Override
    public void serialize(String value, Writer writer) {
    }
  }
}