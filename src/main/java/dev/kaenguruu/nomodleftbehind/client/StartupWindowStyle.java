package dev.kaenguruu.nomodleftbehind.client;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

final class StartupWindowStyle {
    static final Color WINDOW_BACKGROUND = color("#202328");
    static final Color SURFACE = color("#272B31");
    static final Color BORDER = color("#3A4048");
    static final Color TEXT = color("#F1F3F5");
    static final Color MUTED_TEXT = color("#A8B0BB");
    static final Color TITLEBAR_TEXT = color("#BFC6D0");
    static final Color REQUIRED_COLOR = color("#F08B82");
    static final Color OPTIONAL_COLOR = color("#E3BC65");
    static final Color CHECKSUM_COLOR = color("#F2C14E");
    static final Color CHECKSUM_WARNING_BACKGROUND = color("#3A3022");
    static final Color ADDED_COLOR = color("#79C99A");
    static final Color BUTTON_BACKGROUND = color("#30353D");
    static final Color BUTTON_BORDER = color("#454D59");
    static final Color ACCENT = color("#6D9EEB");
    static final Color ACCENT_TEXT = color("#14243D");
    static Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    static Font SMALL_FONT = BASE_FONT.deriveFont(12f);
    static Font TITLE_FONT = BASE_FONT.deriveFont(24f);
    static Font BUTTON_FONT = BASE_FONT.deriveFont(12f);
    private static final EnumMap<ModStatus, FlatSVGIcon> STATUS_ICONS = new EnumMap<>(ModStatus.class);
    private static final Map<ModStatus, StatusPresentation> STATUS_PRESENTATIONS = Map.of(
        ModStatus.REQUIRED, new StatusPresentation(
            "/assets/required.svg",
            "Required",
            "Required",
            REQUIRED_COLOR,
            0,
            0,
            "This required mod is missing. Download it and place it in your mods folder to continue."
        ),
        ModStatus.CHECKSUM_MISMATCH, new StatusPresentation(
            "/assets/warning.svg",
            "Mismatch",
            "Checksum mismatch",
            CHECKSUM_COLOR,
            2,
            1,
            "This file doesn't match the version specified by the modpack author. Replace it with the correct file."
        ),
        ModStatus.OPTIONAL, new StatusPresentation(
            "/assets/optional.svg",
            "Optional",
            "Optional",
            OPTIONAL_COLOR,
            0,
            2,
            "This optional mod is missing. Download it if you want to use it, or choose “Don't show again” to hide it."
        ),
        ModStatus.ADDED, new StatusPresentation(
            "/assets/added.svg",
            "Added",
            "Added",
            ADDED_COLOR,
            1,
            3,
            "Found %s in your mods folder."
        )
    );

    private StartupWindowStyle() {
        /* This utility class should not be instantiated */
    }

    public static void installLookAndFeel() {
        configureHiDpiFallback();
        FlatDarkLaf.setup();

        var userScale = UIScale.getUserScaleFactor();
        BASE_FONT = scaledFont(Font.PLAIN, 14f, userScale);
        SMALL_FONT = scaledFont(Font.PLAIN, 12f, userScale);
        TITLE_FONT = scaledFont(Font.PLAIN, 24f, userScale);
        BUTTON_FONT = scaledFont(Font.PLAIN, 12f, userScale);
    }

    private static void configureHiDpiFallback() {
        if (System.getProperty("flatlaf.uiScale") != null || GraphicsEnvironment.isHeadless()) {
            return;
        }

        try {
            var configuration = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
            var javaScale = Math.max(
                configuration.getDefaultTransform().getScaleX(),
                configuration.getDefaultTransform().getScaleY()
            );
            var desktopScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96d;

            // Some embedded Java clients expose a 1.0 Java2D transform even
            // though Windows reports a larger desktop scale. FlatLaf's custom
            // scale factor fills that gap; when Java already applies the
            // scale, adding it again would make the UI too large.
            if (desktopScale > javaScale + 0.01d) {
                System.setProperty("flatlaf.uiScale", Double.toString(desktopScale / javaScale));
            }
        } catch (HeadlessException | AWTError ignored) {
            // The normal Java2D/FlatLaf scaling path remains available.
        }
    }

    private static Font scaledFont(int style, float size, float scale) {
        return new Font("Segoe UI", style, 1).deriveFont(size * scale);
    }

    public static JLabel createLabel(String text, Font font, Color foreground) {
        var label = new JLabel(text);
        label.setFont(font);
        label.setForeground(foreground);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    public static JPanel createModList() {
        var modList = new JPanel();
        modList.setLayout(new javax.swing.BoxLayout(modList, javax.swing.BoxLayout.Y_AXIS));
        modList.setBackground(SURFACE);
        setFullWidth(modList);

        return modList;
    }

    public static JScrollPane createModListScrollPane(JPanel modList) {
        var scrollPane = new JScrollPane(modList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.setBackground(SURFACE);
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        setExactHeight(scrollPane, UIScale.scale(302));
        setFullWidth(scrollPane);
        var listPreferredWidth = modList.getPreferredSize().width;
        scrollPane.setPreferredSize(new Dimension(listPreferredWidth, scrollPane.getPreferredSize().height));

        return scrollPane;
    }

    public static JPanel createContent() {
        var content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.setBackground(WINDOW_BACKGROUND);

        return content;
    }

    public static JLabel createHeading() {
        var heading = createLabel("Mods needing attention", TITLE_FONT, TEXT);
        setAtLeastHeight(heading, 34);

        return heading;
    }

    public static JLabel createIntro() {
        var intro = createLabel(
            "Download the missing mods and place them in your mods folder.",
            BASE_FONT,
            MUTED_TEXT
        );
        setAtLeastHeight(intro, 20);

        return intro;
    }

    public static JLabel createSecurityWarning() {
        var warning = createLabel(
            "Only download files when you trust the modpack author and linked source.",
            SMALL_FONT,
            OPTIONAL_COLOR
        );
        setAtLeastHeight(warning, 18);

        return warning;
    }

    public static JPanel createChecksumWarning(int mismatchCount) {
        var warningSlot = new JPanel(new BorderLayout());
        warningSlot.setOpaque(false);
        warningSlot.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        setFullWidth(warningSlot);

        var warning = new JPanel(new BorderLayout(12, 0));
        warning.setBackground(CHECKSUM_WARNING_BACKGROUND);
        warning.setBorder(new FlatLineBorder(new Insets(8, 12, 8, 12), CHECKSUM_COLOR, 1f, 6));
        warningSlot.add(warning, BorderLayout.CENTER);
        setAtLeastHeight(warningSlot, 92);
        updateChecksumWarning(warningSlot, mismatchCount);
        return warningSlot;
    }

    public static void updateChecksumWarning(JPanel warningSlot, int mismatchCount) {
        var warning = (JPanel) warningSlot.getComponent(0);
        var copy = checksumWarningCopy(mismatchCount);
        warning.removeAll();

        var icon = new JLabel(getStatusIcon(ModStatus.CHECKSUM_MISMATCH));
        icon.getAccessibleContext().setAccessibleName("Checksum warning");
        warning.add(icon, BorderLayout.WEST);

        var message = new JPanel(new MigLayout(
            "insets 0, fillx, wrap 1, gap 0",
            "[grow, fill]"
        ));
        message.setOpaque(false);

        var heading = createLabel("Warning", scaledFont(Font.BOLD, 15f, UIScale.getUserScaleFactor()), CHECKSUM_COLOR);
        message.add(heading, "growx");

        var explanation = createLabel(
            copy.explanation(),
            scaledFont(Font.BOLD, 14f, UIScale.getUserScaleFactor()),
            TEXT
        );
        message.add(explanation, "growx, gaptop 2");

        var action = createLabel(
            copy.action(),
            scaledFont(Font.BOLD, 14f, UIScale.getUserScaleFactor()),
            TEXT
        );
        message.add(action, "growx, gaptop 2");
        warning.add(message, BorderLayout.CENTER);

        setAtLeastHeight(warning, 80);
        warningSlot.setVisible(mismatchCount > 0);
        warningSlot.revalidate();
        warningSlot.repaint();
    }

    public static JPanel createSummarySlot(JLabel summary) {
        var summarySlot = new JPanel(new BorderLayout());
        summarySlot.setOpaque(false);
        summarySlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 10, 0));
        setFullWidth(summarySlot);
        summarySlot.add(summary, BorderLayout.CENTER);
        setAtLeastHeight(summarySlot, 45);

        return summarySlot;
    }

    public static JCheckBox createOptionalPreference(boolean selected, UnaryOperator<Boolean> changeHandler) {
        return createOptionalPreference(selected, changeHandler, StartupWindowStyle::showPreferenceSaveFailure);
    }

    static JCheckBox createOptionalPreference(
        boolean selected,
        UnaryOperator<Boolean> changeHandler,
        Consumer<Component> failureHandler
    ) {
        var preference = new JCheckBox("Do not show optional mods on future startups");
        preference.setSelected(selected);
        preference.setFont(SMALL_FONT);
        preference.setForeground(TITLEBAR_TEXT);
        preference.setOpaque(false);
        preference.setFocusPainted(false);
        preference.setIconTextGap(9);
        preference.setMargin(new Insets(0, 0, 0, 0));
        preference.addActionListener(event -> {
            var requestedValue = preference.isSelected();
            if (!Boolean.TRUE.equals(changeHandler.apply(requestedValue))) {
                preference.setSelected(!requestedValue);
                failureHandler.accept(preference);
            }
        });

        return preference;
    }

    static void showPreferenceSaveFailure(Component parent) {
        JOptionPane.showMessageDialog(
            parent,
            "Unable to save this preference. Your change was not applied.",
            "Preference not saved",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static JPanel createPreferenceSlot(JCheckBox preference) {
        var preferenceSlot = new JPanel(new BorderLayout());
        preferenceSlot.setOpaque(false);
        preferenceSlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 22, 0));
        setFullWidth(preferenceSlot);
        preferenceSlot.add(preference, BorderLayout.CENTER);
        setAtLeastHeight(preferenceSlot, 56);

        return preferenceSlot;
    }

    public static ResponsiveFooter createFooter() {
        var footer = new ResponsiveFooter();
        footer.setOpaque(false);
        setFullWidth(footer);

        return footer;
    }

    public static JPanel createTitlebar(JButton closeButton) {
        var titlebar = new JPanel(new BorderLayout());
        titlebar.setBackground(SURFACE);
        titlebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(0, 16, 0, 16)
        ));

        var title = createLabel("No Mod Left Behind", SMALL_FONT, TITLEBAR_TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        titlebar.add(Box.createHorizontalStrut(30), BorderLayout.WEST);
        titlebar.add(title, BorderLayout.CENTER);

        var closeSlot = new JPanel(new GridBagLayout());
        closeSlot.setOpaque(false);
        var closeSize = closeButton.getPreferredSize();
        closeSlot.setPreferredSize(new Dimension(closeSize.width, closeSize.height + 8));
        closeSlot.add(closeButton);

        titlebar.add(closeSlot, BorderLayout.EAST);
        return titlebar;
    }

    public static JPanel createRoot(JPanel titlebar, JPanel content) {
        var root = new JPanel(new MigLayout(
            "insets 0, fill, wrap 1, gap 0",
            "[grow, fill]",
            "[38!][grow, fill]"
        ));
        root.setBackground(WINDOW_BACKGROUND);
        root.setBorder(BorderFactory.createLineBorder(BORDER));
        root.add(titlebar, "growx, h 38!");
        root.add(content, "grow, push");

        return root;
    }

    public static JPanel createIdentity(String name, String source, String url) {
        var identity = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 0", "[grow, fill]"));
        identity.setOpaque(false);

        var nameLabel = createLabel(name, BASE_FONT, TEXT);
        identity.add(nameLabel, "growx");

        var sourceLabel = createLabel(source, SMALL_FONT, MUTED_TEXT);
        sourceLabel.setToolTipText(url);
        identity.add(sourceLabel, "growx, gaptop 3");

        return identity;
    }

    public static JPanel createModRow() {
        var row = new JPanel(new GridBagLayout());
        row.setBackground(SURFACE);
        setFullWidth(row);
        setAtLeastHeight(row, 74);

        return row;
    }

    public static JPanel createStatus(ModStatus statusType, String tooltip) {
        var presentation = presentationFor(statusType);
        var status = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        status.setOpaque(false);

        status.add(createStatusLabel(statusType, presentation, tooltip));
        var preferred = status.getPreferredSize();
        status.setPreferredSize(new Dimension(preferredStatusWidth(), preferred.height));

        return status;
    }

    public static void setStatus(JPanel statusPanel, ModStatus statusType, String tooltip) {
        var presentation = presentationFor(statusType);
        statusPanel.removeAll();

        statusPanel.add(createStatusLabel(statusType, presentation, tooltip));
        statusPanel.revalidate();
        statusPanel.repaint();
    }

    private static JLabel createStatusLabel(
        ModStatus statusType,
        StatusPresentation presentation,
        String tooltip
    ) {
        var statusLabel = new JLabel(presentation.label(), getStatusIcon(statusType), SwingConstants.LEFT);
        statusLabel.setFont(SMALL_FONT);
        statusLabel.setForeground(presentation.color());
        statusLabel.setIconTextGap(7);
        statusLabel.setToolTipText(tooltip);
        statusLabel.getAccessibleContext().setAccessibleName(presentation.accessibleName());
        return statusLabel;
    }

    private static int preferredStatusWidth() {
        var width = 0;
        for (var statusType : ModStatus.values()) {
            var presentation = presentationFor(statusType);
            var candidate = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
            candidate.add(createStatusLabel(statusType, presentation, null));
            width = Math.max(width, candidate.getPreferredSize().width);
        }
        return width;
    }

    static String statusTooltip(ModStatus statusType, String fileName) {
        var tooltip = presentationFor(statusType).tooltip();
        if (statusType == ModStatus.ADDED) {
            return tooltip.formatted(fileName == null ? "the mod file" : fileName);
        }
        return tooltip;
    }

    private static FlatSVGIcon getStatusIcon(ModStatus statusType) {
        return STATUS_ICONS.computeIfAbsent(
            statusType,
            value -> {
                var presentation = presentationFor(value);
                return createRequirementIcon(presentation.iconPath(), presentation.color());
            }
        );
    }

    static int statusSortOrder(ModStatus statusType) {
        return presentationFor(statusType).sortOrder();
    }

    static boolean canReplaceStatus(ModStatus currentStatus, ModStatus replacementStatus) {
        if (currentStatus == ModStatus.CHECKSUM_MISMATCH && replacementStatus == ModStatus.ADDED) {
            return true;
        }

        return presentationFor(replacementStatus).priority() >= presentationFor(currentStatus).priority();
    }

    private static StatusPresentation presentationFor(ModStatus statusType) {
        var presentation = STATUS_PRESENTATIONS.get(statusType);
        if (presentation == null) {
            throw new IllegalStateException("Missing startup window status presentation: " + statusType);
        }
        return presentation;
    }

    private static FlatSVGIcon createRequirementIcon(String resourcePath, Color color) {
        var resource = StartupWindowStyle.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing startup window icon resource: " + resourcePath);
        }

        var icon = new FlatSVGIcon(resource).derive(17, 17);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(ignored -> color));
        return icon;
    }

    public static JPanel createEmptyActions() {
        var emptyActions = new JPanel();
        emptyActions.setOpaque(false);
        var dismissButton = createQuietButton("Don't show again");
        var preferredDismissSize = dismissButton.getPreferredSize();
        emptyActions.setPreferredSize(new Dimension(preferredDismissSize.width, preferredDismissSize.height));

        return emptyActions;
    }

    public static JButton createButton(String text, boolean primary) {
        var button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(primary ? ACCENT_TEXT : TEXT);
        button.setBackground(primary ? ACCENT : BUTTON_BACKGROUND);
        button.setMargin(new Insets(8, primary ? 18 : 12, 8, primary ? 18 : 12));
        button.setFocusPainted(false);
        button.setOpaque(true);

        applyButtonStyle(button, null, null, null,
            primary ? ACCENT : BUTTON_BORDER,
            primary ? ACCENT : BUTTON_BORDER,
            primary ? ACCENT : BUTTON_BORDER);
        preserveButtonWidth(button);

        return button;
    }

    public static JButton createCloseButton() {
        var button = createButton("×", false);
        button.setFont(BASE_FONT.deriveFont(18f));
        button.setForeground(TITLEBAR_TEXT);
        button.setBackground(SURFACE);
        button.setMargin(new Insets(0, 0, 1, 0));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setToolTipText("Close");
        button.getAccessibleContext().setAccessibleName("Close");
        applyButtonStyle(button, SURFACE, SURFACE, SURFACE, SURFACE, SURFACE, SURFACE);

        return button;
    }

    public static JButton createQuietButton(String text) {
        var button = createButton(text, false);
        button.setForeground(MUTED_TEXT);
        button.setBackground(SURFACE);
        button.setMargin(new Insets(8, 0, 8, 0));
        applyButtonStyle(button, SURFACE, BUTTON_BACKGROUND, BUTTON_BACKGROUND,
            SURFACE, BUTTON_BACKGROUND, BUTTON_BACKGROUND);
        preserveButtonWidth(button);

        return button;
    }

    private static void preserveButtonWidth(JButton button) {
        var preferred = button.getPreferredSize();
        var minimum = button.getMinimumSize();
        var width = Math.max(preferred.width, minimum.width);

        // GridBagLayout may otherwise shrink an action column below the
        // measured button width when the available logical window width is
        // reduced by display scaling. Keep the text-bearing control intact.
        button.setMinimumSize(new Dimension(width, minimum.height));
        button.setPreferredSize(new Dimension(width, preferred.height));
    }

    public static GridBagConstraints identityConstraints() {
        var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(12, 16, 12, 12);

        return constraints;
    }

    public static GridBagConstraints statusConstraints() {
        return rowConstraints(1, GridBagConstraints.HORIZONTAL, new Insets(12, 0, 12, 12));
    }

    public static GridBagConstraints actionConstraints(int column, int rightInset) {
        return rowConstraints(column, GridBagConstraints.NONE, new Insets(12, 0, 12, rightInset));
    }

    public static void refreshRowBorders(JPanel modList) {
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

    static void setAtLeastHeight(JComponent component, int height) {
        var minimum = component.getMinimumSize();
        var preferred = component.getPreferredSize();
        var maximum = component.getMaximumSize();
        // The requested height is expressed in Swing's logical coordinates. On a
        // HiDPI display FlatLaf may report a larger preferred/minimum height
        // for a control than it does at 100% scaling, so never shrink below
        // those measured values.
        var actualHeight = Math.max(
            UIScale.scale(height),
            Math.max(minimum.height, preferred.height)
        );
        var sized = new Dimension(preferred.width, actualHeight);

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMinimumSize(new Dimension(minimum.width, actualHeight));
        component.setPreferredSize(sized);
        component.setMaximumSize(new Dimension(maximum.width, actualHeight));
    }

    static void setExactHeight(JComponent component, int height) {
        var minimum = component.getMinimumSize();
        var actualHeight = Math.max(height, minimum.height);
        var sized = new Dimension(component.getPreferredSize().width, actualHeight);

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMinimumSize(new Dimension(minimum.width, actualHeight));
        component.setPreferredSize(sized);
        component.setMaximumSize(new Dimension(component.getMaximumSize().width, actualHeight));
    }

    private static void setFullWidth(JComponent component) {
        var maximum = component.getMaximumSize();
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, maximum.height));
    }

    private static GridBagConstraints rowConstraints(int column, int fill, Insets insets) {
        var constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.fill = fill;
        constraints.insets = insets;

        return constraints;
    }

    private static void applyButtonStyle(
        JButton button,
        Color background,
        Color hoverBackground,
        Color pressedBackground,
        Color border,
        Color hoverBorder,
        Color pressedBorder
    ) {
        var style = "arc: 6; focusWidth: 0; innerFocusWidth: 0; ";
        if (background != null) {
            style += "background: " + toHex(background) + "; ";
            style += "hoverBackground: " + toHex(hoverBackground) + "; ";
            style += "pressedBackground: " + toHex(pressedBackground) + "; ";
        }

        style += "borderColor: " + toHex(border) + "; ";
        style += "hoverBorderColor: " + toHex(hoverBorder) + "; ";
        style += "pressedBorderColor: " + toHex(pressedBorder) + ";";

        button.putClientProperty(FlatClientProperties.STYLE, style);
    }

    private static Color color(String hex) {
        return Color.decode(hex);
    }

    private static String toHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    static final class ResponsiveFooter extends JPanel {
        private static final int GAP = 12;
        private int responsiveWidth;

        private ResponsiveFooter() {
            super(new ResponsiveFooterLayout(GAP));
        }

        void setResponsiveWidth(int width) {
            if (responsiveWidth == width) {
                return;
            }

            responsiveWidth = Math.max(0, width);
            revalidate();
        }

        int horizontalPreferredHeight() {
            return layoutManager().horizontalPreferredHeight(this);
        }

        int preferredHeightForWidth(int width) {
            return layoutManager().preferredHeightForWidth(this, width);
        }

        private ResponsiveFooterLayout layoutManager() {
            return (ResponsiveFooterLayout) getLayout();
        }

        @Override
        public Dimension getPreferredSize() {
            var preferred = super.getPreferredSize();
            if (responsiveWidth <= 0) {
                return preferred;
            }

            return new Dimension(preferred.width, preferredHeightForWidth(responsiveWidth));
        }
    }

    private static final class ResponsiveFooterLayout implements LayoutManager2 {
        private final int gap;

        private ResponsiveFooterLayout(int gap) {
            this.gap = gap;
        }

        @Override
        public void addLayoutComponent(Component component, Object constraints) {
            // Footer buttons are laid out by their measured preferred sizes.
        }

        @Override
        public void addLayoutComponent(String name, Component component) {
            // Footer buttons are laid out by their measured preferred sizes.
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return sizeForHorizontalLayout(parent);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            var insets = parent.getInsets();
            var minimumWidth = 0;
            var minimumHeight = 0;
            var visibleCount = 0;
            for (Component component : parent.getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }

                var minimum = component.getMinimumSize();
                minimumWidth = Math.max(minimumWidth, minimum.width);
                minimumHeight += minimum.height;
                visibleCount++;
            }

            if (visibleCount > 1) {
                minimumHeight += gap * (visibleCount - 1);
            }

            return new Dimension(
                insets.left + insets.right + minimumWidth,
                insets.top + insets.bottom + minimumHeight
            );
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public void layoutContainer(Container parent) {
            var insets = parent.getInsets();
            var width = Math.max(0, parent.getWidth() - insets.left - insets.right);
            var height = Math.max(0, parent.getHeight() - insets.top - insets.bottom);
            var visible = visibleComponents(parent);

            if (visible.size() <= 1 || width >= horizontalWidth(parent)) {
                layoutHorizontal(visible, insets.left, insets.top, width, height);
            } else {
                layoutVertical(visible, insets.left, insets.top, width);
            }
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0;
        }

        @Override
        public void invalidateLayout(Container target) {
        }

        private Dimension sizeForHorizontalLayout(Container parent) {
            var insets = parent.getInsets();
            var visible = visibleComponents(parent);
            var width = 0;
            var height = 0;
            for (Component component : visible) {
                var preferred = component.getPreferredSize();
                width += preferred.width;
                height = Math.max(height, preferred.height);
            }
            if (visible.size() > 1) {
                width += gap * (visible.size() - 1);
            }

            return new Dimension(
                insets.left + insets.right + width,
                insets.top + insets.bottom + height
            );
        }

        private int horizontalPreferredHeight(Container parent) {
            return sizeForHorizontalLayout(parent).height;
        }

        private int preferredHeightForWidth(Container parent, int width) {
            var insets = parent.getInsets();
            var availableWidth = Math.max(0, width - insets.left - insets.right);
            if (availableWidth >= horizontalWidth(parent)) {
                return horizontalPreferredHeight(parent);
            }

            var height = 0;
            var visible = visibleComponents(parent);
            for (Component component : visible) {
                height += component.getPreferredSize().height;
            }
            if (visible.size() > 1) {
                height += gap * (visible.size() - 1);
            }
            return insets.top + insets.bottom + height;
        }

        private int horizontalWidth(Container parent) {
            return sizeForHorizontalLayout(parent).width - parent.getInsets().left - parent.getInsets().right;
        }

        private void layoutHorizontal(
            List<Component> visible,
            int x,
            int y,
            int width,
            int height
        ) {
            if (visible.isEmpty()) {
                return;
            }

            var first = visible.get(0);
            var firstSize = first.getPreferredSize();
            first.setBounds(x, y + Math.max(0, (height - firstSize.height) / 2), firstSize.width, firstSize.height);
            if (visible.size() == 1) {
                return;
            }

            var last = visible.get(visible.size() - 1);
            var lastSize = last.getPreferredSize();
            last.setBounds(
                x + width - lastSize.width,
                y + Math.max(0, (height - lastSize.height) / 2),
                lastSize.width,
                lastSize.height
            );
            for (int index = 1; index < visible.size() - 1; index++) {
                var component = visible.get(index);
                var size = component.getPreferredSize();
                component.setBounds(
                    x + (width - size.width) / 2,
                    y + Math.max(0, (height - size.height) / 2),
                    size.width,
                    size.height
                );
            }
        }

        private void layoutVertical(List<Component> visible, int x, int y, int width) {
            for (var component : visible) {
                var preferred = component.getPreferredSize();
                component.setBounds(x, y, width, preferred.height);
                y += preferred.height + gap;
            }
        }

        private static List<Component> visibleComponents(Container parent) {
            var visible = new java.util.ArrayList<Component>();
            for (Component component : parent.getComponents()) {
                if (component.isVisible()) {
                    visible.add(component);
                }
            }
            return visible;
        }
    }

    private static ChecksumWarningCopy checksumWarningCopy(int mismatchCount) {
        if (mismatchCount == 1) {
            return new ChecksumWarningCopy(
                "One of the files does not match the trusted version.",
                "It may have been tampered with or replaced. Review it before continuing."
            );
        }
        return new ChecksumWarningCopy(
            String.format(
                Locale.ROOT,
                "%d of the files do not match their trusted versions.",
                mismatchCount
            ),
            "They may have been tampered with or replaced. Review them before continuing."
        );
    }

    enum ModStatus {
        REQUIRED,
        CHECKSUM_MISMATCH,
        OPTIONAL,
        ADDED
    }

    private record StatusPresentation(
        String iconPath,
        String label,
        String accessibleName,
        Color color,
        int priority,
        int sortOrder,
        String tooltip
    ) {
    }

    private record ChecksumWarningCopy(String explanation, String action) {
    }
}
