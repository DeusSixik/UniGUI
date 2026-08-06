package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

public class Checkbox extends ToggleButton {
    private static final float BOX_SIZE = 12.0f;

    public Checkbox() {
        this("");
    }

    public Checkbox(String text) {
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
        float textWidth = text().isEmpty() ? 0.0f : 4.0f + text().codePointCount(0, text().length()) * APPROX_CHAR_WIDTH;
        setDesiredSize(resolveDesiredSize(context, BOX_SIZE + textWidth, DEFAULT_HEIGHT));
    }

    @Override
    protected void renderContent(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y() + Math.max(0.0f, layoutBounds().height() - BOX_SIZE) * 0.5f;
        context.roundedRect(x, y, BOX_SIZE, BOX_SIZE, 2.0f, Paint.stroke(borderColor(), 1.0f), transform());
        if (checked()) {
            context.rect(x + 3.0f, y + 3.0f, BOX_SIZE - 6.0f, BOX_SIZE - 6.0f, Paint.fill(checkedBackground()), transform());
        }
        if (!text().isEmpty()) {
            context.text(text(),
                    x + BOX_SIZE + 4.0f,
                    layoutBounds().y(),
                    Math.max(0.0f, layoutBounds().width() - BOX_SIZE - 4.0f),
                    layoutBounds().height(),
                    Paint.fill(textColor()),
                    transform());
        }
    }
}
