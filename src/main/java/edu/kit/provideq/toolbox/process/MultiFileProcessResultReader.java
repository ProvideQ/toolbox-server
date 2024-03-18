package edu.kit.provideq.toolbox.process;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiFileProcessResultReader implements ProcessResultReader<String[]> {

    private final String globPattern;

    public MultiFileProcessResultReader(String globPattern) {
        this.globPattern = globPattern;
    }

    public ProcessResult<String[]> read(Path solutionPath, Path problemPath, Path problemDirectory) {

        List<String> solutions = new ArrayList<>();

        // Split globPattern at the last slash
        String directoryPath = globPattern.substring(0, globPattern.lastIndexOf('/'));
        String filePattern = globPattern.substring(globPattern.lastIndexOf('/') + 1);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(problemDirectory.toString(), directoryPath), filePattern)) {
            for (Path file : stream) {
                solutions.add(Files.readString(file));
            }
        } catch (IOException e) {
            return new ProcessResult<String[]>(
                false,
                Optional.empty(),
                Optional.of("Error: The problem data couldn't be read from %s:%n%s%nCommand Output: %s".formatted(
                    solutionPath, e.getMessage()))
            );
        }

        // Return the solution
        return new ProcessResult<String[]>(
            true,
            Optional.of(solutions.toArray(new String[0])),
            Optional.empty()
        );
    }
}