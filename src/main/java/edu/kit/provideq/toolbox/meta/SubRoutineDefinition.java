package edu.kit.provideq.toolbox.meta;

/**
 * Definition of a sub routine that holds basic information
 *
 * @param type        type of the problem that is used in the sub routine
 * @param url         url of the problem to call the sub routine
 * @param description description of the sub routine call to provide information where and why it is needed
 */
public record SubRoutineDefinition(ProblemType type, String url, String description) {
}
