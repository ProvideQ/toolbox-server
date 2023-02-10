package edu.kit.provideq.toolbox;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Runs a process.
 * Is meant to be expanded with the builder pattern.
 */
public class ProcessRunner {
    private final ProcessBuilder processBuilder;

    public ProcessRunner(ProcessBuilder processBuilder) {
        this.processBuilder = processBuilder;
    }

    /**
     * Runs the process provided in the constructor
     * @return Returns the process result
     * @throws IOException when files in the process couldn't be accessed
     * @throws InterruptedException when the console output can't be accessed
     */
    public ProcessResult run() throws IOException, InterruptedException {
        Process process = processBuilder.start();

        StringBuilder sb = new StringBuilder();
        sb.append(readStream(process.inputReader()));
        sb.append(readStream(process.errorReader()));

        String output = sb.toString();

        int i = process.waitFor();
        if (i == 0) {
            return new ProcessResult(true, output);
        }

        return new ProcessResult(false, output);
    }

    private String readStream(BufferedReader reader) throws IOException {
        var inputBuilder = new StringBuilder();
        var line = reader.readLine();
        while (line != null) {
            inputBuilder.append(line).append('\n');
            line = reader.readLine();
        }
        reader.close();

        return inputBuilder.toString();
    }
}
