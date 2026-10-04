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

class ServerStartupCoordinatorTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void continuesWhenAllConfiguredServerModsArePresent() throws IOException {
        var mod = mod("server-1\\.jar", "Server Mod", false);
        validate(mod);
        Files.writeString(FMLPaths.MODSDIR.get().resolve("server-1.jar"), "mod");

        assertEquals(StartupDecision.CONTINUE, ServerStartupCoordinator.decide(configuration(mod)));
    }

    @Test
    void exitsWhenARequiredServerModIsMissing() {
        var mod = mod("server-1\\.jar", "Server Mod", false);
        validate(mod);

        assertEquals(StartupDecision.EXIT, ServerStartupCoordinator.decide(configuration(mod)));
    }

    @Test
    void continuesWhenOnlyOptionalServerModsAreMissing() {
        var mod = mod("optional-server-1\\.jar", "Optional Server Mod", true);
        validate(mod);

        assertEquals(StartupDecision.CONTINUE, ServerStartupCoordinator.decide(configuration(mod)));
    }

    @Test
    void continuesWhenARequiredServerModHasAChecksumMismatch() throws IOException {
        var modFile = FMLPaths.MODSDIR.get().resolve("server-1.jar");
        Files.writeString(modFile, "installed content");
        var mod = new DownloadableModConfiguration(
            "Server Mod",
            "https://modrinth.com/server-mod.jar",
            "server-1\\.jar",
            "not-the-installed-file-hash",
            false
        );
        validate(mod);

        assertEquals(StartupDecision.CONTINUE, ServerStartupCoordinator.decide(configuration(mod)));
    }

    private static DownloadableModConfiguration mod(String filePattern, String name, boolean optional) {
        return new DownloadableModConfiguration(
            "https://modrinth.com/" + name.toLowerCase().replace(' ', '-') + ".jar",
            filePattern,
            name,
            optional
        );
    }

    private static ConfigurationJsonRoot configuration(DownloadableModConfiguration mod) {
        return new ConfigurationJsonRoot(List.of(), List.of(mod));
    }

    private static void validate(DownloadableModConfiguration mod) {
        var issues = ConfigurationValidator.validate(configuration(mod));
        assertTrue(issues.isEmpty(), issues::toString);
    }
}
