package edu.kit.provideq.toolbox.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.families.FormatFamily;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Tests for lazy representation pulls of sub-routine inputs in {@link SubProblems}.
 */
class SubProblemsConversionTest {
  private static final Format<String> FORMAT_F1 = new TestFormat("f1");
  private static final Format<String> FORMAT_F2 = new TestFormat("f2");
  private static final FormatFamily TEST_FAMILY = new TestFamily();
  private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(5);

  @Test
  void subSolverReceivesTheSameFormattedValue() {
    var subType = new ProblemType<>("sub", "sub", TEST_FAMILY, TEST_FAMILY);
    var capturing = runSubRoutine(subType, SubRoutineDefinition.of(subType, "call", true),
        FormattedValue.of("hello", FORMAT_F1), true);

    assertSame("hello", capturing.receivedValue);
    assertSame(FORMAT_F1, capturing.receivedFormat);
  }

  @Test
  void subSolverCanPullConvertedRepresentation() {
    var subType = new ProblemType<>("sub", "sub", TEST_FAMILY, TEST_FAMILY);
    var capturing = runSubRoutine(subType, SubRoutineDefinition.of(subType, "call", true),
        FormattedValue.of("hello", FORMAT_F1), true);

    // the sub solver pulls the f2 representation via the attached conversion service
    assertEquals("hello-f2", capturing.pulledF2);
  }

  @Test
  void pullWithoutConversionServiceThrows() {
    var subType = new ProblemType<>("sub", "sub", TEST_FAMILY, TEST_FAMILY);
    var capturing = runSubRoutine(subType, SubRoutineDefinition.of(subType, "call", true),
        FormattedValue.of("hello", FORMAT_F1), false);

    assertEquals("no-conversion-service", capturing.pulledF2);
  }

  /**
   * Runs a parent problem whose solver forwards {@code input} to the given sub-routine
   * and returns the capturing sub solver that recorded the received input.
   */
  private CapturingSolver runSubRoutine(
      ProblemType<FormatFamily, FormatFamily> subType,
      SubRoutineDefinition<FormatFamily, FormatFamily> definition,
      FormattedValue<?> input,
      boolean withConversionService) {
    var registry = new ConversionRegistry();
    registry.registerFormat(FORMAT_F1);
    registry.registerFormat(FORMAT_F2);
    registry.registerConverter(new FuncConverter<>(FORMAT_F1, FORMAT_F2,
        value -> value + "-f2"));
    registry.seal();
    final var conversionService = new ConversionService(registry);

    var parentType = new ProblemType<>("parent", "parent", TEST_FAMILY, TEST_FAMILY);
    var parent = new Problem<>(parentType);

    var capturing = new CapturingSolver(subType);
    parent.addObserver(new TestObserver(capturing));

    parent.setSolver(new ForwardingSolver(definition, input));
    parent.setInput(FormattedValue.of("parent-input", FORMAT_F1));

    parent.solve(withConversionService ? conversionService : null).block(TIMEOUT);

    return capturing;
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
      return FORMAT_F1;
    }
  }

  private static class TestObserver implements ProblemObserver<FormatFamily, FormatFamily> {
    private final CapturingSolver capturing;

    TestObserver(CapturingSolver capturing) {
      this.capturing = capturing;
    }

    @Override
    public void onInputChanged(Problem<FormatFamily, FormatFamily> problem,
                               FormattedValue<?> newInput) {
    }

    @Override
    public void onSolverChanged(Problem<FormatFamily, FormatFamily> problem,
                                ProblemSolver<FormatFamily, FormatFamily> newSolver) {
    }

    @Override
    public void onSolverReset(Problem<FormatFamily, FormatFamily> problem) {
    }

    @Override
    public void onStateChanged(Problem<FormatFamily, FormatFamily> problem, ProblemState newState) {
    }

    @Override
    public <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemAdded(
        Problem<FormatFamily, FormatFamily> problem,
        Problem<SubInT, SubOutT> addedSubProblem) {
      @SuppressWarnings("unchecked")
      var subProblem = (Problem<FormatFamily, FormatFamily>) addedSubProblem;
      subProblem.setSolver(capturing);
    }

    @Override
    public <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemRemoved(
        Problem<FormatFamily, FormatFamily> problem,
        Problem<SubInT, SubOutT> removedSubProblem) {
    }
  }

  private static class ForwardingSolver implements ProblemSolver<FormatFamily, FormatFamily> {
    private final SubRoutineDefinition<FormatFamily, FormatFamily> definition;
    private final FormattedValue<?> input;

    ForwardingSolver(SubRoutineDefinition<FormatFamily, FormatFamily> definition,
                     FormattedValue<?> input) {
      this.definition = definition;
      this.input = input;
    }

    @Override
    public String getName() {
      return "forwarding";
    }

    @Override
    public String getDescription() {
      return "forwards input to the sub-routine";
    }

    @Override
    public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
      return List.of(definition);
    }

    @Override
    public Mono<Solution<FormatFamily>> solve(
        FormattedValue<?> formattedInput,
        SubRoutineResolver subRoutineResolver,
        SolvingProperties properties,
        ConversionService conversionService) {
      return subRoutineResolver.runSubRoutine(definition, input, conversionService)
          .map(_ -> {
            var solution = new Solution<>(this);
            solution.setSolutionData("done", FORMAT_F1);
            solution.complete();
            return solution;
          });
    }

    @Override
    public ProblemType<FormatFamily, FormatFamily> getProblemType() {
      return new ProblemType<>("parent", "parent", TEST_FAMILY, TEST_FAMILY);
    }
  }

  private static class CapturingSolver implements ProblemSolver<FormatFamily, FormatFamily> {
    private final ProblemType<FormatFamily, FormatFamily> type;
    private Object receivedValue;
    private Format<?> receivedFormat;
    private Object pulledF2;

    CapturingSolver(ProblemType<FormatFamily, FormatFamily> type) {
      this.type = type;
    }

    @Override
    public String getName() {
      return "capturing";
    }

    @Override
    public String getDescription() {
      return "captures the received input";
    }

    @Override
    public ProblemType<FormatFamily, FormatFamily> getProblemType() {
      return type;
    }

    @Override
    public Mono<Solution<FormatFamily>> solve(
        FormattedValue<?> input,
        SubRoutineResolver subRoutineResolver,
        SolvingProperties properties,
        ConversionService conversionService) {
      this.receivedValue = input.getValue();
      this.receivedFormat = input.getFormat();
      try {
        this.pulledF2 = input.as(FORMAT_F2, conversionService);
      } catch (IllegalStateException e) {
        this.pulledF2 = "no-conversion-service";
      } catch (Exception e) {
        this.pulledF2 = "error: " + e.getMessage();
      }
      var solution = new Solution<>(this);
      solution.setSolutionData("done", FORMAT_F1);
      solution.complete();
      return Mono.just(solution);
    }
  }
}