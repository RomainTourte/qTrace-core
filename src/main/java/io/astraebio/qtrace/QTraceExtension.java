package io.astraebio.qtrace;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

import java.util.ServiceLoader;

/**
 * Entry point for the QTrace QuPath extension.
 *
 * Registered via ServiceLoader:
 *   META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 *
 * Adds:
 *  - "Extensions > QTrace" menu
 *  - A toolbar MenuButton (icon + dropdown) with recording state indicator
 */
public class QTraceExtension implements QuPathExtension {

    private static final String COLOR_IDLE      = "#6c7086"; // Catppuccin TEXT_MUTED — no image
    private static final String COLOR_RECORDING = "#f38ba8"; // Catppuccin RED — recording active

    private QTraceController controller;

    @Override
    public void installExtension(QuPathGUI qupath) {
        ServiceLoader.load(QTracePlugin.class, QTracePlugin.class.getClassLoader())
            .findFirst().ifPresent(QTracePluginManager::register);

        controller = new QTraceController(qupath);

        // ── Extensions menu ────────────────────────────────────────────────────
        var menu = qupath.getMenu("Extensions>QTrace", true);

        MenuItem openPanel   = new MenuItem("Open QTrace Panel");
        openPanel.setOnAction(e -> controller.showPanel());

        MenuItem dashboard   = new MenuItem("Dashboard");
        dashboard.setOnAction(e -> controller.showDashboard());

        MenuItem preferences = new MenuItem("Preferences...");
        preferences.setOnAction(e -> controller.showPreferences());

        MenuItem about       = new MenuItem("About QTrace...");
        about.setOnAction(e -> controller.showAbout());

        menu.getItems().addAll(openPanel, dashboard, new SeparatorMenuItem(), preferences, about);

        // ── Toolbar button (added on FX thread after QuPath finishes layout) ───
        Platform.runLater(() -> addToolbarButton(qupath));
    }

    private void addToolbarButton(QuPathGUI qupath) {
        // ── Icon: logo over a coloured rectangle ───────────────────────────────
        Rectangle bg = new Rectangle(22, 22);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        bg.setFill(Color.web(COLOR_IDLE));

        Image logoImg = QTracePanel.loadLogo();
        ImageView logoView = new ImageView();
        if (logoImg != null) {
            logoView.setImage(logoImg);
            logoView.setFitHeight(16);
            logoView.setPreserveRatio(true);
        }

        StackPane icon = new StackPane(bg, logoView);
        icon.setAlignment(Pos.CENTER);
        icon.setPadding(new Insets(2));

        // ── MenuButton ─────────────────────────────────────────────────────────
        MenuButton btn = new MenuButton();
        btn.setGraphic(icon);
        btn.setText("");
        btn.setTooltip(new Tooltip("qTrace — Workflow Provenance"));
        btn.setStyle(
            "-fx-background-color: transparent;"
          + "-fx-padding: 2 4 2 4;"
          + "-fx-cursor: hand;"
        );

        MenuItem miPanel  = new MenuItem("Panel");
        miPanel.setOnAction(e -> controller.showPanel());

        MenuItem miDash   = new MenuItem("Dashboard");
        miDash.setOnAction(e -> controller.showDashboard());

        MenuItem miPrefs  = new MenuItem("Preferences...");
        miPrefs.setOnAction(e -> controller.showPreferences());

        MenuItem miAbout  = new MenuItem("About qTrace...");
        miAbout.setOnAction(e -> controller.showAbout());

        btn.getItems().addAll(miPanel, miDash, new SeparatorMenuItem(), miPrefs, miAbout);

        // ── Dynamic icon colour based on recording state ───────────────────────
        controller.addRecordingListener(recording ->
            bg.setFill(Color.web(recording ? COLOR_RECORDING : COLOR_IDLE))
        );
        // Sync initial state
        bg.setFill(Color.web(controller.isRecording() ? COLOR_RECORDING : COLOR_IDLE));

        qupath.getToolBar().getItems().add(btn);
    }

    @Override
    public String getName() {
        return "QTrace — Workflow Provenance & Certification";
    }

    @Override
    public String getDescription() {
        return "Captures QuPath workflow history and certifies analyses with expert validation stamps.";
    }
}
