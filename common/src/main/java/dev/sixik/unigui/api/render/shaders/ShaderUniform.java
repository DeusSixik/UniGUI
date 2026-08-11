package dev.sixik.unigui.api.render.shaders;

import java.util.Arrays;

public final class ShaderUniform {
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

    public static ShaderUniform float1(float value) {
        return new ShaderUniform(Type.FLOAT, new float[]{value}, 0);
    }

    public static ShaderUniform int1(int value) {
        return new ShaderUniform(Type.INT, null, value);
    }

    public static ShaderUniform vec2(float x, float y) {
        return new ShaderUniform(Type.VEC2, new float[]{x, y}, 0);
    }

    public static ShaderUniform vec3(float x, float y, float z) {
        return new ShaderUniform(Type.VEC3, new float[]{x, y, z}, 0);
    }

    public static ShaderUniform vec4(float x, float y, float z, float w) {
        return new ShaderUniform(Type.VEC4, new float[]{x, y, z, w}, 0);
    }

    public static ShaderUniform mat4(float[] values) {
        if (values == null || values.length != 16) {
            throw new IllegalArgumentException("mat4 uniform requires exactly 16 values");
        }
        return new ShaderUniform(Type.MAT4, values, 0);
    }

    public Type type() {
        return type;
    }

    public float[] floats() {
        return Arrays.copyOf(floats, floats.length);
    }

    public int integer() {
        return integer;
    }

    public ShaderUniform copy() {
        return new ShaderUniform(type, floats, integer);
    }
}