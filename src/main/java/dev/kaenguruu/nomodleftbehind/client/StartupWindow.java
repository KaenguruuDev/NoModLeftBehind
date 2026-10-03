package dev.kaenguruu.nomodleftbehind.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.configuration.model.DownloadableModConfiguration;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.miginfocom.swing.MigLayout;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.AWTError;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class StartupWindow {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Color WINDOW_BACKGROUND = color("#202328");
    private static final Color SURFACE = color("#272B31");
    private static final Color BORDER = color("#3A4048");
    private static final Color TEXT = color("#F1F3F5");
    private static final Color MUTED_TEXT = color("#A8B0BB");
    private static final Color TITLEBAR_TEXT = color("#BFC6D0");
    private static final Color REQUIRED_COLOR = color("#F08B82");
    private static final Color OPTIONAL_COLOR = color("#E3BC65");
    private static final Color BUTTON_BACKGROUND = color("#30353D");
    private static final Color BUTTON_BORDER = color("#454D59");
    private static final Color ACCENT = color("#6D9EEB");
    private static final Color ACCENT_TEXT = color("#14243D");
    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font SMALL_FONT = BASE_FONT.deriveFont(12f);
    private static final Font TITLE_FONT = BASE_FONT.deriveFont(24f);
    private static final Font BUTTON_FONT = BASE_FONT.deriveFont(12f);

    public void show(
        List<DownloadableModConfiguration> missingMods,
        Consumer<StartupDecision> decisionHandler,
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Consumer<Boolean> optionalPreferenceHandler
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
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        boolean neverAskForOptionals,
        Consumer<Boolean> optionalPreferenceHandler
    ) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("defaultFont", BASE_FONT);

            var sortedMissingMods = missingMods.stream()
                .sorted(
                    Comparator.comparing(DownloadableModConfiguration::isOptional)
                        .thenComparing(DownloadableModConfiguration::name, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();

            var requiredCount = (int) sortedMissingMods.stream()
                .filter(DownloadableModConfiguration::isRequired)
                .count();
            var optionalCount = sortedMissingMods.size() - requiredCount;
            var hasRequiredMods = requiredCount > 0;
            int[] remainingOptionalCount = {optionalCount};

            var summary = new JLabel(summaryText(requiredCount, optionalCount));
            summary.setFont(SMALL_FONT);
            summary.setForeground(MUTED_TEXT);

            var modList = new JPanel();
            modList.setLayout(new javax.swing.BoxLayout(modList, javax.swing.BoxLayout.Y_AXIS));
            modList.setBackground(SURFACE);

            var rows = new java.util.ArrayList<JPanel>();
            for (var mod : sortedMissingMods) {
                var row = createModRow(mod, dontShowAgainHandler, decisionHandler, () -> {
                    rows.stream()
                        .filter(candidate -> candidate.getClientProperty(mod) != null)
                        .findFirst().ifPresent(modList::remove);
                    summary.setText(summaryText(requiredCount, --remainingOptionalCount[0]));
                    refreshRowBorders(modList);
                    modList.revalidate();
                    modList.repaint();
                });
                row.putClientProperty(mod, Boolean.TRUE);
                rows.add(row);
                modList.add(row);
            }
            refreshRowBorders(modList);

            var modListScrollPane = new JScrollPane(modList);
            modListScrollPane.setPreferredSize(new Dimension(0, 302));
            modListScrollPane.setMinimumSize(new Dimension(0, 302));
            modListScrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
            modListScrollPane.setBackground(SURFACE);
            modListScrollPane.getViewport().setBackground(SURFACE);
            modListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            modListScrollPane.getVerticalScrollBar().setUnitIncrement(24);

            var content = new JPanel();
            content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
            content.setBackground(WINDOW_BACKGROUND);

            var heading = new JLabel("Missing mods");
            heading.setFont(TITLE_FONT);
            heading.setForeground(TEXT);
            setFixedHeight(heading, 30);
            content.add(heading);

            var intro = new JLabel("Download the missing mods and place them in your mods folder.");
            intro.setFont(BASE_FONT);
            intro.setForeground(MUTED_TEXT);
            setFixedHeight(intro, 20);
            content.add(intro);

            var summarySlot = new JPanel(new java.awt.BorderLayout());
            summarySlot.setOpaque(false);
            summarySlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 10, 0));
            summarySlot.add(summary, java.awt.BorderLayout.CENTER);
            setFixedHeight(summarySlot, 45);
            content.add(summarySlot);
            setFixedHeight(modListScrollPane, 302);
            content.add(modListScrollPane);

            var optionalPreference = new JCheckBox("Do not show optional mods on future startups");
            optionalPreference.setSelected(neverAskForOptionals);
            optionalPreference.setFont(SMALL_FONT);
            optionalPreference.setForeground(TITLEBAR_TEXT);
            optionalPreference.setOpaque(false);
            optionalPreference.setFocusPainted(false);
            optionalPreference.setIconTextGap(9);
            optionalPreference.setMargin(new Insets(0, 0, 0, 0));
            optionalPreference.addActionListener(event ->
                optionalPreferenceHandler.accept(optionalPreference.isSelected())
            );
            var preferenceSlot = new JPanel(new java.awt.BorderLayout());
            preferenceSlot.setOpaque(false);
            preferenceSlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 22, 0));
            preferenceSlot.add(optionalPreference, java.awt.BorderLayout.CENTER);
            setFixedHeight(preferenceSlot, 56);
            content.add(preferenceSlot);

            var footer = new JPanel(new MigLayout(
                "insets 0, fillx",
                "[pref][grow, fill]",
                "[34!]"
            ));
            footer.setOpaque(false);
            setFixedHeight(footer, 34);

            var openModsDirectory = createButton("Open mods folder", false);
            setFixedHeight(openModsDirectory, 34);
            openModsDirectory.addActionListener(event -> openModsDirectory());

            var downloadAll = createButton("Download all", true);
            setFixedHeight(downloadAll, 34);
            downloadAll.addActionListener(event -> {
                sortedMissingMods.forEach(mod -> openUrl(mod.url()));
                decisionHandler.accept(StartupDecision.EXIT);
            });

            footer.add(openModsDirectory, "left, top");
            footer.add(downloadAll, "right, top");
            content.add(footer);

            var titlebar = new JPanel(new java.awt.BorderLayout());
            titlebar.setBackground(SURFACE);
            titlebar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(0, 16, 0, 16)
            ));
            var title = new JLabel("No Mod Left Behind");
            title.setFont(SMALL_FONT);
            title.setForeground(TITLEBAR_TEXT);
            title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            titlebar.add(javax.swing.Box.createHorizontalStrut(30), java.awt.BorderLayout.WEST);
            titlebar.add(title, java.awt.BorderLayout.CENTER);
            var closeButton = createCloseButton();
            var closeSlot = new JPanel(new java.awt.GridBagLayout());
            closeSlot.setOpaque(false);
            closeSlot.setPreferredSize(new Dimension(30, 38));
            closeSlot.add(closeButton);
            titlebar.add(closeSlot, java.awt.BorderLayout.EAST);

            var root = new JPanel(new MigLayout(
                "insets 0, fill, wrap 1, gap 0",
                "[grow, fill]",
                "[38!][grow, fill]"
            ));
            root.setBackground(WINDOW_BACKGROUND);
            root.setBorder(BorderFactory.createLineBorder(BORDER));
            root.add(titlebar, "growx, h 38!");
            root.add(content, "grow, push");

            var window = new JFrame();
            window.setUndecorated(true);
            window.setResizable(false);
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            closeButton.addActionListener(event -> window.dispose());
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent event) {
                    decisionHandler.accept(hasRequiredMods ? StartupDecision.EXIT : StartupDecision.CONTINUE);
                }
            });
            installDragSupport(window, titlebar);
            window.setContentPane(root);
            window.setSize(new Dimension(736, 584));
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        } catch (HeadlessException | AWTError exception) {
            LOGGER.error("Failed to open the startup window because AWT is unavailable.", exception);
            decisionHandler.accept(StartupDecision.EXIT);
        }
    }

    private JPanel createModRow(
        DownloadableModConfiguration mod,
        Consumer<DownloadableModConfiguration> dontShowAgainHandler,
        Consumer<StartupDecision> decisionHandler,
        Runnable removeRow
    ) {
        var row = new JPanel(new java.awt.GridBagLayout());
        row.setBackground(SURFACE);
        setFixedHeight(row, 74);

        var optional = mod.isOptional();
        var requirementColor = optional ? OPTIONAL_COLOR : REQUIRED_COLOR;
        var identity = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 0", "[grow, fill]"));
        identity.setOpaque(false);

        var name = new JLabel(mod.name());
        name.setFont(BASE_FONT);
        name.setForeground(TEXT);
        identity.add(name, "growx");

        var source = new JLabel(displayHost(mod.url()));
        source.setFont(SMALL_FONT);
        source.setForeground(MUTED_TEXT);
        source.setToolTipText(mod.url());
        identity.add(source, "growx, gaptop 3");
        var identityConstraints = new java.awt.GridBagConstraints();
        identityConstraints.gridx = 0;
        identityConstraints.gridy = 0;
        identityConstraints.weightx = 1;
        identityConstraints.fill = java.awt.GridBagConstraints.BOTH;
        identityConstraints.insets = new Insets(12, 16, 12, 12);
        row.add(identity, identityConstraints);

        var status = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        status.setOpaque(false);
        var requirementIconPath = optional ? "/assets/optional.svg" : "/assets/required.svg";
        var requirementIconResource = StartupWindow.class.getResource(requirementIconPath);
        if (requirementIconResource == null) {
            throw new IllegalStateException("Missing startup window icon resource: " + requirementIconPath);
        }

        var requirementIcon = new FlatSVGIcon(requirementIconResource).derive(17, 17);
        requirementIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> requirementColor));
        var requirementIconLabel = new JLabel(requirementIcon);
        requirementIconLabel.getAccessibleContext().setAccessibleName(optional ? "Optional" : "Required");
        status.add(requirementIconLabel);

        var requirement = new JLabel(optional ? "Optional" : "Required");
        requirement.setFont(SMALL_FONT);
        requirement.setForeground(requirementColor);
        status.add(requirement);
        setFixedWidth(status, 102);
        var statusConstraints = new java.awt.GridBagConstraints();
        statusConstraints.gridx = 1;
        statusConstraints.gridy = 0;
        statusConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        statusConstraints.insets = new Insets(12, 0, 12, 12);
        row.add(status, statusConstraints);

        var download = createButton("Download", false);
        setFixedWidth(download, 88);
        setFixedHeight(download, 34);
        download.addActionListener(event -> {
            openUrl(mod.url());
            decisionHandler.accept(StartupDecision.EXIT);
        });
        var downloadConstraints = new java.awt.GridBagConstraints();
        downloadConstraints.gridx = 2;
        downloadConstraints.gridy = 0;
        downloadConstraints.fill = java.awt.GridBagConstraints.NONE;
        downloadConstraints.insets = new Insets(12, 0, 12, 12);
        row.add(download, downloadConstraints);

        if (optional) {
            var dontShowAgain = createQuietButton("Don't show again");
            setFixedWidth(dontShowAgain, 122);
            setFixedHeight(dontShowAgain, 34);
            dontShowAgain.addActionListener(event -> {
                dontShowAgainHandler.accept(mod);
                removeRow.run();
            });
            var dontShowAgainConstraints = new java.awt.GridBagConstraints();
            dontShowAgainConstraints.gridx = 3;
            dontShowAgainConstraints.gridy = 0;
            dontShowAgainConstraints.fill = java.awt.GridBagConstraints.NONE;
            dontShowAgainConstraints.insets = new Insets(12, 0, 12, 16);
            row.add(dontShowAgain, dontShowAgainConstraints);
        } else {
            var emptyActions = new JPanel();
            emptyActions.setOpaque(false);
            setFixedWidth(emptyActions, 122);
            var emptyActionsConstraints = new java.awt.GridBagConstraints();
            emptyActionsConstraints.gridx = 3;
            emptyActionsConstraints.gridy = 0;
            emptyActionsConstraints.fill = java.awt.GridBagConstraints.NONE;
            emptyActionsConstraints.insets = new Insets(12, 0, 12, 16);
            row.add(emptyActions, emptyActionsConstraints);
        }

        return row;
    }

    private static void refreshRowBorders(JPanel modList) {
        var lastVisibleRow = -1;
        for (int index = 0; index < modList.getComponentCount(); index++) {
            if (modList.getComponent(index).isVisible()) {
                lastVisibleRow = index;
            }
        }
        for (int index = 0; index < modList.getComponentCount(); index++) {
            var row = (JPanel) modList.getComponent(index);
            row.setBorder(index == lastVisibleRow
                ? null
                : new MatteBorder(0, 0, 1, 0, BORDER));
        }
    }

    private static String summaryText(int requiredCount, int optionalCount) {
        return String.format(Locale.ROOT, "%d required · %d optional", requiredCount, optionalCount);
    }

    private static void setFixedHeight(JComponent component, int height) {
        var minimum = component.getMinimumSize();
        var preferred = component.getPreferredSize();
        var maximum = component.getMaximumSize();
        var fixed = new Dimension(preferred.width, height);
        component.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        component.setMinimumSize(new Dimension(minimum.width, height));
        component.setPreferredSize(fixed);
        component.setMaximumSize(new Dimension(maximum.width, height));
    }

    private static void setFixedWidth(JComponent component, int width) {
        var minimum = component.getMinimumSize();
        var preferred = component.getPreferredSize();
        var maximum = component.getMaximumSize();
        component.setMinimumSize(new Dimension(width, minimum.height));
        component.setPreferredSize(new Dimension(width, preferred.height));
        component.setMaximumSize(new Dimension(width, maximum.height));
    }

    private static JButton createButton(String text, boolean primary) {
        var button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(primary ? ACCENT_TEXT : TEXT);
        button.setBackground(primary ? ACCENT : BUTTON_BACKGROUND);
        button.putClientProperty(FlatClientProperties.STYLE,
            "arc: 6; focusWidth: 0; innerFocusWidth: 0; "
                + "borderColor: " + toHex(primary ? ACCENT : BUTTON_BORDER) + "; "
                + "hoverBorderColor: " + toHex(primary ? ACCENT : BUTTON_BORDER) + "; "
                + "pressedBorderColor: " + toHex(primary ? ACCENT : BUTTON_BORDER) + ";");
        button.setMargin(new Insets(8, primary ? 18 : 12, 8, primary ? 18 : 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        return button;
    }

    private static JButton createCloseButton() {
        var button = createButton("×", false);
        button.setFont(BASE_FONT.deriveFont(18f));
        button.setForeground(TITLEBAR_TEXT);
        button.setBackground(SURFACE);
        button.setMargin(new Insets(0, 0, 1, 0));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.putClientProperty(FlatClientProperties.STYLE,
            "arc: 6; focusWidth: 0; innerFocusWidth: 0; "
                + "background: " + toHex(SURFACE) + "; "
                + "hoverBackground: " + toHex(SURFACE) + "; "
                + "pressedBackground: " + toHex(SURFACE) + "; "
                + "borderColor: " + toHex(SURFACE) + "; "
                + "hoverBorderColor: " + toHex(SURFACE) + "; "
                + "pressedBorderColor: " + toHex(SURFACE) + ";");
        button.setToolTipText("Close");
        button.getAccessibleContext().setAccessibleName("Close");
        setFixedWidth(button, 30);
        setFixedHeight(button, 30);
        return button;
    }

    private static JButton createQuietButton(String text) {
        var button = createButton(text, false);
        button.setForeground(MUTED_TEXT);
        button.setBackground(SURFACE);
        button.putClientProperty(FlatClientProperties.STYLE,
            "arc: 6; focusWidth: 0; innerFocusWidth: 0; "
                + "background: " + toHex(SURFACE) + "; "
                + "hoverBackground: " + toHex(BUTTON_BACKGROUND) + "; "
                + "pressedBackground: " + toHex(BUTTON_BACKGROUND) + "; "
                + "borderColor: " + toHex(SURFACE) + "; "
                + "hoverBorderColor: " + toHex(BUTTON_BACKGROUND) + "; "
                + "pressedBorderColor: " + toHex(BUTTON_BACKGROUND) + ";");
        button.setMargin(new Insets(8, 0, 8, 0));
        return button;
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

    private static Color color(String hex) {
        return Color.decode(hex);
    }

    private static String toHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
