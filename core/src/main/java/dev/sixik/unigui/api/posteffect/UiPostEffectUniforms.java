package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@WikiPage(
        title = "UiPostEffectUniforms",
        slug = "ui-post-effect-uniforms",
        category = "<key:ui.post.effect.uniforms.category>",
        summary = "<key:ui.post.effect.uniforms.summary>",
        order = 50
)
@WikiSection(
        title = "<key:ui.post.effect.uniforms.section.overview.title>",
        markdown = "<key:ui.post.effect.uniforms.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.post.effect.uniforms.example.dynamic.title>",
        code = "<key:ui.post.effect.uniforms.example.dynamic.code>",
        order = 20
)
@AiDoc(
        summary = "Container for post-processing shader uniforms with static values and dynamic suppliers.",
        purpose = "Allows binding animated properties (time, player stats, color pulses) without recreating pass objects.",
        useWhen = {"Configuring shader parameters on a UiPostEffectPass."},
        related = {"UiPostEffectPass", "ShaderUniforms", "ShaderUniform"}
)
public final class UiPostEffectUniforms {
    private final LinkedHashMap<String, UniformValue> values = new LinkedHashMap<>();

    /** @return новый пустой набор uniforms */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.create.title>",
            summary = "<key:ui.post.effect.uniforms.method.create.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.create.return>")
    public static UiPostEffectUniforms create() {
        return new UiPostEffectUniforms();
    }

    /** @return пустой набор uniforms */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.empty.title>",
            summary = "<key:ui.post.effect.uniforms.method.empty.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.empty.return>")
    public static UiPostEffectUniforms empty() {
        return new UiPostEffectUniforms();
    }

    /** Записывает статический shader uniform. */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.set.title>",
            summary = "<key:ui.post.effect.uniforms.method.set.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.set.return>")
    public UiPostEffectUniforms set(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.set.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.set.uniform>") ShaderUniform uniform) {
        values.put(normalizeName(name), new StaticUniformValue(Objects.requireNonNull(uniform, "uniform").copy()));
        return this;
    }

    /** Записывает динамический uniform, который разрешается перед каждым pass'ом. */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.set_dynamic.title>",
            summary = "<key:ui.post.effect.uniforms.method.set_dynamic.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.set_dynamic.return>")
    public UiPostEffectUniforms setDynamic(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.set_dynamic.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.set_dynamic.supplier>") Supplier<ShaderUniform> supplier) {
        values.put(normalizeName(name), new DynamicUniformValue(Objects.requireNonNull(supplier, "supplier")));
        return this;
    }

    /** @return этот набор после записи float uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.float_value.title>",
            summary = "<key:ui.post.effect.uniforms.method.float_value.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.float_value.return>")
    public UiPostEffectUniforms floatValue(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.float_value.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.float_value.value>") float value) {
        return set(name, ShaderUniform.float1(value));
    }

    /** @return этот набор после записи динамического float uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.float_supplier.title>",
            summary = "<key:ui.post.effect.uniforms.method.float_supplier.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.float_supplier.return>")
    public UiPostEffectUniforms floatSupplier(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.float_supplier.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.float_supplier.supplier>") Supplier<Float> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return setDynamic(name, () -> ShaderUniform.float1(safeFloat(supplier.get(), 0.0f)));
    }

    /** @return этот набор после записи int uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.int_value.title>",
            summary = "<key:ui.post.effect.uniforms.method.int_value.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.int_value.return>")
    public UiPostEffectUniforms intValue(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.int_value.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.int_value.value>") int value) {
        return set(name, ShaderUniform.int1(value));
    }

    /** @return этот набор после записи boolean uniform как int {@code 0/1} */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.bool_value.title>",
            summary = "<key:ui.post.effect.uniforms.method.bool_value.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.bool_value.return>")
    public UiPostEffectUniforms boolValue(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.bool_value.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.bool_value.value>") boolean value) {
        return intValue(name, value ? 1 : 0);
    }

    /** @return этот набор после записи vec2 uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.vec2.title>",
            summary = "<key:ui.post.effect.uniforms.method.vec2.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.vec2.return>")
    public UiPostEffectUniforms vec2(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec2.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec2.x>") float x,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec2.y>") float y) {
        return set(name, ShaderUniform.vec2(x, y));
    }

    /** @return этот набор после записи vec3 uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.vec3.title>",
            summary = "<key:ui.post.effect.uniforms.method.vec3.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.vec3.return>")
    public UiPostEffectUniforms vec3(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec3.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec3.x>") float x,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec3.y>") float y,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec3.z>") float z) {
        return set(name, ShaderUniform.vec3(x, y, z));
    }

    /** @return этот набор после записи vec4 uniform */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.vec4.title>",
            summary = "<key:ui.post.effect.uniforms.method.vec4.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.vec4.return>")
    public UiPostEffectUniforms vec4(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec4.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec4.x>") float x,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec4.y>") float y,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec4.z>") float z,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.vec4.w>") float w) {
        return set(name, ShaderUniform.vec4(x, y, z, w));
    }

    /** Записывает цвет как vec4 uniform. */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.color.title>",
            summary = "<key:ui.post.effect.uniforms.method.color.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.color.return>")
    public UiPostEffectUniforms color(
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.color.name>") String name,
            @WikiParameter(description = "<key:ui.post.effect.uniforms.method.color.color>") ColorView color) {
        if (color == null) {
            return vec4(name, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        return vec4(name, color.r(), color.g(), color.b(), color.a());
    }

    /** @return {@code true}, если набор пустой */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.is_empty.title>",
            summary = "<key:ui.post.effect.uniforms.method.is_empty.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.is_empty.return>")
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Разрешает статические и динамические uniforms в обычный {@link ShaderUniforms}. */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.resolve.title>",
            summary = "<key:ui.post.effect.uniforms.method.resolve.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.resolve.return>")
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

    /** @return независимая копия этого набора uniforms */
    @WikiMethod(
            title = "<key:ui.post.effect.uniforms.method.copy.title>",
            summary = "<key:ui.post.effect.uniforms.method.copy.summary>"
    )
    @WikiReturn("<key:ui.post.effect.uniforms.method.copy.return>")
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
        return value == null || !Float.isFinite(value) ? fallback : value;
    }

    @AiDoc(
            summary = "Internal abstraction representing a static or dynamically evaluated uniform value.",
            purpose = "Encapsulates uniform resolution during shader execution.",
            useWhen = {"Interacting with internal uniform values."},
            related = {"UiPostEffectUniforms"}
    )
    private interface UniformValue {
        ShaderUniform resolve();

        UniformValue copy();
    }

    private static final class StaticUniformValue implements UniformValue {
        private final ShaderUniform uniform;

        private StaticUniformValue(ShaderUniform uniform) {
            this.uniform = Objects.requireNonNull(uniform, "uniform");
        }

        @Override
        public ShaderUniform resolve() {
            return uniform;
        }

        @Override
        public UniformValue copy() {
            return new StaticUniformValue(uniform.copy());
        }
    }

    private static final class DynamicUniformValue implements UniformValue {
        private final Supplier<ShaderUniform> supplier;

        private DynamicUniformValue(Supplier<ShaderUniform> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public ShaderUniform resolve() {
            try {
                ShaderUniform value = supplier.get();
                return value == null ? null : value.copy();
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public UniformValue copy() {
            return new DynamicUniformValue(supplier);
        }
    }
}
