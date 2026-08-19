package dev.sixik.unigui.api.render.shaders;

/**
 * Backend-facing параметры shader draw-команды.
 *
 * <p>Options управляют тем, должен ли backend добавить стандартные uniforms, включить blending и
 * как сместить автоматически передаваемый {@code SquareVertex}. Это отдельный объект, чтобы
 * {@link dev.sixik.unigui.api.render.DrawCommand} оставался backend-neutral.</p>
 */
public final class ShaderDrawOptions {
    private boolean builtinUniforms = true;
    private boolean blend = true;
    private float squareVertexOffset = -0.25f;

    /** @return новый options object со стандартными значениями */
    public static ShaderDrawOptions defaults() {
        return new ShaderDrawOptions();
    }

    /** @return {@code true}, если backend должен добавить стандартные uniforms */
    public boolean builtinUniforms() {
        return builtinUniforms;
    }

    /**
     * Включает или выключает стандартные uniforms.
     *
     * @param builtinUniforms новое значение
     * @return этот options object
     */
    public ShaderDrawOptions builtinUniforms(boolean builtinUniforms) {
        this.builtinUniforms = builtinUniforms;
        return this;
    }

    /** @return {@code true}, если shader-команда должна использовать blending */
    public boolean blend() {
        return blend;
    }

    /**
     * Включает или выключает blending.
     *
     * @param blend новое значение
     * @return этот options object
     */
    public ShaderDrawOptions blend(boolean blend) {
        this.blend = blend;
        return this;
    }

    /**
     * Возвращает смещение для автоматически передаваемого {@code SquareVertex} uniform.
     *
     * <p>Дефолт сохраняет совместимость с существующими full-screen quad shaders из SDMShop2.</p>
     *
     * @return uniform со смещением вершин квадрата
     */
    public float squareVertexOffset() {
        return squareVertexOffset;
    }

    /**
     * Задаёт смещение для {@code SquareVertex} uniform.
     *
     * @param squareVertexOffset новое смещение
     * @return этот options object
     */
    public ShaderDrawOptions squareVertexOffset(float squareVertexOffset) {
        this.squareVertexOffset = Float.isFinite(squareVertexOffset) ? squareVertexOffset : 0.0f;
        return this;
    }

    /** @return независимая копия options */
    public ShaderDrawOptions copy() {
        return new ShaderDrawOptions()
                .builtinUniforms(builtinUniforms)
                .blend(blend)
                .squareVertexOffset(squareVertexOffset);
    }
}