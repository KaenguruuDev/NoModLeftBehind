package dev.kaenguruu.nomodleftbehind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashUtilTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void returnsTheSha256HashForAFile() throws IOException {
        var file = temporaryDirectory.resolve("mod.jar");
        Files.writeString(file, "mod");

        assertEquals(
            "e55cffc81a5ad8cfe85239d944a3ae9513645a9eed79bc884f51b80b2760fc46",
            HashUtil.getHashForFile(file)
        );
    }
}
