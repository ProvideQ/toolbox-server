package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.toolbox.meta.guard.SolutionGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TspSolutionGuard {
  private static final Pattern DIMENSION_PATTERN =
      Pattern.compile("(?im)^\\s*DIMENSION\\s*:\\s*(\\d+)\\s*$");
  private static final Pattern TOUR_SECTION_PATTERN =
      Pattern.compile("(?im)^\\s*TOUR_SECTION\\s*:?\\s*$");
  private static final String ROUTE_END = "-1";
  private static final String EOF = "EOF";

  public static final SolutionGuard<String, String> INSTANCE = TspSolutionGuard::findViolation;

  private TspSolutionGuard() {
  }

  public static boolean isValid(String problem, String solution) {
    return findViolation(problem, solution).isEmpty();
  }

  public static Optional<String> findViolation(String problem, String solution) {
    if (solution == null || solution.isBlank()) {
      return Optional.of("The solution is empty.");
    }

    var dimensionMatcher = DIMENSION_PATTERN.matcher(problem);
    if (!dimensionMatcher.find()) {
      return Optional.of("The problem does not declare a DIMENSION.");
    }
    final int dimension = Integer.parseInt(dimensionMatcher.group(1));

    var tourSectionMatcher = TOUR_SECTION_PATTERN.matcher(solution);
    if (!tourSectionMatcher.find()) {
      return Optional.of("The solution does not contain a TOUR_SECTION.");
    }

    List<List<Integer>> routes;
    try {
      routes = parseRoutes(solution.substring(tourSectionMatcher.end()));
    } catch (NumberFormatException e) {
      return Optional.of("The TOUR_SECTION contains a non-integer entry: " + e.getMessage());
    }

    if (routes.size() != 1) {
      return Optional.of("Expected exactly one route, but found " + routes.size() + ".");
    }

    var route = routes.get(0);
    var visited = new boolean[dimension + 1];
    for (int node : route) {
      if (node < 1 || node > dimension) {
        return Optional.of("Node " + node + " does not exist, nodes range from 1 to "
            + dimension + ".");
      }
      if (visited[node]) {
        return Optional.of("Node " + node + " is visited more than once.");
      }
      visited[node] = true;
    }

    var missing = new ArrayList<Integer>();
    for (int node = 1; node <= dimension; node++) {
      if (!visited[node]) {
        missing.add(node);
      }
    }
    if (!missing.isEmpty()) {
      return Optional.of("Nodes " + missing + " are not visited.");
    }

    return Optional.empty();
  }

  private static List<List<Integer>> parseRoutes(String tourSection) {
    var routes = new ArrayList<List<Integer>>();
    var currentRoute = new ArrayList<Integer>();

    for (var token : tourSection.trim().split("\\s+")) {
      if (token.isEmpty() || token.equals(EOF)) {
        break;
      }
      if (token.equals(ROUTE_END)) {
        if (currentRoute.isEmpty()) {
          break;
        }
        routes.add(currentRoute);
        currentRoute = new ArrayList<>();
        continue;
      }
      currentRoute.add(Integer.parseInt(token));
    }

    if (!currentRoute.isEmpty()) {
      routes.add(currentRoute);
    }
    return routes;
  }
}
