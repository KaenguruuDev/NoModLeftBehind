package dev.kaenguruu.nomodleftbehind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {
    private static final long FILE_STABILITY_CHECK_INTERVAL_MILLIS = 100L;
    private static final int FILE_STABILITY_ATTEMPTS = 50;

    private HashUtil() {
    }

    public static String getHashForFile(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;

                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available in this environment.", exception);
        }
    }

    public static String getHashForFileAfterSettles(Path file) throws IOException {
        var previousSnapshot = FileSnapshot.read(file);

        for (var attempt = 0; attempt < FILE_STABILITY_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(FILE_STABILITY_CHECK_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for file to settle: " + file, exception);
            }

            var currentSnapshot = FileSnapshot.read(file);
            if (!currentSnapshot.equals(previousSnapshot)) {
                previousSnapshot = currentSnapshot;
                continue;
            }

            var hash = getHashForFile(file);
            if (currentSnapshot.equals(FileSnapshot.read(file))) {
                return hash;
            }

            previousSnapshot = FileSnapshot.read(file);
        }

        throw new IOException("File did not settle before the timeout: " + file);
    }

    private record FileSnapshot(long size, FileTime lastModifiedTime) {
        private static FileSnapshot read(Path file) throws IOException {
            return new FileSnapshot(Files.size(file), Files.getLastModifiedTime(file));
        }
    }
}
