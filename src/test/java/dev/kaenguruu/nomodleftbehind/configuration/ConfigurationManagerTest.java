package dev.kaenguruu.nomodleftbehind.configuration;

import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DisabledOptionalDownloadsConfiguration;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationManagerTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void createsAndLoadsEmptyDefaultConfiguration() throws IOException {
        var configuration = ConfigurationManager.tryLoadConfiguration();

        assertNotNull(configuration);
        assertNotNull(configuration.clientMods());
        assertNotNull(configuration.serverMods());
        assertNotNull(configuration.trustedDomains());
        assertTrue(configuration.clientMods().isEmpty());
        assertTrue(configuration.serverMods().isEmpty());
        assertTrue(configuration.trustedDomains().isEmpty());

        var configFile = gameDirectory.resolve("config").resolve(ConfigurationManager.CONFIGURATION_DIRECTORY).resolve("nomodleftbehind.json");
        assertTrue(Files.isRegularFile(configFile));
        assertTrue(Files.readString(configFile).contains("clientMods"));
    }

    @Test
    void loadsConfiguredModsFromJson() throws IOException {
        var configFile = gameDirectory.resolve("config").resolve(ConfigurationManager.CONFIGURATION_DIRECTORY).resolve("nomodleftbehind.json");
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

        var configuration = ConfigurationManager.tryLoadConfiguration();

        assertNotNull(configuration);
        assertEquals(1, configuration.clientMods().size());
        var clientMod = configuration.clientMods().getFirst();
        assertEquals("https://example.com/client.jar", clientMod.url());
        assertEquals("client-.*\\.jar", clientMod.filePattern());
        assertEquals("Client Mod", clientMod.name());
        assertTrue(clientMod.isOptional());
        assertTrue(configuration.serverMods().isEmpty());
        assertTrue(configuration.trustedDomains().isEmpty());
    }

    @Test
    void returnsNullForMalformedConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config").resolve(ConfigurationManager.CONFIGURATION_DIRECTORY).resolve("nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "{ not valid json");

        assertNull(ConfigurationManager.tryLoadConfiguration());
    }

    @Test
    void returnsNullForNullConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config").resolve(ConfigurationManager.CONFIGURATION_DIRECTORY).resolve("nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "null");

        assertNull(ConfigurationManager.tryLoadConfiguration());
    }

    @Test
    void returnsNullForEmptyConfigurationJson() throws IOException {
        var configFile = gameDirectory.resolve("config").resolve(ConfigurationManager.CONFIGURATION_DIRECTORY).resolve("nomodleftbehind.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "");

        assertNull(ConfigurationManager.tryLoadConfiguration());
    }

    @Test
    void savesTheHashForPresentModsWithoutAConfiguredHash() throws IOException {
        var mod = new DownloadableModConfiguration(
            "Hash Mod",
            "https://modrinth.com/hash-mod.jar",
            "hash-1\\.jar",
            null,
            false
        );
        var configuration = new ConfigurationJsonRoot(List.of(mod), List.of());
        assertTrue(ConfigurationValidator.validate(configuration).isEmpty());
        Files.writeString(FMLPaths.MODSDIR.get().resolve("hash-1.jar"), "mod");

        ConfigurationManager.trySaveChecksumsForConfiguration(configuration);

        var savedConfiguration = ConfigurationManager.tryLoadConfiguration();
        assertNotNull(savedConfiguration);
        assertEquals(
            "e55cffc81a5ad8cfe85239d944a3ae9513645a9eed79bc884f51b80b2760fc46",
            savedConfiguration.clientMods().getFirst().fileHash()
        );
    }

    @Test
    void createsAndRoundTripsDisabledOptionalDownloadsConfiguration() {
        var defaults = ConfigurationManager.tryLoadDisabledOptionalDownloadsConfiguration();

        assertNotNull(defaults);
        assertFalse(defaults.neverAskForOptionals());
        assertNotNull(defaults.skipForOptionalModUrl());
        assertTrue(defaults.skipForOptionalModUrl().isEmpty());

        var saved = new DisabledOptionalDownloadsConfiguration(
            true,
            Map.of("https://example.com/optional.jar", true)
        );
        assertTrue(ConfigurationManager.trySaveDisabledOptionalDownloadsConfiguration(saved));

        assertEquals(saved, ConfigurationManager.tryLoadDisabledOptionalDownloadsConfiguration());
    }

    @Test
    void returnsNullForMalformedDisabledOptionalDownloadsJson() throws IOException {
        var configFile = gameDirectory.resolve("config")
            .resolve(ConfigurationManager.CONFIGURATION_DIRECTORY)
            .resolve(ConfigurationManager.DISABLED_DOWNLOADS_CONFIGURATION_FILE);
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "[]");

        assertNull(ConfigurationManager.tryLoadDisabledOptionalDownloadsConfiguration());
    }
}
