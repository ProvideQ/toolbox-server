package edu.kit.provideq.toolbox.process;

import java.util.Optional;

/**
 * Result of running a process.
 *
 * @param success did the process complete successfully
 * @param output  process console output
 */
public record ProcessResult<T>(boolean success, Optional<T> output, Optional<String> errorOutput) {

}
