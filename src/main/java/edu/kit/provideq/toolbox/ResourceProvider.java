package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides streamlined access to resources
 */
@Component
public class ResourceProvider {
    private final String workingDirectoryPath;
    private final ResourceLoader resourceLoader;

    @Autowired
    public ResourceProvider(
            @Value("${working.directory}") String workingDirectoryPath,
            ResourceLoader resourceLoader) {
        this.workingDirectoryPath = workingDirectoryPath;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns the working directory and creates it if non-existing
     * @return File object of the working directory
     * @throws IOException when the directory couldn't be created
     */
    public File getWorkingDirectory() throws IOException {
        var workingDirectory = new File(workingDirectoryPath);

        // Create working directory
        Files.createDirectories(Paths.get(workingDirectory.toURI()));

        return workingDirectory;
    }

    /**
     * Returns the directory for a problem type and solution id
     * @param problem problem to check type for
     * @param solution solution to get an id from
     * @return File object of the directory
     * @throws IOException when the directory couldn't be accessed or created
     */
    public File getProblemDirectory(Problem<?> problem, Solution<?> solution) throws IOException {
        File workingDirectory = getWorkingDirectory();

        Path dir = Paths.get(workingDirectory.toString(), problem.type().toString(), String.valueOf(solution.id()));
        Files.createDirectories(dir);

        return dir.toFile();
    }

    /**
     * Returns the resource at the specified resource path
     * @param resourcePath path that points to the requested resource
     * @return File object of the resource
     * @throws IOException  when the resource is not available
     */
    public File getResource(String resourcePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        return resource.getFile();
    }
}
