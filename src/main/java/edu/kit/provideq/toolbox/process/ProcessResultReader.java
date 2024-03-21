package edu.kit.provideq.toolbox.process;

import java.nio.file.Path;


public interface ProcessResultReader<R> {
    public ProcessResult<R> read(Path solutionPath, Path problemPath, Path problemDirectory);
}
