package dev.sixik.unigui.editor.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.function.BooleanSupplier;

/**
 * Кнопка панели инструментов: векторная иконка, необязательная подпись и один из трёх видов. В покое
 * {@link Variant#GHOST} не имеет фона, поэтому ряд кнопок не шумит; фон появляется при наведении.
 */
public class ToolButton extends Button {
    /** Вид кнопки. */
    public enum Variant {
        /** Без фона в покое: панели инструментов. */
        GHOST,
        /** С фоном: отдельные действия. */
        SOLID,
        /** Залита акцентом: главное действие экрана. */
        ACCENT,
        /** Залита цветом запуска. */
        RUN
    }

    private static final float ICON_SIZE = 12.0f;
    private static final float ICON_GAP = 4.0f;
    private static final MutableColor HOVER_WASH = MutableColor.rgba255(255, 255, 255, 18);
    private static final MutableColor PRESS_WASH = MutableColor.rgba255(255, 255, 255, 10);

    private static final WidgetRender RENDERER = WidgetRender.of(Button.class, (draw, button) -> {
        if (button instanceof ToolButton tool) tool.paint(draw);
    });

    private UiIcon icon;
    private Variant variant;
    private boolean active;
    private ColorView iconColor;
    private BooleanSupplier availability;
    private String tooltipText = "";
    private boolean alignStart;

    /**
     * Кнопка-иконка.
     *
     * @param icon    иконка
     * @param tooltip короткая подпись для автоматизации и всплывающей подсказки
     */
    public ToolButton(UiIcon icon, String tooltip) {
        this(icon, "", Variant.GHOST);
        this.tooltipText = tooltip == null ? "" : tooltip;
        layout(style -> style.size(EditorStyles.ROW_HEIGHT + 2.0f, EditorStyles.ROW_HEIGHT + 2.0f).flexShrink(0.0f));
    }

    /**
     * Кнопка с подписью.
     *
     * @param icon    иконка или {@code null}
     * @param text    подпись
     * @param variant вид
     */
    public ToolButton(UiIcon icon, String text, Variant variant) {
        super(text == null ? "" : text);
        this.icon = icon;
        this.variant = variant;
        themeEnabled(false);
        focusRingVisible(false);
        focusable(false);
        renderer(RENDERER);
        // Подпись измеряет сама кнопка: симметричный отступ оставляет место под иконку, а группа рисуется по центру.
        textPaddingX(icon == null ? 8.0f : 8.0f + (ICON_SIZE + ICON_GAP) * 0.5f);
        layout(style -> style.height(EditorStyles.ROW_HEIGHT + 2.0f).flexShrink(0.0f));
    }

    /** @return короткая подпись действия */
    public String tooltipText() {
        return tooltipText;
    }

    /**
     * @param value иконка
     * @return эта кнопка
     */
    public ToolButton icon(UiIcon value) {
        this.icon = value;
        return this;
    }

    /**
     * @param value вид
     * @return эта кнопка
     */
    public ToolButton variant(Variant value) {
        this.variant = value;
        return this;
    }

    /**
     * Отмечает кнопку как включённую: так показывается выбранный режим.
     *
     * @param value {@code true} — включена
     * @return эта кнопка
     */
    public ToolButton active(boolean value) {
        this.active = value;
        return this;
    }

    /** @return {@code true}, если кнопка отмечена */
    public boolean active() {
        return active;
    }

    /**
     * Прижимает иконку и подпись к левому краю: так кнопка работает строкой списка.
     *
     * @return эта кнопка
     */
    public ToolButton alignStart() {
        this.alignStart = true;
        return this;
    }

    /**
     * @param value постоянный цвет иконки или {@code null}, чтобы брать цвет текста
     * @return эта кнопка
     */
    public ToolButton iconColor(ColorView value) {
        this.iconColor = value;
        return this;
    }

    /**
     * Доступность проверяется при каждой отрисовке: кнопки Undo и Save следуют за состоянием документа без подписок.
     *
     * @param value условие доступности
     * @return эта кнопка
     */
    public ToolButton availableWhen(BooleanSupplier value) {
        this.availability = value;
        return this;
    }

    private void paint(DrawScope draw) {
        if (availability != null) {
            boolean available = availability.getAsBoolean();
            if (available != enabled()) enabled(available);
        }
        RectView b = layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;
        boolean on = enabled();
        boolean hover = on && hovered();
        boolean press = on && pressed();
        float radius = EditorStyles.RADIUS;

        ColorView fill = switch (variant) {
            case GHOST -> active ? EditorStyles.ACCENT_SOFT : null;
            case SOLID -> EditorStyles.BG_BUTTON;
            case ACCENT -> EditorStyles.TAB_ACCENT;
            case RUN -> EditorStyles.PLAY_GREEN;
        };
        if (fill != null) draw.roundedRectFilled(x, y, w, h, radius, fill);
        if (variant == Variant.SOLID) draw.roundedRectStroke(x, y, w, h, radius, EditorStyles.BORDER_SUBTLE, 1.0f);
        if (press) draw.roundedRectFilled(x, y, w, h, radius, PRESS_WASH);
        else if (hover) draw.roundedRectFilled(x, y, w, h, radius, HOVER_WASH);

        boolean filled = variant == Variant.ACCENT || variant == Variant.RUN;
        ColorView textColor = !on ? EditorStyles.BORDER_BUTTON
                : filled ? EditorStyles.TEXT_BOLD
                : active ? EditorStyles.INFO_BLUE
                : hover ? EditorStyles.TEXT_BOLD : EditorStyles.TEXT_LABEL;
        ColorView tint = on && iconColor != null ? iconColor : textColor;

        String label = text();
        boolean hasText = label != null && !label.isEmpty();
        RichText rich = hasText ? RichText.plain(label) : null;
        float textWidth = hasText ? TextEngine.measureLineWidth(draw.context(), rich) : 0.0f;
        float iconWidth = icon == null ? 0.0f : ICON_SIZE;
        float group = iconWidth + (hasText && icon != null ? ICON_GAP : 0.0f) + textWidth;
        float cursor = alignStart ? x + 5.0f : x + Math.max(0.0f, (w - group) * 0.5f);
        if (icon != null) {
            icon.draw(draw, cursor, y + (h - ICON_SIZE) * 0.5f, ICON_SIZE, tint);
            cursor += ICON_SIZE + ICON_GAP;
        }
        if (hasText) {
            float textHeight = TextEngine.measureTextHeight(draw.context(), rich);
            float textY = y + Math.max(0.0f, h - textHeight) * 0.5f;
            draw.pushTextClip(x, y, w, h);
            try {
                TextEngine.drawInline(draw, rich, cursor, textY, textWidth, textHeight, Paint.fill(textColor));
            } finally {
                draw.popClip();
            }
        }
    }
}
