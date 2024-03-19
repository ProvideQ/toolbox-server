package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.TypedProblemType;
import java.util.Optional;
import java.util.Set;

public interface ProblemManagerProvider {
  Set<ProblemManager<?, ?>> getProblemManagers();

  <InputT, ResultT> Optional<ProblemManager<InputT, ResultT>> getProblemManagerForType(
      TypedProblemType<InputT, ResultT> problemType
  );
}
