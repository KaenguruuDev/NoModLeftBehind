package dev.kaenguruu.nomodleftbehind.client;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;

final class StartupWindowStyle {
    static final Color WINDOW_BACKGROUND = color("#202328");
    static final Color SURFACE = color("#272B31");
    static final Color BORDER = color("#3A4048");
    static final Color TEXT = color("#F1F3F5");
    static final Color MUTED_TEXT = color("#A8B0BB");
    static final Color TITLEBAR_TEXT = color("#BFC6D0");
    static final Color REQUIRED_COLOR = color("#F08B82");
    static final Color OPTIONAL_COLOR = color("#E3BC65");
    static final Color BUTTON_BACKGROUND = color("#30353D");
    static final Color BUTTON_BORDER = color("#454D59");
    static final Color ACCENT = color("#6D9EEB");
    static final Color ACCENT_TEXT = color("#14243D");
    static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font SMALL_FONT = BASE_FONT.deriveFont(12f);
    static final Font TITLE_FONT = BASE_FONT.deriveFont(24f);
    static final Font BUTTON_FONT = BASE_FONT.deriveFont(12f);

    private StartupWindowStyle() {
        /* This utility class should not be instantiated */
    }

    public static void installLookAndFeel() {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", BASE_FONT);
    }

    public static JLabel createLabel(String text, Font font, Color foreground) {
        var label = new JLabel(text);
        label.setFont(font);
        label.setForeground(foreground);

        return label;
    }

    public static JPanel createModList() {
        var modList = new JPanel();
        modList.setLayout(new javax.swing.BoxLayout(modList, javax.swing.BoxLayout.Y_AXIS));
        modList.setBackground(SURFACE);

        return modList;
    }

    public static JScrollPane createModListScrollPane(JPanel modList) {
        var scrollPane = new JScrollPane(modList);
        scrollPane.setPreferredSize(new Dimension(0, 302));
        scrollPane.setMinimumSize(new Dimension(0, 302));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.setBackground(SURFACE);
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        setFixedHeight(scrollPane, 302);

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
        var heading = createLabel("Missing mods", TITLE_FONT, TEXT);
        setFixedHeight(heading, 30);

        return heading;
    }

    public static JLabel createIntro() {
        var intro = createLabel(
            "Download the missing mods and place them in your mods folder.",
            BASE_FONT,
            MUTED_TEXT
        );
        setFixedHeight(intro, 20);

        return intro;
    }

    public static JPanel createSummarySlot(JLabel summary) {
        var summarySlot = new JPanel(new BorderLayout());
        summarySlot.setOpaque(false);
        summarySlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 10, 0));
        summarySlot.add(summary, BorderLayout.CENTER);
        setFixedHeight(summarySlot, 45);

        return summarySlot;
    }

    public static JCheckBox createOptionalPreference(boolean selected, Consumer<Boolean> changeHandler) {
        var preference = new JCheckBox("Do not show optional mods on future startups");
        preference.setSelected(selected);
        preference.setFont(SMALL_FONT);
        preference.setForeground(TITLEBAR_TEXT);
        preference.setOpaque(false);
        preference.setFocusPainted(false);
        preference.setIconTextGap(9);
        preference.setMargin(new Insets(0, 0, 0, 0));
        preference.addActionListener(event -> changeHandler.accept(preference.isSelected()));

        return preference;
    }

    public static JPanel createPreferenceSlot(JCheckBox preference) {
        var preferenceSlot = new JPanel(new BorderLayout());
        preferenceSlot.setOpaque(false);
        preferenceSlot.setBorder(BorderFactory.createEmptyBorder(18, 0, 22, 0));
        preferenceSlot.add(preference, BorderLayout.CENTER);
        setFixedHeight(preferenceSlot, 56);

        return preferenceSlot;
    }

    public static JPanel createFooter() {
        var footer = new JPanel(new MigLayout(
            "insets 0, fillx",
            "[pref][grow, fill]",
            "[34!]"
        ));
        footer.setOpaque(false);
        setFixedHeight(footer, 34);

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
        closeSlot.setPreferredSize(new Dimension(30, 38));
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
        setFixedHeight(row, 74);

        return row;
    }

    public static JPanel createRequirementStatus(boolean optional) {
        var requirementColor = optional ? OPTIONAL_COLOR : REQUIRED_COLOR;
        var status = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        status.setOpaque(false);

        var requirementIconPath = optional ? "/assets/optional.svg" : "/assets/required.svg";
        var requirementIconResource = StartupWindowStyle.class.getResource(requirementIconPath);
        if (requirementIconResource == null) {
            throw new IllegalStateException("Missing startup window icon resource: " + requirementIconPath);
        }

        var requirementIcon = new FlatSVGIcon(requirementIconResource).derive(17, 17);
        requirementIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> requirementColor));

        var requirementIconLabel = new JLabel(requirementIcon);
        requirementIconLabel.getAccessibleContext().setAccessibleName(optional ? "Optional" : "Required");
        status.add(requirementIconLabel);

        var requirement = createLabel(
            optional ? "Optional" : "Required",
            SMALL_FONT,
            requirementColor
        );
        status.add(requirement);
        setFixedWidth(status, 102);

        return status;
    }

    public static JPanel createEmptyActions() {
        var emptyActions = new JPanel();
        emptyActions.setOpaque(false);
        setFixedWidth(emptyActions, 122);

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
        setFixedWidth(button, 30);
        setFixedHeight(button, 30);
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

        return button;
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

    public static void setFixedHeight(JComponent component, int height) {
        var minimum = component.getMinimumSize();
        var preferred = component.getPreferredSize();
        var maximum = component.getMaximumSize();
        var fixed = new Dimension(preferred.width, height);

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMinimumSize(new Dimension(minimum.width, height));
        component.setPreferredSize(fixed);
        component.setMaximumSize(new Dimension(maximum.width, height));
    }

    public static void setFixedWidth(JComponent component, int width) {
        var minimum = component.getMinimumSize();
        var preferred = component.getPreferredSize();
        var maximum = component.getMaximumSize();

        component.setMinimumSize(new Dimension(width, minimum.height));
        component.setPreferredSize(new Dimension(width, preferred.height));
        component.setMaximumSize(new Dimension(width, maximum.height));
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
}
