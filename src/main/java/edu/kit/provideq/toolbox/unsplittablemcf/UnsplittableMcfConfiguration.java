package edu.kit.provideq.toolbox.unsplittablemcf;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.unsplittablemcf.solvers.GamsUnsplittableMcfClassicalSolver;
import edu.kit.provideq.toolbox.unsplittablemcf.solvers.GamsUnsplittableMcfQuboSolver;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the Unsplittable Multi Commodity Flow problem.
 */
@Configuration
public class UnsplittableMcfConfiguration {

  /**
   * An optimization problem:
   * For a given time-expanded network with products, orders, and capacity constraints,
   * find optimal routing paths to fulfill customer orders through a Time-Expanded Network
   * without splitting an order's demand.
   */
  public static final ProblemType<String, String> UNSPLITTABLE_MCF = new ProblemType<>(
      "UnsplittableMCF",
      "An optimization problem: For a given time-expanded network with products, orders, "
          + "and capacity constraints, find optimal routing paths to fulfill customer orders "
          + "through a Time-Expanded Network without splitting an order's demand",
      String.class,
      String.class,
      Map.ofEntries(
          Map.entry("products", problem -> getValue(problem, "N_PRODUCTS")),
          Map.entry("time_steps", problem -> getValue(problem, "N_TIME_STEPS")),
          Map.entry("orders", problem -> getValue(problem, "N_ORDERS")),
          Map.entry("factories", problem -> getValue(problem, "N_FACTORIES")),
          Map.entry("distribution_centers", problem -> getValue(problem, "N_DISTRIBUTION"))
      )
  );

  /**
   * Helper method to extract configuration values from JSON input.
   */
  private static String getValue(String json, String key) {
    try {
      // Simple JSON parsing without external library dependency
      int configStart = json.indexOf("\"config\"");
      if (configStart == -1) return "";

      int keyStart = json.indexOf("\"" + key + "\"", configStart);
      if (keyStart == -1) return "";
      
      int colonPos = json.indexOf(":", keyStart);
      if (colonPos == -1) return "";
      
      int valueStart = colonPos + 1;
      while (valueStart < json.length()
          && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\n')) {
        valueStart++;
      }
      
      int valueEnd = valueStart;
      while (valueEnd < json.length()
          && Character.isDigit(json.charAt(valueEnd))) {
        valueEnd++;
      }
      
      return json.substring(valueStart, valueEnd).trim();
    } catch (Exception e) {
      return "";
    }
  }

  @Bean
  ProblemManager<String, String> getUnsplittableMcfManager(
      GamsUnsplittableMcfClassicalSolver classicalSolver,
      GamsUnsplittableMcfQuboSolver lpExportSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        UNSPLITTABLE_MCF,
        Set.of(classicalSolver, lpExportSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("unsplittable_instance.json"),
          "Example instance for UnsplittableMCF is unavailable!"
      );
      var problem = new Problem<>(UNSPLITTABLE_MCF);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(UNSPLITTABLE_MCF, e);
    }
  }
}
