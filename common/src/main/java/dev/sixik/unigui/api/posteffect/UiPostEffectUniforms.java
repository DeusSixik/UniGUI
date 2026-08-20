package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.shaders.ShaderUniform;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Набор uniforms для одного UI PostEffect pass'а.
 *
 * <p>Класс является тонкой надстройкой над {@link ShaderUniforms}: backend загружает обычные shader
 * uniforms, а PostEffect-слой добавляет динамические значения через {@link Supplier}. Так будущий HUD
 * сможет брать состояние host-а снаружи, не протаскивая Minecraft-типы в core API.</p>
 */
public final class UiPostEffectUniforms {
    private final LinkedHashMap<String, UniformValue> values = new LinkedHashMap<>();

    /** @return новый пустой набор uniforms */
    public static UiPostEffectUniforms create() {
        return new UiPostEffectUniforms();
    }

    /** @return пустой набор uniforms */
    public static UiPostEffectUniforms empty() {
        return new UiPostEffectUniforms();
    }

    /** Записывает статический shader uniform. */
    public UiPostEffectUniforms set(String name, ShaderUniform uniform) {
        values.put(normalizeName(name), new StaticUniformValue(Objects.requireNonNull(uniform, "uniform").copy()));
        return this;
    }

    /** Записывает динамический uniform, который разрешается перед каждым pass'ом. */
    public UiPostEffectUniforms setDynamic(String name, Supplier<ShaderUniform> supplier) {
        values.put(normalizeName(name), new DynamicUniformValue(Objects.requireNonNull(supplier, "supplier")));
        return this;
    }

    /** @return этот набор после записи float uniform */
    public UiPostEffectUniforms floatValue(String name, float value) {
        return set(name, ShaderUniform.float1(value));
    }

    /** @return этот набор после записи динамического float uniform */
    public UiPostEffectUniforms floatSupplier(String name, Supplier<Float> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return setDynamic(name, () -> ShaderUniform.float1(safeFloat(supplier.get(), 0.0f)));
    }

    /** @return этот набор после записи int uniform */
    public UiPostEffectUniforms intValue(String name, int value) {
        return set(name, ShaderUniform.int1(value));
    }

    /** @return этот набор после записи boolean uniform как int {@code 0/1} */
    public UiPostEffectUniforms boolValue(String name, boolean value) {
        return intValue(name, value ? 1 : 0);
    }

    /** @return этот набор после записи vec2 uniform */
    public UiPostEffectUniforms vec2(String name, float x, float y) {
        return set(name, ShaderUniform.vec2(x, y));
    }

    /** @return этот набор после записи vec3 uniform */
    public UiPostEffectUniforms vec3(String name, float x, float y, float z) {
        return set(name, ShaderUniform.vec3(x, y, z));
    }

    /** @return этот набор после записи vec4 uniform */
    public UiPostEffectUniforms vec4(String name, float x, float y, float z, float w) {
        return set(name, ShaderUniform.vec4(x, y, z, w));
    }

    /** Записывает цвет как vec4 uniform. */
    public UiPostEffectUniforms color(String name, ColorView color) {
        if (color == null) {
            return vec4(name, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        return vec4(name, color.r(), color.g(), color.b(), color.a());
    }

    /** @return {@code true}, если набор пустой */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Разрешает статические и динамические uniforms в обычный {@link ShaderUniforms}. */
    public ShaderUniforms resolve() {
        ShaderUniforms resolved = ShaderUniforms.create();
        for (Map.Entry<String, UniformValue> entry : values.entrySet()) {
            ShaderUniform uniform = entry.getValue().resolve();
            if (uniform != null) {
                resolved.set(entry.getKey(), uniform);
            }
        }
        return resolved;
    }

    /** @return независимая копия набора uniforms */
    public UiPostEffectUniforms copy() {
        UiPostEffectUniforms copy = new UiPostEffectUniforms();
        for (Map.Entry<String, UniformValue> entry : values.entrySet()) {
            copy.values.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Uniform name must not be empty");
        }
        return normalized;
    }

    private static float safeFloat(Float value, float fallback) {
        if (value == null || !Float.isFinite(value)) return fallback;
        return value;
    }

    private interface UniformValue {
        ShaderUniform resolve();

        UniformValue copy();
    }

    private record StaticUniformValue(ShaderUniform uniform) implements UniformValue {
        @Override
        public ShaderUniform resolve() {
            return uniform.copy();
        }

        @Override
        public UniformValue copy() {
            return new StaticUniformValue(uniform.copy());
        }
    }

    private record DynamicUniformValue(Supplier<ShaderUniform> supplier) implements UniformValue {
        @Override
        public ShaderUniform resolve() {
            ShaderUniform uniform = supplier.get();
            return uniform == null ? null : uniform.copy();
        }

        @Override
        public UniformValue copy() {
            return new DynamicUniformValue(supplier);
        }
    }
}