package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Строка панели контракта: значок, имя и справа цветная метка с типом, как запись переменной в My Blueprint. Детали
 * записи правятся в инспекторе, поэтому строка только показывает, выделяется и служит источником перетаскивания.
 */
final class ContractRow extends Button {
    /** Высота строки. */
    static final float HEIGHT = 20.0f;

    private static final float INDENT = 14.0f;
    private static final float ICON = 11.0f;
    private static final WidgetRender RENDERER = WidgetRender.of(Button.class, (draw, button) -> {
        if (button instanceof ContractRow row) row.paint(draw);
    });

    private final UiIcon icon;
    private final String trailing;
    private final MutableColor pill;
    private boolean selected;

    /**
     * @param text     имя
     * @param icon     значок слева или {@code null}
     * @param trailing подпись справа: тип или число аргументов
     * @param pill     цвет метки типа или {@code null}
     */
    ContractRow(String text, UiIcon icon, String trailing, MutableColor pill) {
        super(text);
        this.icon = icon;
        this.trailing = trailing == null ? "" : trailing;
        this.pill = pill;
        themeEnabled(false);
        focusRingVisible(false);
        focusable(false);
        renderer(RENDERER);
        layout(style -> style.widthPercent(100.0f).height(HEIGHT).flexShrink(0.0f));
    }

    /**
     * @param value {@code true}, если запись показана в инспекторе
     * @return эта строка
     */
    ContractRow selected(boolean value) {
        this.selected = value;
        return this;
    }

    /** @return {@code true}, если запись показана в инспекторе */
    boolean selected() {
        return selected;
    }

    private void paint(DrawScope draw) {
        RectView b = layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;
        if (selected) draw.rectFilled(x, y, w, h, EditorStyles.SELECTION_BLUE);
        else if (hovered()) draw.rectFilled(x, y, w, h, EditorStyles.BG_HEADER);

        ColorView nameColor = selected ? EditorStyles.TEXT_BOLD : EditorStyles.TEXT_PRIMARY;
        float cursor = x + INDENT;
        if (icon != null) {
            icon.draw(draw, cursor, y + (h - ICON) * 0.5f, ICON, selected ? EditorStyles.TEXT_BOLD : EditorStyles.TEXT_LABEL);
            cursor += ICON + 5.0f;
        }

        float right = x + w - 6.0f;
        if (!trailing.isEmpty()) {
            RichText tail = RichText.plain(trailing);
            float tailWidth = TextEngine.measureLineWidth(draw.context(), tail);
            float tailHeight = TextEngine.measureTextHeight(draw.context(), tail);
            float tailX = right - tailWidth;
            draw.pushTextClip(tailX, y, tailWidth, h);
            try {
                TextEngine.drawInline(draw, tail, tailX, y + Math.max(0.0f, h - tailHeight) * 0.5f, tailWidth, tailHeight,
                        Paint.fill(selected ? EditorStyles.TEXT_BOLD : EditorStyles.TEXT_LABEL));
            } finally {
                draw.popClip();
            }
            right = tailX - 5.0f;
            if (pill != null) {
                draw.roundedRectFilled(right - 11.0f, y + (h - 5.0f) * 0.5f, 11.0f, 5.0f, 2.5f, pill);
                right -= 16.0f;
            }
        }

        RichText name = RichText.plain(text());
        float nameWidth = Math.max(0.0f, right - cursor);
        float nameHeight = TextEngine.measureTextHeight(draw.context(), name);
        draw.pushTextClip(cursor, y, nameWidth, h);
        try {
            TextEngine.drawInline(draw, name, cursor, y + Math.max(0.0f, h - nameHeight) * 0.5f,
                    TextEngine.measureLineWidth(draw.context(), name), nameHeight, Paint.fill(nameColor));
        } finally {
            draw.popClip();
        }
    }
}
