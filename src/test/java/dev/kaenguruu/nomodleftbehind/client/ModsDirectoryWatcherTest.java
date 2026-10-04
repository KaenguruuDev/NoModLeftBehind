package dev.kaenguruu.nomodleftbehind.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ModsDirectoryWatcherTest {
    @TempDir
    Path modsDirectory;

    @Test
    void reportsNewRegularFiles() throws IOException, InterruptedException {
        var createdFiles = new CopyOnWriteArrayList<Path>();
        var fileCreated = new CountDownLatch(1);

        try (var ignored = ModsDirectoryWatcher.start(modsDirectory, path -> {
            createdFiles.add(path);
            fileCreated.countDown();
        })) {
            var createdFile = modsDirectory.resolve("example-mod.jar");
            Files.writeString(createdFile, "mod");

            assertTrue(fileCreated.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(createdFile), createdFiles);
        }
    }

    @Test
    void reportsModifiedRegularFiles() throws IOException, InterruptedException {
        var changedFiles = new CopyOnWriteArrayList<Path>();
        var fileChanged = new CountDownLatch(1);
        var changedFile = modsDirectory.resolve("example-mod.jar");
        Files.writeString(changedFile, "mod");

        try (var ignored = ModsDirectoryWatcher.start(modsDirectory, path -> {
            if (path.equals(changedFile)) {
                changedFiles.add(path);
                fileChanged.countDown();
            }
        })) {
            Files.writeString(changedFile, "updated mod");

            assertTrue(fileChanged.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(changedFile), changedFiles);
        }
    }

    @Test
    void ignoresNewDirectories() throws IOException, InterruptedException {
        var fileCreated = new CountDownLatch(1);

        try (var ignored = ModsDirectoryWatcher.start(modsDirectory, path -> fileCreated.countDown())) {
            Files.createDirectory(modsDirectory.resolve("not-a-mod.jar"));

            assertFalse(fileCreated.await(250, TimeUnit.MILLISECONDS));
        }
    }
}
