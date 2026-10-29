package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Short caption text, optionally associated with a focusable control.
 *
 * <p>{@link Text} is generic display text. Label adds the form-caption role:
 * when {@link #focusTarget(Widget)} is set, primary click requests focus for
 * that control.</p>
 */
@XmlWidgetName("Label")
public final class Label extends TextWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.LABEL;

    private Widget focusTarget;

    public Label() {
        noWrap();
    }

    public Label(String text) {
        super(text);
        noWrap();
    }

    public Label(RichText text) {
        richText(text);
        noWrap();
    }

    public Widget focusTarget() {
        return focusTarget;
    }

    public Label focusTarget(Widget focusTarget) {
        if (this.focusTarget == focusTarget) return this;
        this.focusTarget = focusTarget;
        return this;
    }

    public Label labeledControl(Widget control) {
        return focusTarget(control);
    }

    @Override
    public Label text(String text) {
        super.text(text);
        return this;
    }

    @Override
    public Label richText(RichText richText) {
        super.richText(richText);
        return this;
    }

    @Override
    public Label color(ColorView color) {
        super.color(color);
        return this;
    }

    @Override
    public Label textAlignment(Alignment alignment) {
        super.textAlignment(alignment);
        return this;
    }

    @Override
    public Label wrap(boolean wrap) {
        super.wrap(wrap);
        return this;
    }

    @Override
    public Label overflowMode(TextOverflowMode overflowMode) {
        super.overflowMode(overflowMode);
        return this;
    }

    @Override
    public Label background(ColorView color) {
        super.background(color);
        return this;
    }

    @Override
    public Label background(float r, float g, float b, float a) {
        super.background(r, g, b, a);
        return this;
    }

    @Override
    public Label animateBackgroundColor(ColorView color, float durationSeconds) {
        super.animateBackgroundColor(color, durationSeconds);
        return this;
    }

    @Override
    public Label animateBackgroundColor(ColorView color, TransitionSpec spec) {
        super.animateBackgroundColor(color, spec);
        return this;
    }

    @Override
    public Label backgroundVisible(boolean backgroundVisible) {
        super.backgroundVisible(backgroundVisible);
        return this;
    }

    @Override
    public Label backgroundTexture(TextureHandle backgroundTexture) {
        super.backgroundTexture(backgroundTexture);
        return this;
    }

    @Override
    public Label backgroundTextureWidth(int width) {
        super.backgroundTextureWidth(width);
        return this;
    }

    @Override
    public Label backgroundTextureHeight(int height) {
        super.backgroundTextureHeight(height);
        return this;
    }

    @Override
    public Label backgroundTextureSampling(TextureFilter filter) {
        super.backgroundTextureSampling(filter);
        return this;
    }

    @Override
    public Label backgroundTextureWrap(TextureWrap wrap) {
        super.backgroundTextureWrap(wrap);
        return this;
    }

    @Override
    public Label backgroundTextureMipmaps(boolean mipmaps) {
        super.backgroundTextureMipmaps(mipmaps);
        return this;
    }

    @Override
    public Label backgroundTexturePremultipliedAlpha(boolean premultipliedAlpha) {
        super.backgroundTexturePremultipliedAlpha(premultipliedAlpha);
        return this;
    }

    @Override
    public Label backgroundTextureTint(ColorView color) {
        super.backgroundTextureTint(color);
        return this;
    }

    @Override
    public Label animateBackgroundTextureTint(ColorView color, float durationSeconds) {
        super.animateBackgroundTextureTint(color, durationSeconds);
        return this;
    }

    @Override
    public Label animateBackgroundTextureTint(ColorView color, TransitionSpec spec) {
        super.animateBackgroundTextureTint(color, spec);
        return this;
    }

    @Override
    public Label backgroundTextureSource(MutableRect source) {
        super.backgroundTextureSource(source);
        return this;
    }

    @Override
    public Label backgroundTextureSource(float u, float v, float width, float height) {
        super.backgroundTextureSource(u, v, width, height);
        return this;
    }

    @Override
    public Label backgroundTextureFit(ImageFit fit) {
        super.backgroundTextureFit(fit);
        return this;
    }

    @Override
    public Label border(ColorView color) {
        super.border(color);
        return this;
    }

    @Override
    public Label border(ColorView color, float width) {
        super.border(color, width);
        return this;
    }

    @Override
    public Label border(float r, float g, float b, float a) {
        super.border(r, g, b, a);
        return this;
    }

    @Override
    public Label border(float r, float g, float b, float a, float width) {
        super.border(r, g, b, a, width);
        return this;
    }

    @Override
    public Label borderColor(ColorView color) {
        super.borderColor(color);
        return this;
    }

    @Override
    public Label animateBorderColor(ColorView color, float durationSeconds) {
        super.animateBorderColor(color, durationSeconds);
        return this;
    }

    @Override
    public Label animateBorderColor(ColorView color, TransitionSpec spec) {
        super.animateBorderColor(color, spec);
        return this;
    }

    @Override
    public Label borderVisible(boolean borderVisible) {
        super.borderVisible(borderVisible);
        return this;
    }

    @Override
    public Label borderWidth(float borderWidth) {
        super.borderWidth(borderWidth);
        return this;
    }

    @Override
    public Label animateBorderWidth(float borderWidth, float durationSeconds) {
        super.animateBorderWidth(borderWidth, durationSeconds);
        return this;
    }

    @Override
    public Label animateBorderWidth(float borderWidth, TransitionSpec spec) {
        super.animateBorderWidth(borderWidth, spec);
        return this;
    }

    @Override
    public Label radius(float radius) {
        super.radius(radius);
        return this;
    }

    @Override
    public Label animateRadius(float radius, float durationSeconds) {
        super.animateRadius(radius, durationSeconds);
        return this;
    }

    @Override
    public Label animateRadius(float radius, TransitionSpec spec) {
        super.animateRadius(radius, spec);
        return this;
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY
                && focusTarget != null
                && focusTarget.focusable()
                && focusTarget.enabled()
                && focusTarget.visible()
                && uiContext() != null) {
            uiContext().focusManager().requestFocus(focusTarget);
            event.cancel();
        }
    }
}
