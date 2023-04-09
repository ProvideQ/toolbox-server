package edu.kit.provideq.toolbox.meta;

/**
 * Definition of a problem that holds basic information
 * @param type type of the problem
 * @param url url of the problem
 */
public record ProblemDefinition(ProblemType type, String url) {
}
