package dev.sixik.unigui.api.render.shaders;

import java.util.Arrays;

/**
 * Immutable значение одного shader uniform.
 *
 * <p>Uniform хранит тип и либо float-array payload, либо int payload. Массивы копируются при создании
 * и чтении, чтобы draw-команда не зависела от внешнего mutable массива.</p>
 */
public final class ShaderUniform {
    /** Поддерживаемые типы shader uniform. */
    public enum Type {
        FLOAT,
        INT,
        VEC2,
        VEC3,
        VEC4,
        MAT4
    }

    private final Type type;
    private final float[] floats;
    private final int integer;

    private ShaderUniform(Type type, float[] floats, int integer) {
        this.type = type;
        this.floats = floats == null ? new float[0] : Arrays.copyOf(floats, floats.length);
        this.integer = integer;
    }

    /**
     * Создаёт float uniform.
     *
     * @param value float-значение
     * @return uniform типа {@link Type#FLOAT}
     */
    public static ShaderUniform float1(float value) {
        return new ShaderUniform(Type.FLOAT, new float[]{value}, 0);
    }

    /**
     * Создаёт int uniform.
     *
     * @param value int-значение
     * @return uniform типа {@link Type#INT}
     */
    public static ShaderUniform int1(int value) {
        return new ShaderUniform(Type.INT, null, value);
    }

    /** @return uniform типа {@link Type#VEC2} */
    public static ShaderUniform vec2(float x, float y) {
        return new ShaderUniform(Type.VEC2, new float[]{x, y}, 0);
    }

    /** @return uniform типа {@link Type#VEC3} */
    public static ShaderUniform vec3(float x, float y, float z) {
        return new ShaderUniform(Type.VEC3, new float[]{x, y, z}, 0);
    }

    /** @return uniform типа {@link Type#VEC4} */
    public static ShaderUniform vec4(float x, float y, float z, float w) {
        return new ShaderUniform(Type.VEC4, new float[]{x, y, z, w}, 0);
    }

    /**
     * Создаёт mat4 uniform.
     *
     * @param values ровно 16 float-значений
     * @return uniform типа {@link Type#MAT4}
     */
    public static ShaderUniform mat4(float[] values) {
        if (values == null || values.length != 16) {
            throw new IllegalArgumentException("mat4 uniform requires exactly 16 values");
        }
        return new ShaderUniform(Type.MAT4, values, 0);
    }

    /** @return тип uniform */
    public Type type() {
        return type;
    }

    /** @return копия float payload */
    public float[] floats() {
        return Arrays.copyOf(floats, floats.length);
    }

    /** @return int payload uniform'а */
    public int integer() {
        return integer;
    }

    /** @return независимая копия uniform */
    public ShaderUniform copy() {
        return new ShaderUniform(type, floats, integer);
    }
}