package dev.kaenguruu.nomodleftbehind;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationValidator;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class MissingModsResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MissingModsResolver() {
        /* This utility class should not be instantiated */
    }

    public static List<DownloadableModConfiguration> detectMissingMods(List<DownloadableModConfiguration> requiredMods) throws IOException {
        if (requiredMods.isEmpty()) {
            return List.of();
        }

        var installedModFileNames = getInstalledModFileNames();
        if (installedModFileNames.isEmpty()) {
            return List.copyOf(requiredMods);
        }

        var cachedPatterns = ConfigurationValidator.getCachedRegexPatterns();
        var missingMods = new ArrayList<DownloadableModConfiguration>(requiredMods.size());
        Set<String> matchedFileNames = new HashSet<>();
        for (var mod : requiredMods) {
            if (!isModPresent(mod, installedModFileNames, cachedPatterns, matchedFileNames)) {
                missingMods.add(mod);
            }
        }

        return missingMods;
    }

    private static List<String> getInstalledModFileNames() throws IOException {
        try (var paths = Files.list(FMLPaths.MODSDIR.get())) {
            return paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
        }
    }

    private static boolean isModPresent(DownloadableModConfiguration mod, List<String> installedModFileNames, Map<String, Pattern> cachedPatterns, Set<String> matchedFileNames) {
        var pattern = cachedPatterns.get(mod.filePattern());
        for (var fileName : installedModFileNames) {
            if (pattern.matcher(fileName).matches()) {
                warnIfAlreadyMatched(mod, fileName, matchedFileNames);
                return true;
            }
        }

        return false;
    }

    private static void warnIfAlreadyMatched(DownloadableModConfiguration mod, String fileName, Set<String> matchedFileNames) {
        if (!matchedFileNames.add(fileName)) {
            LOGGER.warn("Configured mod pattern '{}' also matches already matched file '{}'.", mod.filePattern(), fileName);
        }
    }
}
