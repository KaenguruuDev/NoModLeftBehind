package dev.kaenguruu.nomodleftbehind.startup;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.MissingModsResolver;
import dev.kaenguruu.nomodleftbehind.client.StartupWindow;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationLoader;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DisabledOptionalDownloadsConfiguration;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class ClientStartupCoordinator {
    private ClientStartupCoordinator() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static StartupDecision decide(ConfigurationJsonRoot configuration) {
        try {
            var disabledOptionalDownloads = ConfigurationLoader.tryLoadDisabledOptionalDownloadsConfiguration();
            if (disabledOptionalDownloads == null) {
                LOGGER.error("Unable to load disabled optional downloads configuration. Aborting");
                return StartupDecision.EXIT;
            }

            var disabledOptionalDownloadsState = new DisabledOptionalDownloadsState(disabledOptionalDownloads);
            var missingMods = MissingModsResolver.detectMissingMods(configuration.clientMods()).stream()
                .filter(mod -> !shouldHide(mod, disabledOptionalDownloads))
                .toList();
            return displayOrContinue(missingMods, disabledOptionalDownloadsState);
        } catch (Exception exception) {
            LOGGER.error("Unable to detect missing mods due to unhandled exception: {}", exception, exception);
            return StartupDecision.EXIT;
        }
    }

    private static boolean shouldHide(
        DownloadableModConfiguration mod,
        DisabledOptionalDownloadsConfiguration disabledOptionalDownloads
    ) {
        if (mod.isRequired()) {
            return false;
        }

        if (disabledOptionalDownloads.neverAskForOptionals()) {
            return true;
        }

        var skippedOptionalModUrls = disabledOptionalDownloads.skipForOptionalModUrl();
        return skippedOptionalModUrls != null
            && Boolean.TRUE.equals(skippedOptionalModUrls.get(mod.url()));
    }

    private static StartupDecision displayOrContinue(
        List<DownloadableModConfiguration> missingMods,
        DisabledOptionalDownloadsState disabledOptionalDownloadsState
    ) {
        if (missingMods.isEmpty()) {
            return StartupDecision.CONTINUE;
        }

        var window = new StartupWindow();
        var decision = new CompletableFuture<StartupDecision>();
        window.show(
            missingMods,
            decision::complete,
            disabledOptionalDownloadsState::skipOptionalMod,
            disabledOptionalDownloadsState.neverAskForOptionalsEnabled(),
            disabledOptionalDownloadsState::setNeverAskForOptionals
        );

        try {
            return decision.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error(exception.getMessage(), exception);
            return StartupDecision.EXIT;
        } catch (ExecutionException exception) {
            LOGGER.error(exception.getMessage(), exception);
            return StartupDecision.EXIT;
        }
    }

    private static final class DisabledOptionalDownloadsState {
        private DisabledOptionalDownloadsConfiguration configuration;

        private DisabledOptionalDownloadsState(DisabledOptionalDownloadsConfiguration configuration) {
            this.configuration = configuration;
        }

        private void skipOptionalMod(DownloadableModConfiguration mod) {
            Map<String, Boolean> skippedOptionalModUrls = new HashMap<>();
            if (configuration.skipForOptionalModUrl() != null) {
                skippedOptionalModUrls.putAll(configuration.skipForOptionalModUrl());
            }
            skippedOptionalModUrls.put(mod.url(), true);

            save(new DisabledOptionalDownloadsConfiguration(
                configuration.neverAskForOptionals(),
                skippedOptionalModUrls
            ));
        }

        private boolean neverAskForOptionalsEnabled() {
            return configuration.neverAskForOptionals();
        }

        private void setNeverAskForOptionals(boolean neverAskForOptionals) {
            save(new DisabledOptionalDownloadsConfiguration(
                neverAskForOptionals,
                configuration.skipForOptionalModUrl()
            ));
        }

        private void save(DisabledOptionalDownloadsConfiguration updatedConfiguration) {
            if (ConfigurationLoader.trySaveDisabledOptionalDownloadsConfiguration(updatedConfiguration)) {
                configuration = updatedConfiguration;
            }
        }
    }
}
