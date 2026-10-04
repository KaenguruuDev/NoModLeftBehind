package dev.kaenguruu.nomodleftbehind.startup;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.ModResolutionResult;
import dev.kaenguruu.nomodleftbehind.ModsResolver;
import dev.kaenguruu.nomodleftbehind.client.StartupWindow;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationManager;
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
            var disabledOptionalDownloads = ConfigurationManager.tryLoadDisabledOptionalDownloadsConfiguration();
            if (disabledOptionalDownloads == null) {
                LOGGER.error("Unable to load disabled optional downloads configuration. Aborting");
                return StartupDecision.EXIT;
            }

            var disabledOptionalDownloadsState = new DisabledOptionalDownloadsState(disabledOptionalDownloads);

            var unresolvedMods = ModsResolver.resolveMods(configuration.clientMods()).stream()
                .filter(result -> result.status() != ModResolutionResult.ModResolutionStatus.PRESENT)
                .filter(result -> !shouldHide(result, disabledOptionalDownloads))
                .toList();

            return displayOrContinue(unresolvedMods, disabledOptionalDownloadsState);
        } catch (Exception exception) {
            LOGGER.error("Unable to resolve configured client mods due to an unhandled exception: {}", exception, exception);
            return StartupDecision.EXIT;
        }
    }

    private static boolean shouldHide(
        ModResolutionResult result,
        DisabledOptionalDownloadsConfiguration disabledOptionalDownloads
    ) {
        var mod = result.mod();
        if (result.status() == ModResolutionResult.ModResolutionStatus.HASH_MISMATCH) {
            var skippedChecksumMismatchMods = disabledOptionalDownloads.skipForChecksumMismatchModUrl();
            return skippedChecksumMismatchMods != null
                && Boolean.TRUE.equals(skippedChecksumMismatchMods.get(mod.url()));
        }

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
        List<ModResolutionResult> unresolvedMods,
        DisabledOptionalDownloadsState disabledOptionalDownloadsState
    ) {
        if (unresolvedMods.isEmpty()) {
            return StartupDecision.CONTINUE;
        }

        var window = new StartupWindow();
        var decision = new CompletableFuture<StartupDecision>();
        window.show(
            unresolvedMods,
            decision::complete,
            disabledOptionalDownloadsState::skipOptionalMod,
            disabledOptionalDownloadsState::skipChecksumMismatchMod,
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

        private boolean skipOptionalMod(DownloadableModConfiguration mod) {
            Map<String, Boolean> skippedOptionalModUrls = new HashMap<>();
            if (configuration.skipForOptionalModUrl() != null) {
                skippedOptionalModUrls.putAll(configuration.skipForOptionalModUrl());
            }
            skippedOptionalModUrls.put(mod.url(), true);

            return save(new DisabledOptionalDownloadsConfiguration(
                configuration.neverAskForOptionals(),
                skippedOptionalModUrls,
                configuration.skipForChecksumMismatchModUrl()
            ));
        }

        private boolean skipChecksumMismatchMod(DownloadableModConfiguration mod) {
            Map<String, Boolean> skippedChecksumMismatchModUrls = new HashMap<>();
            if (configuration.skipForChecksumMismatchModUrl() != null) {
                skippedChecksumMismatchModUrls.putAll(configuration.skipForChecksumMismatchModUrl());
            }
            skippedChecksumMismatchModUrls.put(mod.url(), true);

            return save(new DisabledOptionalDownloadsConfiguration(
                configuration.neverAskForOptionals(),
                configuration.skipForOptionalModUrl(),
                skippedChecksumMismatchModUrls
            ));
        }

        private boolean neverAskForOptionalsEnabled() {
            return configuration.neverAskForOptionals();
        }

        private boolean setNeverAskForOptionals(boolean neverAskForOptionals) {
            return save(new DisabledOptionalDownloadsConfiguration(
                neverAskForOptionals,
                configuration.skipForOptionalModUrl(),
                configuration.skipForChecksumMismatchModUrl()
            ));
        }

        private boolean save(DisabledOptionalDownloadsConfiguration updatedConfiguration) {
            if (ConfigurationManager.trySaveDisabledOptionalDownloadsConfiguration(updatedConfiguration)) {
                configuration = updatedConfiguration;
                return true;
            }

            return false;
        }
    }
}
