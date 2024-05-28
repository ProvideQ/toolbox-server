package edu.kit.provideq.toolbox.process;

import java.nio.file.Path;


public interface ProcessResultReader<T> {
    public ProcessResult<T> read(Path solutionPath, Path problemPath, Path problemDirectory);
}
