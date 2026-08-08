package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

public class Checkbox extends ToggleButton {
    private static final float BOX_SIZE = 12.0f;
    private static final float CHECK_SIZE = 6.0f;
    private static final float TEXT_GAP = 4.0f;

    public Checkbox() {
        this("");
    }

    public Checkbox(String text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
    }

    public Checkbox(RichText text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float textWidth = text().isEmpty() ? 0.0f : 4.0f + TextEngine.measureLineWidth(richText());
        setDesiredSize(resolveDesiredSize(context, BOX_SIZE + textWidth, DEFAULT_HEIGHT));
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot(context));
        renderChildren(context);
    }

    @Override
    protected ButtonRenderer effectiveRenderer() {
        return renderer() == null ? WidgetsRender.checkbox() : renderer();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.CHECKBOX,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                TEXT_PADDING_X,
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                checked(),
                BOX_SIZE,
                CHECK_SIZE,
                TEXT_GAP,
                checkedBackground().copy(),
                borderColor().copy());
    }
}
