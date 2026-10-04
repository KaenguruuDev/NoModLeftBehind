package dev.kaenguruu.nomodleftbehind;

import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;

import java.nio.file.Path;

public record ModResolutionResult(
    DownloadableModConfiguration mod,
    ModResolutionStatus status,
    Path file,
    String installedHash
) {
    public enum ModResolutionStatus {
        MISSING,
        PRESENT,
        HASH_MISMATCH
    }
}
