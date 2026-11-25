package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Вид примитивов, которыми рисует UniGUI.
 *
 * <p>В 26.1 это {@code VertexFormat.Mode}, в 26.2 — {@code PrimitiveTopology}. Общий код линейки 26.x
 * использует этот enum, а перевод в тип конкретной версии делает {@link MinecraftGpuCompat}.</p>
 */
enum UiPrimitive {
    /** Отдельные треугольники. */
    TRIANGLES,
    /** Четырёхугольники (рисуются как два треугольника через общий индексный буфер). */
    QUADS
}
