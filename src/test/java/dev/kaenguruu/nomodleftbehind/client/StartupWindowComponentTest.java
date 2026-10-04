package dev.kaenguruu.nomodleftbehind.client;

import dev.kaenguruu.nomodleftbehind.HashUtil;
import dev.kaenguruu.nomodleftbehind.ModResolutionResult;
import dev.kaenguruu.nomodleftbehind.configuration.ConfigurationValidator;
import dev.kaenguruu.nomodleftbehind.configuration.model.ConfigurationJsonRoot;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.*;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("swing-ui")
class StartupWindowComponentTest {
    @TempDir
    Path temporaryDirectory;

    private LookAndFeel previousLookAndFeel;
    private UIDefaults previousUiDefaults;

    @BeforeEach
    void installLookAndFeel() throws Exception {
        onEdt(() -> {
            previousLookAndFeel = UIManager.getLookAndFeel();
            previousUiDefaults = (UIDefaults) UIManager.getDefaults().clone();
            StartupWindowStyle.installLookAndFeel();
        });
    }

    @AfterEach
    void restoreLookAndFeel() throws Exception {
        if (previousLookAndFeel == null) {
            return;
        }

        onEdt(() -> {
            try {
                UIManager.setLookAndFeel(previousLookAndFeel);
                var currentUiDefaults = UIManager.getDefaults();
                currentUiDefaults.clear();
                currentUiDefaults.putAll(previousUiDefaults);
            } catch (UnsupportedLookAndFeelException exception) {
                throw new AssertionError("Unable to restore the previous look-and-feel", exception);
            }
        });
    }

    @Test
    void optionalPreferenceReportsSuccessfulChanges() throws Exception {
        var requestedValues = new ArrayList<Boolean>();
        var preference = onEdt(() -> StartupWindowStyle.createOptionalPreference(false, requestedValue -> {
            requestedValues.add(requestedValue);
            return true;
        }));

        onEdt((Runnable) preference::doClick);
        onEdt((Runnable) preference::doClick);

        assertEquals(List.of(true, false), requestedValues);
        assertFalse(onEdt(preference::isSelected));
    }

    @Test
    void optionalPreferenceRollsBackWhenChangeCannotBeSaved() throws Exception {
        var requestedValues = new ArrayList<Boolean>();
        var failureParents = new ArrayList<Component>();
        var preference = onEdt(() -> StartupWindowStyle.createOptionalPreference(true, requestedValue -> {
            requestedValues.add(requestedValue);
            return false;
        }, failureParents::add));

        onEdt((Runnable) preference::doClick);

        assertEquals(List.of(false), requestedValues);
        assertTrue(onEdt(preference::isSelected));
        assertEquals(List.of(preference), failureParents);
    }

    @Test
    void optionalPreferenceUsesNeverAskInitialState() throws Exception {
        var optional = resolution(
            "Optional Mod",
            "optional\\.jar",
            true,
            null,
            ModResolutionResult.ModResolutionStatus.MISSING
        );
        var parts = onEdt(() -> createWindowParts(
            List.of(optional),
            ignored -> true,
            ignored -> true,
            true
        ));

        var preference = optionalPreference(parts);
        assertTrue(onEdt(preference::isSelected));
    }

    @Test
    void windowOmitsOptionalPreferenceWhenNoOptionalModsRemain() throws Exception {
        var required = resolution(
            "Required Mod",
            "required\\.jar",
            false,
            null,
            ModResolutionResult.ModResolutionStatus.MISSING
        );
        var parts = onEdt(() -> createWindowParts(
            List.of(required),
            ignored -> true,
            ignored -> true
        ));

        onEdt(() -> {
            var content = content(parts);
            assertEquals(7, content.getComponentCount());
            var footer = (JPanel) content.getComponent(6);
            assertEquals("Open mods folder", ((JButton) footer.getComponent(0)).getText());
        });
    }

    @Test
    void checksumDismissalRemovesRowWhenPreferenceSaves() throws Exception {
        var mismatch = resolution(
            "Checksum Mismatch",
            "mismatch\\.jar",
            false,
            "trusted-hash",
            ModResolutionResult.ModResolutionStatus.HASH_MISMATCH
        );
        var dismissed = new ArrayList<DownloadableModConfiguration>();
        var parts = onEdt(() -> createWindowParts(
            List.of(mismatch),
            ignored -> true,
            mod -> {
                dismissed.add(mod);
                return true;
            }
        ));

        onEdt(() -> ((JButton) ((JPanel) modList(parts).getComponent(0)).getComponent(3)).doClick());

        assertEquals(List.of(mismatch.mod()), dismissed);
        assertEquals(0, onEdt(() -> modList(parts).getComponentCount()));
        assertEquals("0 required · 0 optional · 0 checksum warnings", summary(parts));
        assertFalse(onEdt(() -> checksumWarning(parts).isVisible()));
    }

    @Test
    void checksumDismissalReportsSaveFailureWithoutRemovingRow() throws Exception {
        var mismatch = mod(
            "Checksum Mismatch",
            "mismatch\\.jar",
            "trusted-hash",
            false
        );
        var failureParents = new ArrayList<Component>();
        var button = onEdt(() -> StartupWindow.createDontShowAgainButton(
            mismatch,
            ignored -> false,
            () -> fail("The row must remain when the preference cannot be saved."),
            "Don't show again",
            failureParents::add
        ));

        onEdt((Runnable) button::doClick);

        assertEquals(List.of(button), failureParents);
    }

    @Test
    void checksumWarningIsHiddenWithoutMismatchesAndShowsCorrectCopy() throws Exception {
        var warning = onEdt(() -> StartupWindowStyle.createChecksumWarning(0));

        assertFalse(onEdt(warning::isVisible));

        onEdt(() -> StartupWindowStyle.updateChecksumWarning(warning, 1));

        assertTrue(onEdt(warning::isVisible));
        assertEquals("One of the files does not match the trusted version.", warningMessage(warning, 1));
        assertEquals("It may have been tampered with or replaced. Review it before continuing.", warningMessage(warning, 2));

        onEdt(() -> StartupWindowStyle.updateChecksumWarning(warning, 2));

        assertEquals("2 of the files do not match their trusted versions.", warningMessage(warning, 1));
        assertEquals("They may have been tampered with or replaced. Review them before continuing.", warningMessage(warning, 2));

        onEdt(() -> StartupWindowStyle.updateChecksumWarning(warning, 0));
        assertFalse(onEdt(warning::isVisible));
    }

    @Test
    void statusCanTransitionFromOptionalToAdded() throws Exception {
        var status = onEdt(() -> StartupWindowStyle.createStatus(StartupWindowStyle.ModStatus.OPTIONAL, "Optional mod is missing"));

        assertStatus(status, "Optional", "Optional", "Optional mod is missing");

        onEdt(() -> StartupWindowStyle.setStatus(status, StartupWindowStyle.ModStatus.ADDED, "Found example-mod.jar in your mods folder."));

        assertStatus(status, "Added", "Added", "Found example-mod.jar in your mods folder.");
    }

    @Test
    void identityDisplaysSourceAndKeepsFullUrlAsTooltip() throws Exception {
        var identity = onEdt(() -> StartupWindowStyle.createIdentity("Example Mod", "modrinth.com", "https://modrinth.com/mod/example"));

        var name = (JLabel) identity.getComponent(0);
        var source = (JLabel) identity.getComponent(1);

        onEdt(() -> {
            assertEquals("Example Mod", name.getText());
            assertEquals("modrinth.com", source.getText());
            assertEquals("https://modrinth.com/mod/example", source.getToolTipText());
        });
    }

    @Test
    void windowContentSortsRowsAndUpdatesSummaryWhenOptionalIsDismissed() throws Exception {
        var required = resolution("Zulu Required", "required\\.jar", false, null, ModResolutionResult.ModResolutionStatus.MISSING);
        var mismatch = resolution("Beta Mismatch", "mismatch\\.jar", false, "trusted-hash", ModResolutionResult.ModResolutionStatus.HASH_MISMATCH);
        var optional = resolution("Alpha Optional", "optional\\.jar", true, null, ModResolutionResult.ModResolutionStatus.MISSING);
        var dismissed = new ArrayList<DownloadableModConfiguration>();

        var parts = onEdt(() -> createWindowParts(List.of(optional, required, mismatch), mod -> {
            dismissed.add(mod);
            return true;
        }, ignored -> true));

        assertWindowContent(parts, List.of("Zulu Required", "Beta Mismatch", "Alpha Optional"), "1 required · 1 optional · 1 checksum warning");

        onEdt(() -> ((JButton) ((JPanel) modList(parts).getComponent(2)).getComponent(3)).doClick());

        assertEquals(List.of(optional.mod()), dismissed);
        assertWindowContent(parts, List.of("Zulu Required", "Beta Mismatch"), "1 required · 0 optional · 1 checksum warning");
    }

    @Test
    void windowStateUpdatesWhenAConfiguredFileAppearsWithMatchingHash() throws Exception {
        var createdFile = temporaryDirectory.resolve("optional.jar");
        Files.writeString(createdFile, "mod");
        var optional = mod(
            "Optional Mod",
            "optional\\.jar",
            HashUtil.getHashForFile(createdFile),
            true
        );
        validate(optional);
        var parts = missingModWindow(optional);

        parts.modListState().updateForCreatedFile(createdFile);
        onEdt(() -> {
            assertEquals("Added", statusLabel((JPanel) modList(parts).getComponent(0)).getText());
            assertEquals(
                "Found optional.jar in your mods folder.",
                statusLabel((JPanel) modList(parts).getComponent(0)).getToolTipText()
            );
        });
        assertEquals("0 required · 0 optional · 0 checksum warnings", summary(parts));
        assertFalse(onEdt(() -> checksumWarning(parts).isVisible()));
    }

    @Test
    void windowStateShowsChecksumWarningWhenAConfiguredFileHasWrongHash() throws Exception {
        var createdFile = temporaryDirectory.resolve("optional.jar");
        Files.writeString(createdFile, "mod");
        var optional = mod("Optional Mod", "optional\\.jar", "trusted-hash", true);
        validate(optional);
        var parts = missingModWindow(optional);

        parts.modListState().updateForCreatedFile(createdFile);
        onEdt(() -> {
            assertEquals("Mismatch", statusLabel((JPanel) modList(parts).getComponent(0)).getText());
            assertEquals(
                "Checksum mismatch",
                statusLabel((JPanel) modList(parts).getComponent(0))
                    .getAccessibleContext()
                    .getAccessibleName()
            );
        });
        assertEquals("0 required · 0 optional · 1 checksum warning", summary(parts));
        assertTrue(onEdt(() -> checksumWarning(parts).isVisible()));
    }

    @Test
    void windowStateRemainsUnchangedWhenConfiguredFileCannotBeHashed() throws Exception {
        var optional = mod("Optional Mod", "optional\\.jar", "trusted-hash", true);
        validate(optional);
        var parts = missingModWindow(optional);

        parts.modListState().updateForCreatedFile(temporaryDirectory.resolve("optional.jar"));
        onEdt(() -> {
            assertEquals("Optional", statusLabel((JPanel) modList(parts).getComponent(0)).getText());
            assertEquals(
                "This optional mod is missing. Download it if you want to use it, or choose “Don't show again” to hide it.",
                statusLabel((JPanel) modList(parts).getComponent(0)).getToolTipText()
            );
        });
        assertEquals("0 required · 1 optional · 0 checksum warnings", summary(parts));
        assertFalse(onEdt(() -> checksumWarning(parts).isVisible()));
    }

    @Test
    void closeDecisionDependsOnWhetherRequiredModsRemain() {
        assertEquals(StartupDecision.EXIT, StartupWindow.closeDecision(true));
        assertEquals(StartupDecision.CONTINUE, StartupWindow.closeDecision(false));
    }

    @Test
    void headlessShowExitsWithoutTryingToCreateAWindow() {
        var decisions = new ArrayList<StartupDecision>();

        new StartupWindow().show(List.of(), decisions::add, ignored -> true, ignored -> true, false, ignored -> true);

        assertEquals(List.of(StartupDecision.EXIT), decisions);
    }

    private static String warningMessage(JPanel warningSlot, int index) throws Exception {
        return onEdt(() -> {
            var warning = (JPanel) warningSlot.getComponent(0);
            var message = (JPanel) warning.getComponent(1);
            return ((JLabel) message.getComponent(index)).getText();
        });
    }

    private static StartupWindow.WindowParts createWindowParts(
        List<ModResolutionResult> resolutions,
        Predicate<DownloadableModConfiguration> optionalHandler,
        Predicate<DownloadableModConfiguration> checksumHandler
    ) {
        return createWindowParts(resolutions, optionalHandler, checksumHandler, false);
    }

    private static StartupWindow.WindowParts createWindowParts(
        List<ModResolutionResult> resolutions,
        Predicate<DownloadableModConfiguration> optionalHandler,
        Predicate<DownloadableModConfiguration> checksumHandler,
        boolean neverAskForOptionals
    ) {
        var sortedResolutions = StartupWindow.sortMissingMods(resolutions);
        return new StartupWindow().createWindowParts(
            sortedResolutions,
            StartupWindow.countRequiredMods(sortedResolutions),
            StartupWindow.countOptionalMods(sortedResolutions),
            StartupWindow.countChecksumMismatches(sortedResolutions),
            optionalHandler,
            checksumHandler,
            neverAskForOptionals,
            ignored -> true
        );
    }

    private static StartupWindow.WindowParts missingModWindow(DownloadableModConfiguration mod) throws Exception {
        return onEdt(() -> createWindowParts(
            List.of(new ModResolutionResult(mod, ModResolutionResult.ModResolutionStatus.MISSING, null, null)),
            ignored -> true,
            ignored -> true
        ));
    }

    private static void validate(DownloadableModConfiguration mod) {
        var validationIssues = ConfigurationValidator.validate(
            new ConfigurationJsonRoot(List.of(mod), List.of())
        );
        assertTrue(validationIssues.isEmpty(), validationIssues::toString);
    }

    private static JPanel content(StartupWindow.WindowParts parts) {
        return (JPanel) parts.root().getComponent(1);
    }

    private static JCheckBox optionalPreference(StartupWindow.WindowParts parts) {
        return (JCheckBox) ((JPanel) content(parts).getComponent(6)).getComponent(0);
    }

    private static JLabel summaryLabel(StartupWindow.WindowParts parts) {
        var summarySlot = (JPanel) content(parts).getComponent(4);
        return (JLabel) summarySlot.getComponent(0);
    }

    private static String summary(StartupWindow.WindowParts parts) {
        return onEdtUnchecked(() -> summaryLabel(parts).getText());
    }

    private static JPanel checksumWarning(StartupWindow.WindowParts parts) {
        return (JPanel) content(parts).getComponent(3);
    }

    private static void assertWindowContent(StartupWindow.WindowParts parts, List<String> expectedNames, String expectedSummary) throws Exception {
        onEdt(() -> {
            var content = content(parts);
            var summarySlot = (JPanel) content.getComponent(4);
            assertEquals(expectedSummary, ((JLabel) summarySlot.getComponent(0)).getText());
            assertEquals(8, content.getComponentCount());

            var rows = modList(parts);
            assertEquals(expectedNames.size(), rows.getComponentCount());
            for (int index = 0; index < expectedNames.size(); index++) {
                var row = (JPanel) rows.getComponent(index);
                var identity = (JPanel) row.getComponent(0);
                assertEquals(expectedNames.get(index), ((JLabel) identity.getComponent(0)).getText());
                assertEquals("Download", ((JButton) row.getComponent(2)).getText());
            }

            var footer = (JPanel) content.getComponent(7);
            assertEquals("Open mods folder", ((JButton) footer.getComponent(0)).getText());
            assertEquals("Download all", ((JButton) footer.getComponent(1)).getText());
            return null;
        });
    }

    private static JPanel modList(StartupWindow.WindowParts parts) {
        var content = (JPanel) parts.root().getComponent(1);
        var scrollPane = (JScrollPane) content.getComponent(5);
        return (JPanel) scrollPane.getViewport().getView();
    }

    private static JLabel statusLabel(JPanel row) {
        var status = (JPanel) ((JPanel) row).getComponent(1);
        return (JLabel) status.getComponent(0);
    }

    private static ModResolutionResult resolution(
        String name,
        String filePattern,
        boolean optional,
        String hash,
        ModResolutionResult.ModResolutionStatus status
    ) {
        return new ModResolutionResult(mod(name, filePattern, hash, optional), status, Path.of(name + ".jar"), null);
    }

    private static DownloadableModConfiguration mod(String name, String filePattern, String hash, boolean optional) {
        return new DownloadableModConfiguration(name, "https://modrinth.com/mod/" + name.toLowerCase().replace(' ', '-'), filePattern, hash, optional);
    }

    private static void assertStatus(JPanel status, String expectedText, String expectedAccessibleName, String expectedTooltip) throws Exception {
        onEdt(() -> {
            var label = (JLabel) status.getComponent(0);
            assertEquals(expectedText, label.getText());
            assertEquals(expectedAccessibleName, label.getAccessibleContext().getAccessibleName());
            assertEquals(expectedTooltip, label.getToolTipText());
        });
    }

    private static <T> T onEdtUnchecked(Supplier<T> action) {
        try {
            return onEdt(action);
        } catch (Exception exception) {
            throw new AssertionError("Swing operation failed", exception);
        }
    }

    private static <T> T onEdt(Supplier<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }

        var result = new AtomicReference<T>();
        var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.get());
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });

        if (failure.get() != null) {
            throw new AssertionError("Swing operation failed", failure.get());
        }
        return result.get();
    }

    private static void onEdt(Runnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }
}
