package dev.kaenguruu.nomodleftbehind;

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

class MissingModsResolverTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void returnsNoMissingModsForAnEmptyConfiguration() throws IOException {
        assertTrue(MissingModsResolver.detectMissingMods(List.of()).isEmpty());
    }

    @Test
    void ignoresDirectoriesAndDetectsModsByCompiledFilePattern() throws IOException {
        var present = mod("present-[0-9]+\\.jar", "Present Mod");
        var missing = mod("missing-[0-9]+\\.jar", "Missing Mod");
        validatePatterns(present, missing);

        var modsDirectory = FMLPaths.MODSDIR.get();
        Files.writeString(modsDirectory.resolve("present-1.jar"), "mod");
        Files.createDirectories(modsDirectory.resolve("missing-1.jar"));
        Files.writeString(modsDirectory.resolve("readme.txt"), "not a mod");

        assertEquals(List.of(missing), MissingModsResolver.detectMissingMods(List.of(present, missing)));
    }

    @Test
    void returnsAllModsWhenTheModsDirectoryHasNoFiles() throws IOException {
        var required = mod("required-.*\\.jar", "Required Mod");
        var optional = new DownloadableModConfiguration(
            "https://modrinth.com/optional.jar",
            "optional-.*\\.jar",
            "Optional Mod",
            true
        );
        validatePatterns(required, optional);

        assertEquals(
            List.of(required, optional),
            MissingModsResolver.detectMissingMods(List.of(required, optional))
        );
    }

    @Test
    void findsModsMatchingANewlyCreatedFileName() {
        var matching = mod("matching-[0-9]+\\.jar", "Matching Mod");
        var notMatching = mod("other-[0-9]+\\.jar", "Other Mod");
        validatePatterns(matching, notMatching);

        assertEquals(
            List.of(matching),
            MissingModsResolver.findModsMatchingFileName(
                List.of(matching, notMatching),
                "matching-1.jar"
            )
        );
    }

    private static DownloadableModConfiguration mod(String filePattern, String name) {
        return new DownloadableModConfiguration(
            "https://modrinth.com/" + name.toLowerCase().replace(' ', '-') + ".jar",
            filePattern,
            name,
            false
        );
    }

    private static void validatePatterns(DownloadableModConfiguration... mods) {
        var issues = ConfigurationValidator.validate(
            new ConfigurationJsonRoot(List.of(mods), List.of())
        );
        assertTrue(issues.isEmpty(), issues::toString);
    }
}
