package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.scene.EntityActionClip;

/**
 * Vanilla action presets for living entity models rendered by GTNH 1.7.10.
 */
public final class GtnhEntityActions {
    /**
     * The default six-tick swing at 20 ticks per second.
     */
    private static final EntityActionClip<GtnhEntityPose> SWING =
        new EntityActionClip<>(6f / 20, GtnhEntityPose.class, GtnhEntityPose::swing);

    private GtnhEntityActions() {
    }

    /**
     * @return a seekable vanilla main-hand swing clip, usable with a scene timeline track
     */
    public static EntityActionClip<?> mainHandSwing() {
        return SWING;
    }
}
