package dev.kaenguruu.nomodleftbehind.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupWindowStyleTest {
    @Test
    void allowsVerifiedReplacementToClearChecksumMismatch() {
        assertTrue(StartupWindowStyle.canReplaceStatus(
            StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH,
            StartupWindowStyle.ModStatus.ADDED
        ));
    }

    @Test
    void describesEachModStatusInItsTooltip() {
        assertEquals(
            "This required mod is missing. Download it and place it in your mods folder to continue.",
            StartupWindowStyle.statusTooltip(StartupWindowStyle.ModStatus.REQUIRED, null)
        );
        assertEquals(
            "This optional mod is missing. Download it if you want to use it, or choose “Don't show again” to hide it.",
            StartupWindowStyle.statusTooltip(StartupWindowStyle.ModStatus.OPTIONAL, null)
        );
        assertEquals(
            "This file doesn't match the version specified by the modpack author. Replace it with the correct file.",
            StartupWindowStyle.statusTooltip(StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH, null)
        );
        assertEquals(
            "Found example-mod.jar in your mods folder.",
            StartupWindowStyle.statusTooltip(StartupWindowStyle.ModStatus.ADDED, "example-mod.jar")
        );
    }
}
