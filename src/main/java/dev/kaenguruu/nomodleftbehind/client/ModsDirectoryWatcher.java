package dev.kaenguruu.nomodleftbehind.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

final class ModsDirectoryWatcher implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path directory;
    private final WatchService watchService;
    private final Consumer<Path> fileCreatedHandler;
    private final Thread watcherThread;
    private volatile boolean closed;

    private ModsDirectoryWatcher(Path directory, Consumer<Path> fileCreatedHandler) throws IOException {
        this.directory = directory;
        this.fileCreatedHandler = fileCreatedHandler;
        this.watchService = directory.getFileSystem().newWatchService();

        try {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException exception) {
            try {
                watchService.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }

        this.watcherThread = new Thread(this::watch, "No Mod Left Behind mods watcher");
        this.watcherThread.setDaemon(true);
    }

    static ModsDirectoryWatcher start(Path directory, Consumer<Path> fileCreatedHandler) throws IOException {
        Files.createDirectories(directory);

        var watcher = new ModsDirectoryWatcher(directory, fileCreatedHandler);
        watcher.watcherThread.start();
        return watcher;
    }

    private void watch() {
        try {
            while (!closed) {
                var key = watchService.take();
                processEvents(key);
                if (!key.reset()) {
                    LOGGER.warn("Stopped watching the mods directory because its watch key is no longer valid: {}", directory);
                    return;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException exception) {
            if (!closed) {
                LOGGER.warn("Stopped watching the mods directory because the watch service was closed unexpectedly.", exception);
            }
        }
    }

    private void processEvents(WatchKey key) {
        for (var event : key.pollEvents()) {
            processEvent(event);
        }
    }

    private void processEvent(WatchEvent<?> event) {
        if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE
            || !(event.context() instanceof Path relativePath)) {
            return;
        }

        var createdPath = directory.resolve(relativePath);
        if (!Files.isRegularFile(createdPath)) {
            return;
        }

        try {
            fileCreatedHandler.accept(createdPath);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to process a newly created file in the mods directory: {}", createdPath, exception);
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            watchService.close();
        } catch (IOException exception) {
            LOGGER.warn("Unable to stop watching the mods directory: {}", directory, exception);
        }
    }
}
