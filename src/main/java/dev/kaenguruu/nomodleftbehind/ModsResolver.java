package dev.kaenguruu.nomodleftbehind;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationValidator;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class ModsResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ModsResolver() {
        /* This utility class should not be instantiated */
    }

    public static List<ModResolutionResult> resolveMods(List<DownloadableModConfiguration> mods) throws IOException {
        if (mods.isEmpty()) {
            return List.of();
        }

        var installedModFileNames = getInstalledModFileNames();
        if (installedModFileNames.isEmpty()) {
            return mods.stream()
                .map(mod -> new ModResolutionResult(mod, ModResolutionResult.ModResolutionStatus.MISSING, null, null))
                .toList();
        }

        var cachedPatterns = ConfigurationValidator.getCachedRegexPatterns();

        Set<String> matchedFileNames = new HashSet<>();
        var resolvedMods = new ArrayList<ModResolutionResult>(mods.size());

        for (var mod : mods) {
            String fileMatch = tryFindInstalledFileMatch(cachedPatterns.get(mod.filePattern()), installedModFileNames);
            if (fileMatch == null) {
                resolvedMods.add(new ModResolutionResult(mod, ModResolutionResult.ModResolutionStatus.MISSING, null, null));
                continue;
            }

            warnIfAlreadyMatched(mod, fileMatch, matchedFileNames);
            Path modFile = FMLPaths.MODSDIR.get().resolve(fileMatch);
            resolvedMods.add(resolveInstalledMod(mod, modFile));
        }

        return resolvedMods;
    }

    private static ModResolutionResult resolveInstalledMod(
        DownloadableModConfiguration mod,
        Path modFile
    ) throws IOException {
        if (mod.fileHash() == null) {
            return new ModResolutionResult(mod, ModResolutionResult.ModResolutionStatus.PRESENT, modFile, null);
        }

        var installedHash = HashUtil.getHashForFile(modFile);
        if (!mod.fileHash().equals(installedHash)) {
            return new ModResolutionResult(
                mod,
                ModResolutionResult.ModResolutionStatus.HASH_MISMATCH,
                modFile,
                installedHash
            );
        }

        return new ModResolutionResult(mod, ModResolutionResult.ModResolutionStatus.PRESENT, modFile, installedHash);
    }

    private static String tryFindInstalledFileMatch(Pattern pattern, List<String> installedFiles) {
        for (var fileName : installedFiles) {
            if (pattern.matcher(fileName).matches()) {
                return fileName;
            }
        }

        return null;
    }

    public static List<DownloadableModConfiguration> findModsMatchingFileName(
        List<DownloadableModConfiguration> mods,
        String fileName
    ) {
        var cachedPatterns = ConfigurationValidator.getCachedRegexPatterns();
        return mods.stream()
            .filter(mod -> matchesFileName(mod, fileName, cachedPatterns))
            .toList();
    }

    private static List<String> getInstalledModFileNames() throws IOException {
        try (var paths = Files.list(FMLPaths.MODSDIR.get())) {
            return paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
        }
    }

    private static boolean matchesFileName(
        DownloadableModConfiguration mod,
        String fileName,
        Map<String, Pattern> cachedPatterns
    ) {
        var pattern = cachedPatterns.get(mod.filePattern());
        return pattern != null && pattern.matcher(fileName).matches();
    }

    private static void warnIfAlreadyMatched(DownloadableModConfiguration mod, String fileName, Set<String> matchedFileNames) {
        if (!matchedFileNames.add(fileName)) {
            LOGGER.warn("Configured mod pattern '{}' of mod '{}' also matches already matched file '{}'.", mod.filePattern(), mod.name(), fileName);
        }
    }
}
