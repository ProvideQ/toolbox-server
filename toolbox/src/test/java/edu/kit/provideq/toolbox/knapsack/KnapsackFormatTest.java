package edu.kit.provideq.toolbox.knapsack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.knapsack.formats.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.formats.representation.KnapsackItem;
import edu.kit.provideq.formats.knapsack.formats.representation.KnapsackProblem;
import edu.kit.provideq.formats.knapsack.formats.representation.KnapsackSolution;
import edu.kit.provideq.formats.knapsack.formats.KnapsackSolutionFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnapsackFormatTest {
  @Test
  void roundTripsKnapsackProblem() throws Exception {
    var problem = new KnapsackProblem(
        List.of(new KnapsackItem(1, 5, 5), new KnapsackItem(2, 3, 4)), 9);
    var text = "2" + System.lineSeparator()
        + "1 5 5" + System.lineSeparator()
        + "2 3 4" + System.lineSeparator()
        + "9" + System.lineSeparator();
    var formatted = FormattedValue.of(Document.of(problem), KnapsackFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(problem, FormattedValue.parse(text, KnapsackFormat.INSTANCE)
        .getValue().parse());
  }

  @Test
  void exposesAttributes() throws Exception {
    var problem = FormattedValue.parse(
        "2" + System.lineSeparator()
            + "1 5 5" + System.lineSeparator()
            + "2 3 4" + System.lineSeparator()
            + "9" + System.lineSeparator(),
        KnapsackFormat.INSTANCE);

    assertEquals(2, problem.getFormat().getAttributeReaders()
        .get("item_count").read(problem.getValue()));
    assertEquals(9, problem.getFormat().getAttributeReaders()
        .get("capacity").read(problem.getValue()));
  }

  @Test
  void roundTripsKnapsackSolution() throws Exception {
    var solution = new KnapsackSolution(8, List.of(1, 3));
    var text = "8" + System.lineSeparator()
        + "1" + System.lineSeparator()
        + "3" + System.lineSeparator();
    var formatted = FormattedValue.of(
        Document.of(solution), KnapsackSolutionFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(solution, KnapsackSolutionFormat.INSTANCE.parse(text).parse());
  }
}
