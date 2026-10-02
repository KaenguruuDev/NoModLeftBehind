package dev.kaenguruu.nomodleftbehind.configuration;

public record ConfigurationIssue(
    String path,
    ConfigurationError error,
    String message
) {
}
