package dev.kaenguruu.nomodleftbehind.startup;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.ModResolutionResult;
import dev.kaenguruu.nomodleftbehind.ModsResolver;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import org.slf4j.Logger;

import java.util.List;

public final class ServerStartupCoordinator {
    private ServerStartupCoordinator() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static StartupDecision decide(ConfigurationJsonRoot configuration) {
        try {
            var unresolvedMods = ModsResolver.resolveMods(configuration.serverMods()).stream()
                .filter(result -> result.status() != ModResolutionResult.ModResolutionStatus.PRESENT)
                .toList();
            return logUnresolvedModsOrContinue(unresolvedMods);
        } catch (Exception exception) {
            LOGGER.error("Unable to resolve configured server mods due to an unhandled exception: {}", exception, exception);
            return StartupDecision.EXIT;
        }
    }

    private static StartupDecision logUnresolvedModsOrContinue(List<ModResolutionResult> unresolvedMods) {
        if (unresolvedMods.isEmpty()) {
            return StartupDecision.CONTINUE;
        }

        var hasMissingRequiredMod = false;
        for (var unresolvedMod : unresolvedMods) {
            var mod = unresolvedMod.mod();
            if (unresolvedMod.status() == ModResolutionResult.ModResolutionStatus.HASH_MISMATCH) {
                logChecksumMismatch(unresolvedMod);
            } else if (mod.isRequired()) {
                LOGGER.error("Missing required server mod: '{}'. Please download from '{}'", mod.name(), mod.url());
                hasMissingRequiredMod = true;
            } else {
                LOGGER.warn("Missing optional server mod: '{}'. Please download from '{}'", mod.name(), mod.url());
            }
        }

        return hasMissingRequiredMod ? StartupDecision.EXIT : StartupDecision.CONTINUE;
    }

    private static void logChecksumMismatch(ModResolutionResult resolution) {
        var mod = resolution.mod();
        LOGGER.warn(
            "Checksum mismatch for server mod: '{}'. Expected SHA-256 '{}', but found '{}'. Please download from '{}'",
            mod.name(),
            mod.fileHash(),
            resolution.installedHash(),
            mod.url()
        );
    }
}
