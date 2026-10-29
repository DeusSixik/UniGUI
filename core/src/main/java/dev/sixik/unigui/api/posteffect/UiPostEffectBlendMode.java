package dev.sixik.unigui.api.posteffect;

/**
 * Режим смешивания одного PostEffect pass'а.
 *
 * <p>В первом срезе режим используется backend-ом как подсказка для shader draw command.
 * Большинство post-processing pass'ов должны писать в пустой временный target через {@link #REPLACE}.</p>
 */
public enum UiPostEffectBlendMode {
    /** Pass заменяет содержимое target'а. Это дефолтный режим для ping-pong post-processing. */
    REPLACE(false),

    /** Pass смешивается обычным alpha blending. */
    ALPHA_BLEND(true),

    /** Additive режим зарезервирован для backend-ов, которые умеют отдельные blend equations. */
    ADDITIVE(true);

    private final boolean blendEnabled;

    UiPostEffectBlendMode(boolean blendEnabled) {
        this.blendEnabled = blendEnabled;
    }

    /** @return {@code true}, если backend должен включить blending для pass'а */
    public boolean blendEnabled() {
        return blendEnabled;
    }
}