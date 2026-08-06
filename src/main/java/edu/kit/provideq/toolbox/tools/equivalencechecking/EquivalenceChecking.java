package edu.kit.provideq.toolbox.tools.equivalencechecking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** Runs the circuit equivalence-checking Python tool. */
@Component
public class EquivalenceChecking {
  private static final ProblemType<String, String> TOOL_TYPE = new ProblemType<>(
      "equivalencechecking",
      "Circuit equivalence checking",
      String.class,
      String.class
  );

  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;
  private final ObjectMapper objectMapper;

  /** Creates an equivalence-checking tool backed by the configured Python script. */
  @Autowired
  public EquivalenceChecking(
      @Value("${path.tools.equivalencechecking}") String scriptPath,
      @Value("${venv.tools.equivalencechecking}") String venv,
      ApplicationContext context,
      ObjectMapper objectMapper) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
    this.objectMapper = objectMapper;
  }

  /** Executes the Python tool and returns its JSON response. */
  public JsonNode check(JsonNode input) {
    final String serializedInput;
    try {
      serializedInput = objectMapper.writeValueAsString(input);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Could not serialize the request JSON.", exception);
    }

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(serializedInput)
        .readOutputFile()
        .run(TOOL_TYPE, UUID.randomUUID());

    if (!processResult.success()) {
      throw new IllegalStateException(processResult.errorOutput()
          .orElse("Equivalence checking failed without an error message."));
    }

    var output = processResult.output()
        .orElseThrow(() -> new IllegalStateException(
            "Equivalence checking completed without returning output."));
    try {
      var jsonOutput = objectMapper.readTree(output);
      if (jsonOutput == null) {
        throw new IllegalStateException("Equivalence checking returned empty output.");
      }
      return jsonOutput;
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Equivalence checking returned invalid JSON.", exception);
    }
  }
}
