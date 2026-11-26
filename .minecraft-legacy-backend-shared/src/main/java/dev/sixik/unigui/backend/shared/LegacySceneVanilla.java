package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;

/**
 * Draws {@link dev.sixik.unigui.api.scene.ItemNode} and {@link dev.sixik.unigui.api.scene.EntityNode} content
 * through the game's own fixed-function renderers inside a {@link LegacyGlSceneRenderer} pass.
 *
 * <p>The scene renderer binds the scene target with its depth, loads the projection and calls
 * {@link #beginVanilla()} with the camera view in the modelview matrix, so lights set up there stay fixed in the
 * world. Before each draw the modelview holds {@code view * nodeWorld}. Opaque scene geometry is already in the
 * depth buffer; translucent geometry is drawn afterwards. Implementations change GL state through the game's own
 * state tracking and return it to the UI defaults in {@link #endVanilla()}.</p>
 */
public interface LegacySceneVanilla {
    /**
     * Prepares lighting, depth and blending for vanilla draws.
     */
    void beginVanilla();

    /**
     * Draws an item model centered at the origin of the current modelview, one block wide.
     *
     * @param node        item node, for its {@link ItemNode#display()}.
     * @param item        native item resolved by the platform.
     * @param partialTick fraction of the current game tick for animations.
     * @return false when the item could not be drawn.
     */
    boolean drawItem(ItemNode node, Object item, float partialTick);

    /**
     * Draws an entity standing with its feet at the origin of the current modelview.
     *
     * @param node        entity node, for its head pose.
     * @param entity      native entity handle resolved by the platform.
     * @param partialTick fraction of the current game tick for animations.
     * @return false when the entity is not available yet or could not be drawn.
     */
    boolean drawEntity(EntityNode node, Object entity, float partialTick);

    /**
     * Returns the GL state to the UI defaults after the vanilla draws.
     */
    void endVanilla();
}
