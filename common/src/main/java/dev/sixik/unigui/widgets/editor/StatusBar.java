package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsModel;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;

/** Compact editor status strip for document, diagnostics, selection and zoom state. */
@XmlWidgetName("StatusBar")
public class StatusBar extends LinearBox {
    private final Label dirtyLabel = new Label("Saved");
    private final Label modeLabel = new Label("Mode: Design");
    private final Label diagnosticsLabel = new Label("Diagnostics: OK");
    private final Label selectedPathLabel = new Label("Selection: none");
    private final Label scaleLabel = new Label("Scale: 100%");

    private boolean dirty;
    private String mode = "Design";
    private String selectedNodePath = "";
    private float viewScale = 1.0f;
    private int errorCount;
    private int warningCount;

    public StatusBar() {
        super(Orientation.HORIZONTAL);
        spacing(10.0f);
        layout(style -> style.height(20.0f).width(LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        addStatusLabel(dirtyLabel, 74.0f, 0.0f);
        addStatusLabel(modeLabel, 108.0f, 0.0f);
        addStatusLabel(diagnosticsLabel, 144.0f, 0.0f);
        addStatusLabel(selectedPathLabel, 120.0f, 1.0f);
        addStatusLabel(scaleLabel, 74.0f, 0.0f);
        applyQueuedMutations();
        refreshLabels();
    }

    public boolean dirty() {
        return dirty;
    }

    @XmlAttribute(value = "dirty", category = "State", defaultValue = "false", description = "Whether the current XML project has unsaved changes.")
    public StatusBar dirty(boolean dirty) {
        if (this.dirty == dirty) return this;
        this.dirty = dirty;
        refreshLabels();
        return this;
    }

    public String mode() {
        return mode;
    }

    @XmlAttribute(value = "mode", category = "State", defaultValue = "Design", description = "Current editor mode shown in the status strip.")
    public StatusBar mode(String mode) {
        String normalized = normalize(mode, "Design");
        if (this.mode.equals(normalized)) return this;
        this.mode = normalized;
        refreshLabels();
        return this;
    }

    public String selectedNodePath() {
        return selectedNodePath;
    }

    @XmlAttribute(value = "selectedNodePath", category = "State", defaultValue = "", description = "Selected XML node path, or empty when nothing is selected.")
    public StatusBar selectedNodePath(String selectedNodePath) {
        String normalized = selectedNodePath == null ? "" : selectedNodePath.trim();
        if (this.selectedNodePath.equals(normalized)) return this;
        this.selectedNodePath = normalized;
        refreshLabels();
        return this;
    }

    public StatusBar selectedNodePath(XmlWidgetNodePath selectedNodePath) {
        return selectedNodePath(selectedNodePath == null ? "" : selectedNodePath.toString());
    }

    public float viewScale() {
        return viewScale;
    }

    @XmlAttribute(value = "viewScale", category = "State", defaultValue = "1", description = "Current design canvas zoom factor, where 1 is 100 percent.")
    public StatusBar viewScale(float viewScale) {
        float normalized = Float.isFinite(viewScale) && viewScale > 0.0f ? viewScale : 1.0f;
        if (this.viewScale == normalized) return this;
        this.viewScale = normalized;
        refreshLabels();
        return this;
    }

    public int errorCount() {
        return errorCount;
    }

    @XmlAttribute(value = "errorCount", category = "State", defaultValue = "0", description = "Number of XML errors shown in the compact diagnostics summary.")
    public StatusBar errorCount(int errorCount) {
        int normalized = Math.max(0, errorCount);
        if (this.errorCount == normalized) return this;
        this.errorCount = normalized;
        refreshLabels();
        return this;
    }

    public int warningCount() {
        return warningCount;
    }

    @XmlAttribute(value = "warningCount", category = "State", defaultValue = "0", description = "Number of XML warnings shown in the compact diagnostics summary.")
    public StatusBar warningCount(int warningCount) {
        int normalized = Math.max(0, warningCount);
        if (this.warningCount == normalized) return this;
        this.warningCount = normalized;
        refreshLabels();
        return this;
    }

    public StatusBar diagnostics(XmlWidgetDiagnosticsModel model) {
        XmlWidgetDiagnosticsModel normalized = model == null ? XmlWidgetDiagnosticsModel.empty() : model;
        int errors = normalized.errorCount();
        int warnings = (int) normalized.entries().stream()
                .filter(entry -> entry.severity() == XmlWidgetDiagnosticsModel.Severity.WARNING)
                .count();
        if (errorCount == errors && warningCount == warnings) return this;
        errorCount = errors;
        warningCount = warnings;
        refreshLabels();
        return this;
    }

    public Label dirtyLabel() {
        return dirtyLabel;
    }

    public Label modeLabel() {
        return modeLabel;
    }

    public Label diagnosticsLabel() {
        return diagnosticsLabel;
    }

    public Label selectedPathLabel() {
        return selectedPathLabel;
    }

    public Label scaleLabel() {
        return scaleLabel;
    }

    private void addStatusLabel(Label label, float minWidth, float flexGrow) {
        label.layout(style -> style.height(20.0f).minWidth(minWidth).flexGrow(flexGrow).flexShrink(0.0f));
        addChild(label);
    }

    private void refreshLabels() {
        dirtyLabel.text(dirty ? "Unsaved *" : "Saved");
        modeLabel.text("Mode: " + mode);
        diagnosticsLabel.text(errorCount == 0 && warningCount == 0
                ? "Diagnostics: OK"
                : "Errors: " + errorCount + ", Warnings: " + warningCount);
        selectedPathLabel.text(selectedNodePath.isEmpty() ? "Selection: none" : "Selection: " + selectedNodePath);
        scaleLabel.text("Scale: " + Math.round(viewScale * 100.0f) + "%");
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
