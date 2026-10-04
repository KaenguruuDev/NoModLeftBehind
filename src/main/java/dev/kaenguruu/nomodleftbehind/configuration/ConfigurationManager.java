package dev.kaenguruu.nomodleftbehind.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.HashUtil;
import dev.kaenguruu.nomodleftbehind.ModResolutionResult;
import dev.kaenguruu.nomodleftbehind.ModsResolver;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DisabledOptionalDownloadsConfiguration;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ConfigurationManager {
    private ConfigurationManager() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CONFIGURATION_DIRECTORY = "nomodleftbehind";
    public static final String DISABLED_DOWNLOADS_CONFIGURATION_FILE = "disabled_downloads.json";

    public static ConfigurationJsonRoot tryLoadConfiguration() {
        var configPath = getConfigurationPath("nomodleftbehind.json");
        LOGGER.debug("Loading configuration from {}", configPath);

        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                LOGGER.debug("Configuration file does not exist. Creating empty default.");

                var defaultConfig = new ConfigurationJsonRoot(List.of(), List.of());
                Files.writeString(
                    configPath,
                    GSON.toJson(defaultConfig),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
                );
                return defaultConfig;
            }

            try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, ConfigurationJsonRoot.class);
            }
        } catch (JsonParseException exception) {
            LOGGER.error("Unable to parse configuration at {}", configPath, exception);
            return null;
        } catch (IOException exception) {
            LOGGER.error("Unable to read or create configuration at {}", configPath, exception);
            return null;
        }
    }

    public static DisabledOptionalDownloadsConfiguration tryLoadDisabledOptionalDownloadsConfiguration() {
        var configPath = getConfigurationPath(DISABLED_DOWNLOADS_CONFIGURATION_FILE);
        LOGGER.debug("Loading disabled optional downloads configuration from {}", configPath);

        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                LOGGER.debug("Disabled optional downloads configuration does not exist. Creating empty default.");

                var defaultConfig = new DisabledOptionalDownloadsConfiguration(false, Map.of());
                Files.writeString(
                    configPath,
                    GSON.toJson(defaultConfig),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
                );
                return defaultConfig;
            }

            try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, DisabledOptionalDownloadsConfiguration.class);
            }
        } catch (JsonParseException exception) {
            LOGGER.error("Unable to parse disabled optional downloads configuration at {}", configPath, exception);
            return null;
        } catch (IOException exception) {
            LOGGER.error("Unable to read or create disabled optional downloads configuration at {}", configPath, exception);
            return null;
        }
    }

    public static boolean trySaveDisabledOptionalDownloadsConfiguration(
        DisabledOptionalDownloadsConfiguration configuration
    ) {
        var configPath = getConfigurationPath(DISABLED_DOWNLOADS_CONFIGURATION_FILE);
        LOGGER.debug("Saving disabled optional downloads configuration to {}", configPath);

        try {
            writeJsonAtomically(configPath, configuration);
            return true;
        } catch (AtomicMoveNotSupportedException exception) {
            LOGGER.error("Unable to atomically replace disabled optional downloads configuration at {}", configPath, exception);
            return false;
        } catch (IOException exception) {
            LOGGER.error("Unable to save disabled optional downloads configuration at {}", configPath, exception);
            return false;
        }
    }

    public static void trySaveChecksumsForConfiguration(ConfigurationJsonRoot configuration) {
        try {
            var mods = Stream.of(configuration.clientMods(), configuration.serverMods())
                .flatMap(Collection::stream)
                .toList();
            var presentMods = ModsResolver.resolveMods(mods).stream()
                .filter(result -> result.status() == ModResolutionResult.ModResolutionStatus.PRESENT)
                .filter(result -> result.mod().fileHash() == null)
                .toList();

            if (presentMods.isEmpty()) {
                return;
            }

            var updatedMods = new IdentityHashMap<DownloadableModConfiguration, DownloadableModConfiguration>();
            for (var result : presentMods) {
                updatedMods.put(result.mod(), tryAddFileHash(result));
            }

            var updatedConfiguration = new ConfigurationJsonRoot(
                replaceUpdatedMods(configuration.clientMods(), updatedMods),
                replaceUpdatedMods(configuration.serverMods(), updatedMods),
                configuration.trustedDomains()
            );

            var configPath = getConfigurationPath("nomodleftbehind.json");
            writeJsonAtomically(configPath, updatedConfiguration);

        } catch (Exception exception) {
            LOGGER.error("An error occurred while trying to save checksums: {}", exception.getMessage(), exception);
        }
    }

    private static DownloadableModConfiguration tryAddFileHash(ModResolutionResult result) throws IOException {
        return result.mod().withFileHash(HashUtil.getHashForFileAfterSettles(result.file()));
    }

    private static void writeJsonAtomically(Path configPath, Object configuration) throws IOException {
        Path temporaryPath = null;
        try {
            Files.createDirectories(configPath.getParent());
            temporaryPath = Files.createTempFile(
                configPath.getParent(),
                configPath.getFileName().toString(),
                ".tmp"
            );
            Files.writeString(
                temporaryPath,
                GSON.toJson(configuration),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            Files.move(
                temporaryPath,
                configPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException exception) {
                    LOGGER.warn("Unable to clean up temporary configuration file {}", temporaryPath, exception);
                }
            }
        }
    }

    private static List<DownloadableModConfiguration> replaceUpdatedMods(
        List<DownloadableModConfiguration> mods,
        IdentityHashMap<DownloadableModConfiguration, DownloadableModConfiguration> updatedMods
    ) {
        return mods.stream()
            .map(mod -> updatedMods.getOrDefault(mod, mod))
            .toList();
    }

    private static Path getConfigurationPath(String fileName) {
        return FMLPaths.CONFIGDIR.get()
            .resolve(CONFIGURATION_DIRECTORY)
            .resolve(fileName);
    }

}
