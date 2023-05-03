package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@Component
public class SubRoutinePool {
    private final Map<ProblemType, Function<Object, Solution>> subRoutineCalls;

    @Autowired
    private ProblemControllerProvider problemControllerProvider;

    public SubRoutinePool() {
        subRoutineCalls = Collections.emptyMap();
    }

    public SubRoutinePool(Map<ProblemType, Function<Object, Solution>> subRoutineCalls) {
        this.subRoutineCalls = Map.copyOf(subRoutineCalls);
    }

    /**
     * Request a subroutine for a problem type that invokes the solving process that was previously specified.
     * If no subroutine is available, use the default meta solver strategy in the routine.
     * @param problemType problem type to solve
     * @return function to solve a problem of type problemType
     */
    public Function<Object, Solution> getSubRoutine(ProblemType problemType) {
        var subRoutine = subRoutineCalls.get(problemType);
        if (subRoutine != null) return subRoutine;

        return content -> {
            var solveRequest = new SolveRequest();
            solveRequest.requestContent = content;

            var problemController = problemControllerProvider.getProblemController(problemType);
            return (Solution) problemController.solve(solveRequest);
        };
    }
}
