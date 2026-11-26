package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.function.Supplier;

/**
 * Exercises the offscreen scene pass and ordinary UI rendering on both legacy clients.
 */
public final class LegacySceneDemo {
    /**
     * Unit square in the XY plane, with CCW winding viewed from +Z.
     */
    private static final Mesh3D PANEL = new Mesh3D(
            new float[]{-1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0},
            new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1},
            new float[]{0, 1, 1, 1, 1, 0, 0, 0}, null, new int[]{0, 1, 2, 0, 2, 3});

    private LegacySceneDemo() {
    }

    /**
     * Opens the scene using the installed UniGUI runtime.
     *
     * @param furnace factory for the version-specific block model.
     */
    public static void open(Supplier<? extends SceneNode> furnace) {
        Scene3D viewport = new Scene3D().highlightObjects(true).highlightBlocks(true);
        viewport.updateMode(RenderToTexture.UpdateMode.EVERY_FRAME);
        viewport.resolutionScale(1.5f);
        viewport.clearColor().set(0.045f, 0.073f, 0.113f, 1);
        viewport.camera().setPerspective((float) Math.toRadians(45), 0.2f, 100);
        viewport.orbit().setOrbit(new MutableVec3(0, 0.12f, 0), 6, 0.29, 0.2);

        SceneNode planks = new SceneNode().setMesh(PANEL).setPosition(-0.67f, 0.08f, 0.25f)
                .setMaterial(new Material(0xFFFFFFFF,
                        new SimpleTextureHandle("minecraft:textures/blocks/planks_oak.png", 16, 16),
                        Material.Shading.LIT, Material.AlphaMode.OPAQUE, 0, Material.Culling.NONE));
        SceneNode back = new SceneNode().setMesh(PANEL).setPosition(0.48f, 0.12f, -0.75f)
                .setMaterial(new Material(0xFF4272D7, null, Material.Shading.UNLIT,
                        Material.AlphaMode.OPAQUE, 0, Material.Culling.NONE));
        SceneNode glass = new SceneNode().setMesh(PANEL).setPosition(0.95f, -0.40f, 0.70f)
                .setScale(0.60f, 0.60f, 0.60f)
                .setMaterial(new Material(0x9052E6B5, null, Material.Shading.UNLIT,
                        Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.NONE));
        SceneNode block = furnace.get().setPosition(0.12f, -0.20f, 0.52f);
        viewport.scene().addRoot(planks).addRoot(back).addRoot(glass).addRoot(block);

        viewport.addAnchor(new Label("Oak planks"), planks, new MutableVec3(0, 1.17f, 0));
        Paint hint = Paint.fill(MutableColor.fromArgb(0xFFE3EDF7));
        viewport.painter((context, width, height) ->
                context.text("Drag to orbit. Scroll to zoom.", 16, 14, width - 32, 16, hint));
        new UniScreen("UniGUI 3D Scene", viewport).openGui();
    }
}
