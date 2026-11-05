package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Параметры заливки или обводки draw-команды.
 *
 * <p>{@code Paint} хранит цвет, stroke/fill режим, толщину линии, blend mode и dash pattern.
 * Объект изменяемый для удобной fluent-настройки, но {@link DrawCommand} делает копию paint при
 * записи, поэтому дальнейшие изменения не влияют на уже добавленные команды.</p>
 */
@AiDoc(
        summary = "Styling descriptor for draw commands configuring RGBA colors, stroke widths, fill/stroke modes, Porter-Duff blending, and dashed lines.",
        purpose = "Decouples visual styling configuration from geometry recording, allowing reusable and fluent paint definition.",
        useWhen = {
                "Configure colors, stroke thicknesses, or blend modes for `DrawScope#rect`, `DrawScope#circle`, `DrawScope#nineSlice`, or `DrawScope#path`.",
                "Build animated dashed outlines for selection boxes using `dash()` and `dashOffset()`.",
                "Enable additive luminescence (glows, sparks) or multiply shadows using `blend(BlendMode)`."
        },
        criticalRules = {
                "DrawScope copies or snapshots Paint when recording a DrawCommand; subsequent mutations do not retroactively alter earlier commands.",
                "For high-frequency render loops where memory allocations matter, prefer DrawScope#*Filled overloads accepting ColorView directly.",
                "Dash length and gap must be finite non-negative numbers; negative values are clamped to 0.0."
        },
        related = {"ColorView", "BlendMode", "DrawScope", "DrawCommand", "TextureHandle"},
        examples = {
                @AiDoc.Example(
                        title = "Create dashed outline paint",
                        order = 10,
                        code = """
                                Paint paint = Paint.stroke(ColorView.rgb(0, 180, 255), 1.5f)
                                        .dash(6.0f, 4.0f)
                                        .dashOffset(animPhase);
                                scope.roundedRect(x, y, w, h, 4.0f, paint);
                                """),
                @AiDoc.Example(
                        title = "Create additive glowing paint",
                        order = 20,
                        code = """
                                Paint glow = Paint.fill(ColorView.rgba(0, 220, 255, 90))
                                        .blend(BlendMode.ADDITIVE);
                                scope.circle(cx - 16, cy - 16, 32, 32, glow);
                                """)
        })
@WikiPage(
        title = "<key:paint.title>",
        slug = "paint",
        category = "<key:paint.category>",
        summary = "<key:paint.summary>",
        order = 30)
@WikiSection(
        title = "<key:paint.section.overview.title>",
        markdown = "<key:paint.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:paint.section.copy.title>",
        markdown = "<key:paint.section.copy.body>",
        order = 20)
@WikiSection(
        title = "<key:paint.section.modes.title>",
        markdown = "<key:paint.section.modes.body>",
        order = 30)
@WikiSection(
        title = "<key:paint.section.blend.title>",
        markdown = "<key:paint.section.blend.body>",
        order = 40)
@WikiSection(
        title = "<key:paint.section.dash.title>",
        markdown = "<key:paint.section.dash.body>",
        order = 50)
@WikiExample(
        title = "<key:paint.example.selection.title>",
        code = "<key:paint.example.selection.code>",
        order = 10)
@WikiExample(
        title = "<key:paint.example.glow.title>",
        code = "<key:paint.example.glow.code>",
        order = 20)
public final class Paint {
    private final MutableColor color = new MutableColor();
    private float strokeWidth;
    private boolean stroke;
    private BlendMode blendMode = BlendMode.NORMAL;
    private float dashLength;
    private float dashGap;
    private float dashOffset;

    /**
     * Создаёт paint с параметрами по умолчанию: белый цвет заливки, без обводки, нормальный blend mode.
     */
    @WikiMethod(
            title = "<key:paint.method.constructor.title>",
            summary = "<key:paint.method.constructor.summary>",
            section = "<key:paint.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    public Paint() {
    }

    /**
     * Создаёт fill paint.
     *
     * @param color цвет заливки
     * @return новый paint
     */
    @WikiMethod(
            title = "<key:paint.method.fill.title>",
            summary = "<key:paint.method.fill.summary>",
            section = "<key:paint.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    @WikiReturn("<key:paint.method.fill.return>")
    public static Paint fill(@WikiParameter(name = "color", description = "<key:paint.param.color>") ColorView color) {
        return new Paint().color(color).stroke(false);
    }

    /**
     * Создаёт stroke paint.
     *
     * @param color цвет обводки
     * @param width толщина обводки в UI-пикселях
     * @return новый paint
     */
    @WikiMethod(
            title = "<key:paint.method.stroke_factory.title>",
            summary = "<key:paint.method.stroke_factory.summary>",
            section = "<key:paint.api.section.factories>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:paint.method.stroke_factory.return>")
    public static Paint stroke(@WikiParameter(name = "color", description = "<key:paint.param.color>") ColorView color, @WikiParameter(name = "width", description = "<key:paint.param.strokeWidth>") float width) {
        return new Paint().color(color).stroke(true).strokeWidth(width);
    }

    /**
     * @return mutable цвет paint'а
     */
    @WikiMethod(
            title = "<key:paint.method.color_get.title>",
            summary = "<key:paint.method.color_get.summary>",
            section = "<key:paint.api.section.color>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:paint.method.color_get.return>")
    public MutableColor color() {
        return color;
    }

    /**
     * Задаёт цвет paint'а.
     *
     * @param color новый цвет
     * @return этот paint для fluent-настройки
     */
    @WikiMethod(
            title = "<key:paint.method.color_set.title>",
            summary = "<key:paint.method.color_set.summary>",
            section = "<key:paint.api.section.color>",
            sectionOrder = 20,
            order = 20)
    @WikiReturn("<key:paint.method.color_set.return>")
    public Paint color(@WikiParameter(name = "color", description = "<key:paint.param.color>") ColorView color) {
        this.color.set(color);
        return this;
    }

    /**
     * @return толщина stroke в UI-пикселях
     */
    @WikiMethod(
            title = "<key:paint.method.stroke_width_get.title>",
            summary = "<key:paint.method.stroke_width_get.summary>",
            section = "<key:paint.api.section.stroke>",
            sectionOrder = 30,
            order = 10)
    @WikiReturn("<key:paint.method.stroke_width_get.return>")
    public float strokeWidth() {
        return strokeWidth;
    }

    /**
     * Задаёт толщину stroke.
     *
     * @param strokeWidth толщина в UI-пикселях
     * @return этот paint для fluent-настройки
     */
    @WikiMethod(
            title = "<key:paint.method.stroke_width_set.title>",
            summary = "<key:paint.method.stroke_width_set.summary>",
            section = "<key:paint.api.section.stroke>",
            sectionOrder = 30,
            order = 20)
    @WikiReturn("<key:paint.method.stroke_width_set.return>")
    public Paint strokeWidth(@WikiParameter(name = "strokeWidth", description = "<key:paint.param.strokeWidth>") float strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /**
     * @return {@code true}, если paint описывает обводку, а не заливку
     */
    @WikiMethod(
            title = "<key:paint.method.is_stroke.title>",
            summary = "<key:paint.method.is_stroke.summary>",
            section = "<key:paint.api.section.stroke>",
            sectionOrder = 30,
            order = 30)
    @WikiReturn("<key:paint.method.is_stroke.return>")
    public boolean isStroke() {
        return stroke;
    }

    /**
     * Переключает paint между stroke и fill.
     *
     * @param stroke {@code true} для stroke, {@code false} для fill
     * @return этот paint для fluent-настройки
     */
    @WikiMethod(
            title = "<key:paint.method.stroke_mode_set.title>",
            summary = "<key:paint.method.stroke_mode_set.summary>",
            section = "<key:paint.api.section.stroke>",
            sectionOrder = 30,
            order = 40)
    @WikiReturn("<key:paint.method.stroke_mode_set.return>")
    public Paint stroke(@WikiParameter(name = "stroke", description = "<key:paint.param.stroke>") boolean stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * @return blend mode paint'а
     */
    @WikiMethod(
            title = "<key:paint.method.blend_mode_get.title>",
            summary = "<key:paint.method.blend_mode_get.summary>",
            section = "<key:paint.api.section.blend>",
            sectionOrder = 40,
            order = 10)
    @WikiReturn("<key:paint.method.blend_mode_get.return>")
    public BlendMode blendMode() {
        return blendMode;
    }

    /**
     * Задаёт blend mode.
     *
     * @param blendMode режим смешивания; {@code null} превращается в {@link BlendMode#NORMAL}
     * @return этот paint для fluent-настройки
     */
    @WikiMethod(
            title = "<key:paint.method.blend_mode_set.title>",
            summary = "<key:paint.method.blend_mode_set.summary>",
            section = "<key:paint.api.section.blend>",
            sectionOrder = 40,
            order = 20)
    @WikiReturn("<key:paint.method.blend_mode_set.return>")
    public Paint blend(@WikiParameter(name = "blendMode", description = "<key:paint.param.blendMode>") BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
        return this;
    }

    /**
     * @return {@code true}, если stroke должен рисоваться пунктиром
     */
    @WikiMethod(
            title = "<key:paint.method.dashed.title>",
            summary = "<key:paint.method.dashed.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 10)
    @WikiReturn("<key:paint.method.dashed.return>")
    public boolean dashed() {
        return dashLength > 0.0f && dashGap > 0.0f;
    }

    /**
     * @return длина видимого dash-сегмента
     */
    @WikiMethod(
            title = "<key:paint.method.dash_length_get.title>",
            summary = "<key:paint.method.dash_length_get.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 20)
    @WikiReturn("<key:paint.method.dash_length_get.return>")
    public float dashLength() {
        return dashLength;
    }

    /**
     * @return длина gap между dash-сегментами
     */
    @WikiMethod(
            title = "<key:paint.method.dash_gap_get.title>",
            summary = "<key:paint.method.dash_gap_get.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 30)
    @WikiReturn("<key:paint.method.dash_gap_get.return>")
    public float dashGap() {
        return dashGap;
    }

    /**
     * @return смещение dash pattern
     */
    @WikiMethod(
            title = "<key:paint.method.dash_offset_get.title>",
            summary = "<key:paint.method.dash_offset_get.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 40)
    @WikiReturn("<key:paint.method.dash_offset_get.return>")
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
    @WikiMethod(
            title = "<key:paint.method.dash_set.title>",
            summary = "<key:paint.method.dash_set.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 50)
    @WikiReturn("<key:paint.method.dash_set.return>")
    public Paint dash(@WikiParameter(name = "length", description = "<key:paint.param.length>") float length, @WikiParameter(name = "gap", description = "<key:paint.param.gap>") float gap) {
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
    @WikiMethod(
            title = "<key:paint.method.dash_offset_set.title>",
            summary = "<key:paint.method.dash_offset_set.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 60)
    @WikiReturn("<key:paint.method.dash_offset_set.return>")
    public Paint dashOffset(@WikiParameter(name = "dashOffset", description = "<key:paint.param.dashOffset>") float dashOffset) {
        this.dashOffset = Float.isFinite(dashOffset) ? dashOffset : 0.0f;
        return this;
    }

    /**
     * Очищает dash pattern.
     *
     * @return этот paint для fluent-настройки
     */
    @WikiMethod(
            title = "<key:paint.method.clear_dash.title>",
            summary = "<key:paint.method.clear_dash.summary>",
            section = "<key:paint.api.section.dash>",
            sectionOrder = 50,
            order = 70)
    @WikiReturn("<key:paint.method.clear_dash.return>")
    public Paint clearDash() {
        dashLength = 0.0f;
        dashGap = 0.0f;
        dashOffset = 0.0f;
        return this;
    }

    /**
     * @return независимая копия paint'а
     */
    @WikiMethod(
            title = "<key:paint.method.copy.title>",
            summary = "<key:paint.method.copy.summary>",
            section = "<key:paint.api.section.lifecycle>",
            sectionOrder = 60,
            order = 10)
    @WikiReturn("<key:paint.method.copy.return>")
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
