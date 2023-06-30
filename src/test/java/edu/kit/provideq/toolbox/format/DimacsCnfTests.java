package edu.kit.provideq.toolbox.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DimacsCnfTests {
  @ParameterizedTest
  @MethodSource("testToStringParameters")
  public void testToString(String expression, String result) throws ConversionException {
    result = result.replace("\n", System.lineSeparator());

    var dimacsCnf = DimacsCnf.fromString(expression);
    assertEquals(result, dimacsCnf.toString());
  }

  static Stream<Arguments> testToStringParameters() {
    return Stream.of(
        Arguments.arguments("(!A | B)", """
            c 2 A
            c 1 B
            p cnf 2 1
            1 -2 0
            """),
        Arguments.arguments("!A | B", """
            c 2 A
            c 1 B
            p cnf 2 1
            1 -2 0
            """),
        Arguments.arguments("not A or B", """
            c 2 A
            c 1 B
            p cnf 2 1
            1 -2 0
            """),
        Arguments.arguments("(!A | B) & !C & D", """
            c 4 A
            c 3 B
            c 2 C
            c 1 D
            p cnf 4 3
            1 0
            -2 0
            3 -4 0
            """),
        Arguments.arguments("(A | B) & C", """
            c 2 A
            c 3 B
            c 1 C
            p cnf 3 2
            1 0
            2 3 0
            """),
        Arguments.arguments("""
            c 1 C
            c 2 A
            c 3 B
            p cnf 3 2
            1 0
            2 -3 0
            """, """
            c 1 C
            c 2 A
            c 3 B
            p cnf 3 2
            1 0
            2 -3 0
            """)
    );
  }
}
