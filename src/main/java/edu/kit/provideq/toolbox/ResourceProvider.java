package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Provides streamlined access to resources.
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
   * Returns the working directory and creates it if non-existing.
   *
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
   * Returns the directory for a problem type and solution id.
   *
   * @param problemType problem type to build the directory path
   * @param solutionId  solution id to build the directory path
   * @return File object of the directory
   * @throws IOException when the directory couldn't be accessed or created
   */
  public <InputT, ResultT> File getProblemDirectory(
          ProblemType<InputT, ResultT> problemType,
          long solutionId) throws IOException {
    File workingDirectory = getWorkingDirectory();

    Path dir =
        Paths.get(workingDirectory.toString(), problemType.getId(), String.valueOf(solutionId));
    Files.createDirectories(dir);

    return dir.toFile();
  }

  public List<String> getExampleProblems(String examplesDirectoryPath) throws IOException {
    // Reading the directory yields all names of the files in the directory, one per line
    return readResourceString(examplesDirectoryPath)
            .lines()
            .map(file -> {
              try {
                return readResourceString(examplesDirectoryPath + "/" + file);
              } catch (Exception e) {
                return null;
              }
            })
            .toList();
  }

  /**
   * Reads the input stream of a {@link BufferedReader} into a string.
   *
   * @param reader reader to read from
   * @return full string of the input stream
   * @throws IOException when the input stream couldn't be read
   */
  public String readStream(BufferedReader reader) throws IOException {
    var inputBuilder = new StringBuilder();

    // Process first line manually to avoid adding a newline at the beginning
    var line = reader.readLine();
    if (line != null) {
      inputBuilder.append(line);
    }

    line = reader.readLine();
    while (line != null) {
      inputBuilder.append('\n').append(line);
      line = reader.readLine();
    }
    reader.close();

    return inputBuilder.toString();
  }

  /**
   * Reads the input stream of an {@link InputStream} into a string.
   *
   * @param stream stream to read from
   * @return full string of the input stream
   * @throws IOException when the input stream couldn't be read
   */
  public String readStream(InputStream stream) throws IOException {
    return readStream(new BufferedReader(new InputStreamReader(stream)));
  }

  /**
   * Reads the resource at the specified resource path into a string.
   *
   * @param resourcePath path that points to the requested resource
   * @return full string of the data inside the resource
   * @throws IOException when the resource is not available or couldn't be read
   */
  public String readResourceString(String resourcePath) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

    try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
      return readStream(reader);
    }
  }

  /**
   * Returns the resource at the specified resource path.
   *
   * @param resourcePath path that points to the requested resource
   * @return File object of the resource
   * @throws IOException when the resource is not available
   */
  public File getResource(String resourcePath) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
    return resource.getFile();
  }

  /**
   * Returns the file at the path relative to the root directory.
   *
   * @param path path that points to a file relative to the root directory
   * @return File object of the path
   * @throws IOException when the file is not available
   */
  public File getRootFile(String path) throws IOException {
    // Create directories
    Files.createDirectories(Paths.get(path));

    // Return File
    return new File(path);
  }
}
