package edu.kit.provideq.toolbox.meta;

/**
 * A Problem consists of problem data, i.e. a formatted string, as well as a type by which it can be
 * assigned to a suitable solver
 */
public record Problem<FormatType>(FormatType problemData, ProblemType type) {
}
