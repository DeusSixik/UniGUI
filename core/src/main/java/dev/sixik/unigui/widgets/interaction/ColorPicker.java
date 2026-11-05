package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.*;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

@AiDoc(
        summary = "Interactive color selection control providing RGBA sliders, hex input, and popup palette.",
        purpose = "Allows users to inspect and adjust RGB or RGBA color channels visually in an interactive popup.",
        useWhen = {
                "Custom theme customization and color configuration screens.",
                "Particle or render effect tint pickers."
        },
        criticalRules = {
                "Supports packed 32-bit ARGB values and floating-point ColorView instances.",
                "Emits ColorChangedEvent when channels or hex strings are edited."
        },
        related = {"ColorView", "MutableColor", "Slider", "Popup"})
@WikiPage(
        title = "<key:color.picker.title>",
        slug = "color-picker",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:color.picker.summary>",
        order = 220)
@WikiSection(
        title = "<key:color.picker.section.overview.title>",
        markdown = "<key:color.picker.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:color.picker.example.basic.title>",
        code = "<key:color.picker.example.basic.code>",
        order = 10)
@XamlWidget("ColorPicker")
public final class ColorPicker extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.COLOR_PICKER;

    private static final float PICKER_WIDTH = 304.0f;
    private static final float COLOR_PLANE_SIZE = 196.0f;
    private static final float HUE_BAR_WIDTH = 18.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float CHANNEL_FIELD_WIDTH = 42.0f;
    private static final float CHANNEL_LABEL_WIDTH = 18.0f;
    private static final float CHANNEL_SLIDER_WIDTH = 184.0f;

    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final Button preview = new Button("");
    private final Popup popup = new Popup();
    private final VBox pickerPanel = new VBox();
    private final HBox rgbRow = new HBox();
    private final HBox colorRow = new HBox();
    private final VBox swatchColumn = new VBox();
    private final Button currentSwatch = new Button("");
    private final Button originalSwatch = new Button("");
    private final TextField hexField = new HexField();
    private final HBox hexRow = new HBox();
    private final VBox hsvPanel = new VBox();
    private final VBox argbPanel = new VBox();
    private final ColorPlane colorPlane = new ColorPlane();
    private final HueBar hueBar = new HueBar();
    private final ChannelField alpha = channelField();
    private final ChannelField red = channelField();
    private final ChannelField green = channelField();
    private final ChannelField blue = channelField();
    private final Slider alphaSlider = channelSlider();
    private final Slider redSlider = channelSlider();
    private final Slider greenSlider = channelSlider();
    private final Slider blueSlider = channelSlider();
    private final List<Label> styleLabels = new ObjectArrayList<>();
    private final Label hueReadout = styledLabel("H: 0");
    private final Label saturationReadout = styledLabel("S: 0");
    private final Label valueReadout = styledLabel("V: 255");
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private Type type = Type.HSV;
    private boolean syncing;
    private float hue;
    private float saturation;
    private float value = 1.0f;
    private MutableColor originalColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    public ColorPicker() {
        super(Orientation.VERTICAL);
        spacing(0.0f);

        preview.themeEnabled(false);
        preview.backgroundVisible(true);
        preview.borderVisible(true);
        preview.radius(3.0f);
        preview.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(1).flexShrink(1.0f));
        preview.onClick(event -> togglePopup());
        addChild(preview);

        alpha.onTextChanged(event -> updateFromFields(false));
        red.onTextChanged(event -> updateFromFields(false));
        green.onTextChanged(event -> updateFromFields(false));
        blue.onTextChanged(event -> updateFromFields(false));
        hexField.onTextChanged(event -> updateFromHex());

        alphaSlider.onValueChanged(event -> updateChannelFromSlider(alpha, event.newValue()));
        redSlider.onValueChanged(event -> updateChannelFromSlider(red, event.newValue()));
        greenSlider.onValueChanged(event -> updateChannelFromSlider(green, event.newValue()));
        blueSlider.onValueChanged(event -> updateChannelFromSlider(blue, event.newValue()));
        buildPickerPanel();
        popup.anchor(preview);
        popup.content(pickerPanel);
        popup.padding(EdgeInsets.all(6.0f));
        popup.closeOnOutsideClick(true);

        syncFieldsAndSliders();
        type(Type.HSV);
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        ColorView textColor = styleValue(StyleKeys.TEXT_COLOR, (ColorView) null);
        if (textColor != null) {
            for (Label label : styleLabels) label.color(textColor);
            alpha.textColor().set(textColor);
            red.textColor().set(textColor);
            green.textColor().set(textColor);
            blue.textColor().set(textColor);
            hexField.textColor().set(textColor);
        }

        ColorView swatchBorder = styleValue(StyleKeys.COLOR_PICKER_SWATCH_BORDER_COLOR,
                styleValue(StyleKeys.BORDER_COLOR, (ColorView) null));
        if (swatchBorder != null) {
            preview.borderColor().set(swatchBorder);
            currentSwatch.borderColor().set(swatchBorder);
            originalSwatch.borderColor().set(swatchBorder);
        }
    }

    @WikiMethod(
            title = "<key:color.picker.method.color.title>",
            summary = "<key:color.picker.method.color.summary>")
    public ColorView color() {
        return color;
    }

    @WikiMethod(
            title = "<key:color.picker.method.argb.title>",
            summary = "<key:color.picker.method.argb.summary>")
    public int argb() {
        return (channel(color.a()) << 24)
                | (channel(color.r()) << 16)
                | (channel(color.g()) << 8)
                | channel(color.b());
    }

    @XamlProperty(value = "color", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Selected color parsed from XML color syntax.")
    @WikiMethod(
            title = "<key:color.picker.method.set_color.title>",
            summary = "<key:color.picker.method.set_color.summary>")
    public ColorPicker color(ColorView color) {
        if (color != null) {
            setColor(color.r(), color.g(), color.b(), color.a(), false);
        }
        return this;
    }

    @XamlProperty(value = "argb", category = "Appearance", defaultValue = "-1", description = "Selected color as packed ARGB integer.")
    @WikiMethod(
            title = "<key:color.picker.method.set_argb.title>",
            summary = "<key:color.picker.method.set_argb.summary>")
    public ColorPicker argb(int argb) {
        return rgba255(
                (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF);
    }

    @WikiMethod(
            title = "<key:color.picker.method.rgba255.title>",
            summary = "<key:color.picker.method.rgba255.summary>")
    public ColorPicker rgba255(int red, int green, int blue, int alpha) {
        setColor(
                clamp255(red) / 255.0f,
                clamp255(green) / 255.0f,
                clamp255(blue) / 255.0f,
                clamp255(alpha) / 255.0f,
                false);
        return this;
    }

    @WikiMethod(
            title = "<key:color.picker.method.type.title>",
            summary = "<key:color.picker.method.type.summary>")
    public Type type() {
        return type;
    }

    @XamlProperty(value = "type", category = "Behavior", defaultValue = "hsv", description = "Visible picker editing mode.")
    @WikiMethod(
            title = "<key:color.picker.method.set_type.title>",
            summary = "<key:color.picker.method.set_type.summary>")
    public ColorPicker type(Type type) {
        Type normalized = type == null ? Type.HSV : type;
        this.type = normalized;
        hsvPanel.visibility(normalized == Type.HSV ? Visibility.VISIBLE : Visibility.COLLAPSED);
        argbPanel.visibility(normalized == Type.ARGB ? Visibility.VISIBLE : Visibility.COLLAPSED);
        pickerPanel.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        popup.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:color.picker.method.preview.title>",
            summary = "<key:color.picker.method.preview.summary>")
    public Button preview() {
        return preview;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public ColorPicker overlayLayer(OverlayLayer overlayLayer) {
        if (explicitOverlayLayer == overlayLayer) return this;
        detachPopup();
        explicitOverlayLayer = overlayLayer;
        syncPopupAttachment();
        return this;
    }

    @WikiMethod(
            title = "<key:color.picker.method.popup.title>",
            summary = "<key:color.picker.method.popup.summary>")
    public Popup popup() {
        return popup;
    }

    @Override
    public ColorPicker renderer(WidgetRender renderer) {
        super.renderer(renderer);
        colorPlane.invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:color.picker.method.on_color_changed.title>",
            summary = "<key:color.picker.method.on_color_changed.summary>")
    public EventSubscription onColorChanged(EventListener<? super ColorChangedEvent> listener) {
        return on(ColorChangedEvent.TYPE, listener);
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncPopupAttachment();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncPopupAttachment();
    }

    private void togglePopup() {
        syncPopupAttachment();
        if (attachedOverlayLayer == null) return;
        if (!popup.opened()) {
            originalColor.set(color);
            updateSwatches();
        }
        popup.toggle();
    }

    private void syncPopupAttachment() {
        OverlayLayer target = overlayLayer();
        if (target == null) {
            detachPopup();
            return;
        }
        if (attachedOverlayLayer == target) return;
        detachPopup();
        attachedOverlayLayer = target;
        attachedOverlayLayer.addOverlay(popup);
    }

    private void detachPopup() {
        if (attachedOverlayLayer != null) {
            attachedOverlayLayer.removeOverlay(popup);
            attachedOverlayLayer = null;
        }
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private void buildPickerPanel() {
        pickerPanel.spacing(4.0f);
        pickerPanel.layout(style -> style.size(PICKER_WIDTH, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        rgbRow.spacing(4.0f);
        rgbRow.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        rgbRow.addChild(channelValue("R", red));
        rgbRow.addChild(channelValue("G", green));
        rgbRow.addChild(channelValue("B", blue));

        hexRow.spacing(6.0f);
        hexRow.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label hexLabel = styledLabel("HEX");
        hexLabel.layout(style -> style.size(34.0f, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        hexField.maxLength(9);
        hexField.layout(style -> style.size(104.0f, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        hexRow.addChild(hexLabel);
        hexRow.addChild(hexField);

        hsvPanel.spacing(4.0f);
        hsvPanel.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        colorRow.spacing(5.0f);
        colorRow.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        colorPlane.layout(style -> style.size(COLOR_PLANE_SIZE, COLOR_PLANE_SIZE).flexGrow(0).flexShrink(0.0f));
        hueBar.layout(style -> style.size(HUE_BAR_WIDTH, COLOR_PLANE_SIZE).flexGrow(0).flexShrink(0.0f));
        swatchColumn.spacing(3.0f);
        swatchColumn.layout(style -> style.size(72.0f, COLOR_PLANE_SIZE).flexGrow(0).flexShrink(0.0f));
        currentSwatch.layout(style -> style.size(72.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        originalSwatch.layout(style -> style.size(72.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        currentSwatch.radius(1.0f);
        originalSwatch.radius(1.0f);
        originalSwatch.onClick(event -> setColor(
                originalColor.r(), originalColor.g(), originalColor.b(), originalColor.a(), true));
        Label currentLabel = styledLabel("Current");
        Label originalLabel = styledLabel("Original");
        currentLabel.layout(style -> style.size(72.0f, 14.0f).flexGrow(0).flexShrink(0.0f));
        originalLabel.layout(style -> style.size(72.0f, 14.0f).flexGrow(0).flexShrink(0.0f));
        swatchColumn.addChild(currentLabel);
        swatchColumn.addChild(currentSwatch);
        swatchColumn.addChild(originalLabel);
        swatchColumn.addChild(originalSwatch);
        colorRow.addChild(colorPlane);
        colorRow.addChild(hueBar);
        colorRow.addChild(swatchColumn);
        hsvPanel.addChild(colorRow);
        hsvPanel.addChild(hsvValueRow());

        argbPanel.spacing(5.0f);
        argbPanel.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        argbPanel.addChild(channelRow("A", alpha, alphaSlider));
        argbPanel.addChild(channelRow("R", red, redSlider));
        argbPanel.addChild(channelRow("G", green, greenSlider));
        argbPanel.addChild(channelRow("B", blue, blueSlider));

        pickerPanel.addChild(rgbRow);
        pickerPanel.addChild(hexRow);
        pickerPanel.addChild(hsvPanel);
        pickerPanel.addChild(argbPanel);
    }

    private HBox channelValue(String label, ChannelField field) {
        HBox value = new HBox();
        value.spacing(3.0f);
        value.layout(style -> style.flexGrow(1).flexShrink(0.0f));
        Label name = styledLabel(label);
        name.layout(style -> style.size(14.0f, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        field.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        value.addChild(name);
        value.addChild(field);
        return value;
    }

    private HBox hsvValueRow() {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        row.addChild(hueReadout);
        row.addChild(saturationReadout);
        row.addChild(valueReadout);
        return row;
    }

    private void updateFromFields(boolean normalizeText) {
        if (syncing) return;
        MutableColor oldColor = color.copy();
        int a = alpha.value255();
        int r = red.value255();
        int g = green.value255();
        int b = blue.value255();
        color.set(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
        syncHsvFromColor();
        if (normalizeText) {
            syncFieldsAndSliders();
        } else {
            syncSliders();
            updatePreview();
        }
        invalidate(InvalidationFlags.VISUAL);
        dispatchColorChanged(oldColor);
    }

    private void updateFromHex() {
        if (syncing) return;
        String text = hexField.text();
        if (text == null) return;
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6 && digits.length() != 8) return;
        try {
            long parsed = Long.parseLong(digits, 16);
            int r = (int) ((parsed >>> (digits.length() == 8 ? 24 : 16)) & 0xFF);
            int g = (int) ((parsed >>> (digits.length() == 8 ? 16 : 8)) & 0xFF);
            int b = (int) ((parsed >>> (digits.length() == 8 ? 8 : 0)) & 0xFF);
            int a = digits.length() == 8 ? (int) (parsed & 0xFF) : 255;
            setColor(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f, true);
        } catch (NumberFormatException ignored) {
            // Incomplete text remains editable until it forms a complete hex color.
        }
    }

    private void updateChannelFromSlider(ChannelField field, float value) {
        if (syncing) return;
        syncing = true;
        try {
            field.value255(Math.round(value));
        } finally {
            syncing = false;
        }
        updateFromFields(false);
    }

    private void updateFromHsv() {
        MutableColor oldColor = color.copy();
        float[] rgb = hsvToRgb(hue, saturation, value);
        color.set(rgb[0], rgb[1], rgb[2], alpha.value255() / 255.0f);
        syncFieldsAndSliders();
        invalidate(InvalidationFlags.VISUAL);
        dispatchColorChanged(oldColor);
    }

    private void setColor(float red, float green, float blue, float alpha, boolean emitChange) {
        MutableColor oldColor = color.copy();
        color.set(clamp01(red), clamp01(green), clamp01(blue), clamp01(alpha));
        syncFieldsAndSliders();
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            dispatchColorChanged(oldColor);
        }
    }

    private ColorChangedEvent dispatchColorChanged(ColorView oldColor) {
        if (sameColor(oldColor, color)) return null;
        ColorChangedEvent event = new ColorChangedEvent(this, oldColor, color);
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private boolean renderPlaneCustom(DrawScope draw) {
        WidgetRender custom = customRender();
        if (custom == null) return false;
        custom.render(draw, this);
        return true;
    }

    private void syncFieldsAndSliders() {
        syncing = true;
        try {
            alpha.value255(channel(color.a()));
            red.value255(channel(color.r()));
            green.value255(channel(color.g()));
            blue.value255(channel(color.b()));
            hexField.text(formatHex(color));
            syncHsvFromColor();
            syncSlidersUnchecked();
        } finally {
            syncing = false;
        }
        updatePreview();
    }

    private void syncSliders() {
        syncing = true;
        try {
            syncSlidersUnchecked();
        } finally {
            syncing = false;
        }
    }

    private void syncSlidersUnchecked() {
        alphaSlider.value(alpha.value255());
        redSlider.value(red.value255());
        greenSlider.value(green.value255());
        blueSlider.value(blue.value255());
        hueBar.invalidate(InvalidationFlags.VISUAL);
        hueReadout.text("H: " + Math.round(hue));
        saturationReadout.text("S: " + Math.round(saturation * 255.0f));
        valueReadout.text("V: " + Math.round(value * 255.0f));
        colorPlane.invalidate(InvalidationFlags.VISUAL);
    }

    private void syncHsvFromColor() {
        float previousHue = hue;
        float[] hsv = rgbToHsv(color.r(), color.g(), color.b());
        hue = hsv[1] <= 0.00001f ? previousHue : hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    private void updatePreview() {
        preview.background().set(color);
        currentSwatch.background().set(color);
        MutableColor swatchBorder = swatchBorderColor();
        preview.borderColor().set(swatchBorder);
        preview.textColor().set(1.0f, 1.0f, 1.0f, 0.0f);
        if (!syncing) {
            syncing = true;
            try {
                hexField.text(formatHex(color));
            } finally {
                syncing = false;
            }
        }
        updateSwatches();
    }

    private void updateSwatches() {
        currentSwatch.background().set(color);
        originalSwatch.background().set(originalColor);
        MutableColor border = swatchBorderColor();
        currentSwatch.borderColor().set(border);
        originalSwatch.borderColor().set(border);
    }

    private MutableColor swatchBorderColor() {
        ColorView border = styleValue(StyleKeys.COLOR_PICKER_SWATCH_BORDER_COLOR,
                styleValue(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.85f, 0.90f, 1.0f, 0.85f)));
        return MutableColor.rgba(border.r(), border.g(), border.b(), border.a());
    }

    private ColorView selectionColor() {
        return styleValue(StyleKeys.COLOR_PICKER_SELECTION_COLOR,
                styleValue(StyleKeys.ACCENT_COLOR, MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
    }

    private static String formatHex(ColorView color) {
        return String.format(java.util.Locale.ROOT, "#%02X%02X%02X%02X",
                channel(color.r()), channel(color.g()), channel(color.b()), channel(color.a()));
    }

    private ChannelField channelField() {
        ChannelField field = new ChannelField();
        field.layout(style -> style.size(CHANNEL_FIELD_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        return field;
    }

    private static Slider channelSlider() {
        Slider slider = new Slider().range(0.0f, 255.0f).step(1.0f).value(255.0f);
        slider.layout(style -> style.size(CHANNEL_SLIDER_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        return slider;
    }

    private HBox channelRow(String label, ChannelField field, Slider slider) {
        HBox row = new HBox();
        row.spacing(5.0f);
        row.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label text = styledLabel(label);
        text.layout(style -> style.size(CHANNEL_LABEL_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(field);
        row.addChild(slider);
        return row;
    }

    private Label styledLabel(String text) {
        Label label = new Label(text);
        styleLabels.add(label);
        return label;
    }

    private static int channel(float value) {
        return clamp255(Math.round(value * 255.0f));
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static MutableColor mix(ColorView a, ColorView b, float t) {
        float normalized = Math.max(0.0f, Math.min(1.0f, t));
        return MutableColor.rgba(
                a.r() + (b.r() - a.r()) * normalized,
                a.g() + (b.g() - a.g()) * normalized,
                a.b() + (b.b() - a.b()) * normalized,
                a.a() + (b.a() - a.a()) * normalized);
    }

    private static float[] hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0f) + 360.0f) % 360.0f;
        float c = value * saturation;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = value - c;
        float r;
        float g;
        float b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }
        return new float[]{r + m, g + m, b + m};
    }

    private static float[] rgbToHsv(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        float hue;
        if (delta <= 0.00001f) {
            hue = 0.0f;
        } else if (max == red) {
            hue = 60.0f * (((green - blue) / delta) % 6.0f);
        } else if (max == green) {
            hue = 60.0f * (((blue - red) / delta) + 2.0f);
        } else {
            hue = 60.0f * (((red - green) / delta) + 4.0f);
        }
        if (hue < 0.0f) hue += 360.0f;
        float saturation = max <= 0.00001f ? 0.0f : delta / max;
        return new float[]{hue, saturation, max};
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static boolean sameColor(ColorView left, ColorView right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.r() == right.r()
                && left.g() == right.g()
                && left.b() == right.b()
                && left.a() == right.a();
    }

    public enum Type {
        HSV,
        ARGB
    }

    public enum Part {
        COLOR_PLANE
    }

    private final class ChannelField extends TextField {
        private ChannelField() {
            maxLength(3);
            text("255");
        }

        private int value255() {
            String text = text();
            if (text == null || text.isBlank()) return 0;
            try {
                return clamp255(Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private void value255(int value) {
            text(Integer.toString(clamp255(value)));
        }

        @Override
        public void handle(Event event) {
            if (event instanceof TextInputEvent input && !isDigit(input.codePoint())) {
                event.cancel();
                return;
            }
            super.handle(event);
            if (event instanceof FocusLostEvent) {
                value255(value255());
                updateFromFields(false);
            }
        }

        @Override
        protected String sanitizeTextInput(String text) {
            StringBuilder builder = new StringBuilder(text.length());
            text.codePoints()
                    .filter(ChannelField::isDigit)
                    .limit(3)
                    .forEach(builder::appendCodePoint);
            return builder.toString();
        }

        private static boolean isDigit(int codePoint) {
            return codePoint >= '0' && codePoint <= '9';
        }
    }

    private final class HexField extends TextField {
        private HexField() {
            maxLength(9);
            text("#FFFFFFFF");
        }

        @Override
        protected String sanitizeTextInput(String text) {
            StringBuilder builder = new StringBuilder(Math.min(text.length(), 9));
            text.codePoints()
                    .filter(codePoint -> codePoint == '#' && builder.isEmpty() || isHexDigit(codePoint))
                    .limit(9)
                    .forEach(builder::appendCodePoint);
            return builder.toString();
        }

        private boolean isHexDigit(int codePoint) {
            return codePoint >= '0' && codePoint <= '9'
                    || codePoint >= 'a' && codePoint <= 'f'
                    || codePoint >= 'A' && codePoint <= 'F';
        }
    }

    private final class ColorPlane extends WidgetBase {
        private boolean dragging;

        @Override
        public void measure(dev.sixik.unigui.api.layout.LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, COLOR_PLANE_SIZE, COLOR_PLANE_SIZE));
        }

        @Override
        public void handle(Event event) {
            if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;
            if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
                dragging = true;
                update(pointer.rootX(), pointer.rootY());
                UIContext context = uiContext();
                if (context != null) {
                    context.capturePointer(pointer.pointerId(), this);
                }
                event.cancel();
            } else if (event instanceof PointerMovedEvent pointer && dragging) {
                update(pointer.rootX(), pointer.rootY());
                event.cancel();
            } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
                update(pointer.rootX(), pointer.rootY());
                dragging = false;
                UIContext context = uiContext();
                if (context != null) {
                    context.releasePointer(pointer.pointerId(), this);
                }
                event.cancel();
            }
        }

        @Override
        public void render(RenderContext context) {
            if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;

            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                return;
            }
            if (ColorPicker.this.renderPlaneCustom(draw)) {
                return;
            }
            renderDefaultPlane(draw);
        }

        private void renderDefaultPlane(DrawScope draw) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            if (width <= 0.0f || height <= 0.0f) return;

            int steps = 32;
            float[] hueRgb = hsvToRgb(hue, 1.0f, 1.0f);
            MutableColor hueColor = MutableColor.rgba(hueRgb[0], hueRgb[1], hueRgb[2], 1.0f);
            MutableColor white = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
            MutableColor black = MutableColor.rgba(0.0f, 0.0f, 0.0f, 1.0f);
            for (int i = 0; i < steps; i++) {
                float leftT = i / (float) steps;
                float rightT = (i + 1) / (float) steps;
                MutableColor topLeft = mix(white, hueColor, leftT);
                MutableColor topRight = mix(white, hueColor, rightT);
                float x1 = x + width * leftT;
                float x2 = x + width * rightT;
                draw.addRectFilledMultiColor(x1, y, x2 - x1, height, topLeft, topRight, black, black);
            }
            ColorView planeBorder = styleValue(StyleKeys.COLOR_PICKER_PLANE_BORDER_COLOR,
                    styleValue(StyleKeys.BORDER_COLOR,
                            MutableColor.rgba(0.85f, 0.92f, 1.0f, enabled() ? 0.75f : 0.35f)));
            draw.rect(x, y, width, height, Paint.stroke(planeBorder, 1.0f));
            float cx = x + saturation * width;
            float cy = y + (1.0f - value) * height;
            float radius = dragging || hovered() ? 4.5f : 4.0f;
            ColorView selection = selectionColor();
            draw.addCircle(cx, cy, radius, MutableColor.rgba(0.0f, 0.0f, 0.0f, enabled() ? 0.90f : 0.45f), 16, 2.0f);
            draw.addCircle(cx, cy, Math.max(1.0f, radius - 1.0f), selection, 16, 1.0f);
        }

        private void update(float rootX, float rootY) {
            float width = Math.max(1.0f, layoutBounds().width());
            float height = Math.max(1.0f, layoutBounds().height());
            saturation = clamp01((rootX - layoutBounds().x()) / width);
            value = clamp01(1.0f - (rootY - layoutBounds().y()) / height);
            updateFromHsv();
        }

    }

    private final class HueBar extends WidgetBase {
        private boolean dragging;

        @Override
        public void measure(dev.sixik.unigui.api.layout.LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, HUE_BAR_WIDTH, COLOR_PLANE_SIZE));
        }

        @Override
        public void handle(Event event) {
            if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;
            if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
                dragging = true;
                update(pointer.rootY());
                UIContext context = uiContext();
                if (context != null) context.capturePointer(pointer.pointerId(), this);
                event.cancel();
            } else if (event instanceof PointerMovedEvent pointer && dragging) {
                update(pointer.rootY());
                event.cancel();
            } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
                update(pointer.rootY());
                dragging = false;
                UIContext context = uiContext();
                if (context != null) context.releasePointer(pointer.pointerId(), this);
                event.cancel();
            }
        }

        @Override
        public void render(RenderContext context) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            if (width <= 0.0f || height <= 0.0f) return;
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            for (int i = 0; i < 36; i++) {
                float top = i / 36.0f;
                float bottom = (i + 1) / 36.0f;
                MutableColor topColor = hueColor(top * 360.0f);
                MutableColor bottomColor = hueColor(bottom * 360.0f);
                draw.addRectFilledMultiColor(x, y + height * top, width, height / 36.0f + 1.0f,
                        topColor, topColor, bottomColor, bottomColor);
            }
            float markerY = y + hue / 360.0f * height;
            draw.addLine(x - 2.0f, markerY, x + width + 2.0f, markerY, selectionColor(), 2.0f);
        }

        private void update(float rootY) {
            float height = Math.max(1.0f, layoutBounds().height());
            hue = clamp01((rootY - layoutBounds().y()) / height) * 360.0f;
            updateFromHsv();
        }
    }

    private static MutableColor hueColor(float hue) {
        float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
        return MutableColor.rgba(rgb[0], rgb[1], rgb[2], 1.0f);
    }
}
