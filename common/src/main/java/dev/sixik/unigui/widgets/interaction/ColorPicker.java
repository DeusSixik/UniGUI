package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.ColorChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.ColorPickerRenderer;
import dev.sixik.unigui.widgets.render.ColorPickerState;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.Popup;

@XmlWidgetName("ColorPicker")
public final class ColorPicker extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.COLOR_PICKER;

    private static final float PICKER_WIDTH = 224.0f;
    private static final float COLOR_PLANE_HEIGHT = 128.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float CHANNEL_FIELD_WIDTH = 42.0f;
    private static final float CHANNEL_LABEL_WIDTH = 18.0f;
    private static final float CHANNEL_SLIDER_WIDTH = 154.0f;
    private static final float HSV_LABEL_WIDTH = 42.0f;
    private static final float HSV_SLIDER_WIDTH = 176.0f;

    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final Button preview = new Button("");
    private final Popup popup = new Popup();
    private final VBox pickerPanel = new VBox();
    private final HBox modeRow = new HBox();
    private final Button hsvModeButton = new Button("HSV");
    private final Button argbModeButton = new Button("ARGB");
    private final VBox hsvPanel = new VBox();
    private final VBox argbPanel = new VBox();
    private final ColorPlane colorPlane = new ColorPlane();
    private final Slider hueSlider = new Slider().range(0.0f, 360.0f).step(1.0f).value(0.0f);
    private final ChannelField alpha = channelField();
    private final ChannelField red = channelField();
    private final ChannelField green = channelField();
    private final ChannelField blue = channelField();
    private final Slider alphaSlider = channelSlider();
    private final Slider redSlider = channelSlider();
    private final Slider greenSlider = channelSlider();
    private final Slider blueSlider = channelSlider();
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private ColorPickerRenderer renderer;
    private Type type = Type.HSV;
    private boolean syncing;
    private float hue;
    private float saturation;
    private float value = 1.0f;

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

        alphaSlider.onValueChanged(event -> updateChannelFromSlider(alpha, event.newValue()));
        redSlider.onValueChanged(event -> updateChannelFromSlider(red, event.newValue()));
        greenSlider.onValueChanged(event -> updateChannelFromSlider(green, event.newValue()));
        blueSlider.onValueChanged(event -> updateChannelFromSlider(blue, event.newValue()));
        hueSlider.onValueChanged(event -> {
            if (syncing) return;
            hue = event.newValue();
            updateFromHsv();
        });

        buildPickerPanel();
        popup.anchor(preview);
        popup.content(pickerPanel);
        popup.padding(EdgeInsets.all(6.0f));
        popup.closeOnOutsideClick(true);

        syncFieldsAndSliders();
        type(Type.HSV);
    }

    public ColorView color() {
        return color;
    }

    public int argb() {
        return (channel(color.a()) << 24)
                | (channel(color.r()) << 16)
                | (channel(color.g()) << 8)
                | channel(color.b());
    }

    @XmlAttribute(value = "color", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Selected color parsed from XML color syntax.")
    public ColorPicker color(ColorView color) {
        if (color != null) {
            setColor(color.r(), color.g(), color.b(), color.a(), false);
        }
        return this;
    }

    @XmlAttribute(value = "argb", category = "Appearance", defaultValue = "-1", description = "Selected color as packed ARGB integer.")
    public ColorPicker argb(int argb) {
        return rgba255(
                (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF);
    }

    public ColorPicker rgba255(int red, int green, int blue, int alpha) {
        setColor(
                clamp255(red) / 255.0f,
                clamp255(green) / 255.0f,
                clamp255(blue) / 255.0f,
                clamp255(alpha) / 255.0f,
                false);
        return this;
    }

    public Type type() {
        return type;
    }

    @XmlAttribute(value = "type", category = "Behavior", defaultValue = "hsv", description = "Visible picker editing mode.")
    public ColorPicker type(Type type) {
        Type normalized = type == null ? Type.HSV : type;
        this.type = normalized;
        syncModeButtons();
        hsvPanel.visibility(normalized == Type.HSV ? Visibility.VISIBLE : Visibility.COLLAPSED);
        argbPanel.visibility(normalized == Type.ARGB ? Visibility.VISIBLE : Visibility.COLLAPSED);
        pickerPanel.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        popup.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

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

    public Popup popup() {
        return popup;
    }

    public ColorPickerRenderer renderer() {
        return renderer;
    }

    public ColorPicker renderer(ColorPickerRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        colorPlane.invalidate(InvalidationFlags.VISUAL);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ColorPicker useDefaultRenderer() {
        return renderer(null);
    }

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
        pickerPanel.spacing(6.0f);
        pickerPanel.layout(style -> style.size(PICKER_WIDTH, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        hsvModeButton.layout(style -> style.size(58.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        argbModeButton.layout(style -> style.size(66.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        hsvModeButton.themeEnabled(false).radius(3.0f);
        argbModeButton.themeEnabled(false).radius(3.0f);
        hsvModeButton.onClick(event -> type(Type.HSV));
        argbModeButton.onClick(event -> type(Type.ARGB));

        modeRow.spacing(4.0f);
        modeRow.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label modeLabel = new Label("Type");
        modeLabel.layout(style -> style.size(44.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        modeRow.addChild(modeLabel);
        modeRow.addChild(hsvModeButton);
        modeRow.addChild(argbModeButton);

        hsvPanel.spacing(5.0f);
        hsvPanel.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        colorPlane.layout(style -> style.size(PICKER_WIDTH, COLOR_PLANE_HEIGHT).flexGrow(0).flexShrink(0.0f));
        hueSlider.layout(style -> style.size(HSV_SLIDER_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        hsvPanel.addChild(colorPlane);
        hsvPanel.addChild(sliderRow("Hue", hueSlider));

        argbPanel.spacing(5.0f);
        argbPanel.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        argbPanel.addChild(channelRow("A", alpha, alphaSlider));
        argbPanel.addChild(channelRow("R", red, redSlider));
        argbPanel.addChild(channelRow("G", green, greenSlider));
        argbPanel.addChild(channelRow("B", blue, blueSlider));

        pickerPanel.addChild(modeRow);
        pickerPanel.addChild(hsvPanel);
        pickerPanel.addChild(argbPanel);
    }

    private void syncModeButtons() {
        styleModeButton(hsvModeButton, type == Type.HSV);
        styleModeButton(argbModeButton, type == Type.ARGB);
    }

    private static void styleModeButton(Button button, boolean selected) {
        button.background().set(selected ? 0.18f : 0.07f, selected ? 0.45f : 0.08f, selected ? 0.75f : 0.10f, 1.0f);
        button.borderColor().set(selected ? 0.35f : 0.18f, selected ? 0.85f : 0.25f, selected ? 1.0f : 0.32f, selected ? 0.95f : 0.70f);
        button.textColor().set(selected ? 0.95f : 0.72f, selected ? 0.98f : 0.82f, 1.0f, 1.0f);
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

    private ColorPickerRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(ColorPickerRenderer.class, WidgetsRender.colorPicker()) : renderer;
    }

    private void syncFieldsAndSliders() {
        syncing = true;
        try {
            alpha.value255(channel(color.a()));
            red.value255(channel(color.r()));
            green.value255(channel(color.g()));
            blue.value255(channel(color.b()));
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
        hueSlider.value(Math.round(hue));
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
        preview.borderColor().set(0.85f, 0.90f, 1.0f, 0.85f);
        preview.textColor().set(1.0f, 1.0f, 1.0f, 0.0f);
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

    private static HBox sliderRow(String label, Slider slider) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label text = new Label(label);
        text.layout(style -> style.size(HSV_LABEL_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(slider);
        return row;
    }

    private static HBox channelRow(String label, ChannelField field, Slider slider) {
        HBox row = new HBox();
        row.spacing(5.0f);
        row.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        Label text = new Label(label);
        text.layout(style -> style.size(CHANNEL_LABEL_WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        row.addChild(text);
        row.addChild(field);
        row.addChild(slider);
        return row;
    }

    private static int channel(float value) {
        return clamp255(Math.round(value * 255.0f));
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
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

    private final class ColorPlane extends WidgetBase {
        private boolean dragging;

        @Override
        public void measure(dev.sixik.unigui.api.layout.LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, PICKER_WIDTH, COLOR_PLANE_HEIGHT));
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
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            if (width <= 0.0f || height <= 0.0f) return;

            effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot());
        }

        private ColorPickerState snapshot() {
            return new ColorPickerState(
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    Part.COLOR_PLANE,
                    type,
                    color.copy(),
                    hue,
                    saturation,
                    value,
                    hovered(),
                    dragging,
                    enabled());
        }

        private void update(float rootX, float rootY) {
            float width = Math.max(1.0f, layoutBounds().width());
            float height = Math.max(1.0f, layoutBounds().height());
            saturation = clamp01((rootX - layoutBounds().x()) / width);
            value = clamp01(1.0f - (rootY - layoutBounds().y()) / height);
            updateFromHsv();
        }

    }
}
