package dev.kaenguruu.nomodleftbehind.client;

import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.MissingModsResolver;
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
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StartupWindow {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void show(
        List<DownloadableModConfiguration> missingMods,
        Consumer<StartupDecision> decisionHandler,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Function<Boolean, Boolean> optionalPreferenceHandler
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.error("Cannot open the startup window because AWT is running without a display.");
            decisionHandler.accept(StartupDecision.EXIT);
            return;
        }

        SwingUtilities.invokeLater(() -> createWindow(
            missingMods,
            decisionHandler,
            dontShowAgainHandler,
            neverAskForOptionals,
            optionalPreferenceHandler
        ));
    }

    private void createWindow(
        List<DownloadableModConfiguration> missingMods,
        Consumer<StartupDecision> decisionHandler,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Function<Boolean, Boolean> optionalPreferenceHandler
    ) {
        try {
            StartupWindowStyle.installLookAndFeel();

            var sortedMissingMods = sortMissingMods(missingMods);
            var requiredCount = countRequiredMods(sortedMissingMods);
            var optionalCount = sortedMissingMods.size() - requiredCount;
            var hasRequiredMods = requiredCount > 0;
            var windowParts = createWindowParts(
                sortedMissingMods,
                requiredCount,
                optionalCount,
                dontShowAgainHandler,
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
        window.setSize(new Dimension(736, 584));
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
        } catch (RuntimeException | Error exception) {
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
                path -> modListState.markAddedForFile(path.getFileName().toString())
            );
        } catch (IOException exception) {
            LOGGER.warn("Unable to watch the mods directory for newly added files.", exception);
            return null;
        }
    }

    private WindowParts createWindowParts(
        List<DownloadableModConfiguration> sortedMissingMods,
        int requiredCount,
        int optionalCount,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Function<Boolean, Boolean> optionalPreferenceHandler
    ) {
        var contentParts = createContent(
            sortedMissingMods,
            requiredCount,
            optionalCount,
            dontShowAgainHandler,
            neverAskForOptionals,
            optionalPreferenceHandler
        );
        var closeButton = StartupWindowStyle.createCloseButton();
        var titlebar = StartupWindowStyle.createTitlebar(closeButton);
        var root = StartupWindowStyle.createRoot(titlebar, contentParts.content());

        return new WindowParts(root, titlebar, closeButton, contentParts.modListState());
    }

    private ContentParts createContent(
        List<DownloadableModConfiguration> sortedMissingMods,
        int requiredCount,
        int optionalCount,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Function<Boolean, Boolean> optionalPreferenceHandler
    ) {
        var content = StartupWindowStyle.createContent();
        content.add(StartupWindowStyle.createHeading());
        content.add(StartupWindowStyle.createIntro());
        content.add(StartupWindowStyle.createSecurityWarning());

        var summary = StartupWindowStyle.createLabel(
            summaryText(requiredCount, optionalCount),
            StartupWindowStyle.SMALL_FONT,
            StartupWindowStyle.MUTED_TEXT
        );
        content.add(StartupWindowStyle.createSummarySlot(summary));

        var modListParts = createModListScrollPane(
            sortedMissingMods,
            requiredCount,
            optionalCount,
            summary,
            dontShowAgainHandler
        );
        content.add(modListParts.scrollPane());

        var optionalPreference = StartupWindowStyle.createOptionalPreference(
            neverAskForOptionals,
            optionalPreferenceHandler
        );
        content.add(StartupWindowStyle.createPreferenceSlot(optionalPreference));
        content.add(createFooter(sortedMissingMods));

        return new ContentParts(content, modListParts.modListState());
    }

    private ModListParts createModListScrollPane(
        List<DownloadableModConfiguration> sortedMissingMods,
        int requiredCount,
        int optionalCount,
        javax.swing.JLabel summary,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler
    ) {
        var modList = StartupWindowStyle.createModList();
        var modListState = new ModListState(
            modList,
            summary,
            requiredCount,
            optionalCount,
            sortedMissingMods
        );
        for (var mod : sortedMissingMods) {
            var modRow = createModRow(mod, dontShowAgainHandler, () -> modListState.remove(mod));
            modListState.add(mod, modRow.row(), modRow.status());
        }

        modListState.refreshBorders();
        return new ModListParts(StartupWindowStyle.createModListScrollPane(modList), modListState);
    }

    private JPanel createFooter(
        List<DownloadableModConfiguration> sortedMissingMods
    ) {
        var footer = StartupWindowStyle.createFooter();

        var openModsDirectory = StartupWindowStyle.createButton("Open mods folder", false);
        StartupWindowStyle.setFixedHeight(openModsDirectory, 34);
        openModsDirectory.addActionListener(event -> openModsDirectory());

        var downloadAll = StartupWindowStyle.createButton("Download all", true);
        StartupWindowStyle.setFixedHeight(downloadAll, 34);
        downloadAll.addActionListener(event -> sortedMissingMods.forEach(mod -> openUrl(mod.url())));

        footer.add(openModsDirectory, "left, top");
        footer.add(downloadAll, "right, top");

        return footer;
    }

    private ModRow createModRow(
        DownloadableModConfiguration mod,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        Runnable removeRow
    ) {
        var row = StartupWindowStyle.createModRow();
        var optional = mod.isOptional();
        var status = StartupWindowStyle.createRequirementStatus(optional);
        row.add(
            StartupWindowStyle.createIdentity(mod.name(), displayHost(mod.url()), mod.url()),
            StartupWindowStyle.identityConstraints()
        );
        row.add(
            status,
            StartupWindowStyle.statusConstraints()
        );

        addDownloadButton(row, mod);
        addOptionalAction(row, mod, optional, dontShowAgainHandler, removeRow);

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

    private void addOptionalAction(
        JPanel row,
        DownloadableModConfiguration mod,
        boolean optional,
        Function<DownloadableModConfiguration, Boolean> dontShowAgainHandler,
        Runnable removeRow
    ) {
        if (!optional) {
            row.add(StartupWindowStyle.createEmptyActions(), StartupWindowStyle.actionConstraints(3, 16));
            return;
        }

        var dontShowAgain = StartupWindowStyle.createQuietButton("Don't show again");
        StartupWindowStyle.setFixedWidth(dontShowAgain, 122);
        StartupWindowStyle.setFixedHeight(dontShowAgain, 34);
        dontShowAgain.addActionListener(event -> {
            if (dontShowAgainHandler.apply(mod)) {
                removeRow.run();
            } else {
                showPreferenceSaveFailure(dontShowAgain);
            }
        });
        row.add(dontShowAgain, StartupWindowStyle.actionConstraints(3, 16));
    }

    private static void showPreferenceSaveFailure(Component parent) {
        JOptionPane.showMessageDialog(
            parent,
            "Unable to save this preference. Your change was not applied.",
            "Preference not saved",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static List<DownloadableModConfiguration> sortMissingMods(
        List<DownloadableModConfiguration> missingMods
    ) {
        return missingMods.stream()
            .sorted(
                Comparator.comparing(DownloadableModConfiguration::isOptional)
                    .thenComparing(DownloadableModConfiguration::name, String.CASE_INSENSITIVE_ORDER)
            )
            .toList();
    }

    private static int countRequiredMods(List<DownloadableModConfiguration> mods) {
        return (int) mods.stream()
            .filter(DownloadableModConfiguration::isRequired)
            .count();
    }

    private static String summaryText(int requiredCount, int optionalCount) {
        return String.format(Locale.ROOT, "%d required · %d optional", requiredCount, optionalCount);
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
        private final int requiredCount;
        private final List<DownloadableModConfiguration> trackedMods;
        private final Map<DownloadableModConfiguration, JPanel> rows = new HashMap<>();
        private final Map<DownloadableModConfiguration, JPanel> statusPanels = new HashMap<>();
        private final Set<DownloadableModConfiguration> addedMods = new HashSet<>();
        private int remainingOptionalCount;

        private ModListState(
            JPanel modList,
            javax.swing.JLabel summary,
            int requiredCount,
            int optionalCount,
            List<DownloadableModConfiguration> trackedMods
        ) {
            this.modList = modList;
            this.summary = summary;
            this.requiredCount = requiredCount;
            this.remainingOptionalCount = optionalCount;
            this.trackedMods = List.copyOf(trackedMods);
        }

        private void add(DownloadableModConfiguration mod, JPanel row, JPanel status) {
            rows.put(mod, row);
            statusPanels.put(mod, status);
            modList.add(row);
        }

        private void remove(DownloadableModConfiguration mod) {
            var row = rows.remove(mod);
            statusPanels.remove(mod);
            if (row != null) {
                modList.remove(row);
            }
            summary.setText(summaryText(requiredCount, --remainingOptionalCount));
            refreshBorders();
            modList.revalidate();
            modList.repaint();
        }

        private void markAddedForFile(String fileName) {
            var matchingMods = MissingModsResolver.findModsMatchingFileName(trackedMods, fileName);
            if (matchingMods.isEmpty()) {
                return;
            }

            SwingUtilities.invokeLater(() -> matchingMods.forEach(this::markAdded));
        }

        private void markAdded(DownloadableModConfiguration mod) {
            if (!addedMods.add(mod)) {
                return;
            }

            var status = statusPanels.get(mod);
            if (status != null) {
                StartupWindowStyle.markRequirementAdded(status);
            }
        }

        private void refreshBorders() {
            StartupWindowStyle.refreshRowBorders(modList);
        }
    }
}
