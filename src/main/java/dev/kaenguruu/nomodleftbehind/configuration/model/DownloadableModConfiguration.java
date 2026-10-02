package dev.kaenguruu.nomodleftbehind.configuration.model;

public record DownloadableModConfiguration(String url, String filePattern, String name, boolean isOptional) {
    public boolean isRequired() {
        return !isOptional;
    }
}
