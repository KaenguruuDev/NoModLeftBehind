package dev.kaenguruu.nomodleftbehind.configuration;

import dev.kaenguruu.nomodleftbehind.configuration.model.DisabledOptionalDownloadsConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void createsAndLoadsEmptyDefaultConfiguration() throws IOException {
        var configuration = ConfigurationLoader.tryLoadConfiguration();

        assertNotNull(configuration);
        assertNotNull(configuration.clientMods());
        assertNotNull(configuration.serverMods());
        assertTrue(configuration.clientMods().isEmpty());
        assertTrue(configuration.serverMods().isEmpty());

        var configFile = gameDirectory.resolve("config/nomodsleftbehind/nomodleftbehind.json");
        assertTrue(Files.isRegularFile(configFile));
        assertTrue(Files.readString(configFile).contains("clientMods"));
    }

    @Test
    void loadsConfiguredModsFromJson() throws IOException {
        var configFile = gameDirectory.resolve("config/nomodsleftbehind/nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, """
            {
              "clientMods": [
                {
                  "url": "https://example.com/client.jar",
                  "filePattern": "client-.*\\\\.jar",
                  "name": "Client Mod",
                  "isOptional": true
                }
              ],
              "serverMods": []
            }
            """);

        var configuration = ConfigurationLoader.tryLoadConfiguration();

        assertNotNull(configuration);
        assertEquals(1, configuration.clientMods().size());
        var clientMod = configuration.clientMods().getFirst();
        assertEquals("https://example.com/client.jar", clientMod.url());
        assertEquals("client-.*\\.jar", clientMod.filePattern());
        assertEquals("Client Mod", clientMod.name());
        assertTrue(clientMod.isOptional());
        assertTrue(configuration.serverMods().isEmpty());
    }

    @Test
    void returnsNullForMalformedConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config/nomodsleftbehind/nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "{ not valid json");

        assertNull(ConfigurationLoader.tryLoadConfiguration());
    }

    @Test
    void returnsNullForNullConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config/nomodsleftbehind/nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "null");

        assertNull(ConfigurationLoader.tryLoadConfiguration());
    }

    @Test
    void returnsNullForEmptyConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config/nomodsleftbehind/nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "");

        assertNull(ConfigurationLoader.tryLoadConfiguration());
    }

    @Test
    void createsAndRoundTripsDisabledOptionalDownloadsConfiguration() {
        var defaults = ConfigurationLoader.tryLoadDisabledOptionalDownloadsConfiguration();

        assertNotNull(defaults);
        assertFalse(defaults.neverAskForOptionals());
        assertNotNull(defaults.skipForOptionalModUrl());
        assertTrue(defaults.skipForOptionalModUrl().isEmpty());

        var saved = new DisabledOptionalDownloadsConfiguration(
            true,
            Map.of("https://example.com/optional.jar", true)
        );
        assertTrue(ConfigurationLoader.trySaveDisabledOptionalDownloadsConfiguration(saved));

        assertEquals(saved, ConfigurationLoader.tryLoadDisabledOptionalDownloadsConfiguration());
    }

    @Test
    void returnsNullForMalformedDisabledOptionalDownloadsJson() throws IOException {
        var configFile = gameDirectory.resolve("config/nomodsleftbehind/disabled_optional_downloads.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "[]");

        assertNull(ConfigurationLoader.tryLoadDisabledOptionalDownloadsConfiguration());
    }
}
