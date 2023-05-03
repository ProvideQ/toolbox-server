package edu.kit.provideq.toolbox.format;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCNF;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DimacsCNFTests {
    @ParameterizedTest
    @MethodSource("testToStringParameters")
    public void testToString(String expression, String result) {
        result = result.replace("\n", System.lineSeparator());

        var dimacsCNF = DimacsCNF.fromString(expression);
        assertEquals(result, dimacsCNF.toString());
    }

    static Stream<Arguments> testToStringParameters() {
        return Stream.of(
                Arguments.arguments("(!A | B)", """
                        c A 2
                        c B 1
                        p cnf 2 1
                        1 -2 0
                        """),
                Arguments.arguments("!A | B", """
                        c A 2
                        c B 1
                        p cnf 2 1
                        1 -2 0
                        """),
                Arguments.arguments("not A or B", """
                        c A 2
                        c B 1
                        p cnf 2 1
                        1 -2 0
                        """),
                Arguments.arguments("(!A | B) & !C & D", """
                        c A 4
                        c B 3
                        c C 2
                        c D 1
                        p cnf 4 3
                        1 0
                        -2 0
                        3 -4 0
                        """),
                Arguments.arguments("(A | B) & C", """
                        c A 2
                        c B 3
                        c C 1
                        p cnf 3 2
                        1 0
                        2 3 0
                        """),
                Arguments.arguments("""
                        c C 1
                        c A 2
                        c B 3
                        p cnf 3 2
                        1 0
                        2 -3 0
                        """, """
                        c C 1
                        c A 2
                        c B 3
                        p cnf 3 2
                        1 0
                        2 -3 0
                        """)
        );
    }
}
