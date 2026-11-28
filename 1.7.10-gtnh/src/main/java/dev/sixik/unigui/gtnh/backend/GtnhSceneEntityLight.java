package dev.sixik.unigui.gtnh.backend;

import net.minecraft.entity.Entity;

/**
 * The entity a scene is drawing right now. Kept free of client classes, because {@link Entity#getBrightness} asks
 * it from the server thread as well.
 */
public final class GtnhSceneEntityLight {
    /**
     * Entity inside {@link GtnhSceneContent#drawEntity}, or null; written on the render thread only.
     */
    static Entity drawing;

    private GtnhSceneEntityLight() {
    }

    /**
     * @param entity entity asking for its brightness
     * @return whether a scene is drawing this entity, so it is lit by the scene and not by the world
     */
    public static boolean isDrawing(Entity entity) {
        return entity == drawing;
    }
}
