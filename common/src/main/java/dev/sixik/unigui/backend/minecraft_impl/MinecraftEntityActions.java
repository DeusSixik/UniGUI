package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.scene.EntityActionClip;

/**
 * Ready-to-use vanilla actions for scene entity tracks.
 */
@WikiPage(title = "MinecraftEntityActions", slug = "minecraft-entity-actions", category = "<key:scene.category>",
        summary = "<key:minecraft.entity.actions.summary>")
@WikiLink(label = "EntityActionClip", target = "page:entity-action-clip")
@AiDoc(summary = "Library of native Scene3D entity actions for Minecraft 1.20.1, 1.21.1 and 26.x.",
        related = {"EntityActionClip", "EntityNode", "SceneTimeline"})
public final class MinecraftEntityActions {
    /**
     * This class supplies immutable action presets.
     */
    private MinecraftEntityActions() {
    }

    /**
     * Plays the default vanilla main-hand swing on compatible living entity models.
     * The model determines the pose, including the movement of any held item.
     *
     * @return reusable swing clip for Minecraft 1.20.1, 1.21.1 and 26.x
     */
    @WikiMethod(summary = "<key:minecraft.entity.actions.api.main.hand.swing.summary>")
    @WikiReturn("<key:scene.api.return.swing>")
    @AiDoc(summary = "Returns a vanilla main-hand swing clip. Use it on a SceneTimeline entity track.")
    public static EntityActionClip<?> mainHandSwing() {
        return MinecraftClientCompat.mainHandSwing();
    }
}
