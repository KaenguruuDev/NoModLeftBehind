package dev.kaenguruu.nomodleftbehind;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;

/**
 * NeoForge discovers this service before it scans and validates the regular mod list.
 * The locator itself is intentionally empty; constructing it is the early entrypoint.
 */
public final class NoModLeftBehindEarlyService implements IModFileCandidateLocator {
    public NoModLeftBehindEarlyService() {
        NoModLeftBehind.start(FMLLoader.getDist());
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        // This service performs startup checks; it does not contribute a mod file.
    }
}
