package edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Object representation of an unsplittable multicommodity flow instance.
 *
 * <p>The instance is stored as its JSON document; the configuration section is parsed
 * lazily on first access to avoid parsing work when only the raw document is needed.
 */
public final class UmcfInstance {
  private static final Pattern CONFIG_KEY_PATTERN =
      Pattern.compile("\"([A-Z_0-9]+)\"\\s*:\\s*(-?[0-9.eE+]+)");

  private final String json;
  private volatile Map<String, String> configCache;

  /**
   * Creates an instance from its JSON document.
   *
   * @param json the JSON document of the instance
   */
  public UmcfInstance(String json) {
    this.json = json;
  }

  /**
   * Returns the JSON document of this instance.
   */
  public String json() {
    return json;
  }

  /**
   * Returns the configuration values of the {@code config} section, parsed lazily.
   */
  public Map<String, String> getConfig() {
    var config = configCache;
    if (config == null) {
      synchronized (this) {
        config = configCache;
        if (config == null) {
          config = parseConfig(json);
          configCache = config;
        }
      }
    }
    return config;
  }

  /**
   * Returns a configuration value of the {@code config} section, or an empty string if
   * the key is not present.
   *
   * @param key the configuration key, e.g. {@code N_PRODUCTS}
   * @return the configuration value
   */
  public String getConfigValue(String key) {
    return getConfig().getOrDefault(key, "");
  }

  private static Map<String, String> parseConfig(String json) {
    var configStart = json.indexOf("\"config\"");
    if (configStart == -1) {
      return Map.of();
    }

    var searchEnd = json.length();
    var configObjectStart = json.indexOf('{', configStart);
    if (configObjectStart != -1) {
      searchEnd = findConfigObjectEnd(json, configObjectStart);
    }

    Map<String, String> config = new HashMap<>();
    var matcher = CONFIG_KEY_PATTERN.matcher(json.substring(configStart, searchEnd));
    while (matcher.find()) {
      config.putIfAbsent(matcher.group(1), matcher.group(2));
    }
    return Map.copyOf(config);
  }

  private static int findConfigObjectEnd(String json, int objectStart) {
    var depth = 0;
    for (var i = objectStart; i < json.length(); i++) {
      var c = json.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return json.length();
  }
}
