package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.backend.minecraft.MinecraftGuiRenderBackend;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.Box;

import java.util.Objects;

public abstract class MinecraftPreviewWidget extends Box {
    private static final float DEFAULT_SIZE = 54.0f;
    private static final float PADDING = 6.0f;
    private static final float LABEL_HEIGHT = 12.0f;

    private final MutableColor labelColor = new MutableColor(0.86f, 0.90f, 0.98f, 1.0f);
    private final MutableColor fallbackColor = new MutableColor(1.0f, 0.70f, 0.25f, 1.0f);
    private String label = "";
    private RichText richLabel = RichText.plain("");
    private float previewSize = DEFAULT_SIZE;
    private boolean labelVisible = true;

    protected MinecraftPreviewWidget(String label) {
        this.label = label == null ? "" : label;
        this.richLabel = RichText.plain(this.label);
        backgroundVisible(true);
        borderVisible(true);
        radius(4.0f);
        background().set(0.035f, 0.040f, 0.055f, 0.94f);
        borderColor().set(0.20f, 0.23f, 0.30f, 0.92f);
        labelColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fallbackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public String label() {
        return label;
    }

    @XmlAttribute(value = "label", category = "Content", defaultValue = "", description = "Preview caption text.")
    public MinecraftPreviewWidget label(String label) {
        String normalized = label == null ? "" : label;
        if (Objects.equals(this.label, normalized)) return this;
        this.label = normalized;
        this.richLabel = RichText.plain(normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richLabel() {
        return richLabel;
    }

    public MinecraftPreviewWidget richLabel(RichText label) {
        RichText normalized = label == null ? RichText.plain("") : label;
        if (Objects.equals(this.richLabel, normalized)) return this;
        this.richLabel = normalized;
        this.label = normalized.plainText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float previewSize() {
        return previewSize;
    }

    @XmlAttribute(value = "previewSize", category = "Layout", defaultValue = "54", description = "Square preview area size in UI pixels.")
    public MinecraftPreviewWidget previewSize(float previewSize) {
        float normalized = Float.isFinite(previewSize) ? Math.max(16.0f, previewSize) : DEFAULT_SIZE;
        if (this.previewSize == normalized) return this;
        this.previewSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean labelVisible() {
        return labelVisible;
    }

    @XmlAttribute(value = "labelVisible", category = "Appearance", defaultValue = "true", description = "Whether the preview caption is rendered.")
    public MinecraftPreviewWidget labelVisible(boolean labelVisible) {
        if (this.labelVisible == labelVisible) return this;
        this.labelVisible = labelVisible;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor labelColor() {
        return labelColor;
    }

    public MutableColor fallbackColor() {
        return fallbackColor;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        float labelWidth = labelVisible && !label.isEmpty() ? TextEngine.measureLineWidth(richLabel) : 0.0f;
        float width = Math.max(previewSize, labelWidth) + PADDING * 2.0f;
        float height = previewSize + PADDING * 2.0f + (labelVisible ? LABEL_HEIGHT : 0.0f);
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    protected void renderContent(RenderContext context) {
        float squareSize = Math.min(previewSize,
                Math.max(0.0f, Math.min(layoutBounds().width() - PADDING * 2.0f, layoutBounds().height() - PADDING * 2.0f)));
        float previewX = layoutBounds().x() + Math.max(PADDING, (layoutBounds().width() - squareSize) * 0.5f);
        float previewY = layoutBounds().y() + PADDING;
        float capturedOpacity = context.opacityMultiplier();
        if (!renderPreviewTexture(context, previewX, previewY, squareSize, capturedOpacity)) {
            context.custom(backend -> {
                if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                    renderMinecraftPreview(minecraftBackend, previewX, previewY, squareSize, capturedOpacity);
                }
            });
        }

        RenderBackend backend = context.backend();
        if (!(backend instanceof MinecraftGuiRenderBackend)) {
            TextEngine.draw(context,
                    RichText.plain(fallbackText()),
                    previewX,
                    previewY + Math.max(0.0f, squareSize - TextEngine.LINE_HEIGHT) * 0.5f,
                    squareSize,
                    TextEngine.LINE_HEIGHT,
                    Paint.fill(fallbackColor),
                    transform(),
                    Alignment.CENTER,
                    Alignment.CENTER);
        }

        if (labelVisible && !label.isEmpty()) {
            TextEngine.draw(context,
                    richLabel,
                    layoutBounds().x() + PADDING,
                    layoutBounds().y() + layoutBounds().height() - PADDING - LABEL_HEIGHT,
                    Math.max(0.0f, layoutBounds().width() - PADDING * 2.0f),
                    LABEL_HEIGHT,
                    Paint.fill(labelColor),
                    transform(),
                    Alignment.CENTER,
                    Alignment.CENTER);
        }

        super.renderContent(context);
    }

    protected abstract void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity);

    protected boolean renderPreviewTexture(RenderContext context, float x, float y, float size, float opacity) {
        TextureHandle texture = previewTexture(context, size);
        if (texture == null) return false;
        context.texture(texture, x, y, size, size, Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
        return true;
    }

    protected TextureHandle previewTexture(RenderContext context, float size) {
        return null;
    }

    protected abstract String fallbackText();
}
