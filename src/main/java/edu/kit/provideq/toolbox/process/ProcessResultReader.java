package edu.kit.provideq.toolbox.process;

import java.nio.file.Path;


public interface ProcessResultReader<ResultType> {
    public ProcessResult<ResultType> read(Path solutionPath, Path problemPath, Path problemDirectory);
}
