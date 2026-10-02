package dev.kaenguruu.nomodleftbehind.configuration.model;

import java.util.Map;

public record DisabledOptionalDownloadsConfiguration(boolean neverAskForOptionals,
                                                     Map<String, Boolean> skipForOptionalModUrl) {
}
