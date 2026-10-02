package dev.kaenguruu.nomodleftbehind.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DisabledOptionalDownloadsConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public final class ConfigurationLoader {
    private ConfigurationLoader() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIGURATION_DIRECTORY = "nomodsleftbehind";

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
        var configPath = getConfigurationPath("disabled_optional_downloads.json");
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
        var configPath = getConfigurationPath("disabled_optional_downloads.json");
        LOGGER.debug("Saving disabled optional downloads configuration to {}", configPath);

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(
                configPath,
                GSON.toJson(configuration),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return true;
        } catch (IOException exception) {
            LOGGER.error("Unable to save disabled optional downloads configuration at {}", configPath, exception);
            return false;
        }
    }

    private static Path getConfigurationPath(String fileName) {
        return FMLPaths.CONFIGDIR.get()
            .resolve(CONFIGURATION_DIRECTORY)
            .resolve(fileName);
    }
}
