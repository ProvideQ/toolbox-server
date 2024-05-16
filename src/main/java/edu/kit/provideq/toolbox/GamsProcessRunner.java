package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.UUID;
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
   * @param directory      the working directory to run GAMS in.
   * @param scriptFileName the filename of the GAMS script to run.
   */
  public GamsProcessRunner(String directory, String scriptFileName) {
    this(directory, scriptFileName, new String[0]);
  }

  /**
   * Creates a process runner for a GAMS task.
   *
   * @param directory      the working directory to run GAMS in.
   * @param scriptFileName the filename of the GAMS script to run.
   * @param arguments      extra arguments to pass to GAMS. Use this to pass problem input to the
   *                       solver.
   */
  public GamsProcessRunner(String directory, String scriptFileName, String... arguments) {
    super(createGenericProcessBuilder(directory, GAMS_EXECUTABLE_NAME, scriptFileName, arguments));

    addProblemFilePathToProcessCommand("--INPUT=\"%s\"");
  }

  @Override
  public ProcessResult run(ProblemType<?, ?> problemType, UUID solutionId, String problemData) {
    var result = super.run(problemType, solutionId, problemData);

    var obfuscatedOutput = obfuscateGamsLicense(result.output());
    return new ProcessResult(result.success(), obfuscatedOutput);
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
}
