package dev.kaenguruu.nomodleftbehind.client;

import dev.kaenguruu.nomodleftbehind.HashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartupWindowFileReadinessTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void hashesFileAfterCopyFinishes() throws Exception {
        var file = temporaryDirectory.resolve("mod.jar");
        Files.writeString(file, "partial contents");

        var hash = CompletableFuture.supplyAsync(() -> hash(file));
        Thread.sleep(25);
        Files.writeString(file, "complete contents");

        assertEquals(HashUtil.getHashForFile(file), hash.get(2, TimeUnit.SECONDS));
    }

    private static String hash(Path file) {
        try {
            return HashUtil.getHashForFileAfterSettles(file);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
