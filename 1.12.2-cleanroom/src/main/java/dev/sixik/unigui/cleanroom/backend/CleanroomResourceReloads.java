package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;

/**
 * Counts resource reloads, so caches of resource-derived objects (shader programs) can notice that they are stale
 * without each of them registering its own listener, which would keep every closed screen's renderer alive.
 */
final class CleanroomResourceReloads {
    private static int generation;
    private static boolean registered;

    private CleanroomResourceReloads() {
    }

    /**
     * @return a number that changes after every resource reload; compare it with the value seen last time
     */
    static synchronized int generation() {
        if (!registered) {
            registered = true;
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.getResourceManager() instanceof IReloadableResourceManager manager) {
                manager.registerReloadListener(resourceManager -> advance());
            }
        }
        return generation;
    }

    private static synchronized void advance() {
        generation++;
    }
}
