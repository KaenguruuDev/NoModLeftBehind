package dev.kaenguruu.nomodleftbehind.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.AWTError;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.neoforged.fml.loading.FMLPaths;

public final class StartupWindow {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Color REQUIRED_COLOR = new Color(0xF87171);
    private static final Color OPTIONAL_COLOR = new Color(0xFBBF24);

    public void show(
        List<DownloadableModConfiguration> missingMods,
        Consumer<StartupDecision> decisionHandler,
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        Runnable dontAskAgainHandler
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
            dontAskAgainHandler
        ));
    }

    private void createWindow(
        List<DownloadableModConfiguration> missingMods,
        Consumer<StartupDecision> decisionHandler,
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        Runnable dontAskAgainHandler
    ) {
        try {
            FlatDarkLaf.setup();

            var sortedMissingMods = missingMods.stream()
                .sorted(
                    Comparator.comparing(DownloadableModConfiguration::isOptional)
                        .thenComparing(DownloadableModConfiguration::name, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();

            boolean hasRequiredMods = sortedMissingMods.stream()
                .anyMatch(DownloadableModConfiguration::isRequired);
            boolean hasOptionalMods = sortedMissingMods.stream()
                .anyMatch(DownloadableModConfiguration::isOptional);
            JPanel actionBar = createActionBar(
                sortedMissingMods,
                hasOptionalMods,
                dontAskAgainHandler,
                decisionHandler
            );

            JPanel modList = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]"));
            for (var mod : sortedMissingMods) {
                modList.add(createModRow(mod, dontShowAgainHandler, decisionHandler), "growx, wrap");
            }

            JPanel bottomPadding = new JPanel();
            bottomPadding.setOpaque(false);
            bottomPadding.setPreferredSize(new Dimension(1, actionBar.getPreferredSize().height + 8));
            modList.add(bottomPadding, "growx, wrap");

            JScrollPane modListScrollPane = new JScrollPane(modList);
            modListScrollPane.setBorder(null);
            modListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            modListScrollPane.getVerticalScrollBar().setUnitIncrement(24);

            JLayeredPane modListWithActions = new JLayeredPane() {
                @Override
                public void doLayout() {
                    modListScrollPane.setBounds(0, 0, getWidth(), getHeight());

                    Dimension actionBarSize = actionBar.getPreferredSize();
                    int actionBarHeight = Math.min(actionBarSize.height, getHeight());
                    actionBar.setBounds(0, getHeight() - actionBarHeight, getWidth(), actionBarHeight);
                }
            };
            modListWithActions.setPreferredSize(new Dimension(640, 360));
            modListWithActions.add(modListScrollPane, JLayeredPane.DEFAULT_LAYER);
            modListWithActions.add(actionBar, JLayeredPane.PALETTE_LAYER);

            JPanel content = new JPanel(new MigLayout("insets 16, fill", "[grow, fill]", "[grow, fill]"));
            content.add(modListWithActions, "grow, push");

            JFrame window = new JFrame("No Mod Left Behind - Missing Mods Detected");
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    decisionHandler.accept(hasRequiredMods ? StartupDecision.EXIT : StartupDecision.CONTINUE);
                }
            });
            window.setContentPane(content);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        } catch (HeadlessException | AWTError exception) {
            LOGGER.error("Failed to open the startup window because AWT is unavailable.", exception);
            decisionHandler.accept(StartupDecision.EXIT);
        }
    }

    private JPanel createActionBar(
        List<DownloadableModConfiguration> missingMods,
        boolean hasOptionalMods,
        Runnable dontAskAgainHandler,
        Consumer<StartupDecision> decisionHandler
    ) {
        String columns = hasOptionalMods ? "[grow][grow][grow]" : "[grow][grow]";
        JPanel actionBar = new JPanel(new MigLayout("insets 12 0 0 0, fillx", columns));

        JButton openModsDirectory = new JButton("Open Mods Directory");
        JButton dontAskAgain = new JButton("Don't ask again for optional mods");
        JButton downloadAll = new JButton("Download All");

        openModsDirectory.addActionListener(event -> openModsDirectory());
        downloadAll.addActionListener(event -> {
            missingMods.forEach(mod -> openUrl(mod.url()));
            decisionHandler.accept(StartupDecision.EXIT);
        });
        if (hasOptionalMods) {
            dontAskAgain.addActionListener(event -> dontAskAgainHandler.run());
        }

        List<JButton> actionButtons = hasOptionalMods
            ? List.of(openModsDirectory, dontAskAgain, downloadAll)
            : List.of(openModsDirectory, downloadAll);
        for (JButton button : actionButtons) {
            button.setFocusPainted(false);
            button.setMargin(new Insets(12, 20, 12, 20));
            button.putClientProperty(FlatClientProperties.STYLE, "arc: 6; focusWidth: 0; innerFocusWidth: 0; focusedBorderColor: #00000000; hoverBorderColor: #00000000; pressedBorderColor: #00000000;");
        }

        actionBar.add(openModsDirectory, "left");
        if (hasOptionalMods) {
            actionBar.add(dontAskAgain, "center");
        }
        actionBar.add(downloadAll, "right");

        return actionBar;
    }

    private JPanel createModRow(
        DownloadableModConfiguration mod,
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        Consumer<StartupDecision> decisionHandler
    ) {
        JPanel row = new JPanel(new MigLayout("insets 10 12, fillx, wrap 2, gap 8 0", "[grow, fill][pref]", "[]0[pref][]"));

        boolean optional = mod.isOptional();
        String requirement = optional ? "optional" : "required";
        Color requirementColor = optional ? OPTIONAL_COLOR : REQUIRED_COLOR;

        JPanel modIdentity = new JPanel(new MigLayout("insets 0, fillx", "[pref]8[grow]"));
        modIdentity.setOpaque(false);

        String requirementIconPath = optional ? "/assets/optional.svg" : "/assets/required.svg";
        var requirementIconResource = StartupWindow.class.getResource(requirementIconPath);
        if (requirementIconResource == null) {
            throw new IllegalStateException("Missing startup window icon resource: " + requirementIconPath);
        }

        FlatSVGIcon requirementIcon = new FlatSVGIcon(requirementIconResource).derive(20, 20);
        requirementIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> requirementColor));

        JLabel requirementIconLabel = new JLabel(requirementIcon);
        requirementIconLabel.getAccessibleContext().setAccessibleName(requirement);

        JLabel name = new JLabel(mod.name());
        name.setFont(name.getFont().deriveFont(18f));

        JLabel requirementLabel = new JLabel(requirement);
        requirementLabel.setForeground(requirementColor);

        JPanel nameAndRequirement = new JPanel(new MigLayout("insets 0, fillx", "[pref]8[pref]"));
        nameAndRequirement.setOpaque(false);
        nameAndRequirement.add(name);
        nameAndRequirement.add(requirementLabel, "right");

        JButton download = new JButton("Download");
        download.setFocusPainted(false);
        download.setMargin(new Insets(8, 18, 8, 18));
        download.putClientProperty(FlatClientProperties.STYLE, "arc: 6; focusWidth: 0; innerFocusWidth: 0; focusedBorderColor: #00000000; hoverBorderColor: #00000000; pressedBorderColor: #00000000;");
        download.addActionListener(event -> {
            openUrl(mod.url());
            decisionHandler.accept(StartupDecision.EXIT);
        });

        JPanel modActions = new JPanel(new MigLayout("insets 0, gap 8", "[pref][pref]"));
        modActions.setOpaque(false);
        modActions.add(download);
        if (optional) {
            JButton dontShowAgain = new JButton("Don't show again");
            dontShowAgain.setFocusPainted(false);
            dontShowAgain.setMargin(new Insets(8, 12, 8, 12));
            dontShowAgain.putClientProperty(FlatClientProperties.STYLE, "arc: 6; focusWidth: 0; innerFocusWidth: 0; focusedBorderColor: #00000000; hoverBorderColor: #00000000; pressedBorderColor: #00000000;");
            dontShowAgain.addActionListener(event -> dontShowAgainHandler.accept(mod));
            modActions.add(dontShowAgain);
        }

        JLabel url = new JLabel(mod.url());
        url.setFont(url.getFont().deriveFont(12f));
        url.setToolTipText(mod.url());

        JSeparator separator = new JSeparator();

        modIdentity.add(requirementIconLabel, "center");
        modIdentity.add(nameAndRequirement, "growx");

        row.add(modIdentity, "growx");
        row.add(modActions, "right, wrap");
        row.add(url, "span, growx, wrap");
        row.add(separator, "span, growx, gaptop 8");

        return row;
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
}
