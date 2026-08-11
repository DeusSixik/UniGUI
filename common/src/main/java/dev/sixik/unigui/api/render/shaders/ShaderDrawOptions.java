package dev.sixik.unigui.api.render.shaders;

/**
 * Backend-facing options for shader draw commands.
 */
public final class ShaderDrawOptions {
    private boolean builtinUniforms = true;
    private boolean blend = true;
    private float squareVertexOffset = -0.25f;

    public static ShaderDrawOptions defaults() {
        return new ShaderDrawOptions();
    }

    public boolean builtinUniforms() {
        return builtinUniforms;
    }

    public ShaderDrawOptions builtinUniforms(boolean builtinUniforms) {
        this.builtinUniforms = builtinUniforms;
        return this;
    }

    public boolean blend() {
        return blend;
    }

    public ShaderDrawOptions blend(boolean blend) {
        this.blend = blend;
        return this;
    }

    /**
     * Offset applied to the auto {@code SquareVertex} uniform. The default keeps
     * compatibility with the existing SDMShop2 full-screen quad shaders.
     */
    public float squareVertexOffset() {
        return squareVertexOffset;
    }

    public ShaderDrawOptions squareVertexOffset(float squareVertexOffset) {
        this.squareVertexOffset = Float.isFinite(squareVertexOffset) ? squareVertexOffset : 0.0f;
        return this;
    }

    public ShaderDrawOptions copy() {
        return new ShaderDrawOptions()
                .builtinUniforms(builtinUniforms)
                .blend(blend)
                .squareVertexOffset(squareVertexOffset);
    }
}