package dev.kaenguruu.nomodleftbehind.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class ModsDirectoryWatcher implements AutoCloseable {
    private static final long FILE_CHANGE_DEBOUNCE_MILLIS = 100;
    private static final long FILE_CHANGE_DEBOUNCE_NANOS =
        TimeUnit.MILLISECONDS.toNanos(FILE_CHANGE_DEBOUNCE_MILLIS);
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path directory;
    private final WatchService watchService;
    private final Consumer<Path> fileChangedHandler;
    private final Thread watcherThread;
    private final Set<Path> pendingChangedFiles = new LinkedHashSet<>();
    private long lastChangeNanos;
    private volatile boolean closed;

    private ModsDirectoryWatcher(Path directory, Consumer<Path> fileChangedHandler) throws IOException {
        this.directory = directory;
        this.fileChangedHandler = fileChangedHandler;
        this.watchService = directory.getFileSystem().newWatchService();

        try {
            directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY
            );
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

    static ModsDirectoryWatcher start(Path directory, Consumer<Path> fileChangedHandler) throws IOException {
        Files.createDirectories(directory);

        var watcher = new ModsDirectoryWatcher(directory, fileChangedHandler);
        watcher.watcherThread.start();
        return watcher;
    }

    private void watch() {
        try {
            while (!closed) {
                var key = watchService.poll(FILE_CHANGE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
                if (key != null) {
                    processEvents(key);
                    if (!key.reset()) {
                        LOGGER.warn("Stopped watching the mods directory because its watch key is no longer valid: {}", directory);
                        return;
                    }
                }
                notifySettledFiles();
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
            var changedFile = changedFile(event);
            if (changedFile != null) {
                pendingChangedFiles.add(changedFile);
                lastChangeNanos = System.nanoTime();
            }
        }
    }

    private void notifySettledFiles() {
        if (pendingChangedFiles.isEmpty()
            || System.nanoTime() - lastChangeNanos < FILE_CHANGE_DEBOUNCE_NANOS) {
            return;
        }

        for (var changedFile : pendingChangedFiles) {
            try {
                fileChangedHandler.accept(changedFile);
            } catch (RuntimeException exception) {
                LOGGER.warn("Unable to process a changed file in the mods directory: {}", changedFile, exception);
            }
        }
        pendingChangedFiles.clear();
    }

    private Path changedFile(WatchEvent<?> event) {
        if (!isSupportedEvent(event) || !(event.context() instanceof Path relativePath)) {
            return null;
        }

        var changedPath = directory.resolve(relativePath);
        if (!Files.isRegularFile(changedPath)) {
            return null;
        }

        return changedPath;
    }

    private static boolean isSupportedEvent(WatchEvent<?> event) {
        return event.kind() == StandardWatchEventKinds.ENTRY_CREATE
            || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY;
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
