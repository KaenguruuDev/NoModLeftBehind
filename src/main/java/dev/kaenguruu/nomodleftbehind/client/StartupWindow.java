package dev.kaenguruu.nomodleftbehind.client;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.HashUtil;
import dev.kaenguruu.nomodleftbehind.ModResolutionResult;
import dev.kaenguruu.nomodleftbehind.ModsResolver;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class StartupWindow {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void show(
        List<ModResolutionResult> unresolvedMods,
        Consumer<StartupDecision> decisionHandler,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        boolean neverAskForOptionals,
        UnaryOperator<Boolean> optionalPreferenceHandler
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.error("Cannot open the startup window because AWT is running without a display.");
            decisionHandler.accept(StartupDecision.EXIT);
            return;
        }

        SwingUtilities.invokeLater(() -> createWindow(
            unresolvedMods,
            decisionHandler,
            dontShowOptionalAgainHandler,
            dontShowChecksumMismatchAgainHandler,
            neverAskForOptionals,
            optionalPreferenceHandler
        ));
    }

    private void createWindow(
        List<ModResolutionResult> unresolvedMods,
        Consumer<StartupDecision> decisionHandler,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        boolean neverAskForOptionals,
        UnaryOperator<Boolean> optionalPreferenceHandler
    ) {
        try {
            StartupWindowStyle.installLookAndFeel();

            var sortedMissingMods = sortMissingMods(unresolvedMods);
            var requiredCount = countRequiredMods(sortedMissingMods);
            var optionalCount = countOptionalMods(sortedMissingMods);
            var checksumMismatchCount = countChecksumMismatches(sortedMissingMods);
            var hasRequiredMods = hasRequiredMissingMods(sortedMissingMods);
            var windowParts = createWindowParts(
                sortedMissingMods,
                requiredCount,
                optionalCount,
                checksumMismatchCount,
                dontShowOptionalAgainHandler,
                dontShowChecksumMismatchAgainHandler,
                neverAskForOptionals,
                optionalPreferenceHandler
            );

            var window = createWindow(decisionHandler, windowParts, hasRequiredMods);
            windowParts.closeButton().addActionListener(event -> window.dispose());
            installDragSupport(window, windowParts.titlebar());
        } catch (HeadlessException | AWTError exception) {
            LOGGER.error("Failed to open the startup window because AWT is unavailable.", exception);
            decisionHandler.accept(StartupDecision.EXIT);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to open the startup window because of an unexpected UI error.", exception);
            decisionHandler.accept(StartupDecision.EXIT);
        }
    }

    private static @NotNull JFrame createWindow(Consumer<StartupDecision> decisionHandler, WindowParts windowParts, boolean hasRequiredMods) {
        var window = new JFrame();
        window.setUndecorated(true);
        window.setResizable(false);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setContentPane(windowParts.root());
        window.setSize(new Dimension(736, 690));
        window.setLocationRelativeTo(null);
        var watcher = startModsDirectoryWatcher(windowParts.modListState());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (watcher != null) {
                    watcher.close();
                }
                decisionHandler.accept(hasRequiredMods ? StartupDecision.EXIT : StartupDecision.CONTINUE);
            }
        });

        try {
            window.setVisible(true);
        } catch (Exception exception) {
            if (watcher != null) {
                watcher.close();
            }
            throw exception;
        }

        return window;
    }

    private static ModsDirectoryWatcher startModsDirectoryWatcher(ModListState modListState) {
        try {
            return ModsDirectoryWatcher.start(
                FMLPaths.MODSDIR.get(),
                modListState::updateForCreatedFile
            );
        } catch (IOException exception) {
            LOGGER.warn("Unable to watch the mods directory for newly added files.", exception);
            return null;
        }
    }

    private WindowParts createWindowParts(
        List<ModResolutionResult> sortedMissingMods,
        int requiredCount,
        int optionalCount,
        int checksumMismatchCount,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        boolean neverAskForOptionals,
        UnaryOperator<Boolean> optionalPreferenceHandler
    ) {
        var contentParts = createContent(
            sortedMissingMods,
            requiredCount,
            optionalCount,
            checksumMismatchCount,
            dontShowOptionalAgainHandler,
            dontShowChecksumMismatchAgainHandler,
            neverAskForOptionals,
            optionalPreferenceHandler
        );
        var closeButton = StartupWindowStyle.createCloseButton();
        var titlebar = StartupWindowStyle.createTitlebar(closeButton);
        var root = StartupWindowStyle.createRoot(titlebar, contentParts.content());

        return new WindowParts(root, titlebar, closeButton, contentParts.modListState());
    }

    private ContentParts createContent(
        List<ModResolutionResult> sortedMissingMods,
        int requiredCount,
        int optionalCount,
        int checksumMismatchCount,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        boolean neverAskForOptionals,
        UnaryOperator<Boolean> optionalPreferenceHandler
    ) {
        var content = StartupWindowStyle.createContent();
        content.add(StartupWindowStyle.createHeading());
        content.add(StartupWindowStyle.createIntro());
        content.add(StartupWindowStyle.createSecurityWarning());
        var checksumWarning = StartupWindowStyle.createChecksumWarning(checksumMismatchCount);
        content.add(checksumWarning);

        var summary = StartupWindowStyle.createLabel(
            summaryText(requiredCount, optionalCount, checksumMismatchCount),
            StartupWindowStyle.SMALL_FONT,
            StartupWindowStyle.MUTED_TEXT
        );
        content.add(StartupWindowStyle.createSummarySlot(summary));

        var modListParts = createModListScrollPane(
            sortedMissingMods,
            summary,
            checksumWarning,
            dontShowOptionalAgainHandler,
            dontShowChecksumMismatchAgainHandler
        );
        content.add(modListParts.scrollPane());

        if (optionalCount > 0) {
            var optionalPreference = StartupWindowStyle.createOptionalPreference(
                neverAskForOptionals,
                optionalPreferenceHandler
            );
            content.add(StartupWindowStyle.createPreferenceSlot(optionalPreference));
        }
        content.add(createFooter(sortedMissingMods));

        return new ContentParts(content, modListParts.modListState());
    }

    private ModListParts createModListScrollPane(
        List<ModResolutionResult> sortedMissingMods,
        javax.swing.JLabel summary,
        JPanel checksumWarning,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler
    ) {
        var modList = StartupWindowStyle.createModList();
        var modListState = new ModListState(
            modList,
            summary,
            checksumWarning,
            sortedMissingMods
        );
        for (var resolution : sortedMissingMods) {
            var mod = resolution.mod();
            var modRow = createModRow(
                resolution,
                dontShowOptionalAgainHandler,
                dontShowChecksumMismatchAgainHandler,
                () -> modListState.remove(mod)
            );
            modListState.add(mod, modRow.row(), modRow.status());
        }

        modListState.refreshBorders();
        return new ModListParts(StartupWindowStyle.createModListScrollPane(modList), modListState);
    }

    private JPanel createFooter(
        List<ModResolutionResult> sortedMissingMods
    ) {
        var footer = StartupWindowStyle.createFooter();

        var openModsDirectory = StartupWindowStyle.createButton("Open mods folder", false);
        StartupWindowStyle.setFixedHeight(openModsDirectory, 34);
        openModsDirectory.addActionListener(event -> openModsDirectory());

        var downloadAll = StartupWindowStyle.createButton("Download all", true);
        StartupWindowStyle.setFixedHeight(downloadAll, 34);
        downloadAll.addActionListener(event -> sortedMissingMods.forEach(mod -> openUrl(mod.mod().url())));

        footer.add(openModsDirectory, "left, top");
        footer.add(downloadAll, "right, top");

        return footer;
    }

    private ModRow createModRow(
        ModResolutionResult resolution,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        Runnable removeRow
    ) {
        var mod = resolution.mod();
        var row = StartupWindowStyle.createModRow();
        var statusType = statusType(resolution);
        var status = StartupWindowStyle.createStatus(
            statusType,
            StartupWindowStyle.statusTooltip(statusType, fileName(resolution.file()))
        );
        row.add(
            StartupWindowStyle.createIdentity(mod.name(), displayHost(mod.url()), mod.url()),
            StartupWindowStyle.identityConstraints()
        );
        row.add(
            status,
            StartupWindowStyle.statusConstraints()
        );

        addDownloadButton(row, mod);
        addDismissAction(
            row,
            mod,
            statusType,
            dontShowOptionalAgainHandler,
            dontShowChecksumMismatchAgainHandler,
            removeRow
        );

        return new ModRow(row, status);
    }

    private void addDownloadButton(
        JPanel row,
        DownloadableModConfiguration mod
    ) {
        var download = StartupWindowStyle.createButton("Download", false);
        StartupWindowStyle.setFixedWidth(download, 88);
        StartupWindowStyle.setFixedHeight(download, 34);
        download.addActionListener(event -> openUrl(mod.url()));

        row.add(download, StartupWindowStyle.actionConstraints(2, 12));
    }

    private void addDismissAction(
        JPanel row,
        DownloadableModConfiguration mod,
        StartupWindowStyle.ModStatus statusType,
        Predicate<DownloadableModConfiguration> dontShowOptionalAgainHandler,
        Predicate<DownloadableModConfiguration> dontShowChecksumMismatchAgainHandler,
        Runnable removeRow
    ) {
        var action = switch (statusType) {
            case OPTIONAL -> createDontShowAgainButton(
                mod,
                dontShowOptionalAgainHandler,
                removeRow,
                "Don't show again"
            );
            case CHECKSUM_MISMATCH -> createDontShowAgainButton(
                mod,
                dontShowChecksumMismatchAgainHandler,
                removeRow,
                "Don't show again"
            );
            case REQUIRED, ADDED -> StartupWindowStyle.createEmptyActions();
        };
        row.add(action, StartupWindowStyle.actionConstraints(3, 16));
    }

    private static JButton createDontShowAgainButton(
        DownloadableModConfiguration mod,
        Predicate<DownloadableModConfiguration> dontShowAgainHandler,
        Runnable removeRow,
        String text
    ) {
        var dontShowAgain = StartupWindowStyle.createQuietButton(text);
        StartupWindowStyle.setFixedWidth(dontShowAgain, 122);
        StartupWindowStyle.setFixedHeight(dontShowAgain, 34);
        dontShowAgain.addActionListener(event -> {
            if (dontShowAgainHandler.test(mod)) {
                removeRow.run();
            } else {
                showPreferenceSaveFailure(dontShowAgain);
            }
        });
        return dontShowAgain;
    }

    private static void showPreferenceSaveFailure(Component parent) {
        JOptionPane.showMessageDialog(
            parent,
            "Unable to save this preference. Your change was not applied.",
            "Preference not saved",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static List<ModResolutionResult> sortMissingMods(
        List<ModResolutionResult> unresolvedMods
    ) {
        return unresolvedMods.stream()
            .sorted(
                Comparator.comparingInt((ModResolutionResult result) ->
                        StartupWindowStyle.statusSortOrder(statusType(result)))
                    .thenComparing(result -> result.mod().name(), String.CASE_INSENSITIVE_ORDER)
            )
            .toList();
    }

    private static int countRequiredMods(List<ModResolutionResult> resolutions) {
        return (int) resolutions.stream()
            .filter(result -> statusType(result) == StartupWindowStyle.ModStatus.REQUIRED)
            .count();
    }

    private static int countOptionalMods(List<ModResolutionResult> resolutions) {
        return (int) resolutions.stream()
            .filter(result -> statusType(result) == StartupWindowStyle.ModStatus.OPTIONAL)
            .count();
    }

    private static int countChecksumMismatches(List<ModResolutionResult> resolutions) {
        return (int) resolutions.stream()
            .filter(result -> statusType(result) == StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH)
            .count();
    }

    private static boolean hasRequiredMissingMods(List<ModResolutionResult> resolutions) {
        return countRequiredMods(resolutions) > 0;
    }

    private static String summaryText(int requiredCount, int optionalCount, int checksumMismatchCount) {
        return String.format(
            Locale.ROOT,
            "%d required · %d optional · %d checksum warning%s",
            requiredCount,
            optionalCount,
            checksumMismatchCount,
            checksumWarningSuffix(checksumMismatchCount)
        );
    }

    private static String checksumWarningSuffix(int checksumMismatchCount) {
        if (checksumMismatchCount == 1) {
            return "";
        }
        return "s";
    }

    private static StartupWindowStyle.ModStatus statusType(ModResolutionResult resolution) {
        return switch (resolution.status()) {
            case HASH_MISMATCH -> StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH;
            case MISSING -> missingStatus(resolution.mod());
            case PRESENT -> StartupWindowStyle.ModStatus.ADDED;
        };
    }

    private static StartupWindowStyle.ModStatus missingStatus(DownloadableModConfiguration mod) {
        if (mod.isOptional()) {
            return StartupWindowStyle.ModStatus.OPTIONAL;
        }
        return StartupWindowStyle.ModStatus.REQUIRED;
    }

    private static void installDragSupport(JFrame window, JPanel titlebar) {
        var dragOrigin = new Point();
        var dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                dragOrigin.setLocation(event.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                var screen = event.getLocationOnScreen();
                window.setLocation(screen.x - dragOrigin.x, screen.y - dragOrigin.y);
            }
        };

        titlebar.addMouseListener(dragHandler);
        titlebar.addMouseMotionListener(dragHandler);
    }

    private static String displayHost(String url) {
        try {
            var host = URI.create(url).getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (IllegalArgumentException ignored) {
            // The full URL remains available as the tooltip even for malformed input.
        }

        return url.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "")
            .split("[/;?#]", 2)[0];
    }

    private static String fileName(Path file) {
        return file == null ? null : file.getFileName().toString();
    }

    private static void openUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                LOGGER.error("Cannot open download URL because desktop integration is unavailable: {}", url);
                return;
            }

            var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                LOGGER.error("Cannot open download URL because browser integration is unavailable: {}", url);
                return;
            }

            desktop.browse(URI.create(url));
        } catch (IOException | IllegalArgumentException | HeadlessException exception) {
            LOGGER.error("Unable to open download URL in the default browser: {}", url, exception);
        }
    }

    private static void openModsDirectory() {
        var modsDirectory = FMLPaths.MODSDIR.get();

        try {
            if (!Desktop.isDesktopSupported()) {
                LOGGER.error("Cannot open the mods directory because desktop integration is unavailable: {}", modsDirectory);
                return;
            }

            var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                LOGGER.error("Cannot open the mods directory because file-manager integration is unavailable: {}", modsDirectory);
                return;
            }

            Files.createDirectories(modsDirectory);
            desktop.open(modsDirectory.toFile());
        } catch (IOException | IllegalArgumentException | HeadlessException exception) {
            LOGGER.error("Unable to open the mods directory: {}", modsDirectory, exception);
        }
    }

    private record WindowParts(
        JPanel root,
        JPanel titlebar,
        JButton closeButton,
        ModListState modListState
    ) {
    }

    private record ContentParts(JPanel content, ModListState modListState) {
    }

    private record ModListParts(JScrollPane scrollPane, ModListState modListState) {
    }

    private record ModRow(JPanel row, JPanel status) {
    }

    private static final class ModListState {
        private final JPanel modList;
        private final javax.swing.JLabel summary;
        private final JPanel checksumWarning;
        private final List<ModResolutionResult> trackedResolutions;
        private final Map<DownloadableModConfiguration, JPanel> rows = new HashMap<>();
        private final Map<DownloadableModConfiguration, JPanel> statusPanels = new HashMap<>();
        private final Map<DownloadableModConfiguration, StartupWindowStyle.ModStatus> statuses = new HashMap<>();

        private ModListState(
            JPanel modList,
            javax.swing.JLabel summary,
            JPanel checksumWarning,
            List<ModResolutionResult> trackedResolutions
        ) {
            this.modList = modList;
            this.summary = summary;
            this.checksumWarning = checksumWarning;
            this.trackedResolutions = List.copyOf(trackedResolutions);
        }

        private void add(DownloadableModConfiguration mod, JPanel row, JPanel statusPanel) {
            rows.put(mod, row);
            statusPanels.put(mod, statusPanel);
            statuses.put(mod, statusType(findResolution(mod)));
            modList.add(row);
            refreshSummary();
        }

        private void remove(DownloadableModConfiguration mod) {
            var row = rows.remove(mod);
            statusPanels.remove(mod);
            statuses.remove(mod);
            if (row != null) {
                modList.remove(row);
            }
            refreshSummary();
            refreshBorders();
            modList.revalidate();
            modList.repaint();
        }

        private void updateForCreatedFile(Path createdFile) {
            var fileName = createdFile.getFileName().toString();
            var trackedMods = trackedResolutions.stream()
                .map(ModResolutionResult::mod)
                .toList();
            var matchingMods = ModsResolver.findModsMatchingFileName(trackedMods, fileName);
            var matchingResolutions = trackedResolutions.stream()
                .filter(resolution -> matchingMods.contains(resolution.mod()))
                .toList();

            for (var resolution : matchingResolutions) {
                var status = statusForCreatedFile(resolution.mod(), createdFile);
                if (status != null) {
                    SwingUtilities.invokeLater(() -> updateStatus(
                        resolution.mod(),
                        status,
                        fileName
                    ));
                }
            }
        }

        private StartupWindowStyle.ModStatus statusForCreatedFile(
            DownloadableModConfiguration mod,
            Path createdFile
        ) {
            if (mod.fileHash() == null) {
                return StartupWindowStyle.ModStatus.ADDED;
            }

            try {
                var localHash = HashUtil.getHashForFileAfterSettles(createdFile);
                if (!mod.fileHash().equals(localHash)) {
                    return StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH;
                }
                return StartupWindowStyle.ModStatus.ADDED;
            } catch (IOException exception) {
                LOGGER.warn("Unable to validate the hash of a newly created mod file: {}", createdFile, exception);
                return null;
            }
        }

        private void updateStatus(
            DownloadableModConfiguration mod,
            StartupWindowStyle.ModStatus newStatus,
            String fileName
        ) {
            var currentStatus = statuses.get(mod);
            if (currentStatus != null && !StartupWindowStyle.canReplaceStatus(currentStatus, newStatus)) {
                return;
            }

            statuses.put(mod, newStatus);
            var statusPanel = statusPanels.get(mod);
            if (statusPanel != null) {
                StartupWindowStyle.setStatus(
                    statusPanel,
                    newStatus,
                    StartupWindowStyle.statusTooltip(newStatus, fileName)
                );
            }
            refreshSummary();
        }

        private ModResolutionResult findResolution(DownloadableModConfiguration mod) {
            return trackedResolutions.stream()
                .filter(resolution -> resolution.mod().equals(mod))
                .findFirst()
                .orElseThrow();
        }

        private void refreshSummary() {
            var requiredCount = countStatuses(StartupWindowStyle.ModStatus.REQUIRED);
            var optionalCount = countStatuses(StartupWindowStyle.ModStatus.OPTIONAL);
            var checksumMismatchCount = countStatuses(StartupWindowStyle.ModStatus.CHECKSUM_MISMATCH);
            summary.setText(summaryText(requiredCount, optionalCount, checksumMismatchCount));
            StartupWindowStyle.updateChecksumWarning(checksumWarning, checksumMismatchCount);
        }

        private int countStatuses(StartupWindowStyle.ModStatus status) {
            return (int) statuses.values().stream()
                .filter(status::equals)
                .count();
        }

        private void refreshBorders() {
            StartupWindowStyle.refreshRowBorders(modList);
        }
    }

}
