package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Параметры заливки или обводки draw-команды.
 *
 * <p>{@code Paint} хранит цвет, stroke/fill режим, толщину линии, blend mode и dash pattern.
 * Объект изменяемый для удобной fluent-настройки, но {@link DrawCommand} делает копию paint при
 * записи, поэтому дальнейшие изменения не влияют на уже добавленные команды.</p>
 */
public final class Paint {
    private final MutableColor color = new MutableColor();
    private float strokeWidth;
    private boolean stroke;
    private BlendMode blendMode = BlendMode.NORMAL;
    private float dashLength;
    private float dashGap;
    private float dashOffset;

    /**
     * Создаёт fill paint.
     *
     * @param color цвет заливки
     * @return новый paint
     */
    public static Paint fill(ColorView color) {
        return new Paint().color(color).stroke(false);
    }

    /**
     * Создаёт stroke paint.
     *
     * @param color цвет обводки
     * @param width толщина обводки в UI-пикселях
     * @return новый paint
     */
    public static Paint stroke(ColorView color, float width) {
        return new Paint().color(color).stroke(true).strokeWidth(width);
    }

    /** @return mutable цвет paint'а */
    public MutableColor color() {
        return color;
    }

    /**
     * Задаёт цвет paint'а.
     *
     * @param color новый цвет
     * @return этот paint для fluent-настройки
     */
    public Paint color(ColorView color) {
        this.color.set(color);
        return this;
    }

    /** @return толщина stroke в UI-пикселях */
    public float strokeWidth() {
        return strokeWidth;
    }

    /**
     * Задаёт толщину stroke.
     *
     * @param strokeWidth толщина в UI-пикселях
     * @return этот paint для fluent-настройки
     */
    public Paint strokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /** @return {@code true}, если paint описывает обводку, а не заливку */
    public boolean isStroke() {
        return stroke;
    }

    /**
     * Переключает paint между stroke и fill.
     *
     * @param stroke {@code true} для stroke, {@code false} для fill
     * @return этот paint для fluent-настройки
     */
    public Paint stroke(boolean stroke) {
        this.stroke = stroke;
        return this;
    }

    /** @return blend mode paint'а */
    public BlendMode blendMode() {
        return blendMode;
    }

    /**
     * Задаёт blend mode.
     *
     * @param blendMode режим смешивания; {@code null} превращается в {@link BlendMode#NORMAL}
     * @return этот paint для fluent-настройки
     */
    public Paint blend(BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
        return this;
    }

    /** @return {@code true}, если stroke должен рисоваться пунктиром */
    public boolean dashed() {
        return dashLength > 0.0f && dashGap > 0.0f;
    }

    /** @return длина видимого dash-сегмента */
    public float dashLength() {
        return dashLength;
    }

    /** @return длина gap между dash-сегментами */
    public float dashGap() {
        return dashGap;
    }

    /** @return смещение dash pattern */
    public float dashOffset() {
        return dashOffset;
    }

    /**
     * Задаёт dash pattern для stroke-линий.
     *
     * @param length длина видимого сегмента
     * @param gap длина промежутка
     * @return этот paint для fluent-настройки
     */
    public Paint dash(float length, float gap) {
        dashLength = sanitizeDash(length);
        dashGap = sanitizeDash(gap);
        return this;
    }

    /**
     * Задаёт смещение dash pattern.
     *
     * @param dashOffset смещение pattern
     * @return этот paint для fluent-настройки
     */
    public Paint dashOffset(float dashOffset) {
        this.dashOffset = Float.isFinite(dashOffset) ? dashOffset : 0.0f;
        return this;
    }

    /**
     * Очищает dash pattern.
     *
     * @return этот paint для fluent-настройки
     */
    public Paint clearDash() {
        dashLength = 0.0f;
        dashGap = 0.0f;
        dashOffset = 0.0f;
        return this;
    }

    /** @return независимая копия paint'а */
    public Paint copy() {
        Paint copy = new Paint();
        copy.copyFrom(this);
        return copy;
    }

    /** Копирует состояние paint в уже существующий объект без создания нового paint. */
    Paint copyFrom(Paint source) {
        if (source == null) {
            return reset();
        }
        color.set(source.color);
        strokeWidth = source.strokeWidth;
        stroke = source.stroke;
        blendMode = source.blendMode;
        dashLength = source.dashLength;
        dashGap = source.dashGap;
        dashOffset = source.dashOffset;
        return this;
    }

    /** Сбрасывает paint в состояние по умолчанию для повторного использования draw command. */
    Paint reset() {
        color.set(1.0f, 1.0f, 1.0f, 1.0f);
        strokeWidth = 0.0f;
        stroke = false;
        blendMode = BlendMode.NORMAL;
        dashLength = 0.0f;
        dashGap = 0.0f;
        dashOffset = 0.0f;
        return this;
    }

    private static float sanitizeDash(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
