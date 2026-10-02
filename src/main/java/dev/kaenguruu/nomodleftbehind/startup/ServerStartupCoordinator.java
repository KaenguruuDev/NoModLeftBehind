package dev.kaenguruu.nomodleftbehind.startup;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.MissingModsResolver;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import org.slf4j.Logger;

import java.util.List;

public final class ServerStartupCoordinator {
    private ServerStartupCoordinator() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static StartupDecision decide(ConfigurationJsonRoot configuration) {
        try {
            var missingMods = MissingModsResolver.detectMissingMods(configuration.serverMods());
            return logMissingModsOrContinue(missingMods);
        } catch (Exception exception) {
            LOGGER.error("Unable to detect missing mods due to unhandled exception: {}", exception, exception);
            return StartupDecision.EXIT;
        }
    }

    private static StartupDecision logMissingModsOrContinue(List<DownloadableModConfiguration> missingMods) {
        if (missingMods.isEmpty()) {
            return StartupDecision.CONTINUE;
        }

        var missingRequiredMod = false;
        for (var missingMod : missingMods) {
            if (missingMod.isRequired()) {
                LOGGER.error("Missing required server mod: '{}'. Please download from '{}'", missingMod.name(), missingMod.url());
                missingRequiredMod = true;
            } else {
                LOGGER.warn("Missing optional server mod: '{}'. Please download from '{}'", missingMod.name(), missingMod.url());
            }
        }

        return missingRequiredMod ? StartupDecision.EXIT : StartupDecision.CONTINUE;
    }
}
