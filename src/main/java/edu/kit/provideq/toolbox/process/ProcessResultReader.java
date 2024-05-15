package edu.kit.provideq.toolbox.process;

import java.nio.file.Path;


public interface ProcessResultReader<R> {
    public ProcessResult read(Path solutionPath, Path problemPath, Path problemDirectory);
}
