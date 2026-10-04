package dev.kaenguruu.nomodleftbehind.configuration.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public final class DownloadableModConfiguration {
    private final String name;
    private final String url;
    private final String filePattern;

    @SerializedName("_comment_fileHash")
    private final String fileHashComment = "Remove the fileHash field whenever you update this mod. It will be automatically regenerated on startup.";
    private final String fileHash;

    private final boolean isOptional;

    public DownloadableModConfiguration(String name, String url, String filePattern, String fileHash, boolean isOptional) {
        this.name = name;
        this.url = url;
        this.filePattern = filePattern;
        this.fileHash = fileHash;
        this.isOptional = isOptional;
    }

    public DownloadableModConfiguration(String url, String filePattern, String name, boolean isOptional) {
        this(name, url, filePattern, null, isOptional);
    }

    public boolean isRequired() {
        return !isOptional;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    public String filePattern() {
        return filePattern;
    }

    public String fileHash() {
        return fileHash;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public DownloadableModConfiguration withFileHash(String fileHash) {
        return new DownloadableModConfiguration(name, url, filePattern, fileHash, isOptional);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DownloadableModConfiguration other)) {
            return false;
        }
        return isOptional == other.isOptional
            && Objects.equals(name, other.name)
            && Objects.equals(url, other.url)
            && Objects.equals(filePattern, other.filePattern)
            && Objects.equals(fileHash, other.fileHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, filePattern, fileHash, isOptional);
    }
}
