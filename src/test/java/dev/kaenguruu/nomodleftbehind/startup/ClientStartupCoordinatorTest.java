package dev.kaenguruu.nomodleftbehind.startup;

import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationValidator;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStartupCoordinatorTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void continuesWhenNoClientModsAreMissing() {
        assertEquals(StartupDecision.CONTINUE, ClientStartupCoordinator.decide(new ConfigurationJsonRoot(List.of(), List.of())));
    }

    @Test
    void continuesWhenMissingOptionalModIsGloballySuppressed() throws IOException {
        var mod = mod("optional-client-1\\.jar", "Optional Client Mod");
        validate(mod);
        writeDisabledOptionalDownloadsConfiguration("""
            {
              "neverAskForOptionals": true,
              "skipForOptionalModUrl": {}
            }
            """);

        assertEquals(StartupDecision.CONTINUE, ClientStartupCoordinator.decide(configuration(mod)));
    }

    @Test
    void continuesWhenMissingOptionalModIsSuppressedByUrl() throws IOException {
        var mod = mod("optional-client-1\\.jar", "Optional Client Mod");
        validate(mod);
        writeDisabledOptionalDownloadsConfiguration("""
            {
              "neverAskForOptionals": false,
              "skipForOptionalModUrl": {
                "https://modrinth.com/optional-client-mod.jar": true
              }
            }
            """);

        assertEquals(StartupDecision.CONTINUE, ClientStartupCoordinator.decide(configuration(mod)));
    }

    @Test
    void exitsWhenDisabledOptionalDownloadsConfigurationIsMalformed() throws IOException {
        writeDisabledOptionalDownloadsConfiguration("[]");

        assertEquals(StartupDecision.EXIT, ClientStartupCoordinator.decide(new ConfigurationJsonRoot(List.of(), List.of())));
    }

    private static DownloadableModConfiguration mod(String filePattern, String name) {
        return new DownloadableModConfiguration("https://modrinth.com/" + name.toLowerCase().replace(' ', '-') + ".jar", filePattern, name, true);
    }

    private static ConfigurationJsonRoot configuration(DownloadableModConfiguration mod) {
        return new ConfigurationJsonRoot(List.of(mod), List.of());
    }

    private static void validate(DownloadableModConfiguration mod) {
        var issues = ConfigurationValidator.validate(configuration(mod));
        assertTrue(issues.isEmpty(), issues::toString);
    }

    private static void writeDisabledOptionalDownloadsConfiguration(String contents) throws IOException {
        var configFile = FMLPaths.CONFIGDIR.get().resolve("nomodsleftbehind").resolve("disabled_optional_downloads.json");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, contents);
    }
}
