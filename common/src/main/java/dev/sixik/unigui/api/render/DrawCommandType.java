package dev.sixik.unigui.api.render;

/**
 * Тип команды в retained draw list.
 *
 * <p>{@link DrawCommand} хранит общий набор полей, а этот enum говорит backend'у, какие из них
 * нужно интерпретировать: bounds/paint для прямоугольника, texture/uv для изображения,
 * shader/uniforms для shader-команды и так далее.</p>
 */
public enum DrawCommandType {
    /** Прямоугольник без скругления. */
    RECT,
    /** Прямоугольник со скруглёнными углами. */
    ROUNDED_RECT,
    /** Линия между двумя точками. */
    LINE,
    /** Эллипс или круг внутри bounds. */
    CIRCLE,
    /** Векторный path. */
    PATH,
    /** Plain или rich text. */
    TEXT,
    /** Текстура внутри bounds/uv. */
    TEXTURE,
    /** Произвольный текстурированный quad с четырьмя позициями и UV. */
    TEXTURED_QUAD,
    /** Начало clip/scissor области. */
    PUSH_CLIP,
    /** Завершение последней clip/scissor области. */
    POP_CLIP,
    /** Произвольный triangle mesh. */
    MESH,
    /** Quad, который рисуется через shader. */
    SHADER,
    /** Низкоуровневая draw command marker/заглушка. */
    DRAW_CMD,
    /** Backend callback через {@link CustomDraw}. */
    CUSTOM
}
