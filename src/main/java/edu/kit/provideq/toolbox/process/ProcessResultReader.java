package edu.kit.provideq.toolbox.process;

import java.nio.file.Path;


public interface ProcessResultReader<T> {
  ProcessResult<T> read(Path solutionPath, Path problemDirectory);
}
