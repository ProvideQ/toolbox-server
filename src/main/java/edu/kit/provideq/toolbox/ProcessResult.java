package edu.kit.provideq.toolbox;

/**
 * Result of running a process.
 *
 * @param success did the process complete successfully
 * @param output  process console output
 */
public record ProcessResult(boolean success, String output) {

}
