package edu.kit.provideq.formats.core;

/**
 * Kind of a format: problem input format or solution format.
 * A format may be both, e.g. when the same representation is used for problems and solutions.
 */
public enum FormatKind {
  PROBLEM,
  SOLUTION,
  PROBLEM_AND_SOLUTION
}
