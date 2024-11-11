package edu.kit.provideq.toolbox.process;

import java.util.function.BiFunction;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process runner with input & output post-processing specifically for invoking GAMS.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GamsProcessRunner extends ProcessRunner {
  /**
   * GAMS will print the following prefix in front of its license output.
   */
  private static final String LICENSE_HEADER_PREFIX = "Licensee:";

  /**
   * GAMS will print a license header with this number of lines on each CLI invocation.
   * Note that the printed license summary is shorter than the "gamslice.txt" contents.
   */
  private static final int LICENSE_LINE_COUNT = 5;

  /**
   * The name of the file to call for running a GAMS script.
   */
  private static final String GAMS_EXECUTABLE_NAME = "gams";

  /**
   * Creates a process runner for a GAMS task.
   *
   * @param gameScriptPath the filepath of the GAMS script to run.
   */
  public GamsProcessRunner(String gameScriptPath) {
    super(new ProcessBuilder());

    withArguments(
        GAMS_EXECUTABLE_NAME,
        gameScriptPath);
  }

  /**
   * Removes GAMS' license output from an output log.
   */
  private static String obfuscateGamsLicense(String output) {
    String[] lines = output.split("\n");

    // init negatively to avoid unintended obfuscation in the beginning
    int lastLicenseHeader = -LICENSE_LINE_COUNT;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith(LICENSE_HEADER_PREFIX)) {
        lastLicenseHeader = i;
      }

      if (i - lastLicenseHeader < LICENSE_LINE_COUNT) {
        lines[i] = obfuscateLine(lines[i]);
      }
    }

    return String.join("\n", lines);
  }

  /**
   * Obfuscates a given string.
   */
  private static String obfuscateLine(String line) {
    return Strings.repeat("*", line.length());
  }

  @Override
  protected <T> ProcessRunnerExecutor<T> getExecutor(
      BiFunction<String, String, ProcessResult<T>> outputProcessor) {
    return (problemType, solutionId) -> {
      if (problemType.getResultClass() == String.class) {
        var processRunner = super.getExecutor(outputProcessor);
        var result = processRunner.run(problemType, solutionId);

        @SuppressWarnings("unchecked") // we know that T is a String
        var obfuscatedOutput = result
            .output()
            .map(String::valueOf)
            .map(x -> (T) obfuscateGamsLicense(x));

        var obfuscatedErrorOutput = result
            .errorOutput()
            .map(GamsProcessRunner::obfuscateGamsLicense);

        return new ProcessResult<>(result.success(), obfuscatedOutput, obfuscatedErrorOutput);
      }

      return super.getExecutor(outputProcessor).run(problemType, solutionId);
    };
  }
}
