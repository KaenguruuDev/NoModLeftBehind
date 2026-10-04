package dev.kaenguruu.nomodleftbehind.configuration.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DownloadableModConfigurationTest {
    @Test
    void configurationsWithTheSameValuesCompareAsEqual() {
        var first = new DownloadableModConfiguration(
            "Example Mod",
            "https://example.com/mod.jar",
            "example-.*\\.jar",
            "hash",
            false
        );
        var second = new DownloadableModConfiguration(
            "Example Mod",
            "https://example.com/mod.jar",
            "example-.*\\.jar",
            "hash",
            false
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second.withFileHash("different-hash"));
    }
}
