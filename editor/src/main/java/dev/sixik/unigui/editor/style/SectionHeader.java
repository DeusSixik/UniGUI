package dev.sixik.unigui.editor.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Заголовок сворачиваемой группы: шеврон слева и название. Текст кнопки может начинаться с символа состояния
 * ({@code "▾ Layout"}); вместо символа рисуется векторный шеврон, а сам текст остаётся читаемым для кода и тестов.
 */
public class SectionHeader extends Button {
    private static final WidgetRender RENDERER = WidgetRender.of(Button.class, (draw, button) -> {
        if (button instanceof SectionHeader header) header.paint(draw);
    });

    /** @param text подпись, обычно с символом состояния */
    public SectionHeader(String text) {
        super(text);
        themeEnabled(false);
        focusRingVisible(false);
        focusable(false);
        renderer(RENDERER);
        layout(style -> style.widthPercent(100.0f).height(EditorStyles.COMPONENT_HEADER_HEIGHT).flexShrink(0.0f));
    }

    private void paint(DrawScope draw) {
        RectView b = layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;
        if (hovered()) draw.roundedRectFilled(x, y, w, h, EditorStyles.RADIUS, EditorStyles.BG_HEADER);

        String label = text() == null ? "" : text();
        boolean closed = label.startsWith("▸");
        if (label.startsWith("▸") || label.startsWith("▾")) label = label.substring(1).trim();
        ColorView tint = hovered() ? EditorStyles.TEXT_BOLD : EditorStyles.TEXT_MUTED;
        float icon = 10.0f;
        (closed ? EditorIcons.CHEVRON_RIGHT : EditorIcons.CHEVRON_DOWN).draw(draw, x + 2.0f, y + (h - icon) * 0.5f, icon, tint);

        RichText rich = RichText.plain(label);
        float textWidth = TextEngine.measureLineWidth(draw.context(), rich);
        float textHeight = TextEngine.measureTextHeight(draw.context(), rich);
        float textX = x + 15.0f;
        draw.pushTextClip(x, y, w, h);
        try {
            TextEngine.drawInline(draw, rich, textX, y + Math.max(0.0f, h - textHeight) * 0.5f, textWidth, textHeight,
                    Paint.fill(EditorStyles.TEXT_PRIMARY));
        } finally {
            draw.popClip();
        }
    }
}
