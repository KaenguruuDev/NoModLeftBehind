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

import static org.junit.jupiter.api.Assertions.*;

class ModsResolverTest {
    @TempDir
    Path gameDirectory;

    @BeforeEach
    void setUpGameDirectory() {
        FMLPaths.loadAbsolutePaths(gameDirectory);
    }

    @Test
    void returnsNoMissingModsForAnEmptyConfiguration() throws IOException {
        assertTrue(ModsResolver.resolveMods(List.of()).isEmpty());
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

        var resolvedModsResult = ModsResolver.resolveMods(List.of(present, missing));
        assertEquals(2, resolvedModsResult.size());
        assertEquals(ModResolutionResult.ModResolutionStatus.PRESENT, resolvedModsResult.getFirst().status());
        assertNull(resolvedModsResult.getFirst().installedHash());
        assertEquals(ModResolutionResult.ModResolutionStatus.MISSING, resolvedModsResult.getLast().status());
    }

    @Test
    void returnsPresentWhenTheConfiguredHashMatches() throws IOException {
        var mod = new DownloadableModConfiguration(
            "Hash Mod",
            "https://modrinth.com/hash-mod.jar",
            "hash-1\\.jar",
            "e55cffc81a5ad8cfe85239d944a3ae9513645a9eed79bc884f51b80b2760fc46",
            false
        );
        validatePatterns(mod);

        var modFile = FMLPaths.MODSDIR.get().resolve("hash-1.jar");
        Files.writeString(modFile, "mod");

        var result = ModsResolver.resolveMods(List.of(mod)).getFirst();

        assertEquals(ModResolutionResult.ModResolutionStatus.PRESENT, result.status());
        assertEquals(
            "e55cffc81a5ad8cfe85239d944a3ae9513645a9eed79bc884f51b80b2760fc46",
            result.installedHash()
        );
    }

    @Test
    void returnsAllModsWhenTheModsDirectoryHasNoFiles() throws IOException {
        var required = mod("required-.*\\.jar", "Required Mod");
        var optional = new DownloadableModConfiguration(
            "Optional Mod",
            "https://modrinth.com/optional.jar",
            "optional-.*\\.jar",
            null,
            true
        );
        validatePatterns(required, optional);

        var resolvedModsResult = ModsResolver.resolveMods(List.of(required, optional));
        assertEquals(2, resolvedModsResult.size());
        assertEquals(ModResolutionResult.ModResolutionStatus.MISSING, resolvedModsResult.getFirst().status());
        assertEquals(ModResolutionResult.ModResolutionStatus.MISSING, resolvedModsResult.getLast().status());
    }

    @Test
    void findsModsMatchingANewlyCreatedFileName() {
        var matching = mod("matching-[0-9]+\\.jar", "Matching Mod");
        var notMatching = mod("other-[0-9]+\\.jar", "Other Mod");
        validatePatterns(matching, notMatching);

        assertEquals(
            List.of(matching),
            ModsResolver.findModsMatchingFileName(
                List.of(matching, notMatching),
                "matching-1.jar"
            )
        );
    }

    @Test
    void includesTheActualHashWhenAConfiguredHashDoesNotMatch() throws IOException {
        var mod = new DownloadableModConfiguration(
            "Hash Mod",
            "https://modrinth.com/hash-mod.jar",
            "hash-1\\.jar",
            "not-the-installed-file-hash",
            false
        );
        validatePatterns(mod);

        var modFile = FMLPaths.MODSDIR.get().resolve("hash-1.jar");
        Files.writeString(modFile, "mod");

        var result = ModsResolver.resolveMods(List.of(mod)).getFirst();

        assertEquals(ModResolutionResult.ModResolutionStatus.HASH_MISMATCH, result.status());
        assertEquals(
            "e55cffc81a5ad8cfe85239d944a3ae9513645a9eed79bc884f51b80b2760fc46",
            result.installedHash()
        );
    }

    private static DownloadableModConfiguration mod(String filePattern, String name) {
        return new DownloadableModConfiguration(
            name,
            "https://modrinth.com/" + name.toLowerCase().replace(' ', '-') + ".jar",
            filePattern,
            null,
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
