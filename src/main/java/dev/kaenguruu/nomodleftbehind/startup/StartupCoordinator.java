package dev.kaenguruu.nomodleftbehind.startup;

import dev.kaenguruu.nomodleftbehind.client.StartupWindow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class StartupCoordinator {
    private final StartupWindow window;

    public StartupCoordinator() {
        this(new StartupWindow());
    }

    StartupCoordinator(StartupWindow window) {
        this.window = window;
    }

    public StartupDecision awaitDecision() {
        CompletableFuture<StartupDecision> decision = new CompletableFuture<>();
        window.show(decision::complete);

        try {
            return decision.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return StartupDecision.EXIT;
        } catch (ExecutionException exception) {
            return StartupDecision.EXIT;
        }
    }
}
