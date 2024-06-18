package edu.kit.provideq.toolbox.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process runner with output post-processing specifically for invoking Python scripts.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BinaryProcessRunner extends ProcessRunner {

    /**
     * Creates a process runner for a binary.
     *
     * @param directory      the working directory to run the binary in.
     * @param executable     the filename of the binary to run.
     * @param command        first argument for the binary / command to run.
     */
    public BinaryProcessRunner(String directory, String executable, String command) {
        this(directory, executable, command, new String[0]);
    }

    /**
     * Creates a process runner for a binary.
     *
     * @param directory      the working directory to run the binary in.
     * @param executable     the filename of the binary to run.
     * @param command        first argument for the binary / command to run.
     * @param arguments      extra arguments to pass to the binary. Use this to pass problem input to the
     *                       solver.
     */
    @Autowired
    public BinaryProcessRunner(String directory, String executable, String command, String... arguments) {
        super(createGenericProcessBuilder(directory, executable, command), arguments);
    }
}
