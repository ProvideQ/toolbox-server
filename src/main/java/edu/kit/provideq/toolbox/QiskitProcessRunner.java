package edu.kit.provideq.toolbox;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process runner with output post-processing specifically for invoking GAMS.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class QiskitProcessRunner extends ProcessRunner {
    /**
     * The name of the file to call for running a GAMS script.
     */
    private static final String QISKIT_EXECUTABLE_NAME = "python";

    /**
     * Creates a process runner for a Qiskit script.
     * @param directory the working directory to run Qiskit in.
     * @param scriptFileName the filename of the Qiskit script to run.
     */
    public QiskitProcessRunner(String directory, String scriptFileName) {
        this(directory, scriptFileName, new String[0]);
    }

    /**
     * Creates a process runner for a Qiskit script.
     * @param directory the working directory to run Qiskit in.
     * @param scriptFileName the filename of the Qiskit script to run.
     * @param arguments extra arguments to pass to Qiskit. Use this to pass problem input to the solver.
     */
    public QiskitProcessRunner(String directory, String scriptFileName, String... arguments) {
        super(createGenericProcessBuilder(directory, QISKIT_EXECUTABLE_NAME, scriptFileName, arguments));
    }
}
