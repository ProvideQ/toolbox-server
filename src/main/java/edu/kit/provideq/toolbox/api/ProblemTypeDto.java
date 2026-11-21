package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.meta.AttributeDto;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Comparator;
import java.util.List;

public record ProblemTypeDto(
    String id,
    String description,
    List<AttributeDto> attributes) {
  public static ProblemTypeDto fromProblemType(
      final ProblemType<?, ?> problemType,
      final ConversionRegistry conversionRegistry) {
    return new ProblemTypeDto(
        problemType.getId(),
        problemType.getDescription(),
        conversionRegistry.getFormats(problemType.getInputFamily())
            .stream()
            .flatMap(format -> format.getAttributeReaders().entrySet().stream())
            .map(e -> new AttributeDto(e.getKey(), e.getValue().getType().getSimpleName()))
            .sorted(Comparator.comparing(AttributeDto::name))
            .toList());
  }
}
