package dev.sixik.unigui.api.render;

/**
 * Режим смешивания, который draw-команда запрашивает у render backend'а.
 *
 * <p>Backend может сопоставлять эти значения со своими GPU blend state. Команда хранит только
 * намерение рендера, а не конкретные OpenGL/DirectX/Vulkan параметры.</p>
 */
public enum BlendMode {
    /** Обычное alpha blending для UI. */
    NORMAL,

    /** Additive blending для glow, light и highlight эффектов. */
    ADDITIVE
}