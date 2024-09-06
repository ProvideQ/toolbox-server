package edu.kit.provideq.toolbox.meta.setting;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolverSettingDeserializer extends JsonDeserializer<SolverSetting> {
  @Override
  public SolverSetting deserialize(JsonParser jsonParser,
                                   DeserializationContext deserializationContext)
      throws IOException {
    ObjectCodec codec = jsonParser.getCodec();
    JsonNode node = codec.readTree(jsonParser);

    // Get type
    SolverSettingType type = SolverSettingType.valueOf(node.get("type").asText());

    var name = node.get("name").asText();
    var description = node.get("description").asText();
    var required = node.get("required").asBoolean();

    var solver = getSolverSetting(type, name, description, node, codec);
    solver.setRequired(required);
    return solver;
  }

  private static SolverSetting getSolverSetting(
      SolverSettingType type, String name, String description, JsonNode node, ObjectCodec codec)
      throws JsonProcessingException {
    // Create subclass based on the type
    switch (type) {
      case CHECKBOX -> {
        return new BooleanState(
            name,
            description,
            node.get("state").asBoolean());
      }
      case INTEGER -> {
        var min = node.get("min").asInt();
        var max = node.get("max").asInt();
        var value = node.get("value").asInt();
        if (value < min || value > max) {
          throw new IllegalArgumentException("Value must be between min and max");
        }

        return new BoundedInteger(
            name,
            description,
            min,
            max,
            value);
      }
      case DOUBLE -> {
        var min = node.get("min").asDouble();
        var max = node.get("max").asDouble();
        var value = node.get("value").asDouble();
        if (value < min || value > max) {
          throw new IllegalArgumentException("Value must be between min and max");
        }

        return new BoundedDouble(
            name,
            description,
            min,
            max,
            value);
      }
      case SELECT -> {
        ArrayNode optionsNode = (ArrayNode) node.get("options");
        List<String> options = new ArrayList<>();
        for (JsonNode optionNode : optionsNode) {
          options.add(codec.treeToValue(optionNode, Object.class).toString());
        }
        return new Select<>(
            name,
            description,
            options,
            codec.treeToValue(node.get("selectedOption"), Object.class).toString());
      }
      case TEXT -> {
        return new Text(
            name,
            description,
            node.get("text").asText());
      }
      default -> throw new IllegalArgumentException("Invalid MetaSolverSettingType: " + type);
    }
  }
}
