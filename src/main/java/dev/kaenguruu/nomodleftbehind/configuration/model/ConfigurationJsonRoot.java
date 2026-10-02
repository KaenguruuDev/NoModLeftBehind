package dev.kaenguruu.nomodleftbehind.configuration.model;

import java.util.List;

public record ConfigurationJsonRoot(List<DownloadableModConfiguration> clientMods,
                                    List<DownloadableModConfiguration> serverMods) {
}
