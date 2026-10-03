package dev.kaenguruu.nomodleftbehind;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationLoader;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationValidator;
import dev.kaenguruu.nomodleftbehind.startup.ClientStartupCoordinator;
import dev.kaenguruu.nomodleftbehind.startup.ServerStartupCoordinator;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.neoforged.api.distmarker.Dist;
import org.slf4j.Logger;

public final class NoModLeftBehind {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String MOD_ID = "nomodleftbehind";
    private static final int STARTUP_ABORT_EXIT_CODE = 1;

    private NoModLeftBehind() {
        /* Utility class */
    }

    public static void start(Dist dist) {
        if (dist == Dist.CLIENT) {
            // Configure AWT before any client UI class can initialize ImageIcon and cache headless mode.
            System.setProperty("java.awt.headless", "false");
        }

        var config = ConfigurationLoader.tryLoadConfiguration();
        if (config == null) {
            LOGGER.error("No configuration could be loaded. Aborting");
            abortStartup();
            return;
        }

        var validationIssues = ConfigurationValidator.validate(config);
        if (!validationIssues.isEmpty()) {
            for (var issue : validationIssues) {
                LOGGER.error(
                    "Configuration validation failed at {} ({}): {}",
                    issue.path(),
                    issue.error(),
                    issue.message()
                );
            }
            abortStartup();
            return;
        }

        StartupDecision decision;

        if (dist == Dist.CLIENT) {
            LOGGER.info("Detected NoModLeftBehind running on client.");
            decision = ClientStartupCoordinator.decide(config);
        } else {
            LOGGER.info("Detected NoModLeftBehind running on dedicated server. Skipping UI initialization.");
            decision = ServerStartupCoordinator.decide(config);
        }

        if (decision == StartupDecision.EXIT) {
            LOGGER.info("StartupDecision is EXIT. Aborting");
            abortStartup();
        }
    }

    private static void abortStartup() {
        System.exit(STARTUP_ABORT_EXIT_CODE);
    }
}
