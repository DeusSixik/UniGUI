package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.renderer.state.gui.GuiItemRenderState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Прозрачность ванильных предметов в GUI Minecraft 26.1.
 *
 * <p>В 26.1 предмет GUI рисуется позже извлечения: {@code GuiRenderer} кладёт его в атлас предметов и переносит
 * на экран blit'ом с цветом {@code -1}, то есть всегда непрозрачно. UniGUI запоминает нужную прозрачность для
 * состояния предмета в момент его добавления ({@link #pending(float)} вокруг {@code extractor.item}), а миксин
 * {@code GuiRenderer} подставляет её в цвет blit'а.</p>
 */
public final class UiItemAlpha {
    private static final Map<GuiItemRenderState, Float> ALPHA = new WeakHashMap<>();
    private static float pending = 1.0f;

    private UiItemAlpha() {
    }

    /**
     * Задаёт прозрачность для предметов, которые будут добавлены следующими.
     *
     * @param alpha прозрачность 0..1; {@code 1} выключает подмену
     */
    static void pending(float alpha) {
        pending = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    /**
     * Вызывается миксином при добавлении предмета в {@code GuiRenderState}.
     *
     * @param state состояние предмета
     */
    public static void capture(GuiItemRenderState state) {
        if (pending < 1.0f && state != null) ALPHA.put(state, pending);
    }

    /**
     * Цвет blit'а предмета из атласа.
     *
     * <p>Пайплайн premultiplied alpha: цвет умножается на текстуру, поэтому для прозрачности {@code a} нужен
     * цвет {@code (a, a, a, a)}, а не только альфа.</p>
     *
     * @param state    состояние предмета
     * @param original исходный цвет
     * @return цвет с учётом прозрачности
     */
    public static int color(GuiItemRenderState state, int original) {
        Float alpha = state == null ? null : ALPHA.get(state);
        if (alpha == null) return original;
        int channel = Math.round(alpha * 255.0f);
        return channel << 24 | channel << 16 | channel << 8 | channel;
    }
}
