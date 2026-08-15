package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Convenience settings row: a caption label on the left and a parameter/control widget on the right.
 *
 * <p>Use {@link PanelRowWidget} directly when either side needs arbitrary custom widgets.</p>
 */
public class SettingRow extends PanelRowWidget {
    public static final float DEFAULT_ROW_HEIGHT = PanelRowWidget.DEFAULT_ROW_HEIGHT;
    public static final float DEFAULT_GAP = PanelRowWidget.DEFAULT_GAP;

    private final Label label = new Label();
    private Widget control;

    public SettingRow() {
        left(label);
    }

    public SettingRow(String labelText, Widget control) {
        this();
        labelText(labelText);
        control(control);
    }

    public SettingRow(RichText labelText, Widget control) {
        this();
        labelText(labelText);
        control(control);
    }

    public Label label() {
        return label;
    }

    public String labelText() {
        return label.text();
    }

    public SettingRow labelText(String labelText) {
        label.text(labelText);
        return this;
    }

    public RichText labelRichText() {
        return label.richText();
    }

    public SettingRow labelText(RichText labelText) {
        label.richText(labelText);
        return this;
    }

    public Widget control() {
        return control;
    }

    public SettingRow control(Widget control) {
        if (this.control == control) return this;
        this.control = control;
        label.focusTarget(control);
        right(control);
        return this;
    }

    @Override
    public SettingRow rowHeight(float rowHeight) {
        super.rowHeight(rowHeight);
        return this;
    }

    @Override
    public SettingRow gap(float gap) {
        super.gap(gap);
        return this;
    }

    public float controlWidth() {
        return rightWidth();
    }

    public SettingRow controlWidth(float controlWidth) {
        rightWidth(controlWidth);
        return this;
    }

    public SettingRow parameterWidth(float width) {
        return controlWidth(width);
    }

    @Override
    public SettingRow rightWidth(float rightWidth) {
        super.rightWidth(rightWidth);
        return this;
    }

    @Override
    public SettingRow leftWidth(float leftWidth) {
        super.leftWidth(leftWidth);
        return this;
    }

    @Override
    public SettingRow leftGap(float leftGap) {
        super.leftGap(leftGap);
        return this;
    }

    @Override
    public SettingRow rightGap(float rightGap) {
        super.rightGap(rightGap);
        return this;
    }

    public SettingRow autoControlWidth() {
        return controlWidth(LayoutConstraints.AUTO);
    }
}