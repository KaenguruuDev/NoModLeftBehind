package dev.kaenguruu.nomodleftbehind.configuration.model;

import java.util.List;

public record ConfigurationJsonRoot(List<DownloadableModConfiguration> clientMods,
                                    List<DownloadableModConfiguration> serverMods,
                                    List<String> trustedDomains) {
    public ConfigurationJsonRoot(
        List<DownloadableModConfiguration> clientMods,
        List<DownloadableModConfiguration> serverMods
    ) {
        this(clientMods, serverMods, List.of());
    }

    public ConfigurationJsonRoot {
        if (trustedDomains == null) {
            trustedDomains = List.of();
        }
    }
}
