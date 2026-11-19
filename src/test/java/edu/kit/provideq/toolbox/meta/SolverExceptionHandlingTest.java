package edu.kit.provideq.toolbox.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.exception.SolverException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Tests the central conversion of solver exceptions into completed solutions in
 * {@link Problem#solve(ConversionService)}.
 */
class SolverExceptionHandlingTest {
  private static final Format<String> FORMAT_A = new TestFormat("a");
  private static final Format<String> FORMAT_B = new TestFormat("b");
  private static final TestFamily TEST_FAMILY = new TestFamily();
  private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(5);

  @Test
  void invalidInputExceptionYieldsAbortedSolution() {
    var solution = solveWith(solverThatThrows(
        new ConversionException("Could not convert input from a to b: broken")));

    assertSame(SolutionStatus.INVALID, solution.getStatus());
    assertEquals("Could not convert input from a to b: broken", solution.getDebugData());
  }

  @Test
  void solverExceptionYieldsFailedSolution() {
    var solution = solveWith(solverThatThrows(new SolverException("solver exploded")));

    assertSame(SolutionStatus.ERROR, solution.getStatus());
    assertEquals("solver exploded", solution.getDebugData());
  }

  @Test
  void exceptionInsideReactiveChainIsConverted() {
    var solver = new ThrowingSolver() {
      @Override
      public Mono<Solution<FormatFamily>> solve(
          FormattedValue<?> input,
          SubRoutineResolver subRoutineResolver,
          SolvingProperties properties,
          ConversionService conversionService) {
        return Mono.just("value")
            .map(value -> {
              throw new ConversionException("late conversion failure");
            });
      }
    };

    var solution = solveWith(solver);

    assertSame(SolutionStatus.INVALID, solution.getStatus());
    assertEquals("late conversion failure", solution.getDebugData());
  }

  @Test
  void unexpectedExceptionYieldsFailedSolution() {
    var solution = solveWith(solverThatThrows(new IllegalStateException("bug")));

    assertSame(SolutionStatus.ERROR, solution.getStatus());
    assertNotNull(solution.getDebugData());
  }

  @Test
  void convertThrowsInvalidInputExceptionOnConversionFailure() {
    var registry = new ConversionRegistry();
    registry.registerFormat(FORMAT_A);
    registry.registerFormat(FORMAT_B);
    registry.registerConverter(new FailingConverter());
    registry.seal();
    var conversionService = new ConversionService(registry);

    var solver = new ThrowingSolver();
    var exception = assertThrows(ConversionException.class, () -> solver.convert(
        FormattedValue.of("value", FORMAT_A), FORMAT_B, conversionService));

    assertEquals("Could not convert input from a to b: conversion failed",
        exception.getMessage());
  }

  private static final ProblemType<FormatFamily, FormatFamily> TEST_TYPE =
      new ProblemType<>("test", "test", TEST_FAMILY, TEST_FAMILY);

  private Solution<FormatFamily> solveWith(ProblemSolver<FormatFamily, FormatFamily> solver) {
    var problem = new Problem<>(TEST_TYPE);
    problem.setSolver(solver);
    problem.setInput(FormattedValue.of("input", FORMAT_A));
    return problem.solve(null).block(TIMEOUT);
  }

  private static ThrowingSolver solverThatThrows(RuntimeException exception) {
    return new ThrowingSolver() {
      @Override
      public Mono<Solution<FormatFamily>> solve(
          FormattedValue<?> input,
          SubRoutineResolver subRoutineResolver,
          SolvingProperties properties,
          ConversionService conversionService) {
        throw exception;
      }
    };
  }

  private static class ThrowingSolver implements ProblemSolver<FormatFamily, FormatFamily> {
    @Override
    public String getName() {
      return "throwing";
    }

    @Override
    public String getDescription() {
      return "throws exceptions for testing";
    }

    @Override
    public ProblemType<FormatFamily, FormatFamily> getProblemType() {
      return TEST_TYPE;
    }

    @Override
    public Mono<Solution<FormatFamily>> solve(
        FormattedValue<?> input,
        SubRoutineResolver subRoutineResolver,
        SolvingProperties properties,
        ConversionService conversionService) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FailingConverter implements Converter<String, String> {
    @Override
    public Format<String> getSource() {
      return FORMAT_A;
    }

    @Override
    public Format<String> getTarget() {
      return FORMAT_B;
    }

    @Override
    public String convert(String value) throws FormatException {
      throw new FormatException("conversion failed");
    }
  }

  private static class TestFormat implements Format<String> {
    private final String name;

    TestFormat(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
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
    public String parse(Reader reader) {
      return "";
    }

    @Override
    public void serialize(String value, Writer writer) {
    }
  }

  private record TestFamily() implements FormatFamily {
    @Override
    public String getName() {
      return "test";
    }

    @Override
    public Format<?> getCanonicalFormat() {
      return FORMAT_A;
    }
  }
}