package io.astraebio.qtrace;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.Optional;

/**
 * Phase 5 — Expert validation dialog.
 *
 * Collects: validator name, scope, confidence level, free-text notes.
 * Returns a {@link ValidationStamp} that QTraceController stores and
 * Phase 6 embeds in the .qtrace JSON sidecar.
 */
public class ValidationStamper {

    private ValidationStamper() {}

    /**
     * Show the validation dialog and return the stamp if the user confirmed.
     *
     * @param owner    owning stage (for modal positioning)
     * @param gitHash  commit hash from Phase 4 (may be null if not yet committed)
     * @param imgHash  SHA-256 of the source image (may be null if still computing)
     * @param fidelity classifier fidelity computed by ActionLogger
     */
    public static final String[] STATUS_LABELS = {"0-To Begin", "1-In Progress", "2-Finished"};

    public static Optional<ValidationStamp> show(Stage owner, String gitHash, String imgHash,
                                                  ClassifierFidelity fidelity,
                                                  String currentStatusLabel) {
        Dialog<ValidationStamp> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("QTrace — Expert Validation");
        dialog.setHeaderText("Validate this workflow capture");

        // ── Form fields ──────────────────────────────────────────────────────
        // If Enterprise + valid license: identity is locked to the license holder
        LicenseInfo activeLicense = null;
        QTracePlugin ep = QTracePluginManager.get();
        if (ep != null) activeLicense = ep.getActiveLicenseInfo();

        String configuredValidator = (activeLicense != null)
            ? activeLicense.name()
            : QTraceConfig.get().getValidatorName();

        TextField validatorField = new TextField(configuredValidator);
        validatorField.setPromptText("Dr. Lastname / Analyst ID");
        validatorField.setPrefWidth(280);

        if (activeLicense != null) {
            // Identity certified — field is read-only
            validatorField.setEditable(false);
            validatorField.setStyle(
                "-fx-background-color: derive(-fx-control-inner-background, -5%);"
              + "-fx-text-fill: -fx-text-inner-color;"
              + "-fx-opacity: 1;"
            );
            validatorField.setTooltip(new javafx.scene.control.Tooltip(
                "Identity locked — certified by your qTrace Enterprise license.\n"
              + "Institution: " + activeLicense.institution() + "\n"
              + "Valid until: " + activeLicense.expiresAtFormatted()
            ));
        }

        ComboBox<String> scopeBox = new ComboBox<>(FXCollections.observableArrayList(
            "Full Workflow",
            "Image QC",
            "Segmentation",
            "Feature Extraction",
            "Phenotyping",
            "Spatial Analysis"
        ));
        scopeBox.setValue("Full Workflow");

        ComboBox<String> confidenceBox = new ComboBox<>(FXCollections.observableArrayList(
            "High", "Medium", "Low"
        ));
        confidenceBox.setValue("High");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional — anomalies, caveats, deviations from SOP…");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        ComboBox<String> statusBox = new ComboBox<>(
            FXCollections.observableArrayList(STATUS_LABELS));
        String preselect = "1-In Progress";
        if (currentStatusLabel != null) {
            for (String s : STATUS_LABELS) if (s.equals(currentStatusLabel)) { preselect = s; break; }
        }
        statusBox.setValue(preselect);

        // Read-only provenance display
        Label gitLabel = new Label(gitHash != null ? gitHash : "(not committed yet)");
        Label imgLabel = new Label(imgHash != null ? imgHash.substring(0, 16) + "…" : "(pending)");
        gitLabel.setStyle("-fx-font-family: monospace; -fx-text-fill: #888;");
        imgLabel.setStyle("-fx-font-family: monospace; -fx-text-fill: #888;");

        // Classifier_Fidelity — computed from ActionLogger, shown read-only
        String fidelityColor = switch (fidelity) {
            case HIGH        -> "#a6e3a1"; // green
            case DEGRADED    -> "#fab387"; // orange
            case COMPROMISED -> "#f38ba8"; // red
        };
        Label fidelityLabel = new Label(fidelity.name());
        fidelityLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;"
                              + "-fx-text-fill: " + fidelityColor + ";");

        // ── Layout ───────────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        int row = 0;
        Label validatorLabel = activeLicense != null
            ? styledLabel("Validator  ✓", "#a6e3a1")   // green certified
            : new Label("Validator *");
        grid.add(validatorLabel, 0, row); grid.add(validatorField, 1, row++);
        grid.add(new Label("Scope"),              0, row); grid.add(scopeBox,       1, row++);
        grid.add(new Label("Confidence"),         0, row); grid.add(confidenceBox,  1, row++);
        grid.add(new Label("Notes"),              0, row); grid.add(notesArea,      1, row++);
        grid.add(new Label("Workflow status"),    0, row); grid.add(statusBox,      1, row++);
        grid.add(new Label("Git hash"),           0, row); grid.add(gitLabel,       1, row++);
        grid.add(new Label("Image SHA-256"),      0, row); grid.add(imgLabel,       1, row++);
        grid.add(new Label("Classifier Fidelity"),0, row); grid.add(fidelityLabel,  1, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Disable OK until a validator name is entered (license lock = always valid)
        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(validatorField.getText().trim().isEmpty());
        if (activeLicense == null) {
            validatorField.textProperty().addListener((obs, o, n) ->
                okBtn.setDisable(n.trim().isEmpty()));
        }

        // ── Result converter ─────────────────────────────────────────────────
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String sel = statusBox.getValue();
            int idx = 1;
            for (int i = 0; i < STATUS_LABELS.length; i++)
                if (STATUS_LABELS[i].equals(sel)) { idx = i; break; }
            return new ValidationStamp(
                validatorField.getText().trim(),
                Instant.now(),
                scopeBox.getValue(),
                confidenceBox.getValue(),
                notesArea.getText().trim(),
                gitHash,
                imgHash,
                fidelity.name(),
                idx,
                sel
            );
        });

        return dialog.showAndWait();
    }

    private static javafx.scene.control.Label styledLabel(String text, String hexColor) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
        lbl.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
        return lbl;
    }
}
