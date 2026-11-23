package edu.kit.provideq.toolbox.api.dto;

/**
 * A problem type attribute with its value type.
 *
 * @param name attribute name, e.g. {@code variable_count}
 * @param type simple name of the attribute value type, e.g. {@code Integer}
 */
public record AttributeDto(String name, String type) {
}
