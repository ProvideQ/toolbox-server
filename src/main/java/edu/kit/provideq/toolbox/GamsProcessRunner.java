package edu.kit.provideq.toolbox;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

/**
 * Process runner with output post-processing specifically for invoking GAMS.
 */
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
   * @param directory the working directory to run GAMS in.
   * @param scriptFileName the filename of the GAMS script to run.
   * @param arguments extra arguments to pass to GAMS. Use this to pass problem input to the solver.
   */
  public GamsProcessRunner(File directory, String scriptFileName, String... arguments) {
    super(buildGamsProcess(directory, scriptFileName, arguments));
  }

  @Override
  public ProcessResult run() throws IOException, InterruptedException {
    var result = super.run();

    var obfuscatedOutput = obfuscateGamsLicense(result.output());
    return new ProcessResult(result.success(), obfuscatedOutput);
  }

  /**
   * Removes GAMS' license output from an output log.
   */
  private static String obfuscateGamsLicense(String output) {
    String[] lines = output.split("\n");

    // init negatively to avoid unintended obfuscation in the beginning
    int lastLicenseHeader = - LICENSE_LINE_COUNT;
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

  private static ProcessBuilder buildGamsProcess(File directory, String scriptFilename,
                                                 String[] arguments) {
    String[] command = new String[arguments.length + 2];
    command[0] = GAMS_EXECUTABLE_NAME;
    command[1] = scriptFilename;
    System.arraycopy(arguments, 0, command, 2, arguments.length);

    return new ProcessBuilder()
        .directory(directory)
        .command(command);
  }
}
