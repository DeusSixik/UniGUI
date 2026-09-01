package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonState;

/** Compact toolbar-friendly button with optional icon, text and command metadata. */
@XmlWidgetName("ToolButton")
public class ToolButton extends Button {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TOOL_BUTTON;

    private String icon = "";
    private String label = "";
    private String tooltip = "";
    private String commandId = "";
    private DisplayMode displayMode = DisplayMode.ICON_AND_TEXT;
    private boolean checked;

    public ToolButton() {
        textPadding(6.0f, 2.0f);
        layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
        refreshText();
    }

    public ToolButton(String label) {
        this();
        label(label);
    }

    public String icon() {
        return icon;
    }

    @XmlAttribute(value = "icon", category = "Content", defaultValue = "", description = "Short icon glyph or icon resource id displayed by the tool button.")
    public ToolButton icon(String icon) {
        String normalized = normalize(icon);
        if (this.icon.equals(normalized)) return this;
        this.icon = normalized;
        refreshText();
        return this;
    }

    public String label() {
        return label;
    }

    @XmlAttribute(value = "label", category = "Content", defaultValue = "", description = "Text label for text or icon+text toolbar modes.")
    public ToolButton label(String label) {
        String normalized = normalize(label);
        if (this.label.equals(normalized)) return this;
        this.label = normalized;
        refreshText();
        return this;
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Alias for the toolbar button label.")
    public ToolButton text(String text) {
        return label(text);
    }

    public String tooltip() {
        return tooltip;
    }

    @XmlAttribute(value = "tooltip", category = "Content", defaultValue = "", description = "Tooltip text shown by hosts that materialize tooltips.")
    public ToolButton tooltip(String tooltip) {
        this.tooltip = normalize(tooltip);
        return this;
    }

    public String commandId() {
        return commandId;
    }

    @XmlAttribute(value = "command", category = "Behavior", defaultValue = "", description = "Command id executed by a command-aware toolbar host.")
    public ToolButton commandId(String commandId) {
        this.commandId = normalize(commandId);
        return this;
    }

    public DisplayMode displayMode() {
        return displayMode;
    }

    @XmlAttribute(value = "displayMode", category = "Appearance", defaultValue = "icon_and_text", description = "Whether the toolbar button renders icon, text or both.")
    public ToolButton displayMode(DisplayMode displayMode) {
        DisplayMode normalized = displayMode == null ? DisplayMode.ICON_AND_TEXT : displayMode;
        if (this.displayMode == normalized) return this;
        this.displayMode = normalized;
        refreshText();
        return this;
    }

    public boolean checked() {
        return checked;
    }

    @XmlAttribute(value = "checked", category = "State", defaultValue = "false", description = "Whether the tool button is rendered as active.")
    public ToolButton checked(boolean checked) {
        if (this.checked == checked) return this;
        this.checked = checked;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public ToolButton enabled(boolean enabled) {
        super.enabled(enabled);
        return this;
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        if (checked) return WidgetState.CHECKED;
        return super.styleState();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.BUTTON,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                textPaddingX(),
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(context, richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                checked,
                false,
                0.0f,
                0.0f,
                0.0f,
                background().copy(),
                borderColor().copy());
    }

    protected void refreshText() {
        super.text(renderText());
    }

    private String renderText() {
        return switch (displayMode) {
            case ICON_ONLY -> !icon.isEmpty() ? icon : label;
            case TEXT_ONLY -> label;
            case ICON_AND_TEXT -> icon.isEmpty() ? label : label.isEmpty() ? icon : icon + " " + label;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public enum DisplayMode {
        ICON_ONLY,
        TEXT_ONLY,
        ICON_AND_TEXT
    }
}
