package dev.kaenguruu.nomodleftbehind.configuration.model;

import java.util.Map;

public record DisabledOptionalDownloadsConfiguration(boolean neverAskForOptionals,
                                                     Map<String, Boolean> skipForOptionalModUrl,
                                                     Map<String, Boolean> skipForChecksumMismatchModUrl) {
    public DisabledOptionalDownloadsConfiguration(
        boolean neverAskForOptionals,
        Map<String, Boolean> skipForOptionalModUrl
    ) {
        this(neverAskForOptionals, skipForOptionalModUrl, Map.of());
    }

    public DisabledOptionalDownloadsConfiguration {
        if (skipForOptionalModUrl == null) {
            skipForOptionalModUrl = Map.of();
        }
        if (skipForChecksumMismatchModUrl == null) {
            skipForChecksumMismatchModUrl = Map.of();
        }
    }
}
