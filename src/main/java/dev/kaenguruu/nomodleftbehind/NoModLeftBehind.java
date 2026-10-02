package dev.kaenguruu.nomodleftbehind;

import dev.kaenguruu.nomodleftbehind.startup.StartupCoordinator;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(NoModLeftBehind.MOD_ID)
public final class NoModLeftBehind {
    public static final String MOD_ID = "nomodleftbehind";
    private static final int STARTUP_ABORT_EXIT_CODE = 1;

    public NoModLeftBehind(Dist dist) {
        if (dist == Dist.CLIENT) {
            StartupDecision decision = new StartupCoordinator().awaitDecision();

            if (decision == StartupDecision.EXIT) {
                System.exit(STARTUP_ABORT_EXIT_CODE);
            }
        }
    }
}
