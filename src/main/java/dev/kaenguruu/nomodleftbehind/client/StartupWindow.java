package dev.kaenguruu.nomodleftbehind.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.mojang.logging.LogUtils;
import dev.kaenguruu.nomodleftbehind.startup.StartupDecision;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.AWTError;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public final class StartupWindow {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void show(Consumer<StartupDecision> decisionHandler) {
        // Minecraft's client run can inherit headless mode even though its GLFW display is available.
        System.setProperty("java.awt.headless", "false");

        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.error("Cannot open the startup window because AWT is running without a display.");
            decisionHandler.accept(StartupDecision.EXIT);
            return;
        }

        SwingUtilities.invokeLater(() -> createWindow(decisionHandler));
    }

    private void createWindow(Consumer<StartupDecision> decisionHandler) {
        try {
            FlatDarkLaf.setup();

            JPanel content = new JPanel(new MigLayout("insets 0"));
            content.setPreferredSize(new Dimension(640, 360));

            JFrame window = new JFrame("No Mod Left Behind - Missing Mods Detected");
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    decisionHandler.accept(StartupDecision.EXIT);
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
}
