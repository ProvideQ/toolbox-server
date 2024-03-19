package edu.kit.provideq.toolbox.meta.setting;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetaSolverSettingDeserializer extends JsonDeserializer<MetaSolverSetting> {
  @Override
  public MetaSolverSetting deserialize(JsonParser jsonParser,
                                       DeserializationContext deserializationContext)
      throws IOException {
    ObjectCodec codec = jsonParser.getCodec();
    JsonNode node = codec.readTree(jsonParser);

    // Get type
    MetaSolverSettingType type = MetaSolverSettingType.valueOf(node.get("type").asText());
    

    var name = node.get("name").asText();
    var title = node.get("title").asText();
    // Create subclass based on the type
    switch (type) {
      case CHECKBOX -> {
        return new BooleanState(
            name,
            title,
            node.get("state").asBoolean());
      }
      case RANGE -> {
        return new BoundedInteger(
            name,
            title,
            node.get("min").asDouble(),
            node.get("max").asDouble(),
            node.get("value").asDouble());
      }
      case SELECT -> {
        ArrayNode optionsNode = (ArrayNode) node.get("options");
        List<String> options = new ArrayList<>();
        for (JsonNode optionNode : optionsNode) {
          options.add(codec.treeToValue(optionNode, Object.class).toString());
        }
        return new Select<>(
            name,
            title,
            options,
            codec.treeToValue(node.get("selectedOption"), Object.class).toString());
      }
      case TEXT -> {
        return new Text(
            name,
            title,
            node.get("text").asText());
      }
      case INTEGER -> {
        return new IntegerSetting(
            name,
            title,
            node.get("number").asInt());
      }
      default -> throw new IllegalArgumentException("Invalid MetaSolverSettingType: " + type);
    }
  }
}
