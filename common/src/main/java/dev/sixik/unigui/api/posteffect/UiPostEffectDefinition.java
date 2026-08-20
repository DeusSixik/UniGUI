package dev.sixik.unigui.api.posteffect;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Именованное описание UI PostEffect.
 *
 * <p>Definition является immutable-данными: стабильный id, упорядоченный список pass'ов и флаг
 * включения по умолчанию. Такой объект можно зарегистрировать в {@link UiPostEffectRegistry},
 * сослаться на него из Java/XML/StylePack и позже показать в редакторе как обычный набор свойств.</p>
 */
public final class UiPostEffectDefinition {
    private final String id;
    private final List<UiPostEffectPass> passes;
    private final boolean enabledByDefault;

    private UiPostEffectDefinition(String id, List<UiPostEffectPass> passes, boolean enabledByDefault) {
        this.id = normalizeId(id);
        if (passes == null || passes.isEmpty()) {
            throw new IllegalArgumentException("PostEffect definition must contain at least one pass");
        }
        ObjectArrayList<UiPostEffectPass> copy = new ObjectArrayList<>(passes.size());
        for (UiPostEffectPass pass : passes) {
            copy.add(Objects.requireNonNull(pass, "pass").copy());
        }
        this.passes = Collections.unmodifiableList(copy);
        this.enabledByDefault = enabledByDefault;
    }

    /** Начинает сборку definition. */
    public static Builder create(String id) {
        return new Builder(id);
    }

    /** Создаёт definition из одного pass'а с тем же id, что и эффект. */
    public static UiPostEffectDefinition singlePass(String id) {
        return create(id).pass(id).build();
    }

    /** @return id эффекта */
    public String id() {
        return id;
    }

    /** @return read-only список pass'ов */
    public List<UiPostEffectPass> passes() {
        return passes;
    }

    /** @return {@code true}, если эффект можно включать дефолтно в UI/editor presets */
    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    /** @return независимая копия definition */
    public UiPostEffectDefinition copy() {
        return new UiPostEffectDefinition(id, passes, enabledByDefault);
    }

    private static String normalizeId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("PostEffect id must not be empty");
        }
        return normalized;
    }

    /** Builder для программного создания PostEffect definition'ов. */
    public static final class Builder {
        private final String id;
        private final ObjectArrayList<UiPostEffectPass> passes = new ObjectArrayList<>();
        private boolean enabledByDefault = true;

        private Builder(String id) {
            this.id = normalizeId(id);
        }

        /** @return этот builder после добавления pass'а по shader id */
        public Builder pass(String shaderId) {
            passes.add(UiPostEffectPass.shader(shaderId));
            return this;
        }

        /** @return этот builder после добавления pass'а с uniforms callback */
        public Builder pass(String shaderId, Consumer<UiPostEffectUniforms> uniforms) {
            passes.add(UiPostEffectPass.shader(shaderId).uniforms(uniforms));
            return this;
        }

        /** @return этот builder после добавления готового pass'а */
        public Builder pass(UiPostEffectPass pass) {
            passes.add(Objects.requireNonNull(pass, "pass").copy());
            return this;
        }

        /** @return этот builder после установки флага включения по умолчанию */
        public Builder enabledByDefault(boolean enabledByDefault) {
            this.enabledByDefault = enabledByDefault;
            return this;
        }

        /** @return immutable definition */
        public UiPostEffectDefinition build() {
            return new UiPostEffectDefinition(id, passes, enabledByDefault);
        }
    }
}