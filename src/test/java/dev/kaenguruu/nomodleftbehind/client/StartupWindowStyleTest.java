package dev.kaenguruu.nomodleftbehind.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupWindowStyleTest {
    @Test
    void allowsVerifiedReplacementToClearChecksumMismatch() {
        assertTrue(StartupWindowStyle.canReplaceStatus(
            StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH,
            StartupWindowStyle.ModStatus.ADDED
        ));
    }
}
