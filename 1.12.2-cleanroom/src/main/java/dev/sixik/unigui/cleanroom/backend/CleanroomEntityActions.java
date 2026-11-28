package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.scene.EntityActionClip;

/**
 * Vanilla action presets for living entity models rendered by Cleanroom 1.12.2.
 */
public final class CleanroomEntityActions {
    /**
     * The default six-tick swing at 20 ticks per second.
     */
    private static final EntityActionClip<CleanroomEntityPose> SWING =
            new EntityActionClip<>(6f / 20, CleanroomEntityPose.class, CleanroomEntityPose::swing);

    private CleanroomEntityActions() {
    }

    /**
     * @return a seekable vanilla main-hand swing clip, usable with a scene timeline track
     */
    public static EntityActionClip<?> mainHandSwing() {
        return SWING;
    }
}
