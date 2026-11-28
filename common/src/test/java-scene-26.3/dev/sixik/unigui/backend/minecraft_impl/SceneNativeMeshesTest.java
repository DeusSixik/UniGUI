package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises 26.3 submissions and cache invalidation without a graphics device. */
final class SceneNativeMeshesTest {
    /** Stable scene node for consecutive renders. */
    private final SceneNode node = new SceneNode();
    /** GPU retirement requests, recorded in order. */
    private final List<Mesh3D> retired = new ArrayList<>();
    /** Borrowed texture view owned by Minecraft. */
    private GpuTextureView view = mock(GpuTextureView.class);
    /** Cache using vanilla submission but test texture bindings. */
    private final SceneNativeMeshes cache = new SceneNativeMeshes(retired::add, (type, style) -> {
        var binding = new UiTextureBinding(view, null, false, false, TextureOptions.defaults(), view);
        return new SceneNativeLayer(binding, style.integratedGlint() ? binding : null, style);
    });

    /** Verifies that transforms outside the native pose reuse geometry and an animated part invalidates it. */
    @Test
    void bake_entityPoseChanges_replacesOnlyStaleGeometry() {
        var model = testModel();
        var first = cache.bake(node, modelSubmit(model, 0));
        assertFalse(first.isEmpty());
        node.setPosition(1, 2, 3);
        assertSame(first, cache.bake(node, modelSubmit(model, 0)));
        assertTrue(retired.isEmpty());

        var turned = cache.bake(node, modelSubmit(model, 0.5f));
        assertNotSame(first, turned);
        assertEquals(List.of(first.getFirst().mesh()), retired);
        assertSame(turned, cache.bake(node, modelSubmit(model, 0.5f)));
    }

    /** Verifies that model overrides, tints, display context and foil affect reuse. */
    @Test
    void bake_itemChanges_rebuildsAndPreservesFallback() {
        int[] tints = {0xFFFFFFFF};
        var quads = itemQuads(1);
        var first = cache.bake(node, itemSubmit(quads, tints, ItemDisplayContext.FIXED, FoilType.NONE));
        assertFalse(first.isEmpty());
        assertSame(first, cache.bake(node, itemSubmit(quads, tints, ItemDisplayContext.FIXED, FoilType.NONE)));

        tints[0] = 0xFF00FFFF;
        var tinted = cache.bake(node, itemSubmit(quads, tints, ItemDisplayContext.FIXED, FoilType.NONE));
        var differentModel = cache.bake(node, itemSubmit(itemQuads(2), tints, ItemDisplayContext.FIXED, FoilType.NONE));
        var display = cache.bake(node, itemSubmit(itemQuads(2), tints, ItemDisplayContext.GROUND, FoilType.NONE));
        assertNotSame(first, tinted);
        assertNotSame(tinted, differentModel);
        assertNotSame(differentModel, display);
        var enchanted = cache.bake(node, itemSubmit(itemQuads(2), tints, ItemDisplayContext.GROUND, FoilType.STANDARD));
        assertEquals(SceneNativeLayer.Style.ITEM_GLINT, enchanted.getFirst().nativeLayer().style());
        assertSame(enchanted, cache.bake(node, itemSubmit(itemQuads(2), tints, ItemDisplayContext.GROUND, FoilType.STANDARD)));
        assertTrue(cache.bake(node, itemSubmit(quads, tints, ItemDisplayContext.GROUND, FoilType.SPECIAL)).isEmpty());
        assertEquals(List.of(first.getFirst().mesh(), tinted.getFirst().mesh(), differentModel.getFirst().mesh(),
                display.getFirst().mesh(), enchanted.getFirst().mesh()), retired);
    }

    /** Verifies that resource reload, world changes and closed texture views all invalidate bindings. */
    @Test
    void bake_resourceOrWorldChanges_retiresCachedMeshes() {
        var submit = modelSubmit(testModel(), 0);
        Object models = new Object();
        Object world = new Object();
        cache.updateResources(models, world);
        var first = cache.bake(node, submit);
        cache.updateResources(models, world);
        assertSame(first, cache.bake(node, submit));

        Object reloaded = new Object();
        cache.updateResources(reloaded, world);
        var second = cache.bake(node, submit);
        cache.updateResources(reloaded, null);
        var third = cache.bake(node, submit);
        when(view.isClosed()).thenReturn(true);
        view = mock(GpuTextureView.class);
        var latest = cache.bake(node, submit);
        assertSame(view, latest.getFirst().nativeLayer().texture().view());
        assertEquals(List.of(first.getFirst().mesh(), second.getFirst().mesh(), third.getFirst().mesh()), retired);
    }

    /** Verifies that overlays stay after their armor and that the emissive layer retains transparency. */
    @Test
    void bake_armorAndEmissiveLayers_preservesRenderStateAndOrder() {
        var model = testModel();
        var texture = Identifier.withDefaultNamespace("textures/entity/test.png");
        var armor = RenderTypes.armorCutoutNoCullGlint(texture);
        var decal = RenderTypes.armorTrim(texture, true);
        var eyes = RenderTypes.eyes(texture);
        var submit = List.of(armor, decal, armor, RenderTypes.trimmedArmorGlint(), eyes);
        Consumer<SubmitNodeStorage> commands = storage -> {
            for (int i = 0; i < submit.size(); i++) {
                storage.order(i).solid.submit(modelCommand(model, 0, submit.get(i), new PoseStack()));
            }
        };
        var surfaces = cache.bake(node, commands);
        assertEquals(List.of(SceneNativeLayer.Style.ARMOR_GLINT, SceneNativeLayer.Style.DECAL,
                        SceneNativeLayer.Style.ARMOR_GLINT, SceneNativeLayer.Style.ARMOR_GLINT_OVERLAY,
                        SceneNativeLayer.Style.EMISSIVE),
                surfaces.stream().map(surface -> surface.nativeLayer().style()).toList());
        assertEquals(Material.Shading.UNLIT, surfaces.getLast().material().shading());
        assertEquals(Material.AlphaMode.TRANSLUCENT, surfaces.getLast().material().alphaMode());
        assertSame(surfaces, cache.bake(node, commands));
    }

    /** Verifies that a transparent layer and an added mesh extend the shared effect volume. */
    @Test
    void surfaceBounds_layersAndCustomMesh_shareFullLocalBounds() {
        var base = new Mesh3D(new float[]{0, 0, 0, 2, 1, 0, 0, 1, 0}, null, null, null, new int[]{0, 1, 2});
        var overlay = new Mesh3D(new float[]{-1, 2, 1, 1, 2, 1, 0, 3, 1}, null, null, null, new int[]{0, 1, 2});
        var translucent = new Material(0x80FFFFFF, null, Material.Shading.UNLIT,
                Material.AlphaMode.TRANSLUCENT, 0, Material.Culling.NONE);
        var opaque = new SceneBlockMeshes.Surface(base, Material.DEFAULT);
        var transparent = new SceneBlockMeshes.Surface(overlay, translucent);

        assertSame(base.bounds(), SceneBlockMeshes.surfaceBounds(List.of(opaque), null));
        assertEquals(new Aabb(-1, 0, 0, 2, 3, 1),
                SceneBlockMeshes.surfaceBounds(List.of(opaque, transparent), null));
        assertEquals(new Aabb(-3, -2, 0, 2, 3, 4),
                SceneBlockMeshes.surfaceBounds(List.of(opaque, transparent), new Aabb(-3, -2, 0, 1, 1, 4)));
    }

    /** Verifies reuse of mapped armor geometry and invalidation when its atlas interval changes. */
    @Test
    void bake_trimAtlasChanges_rebuildsMappedUvs() {
        var model = testModel();
        var texture = Identifier.withDefaultNamespace("textures/entity/test.png");
        var type = RenderTypes.armorTrim(texture, true);
        var pose = new PoseStack();
        PalettedTextureManager.Handle mapping = mock(PalettedTextureManager.Handle.class,
                org.mockito.Answers.CALLS_REAL_METHODS);
        when(mapping.getU(1)).thenReturn(0.5f);
        when(mapping.getV(1)).thenReturn(0.5f);
        Consumer<SubmitNodeStorage> submit = storage -> storage.order(0).solid.submit(
                new ModelFeatureRenderer.Submit<>(type, pose.last().copy(), model, 0f, 0, 0, 0xFFFFFFFF, mapping, null));

        var first = cache.bake(node, submit);
        assertFalse(first.isEmpty());
        assertSame(first, cache.bake(node, submit));
        when(mapping.getU(1)).thenReturn(0.25f);
        assertNotSame(first, cache.bake(node, submit));
        assertEquals(List.of(first.getFirst().mesh()), retired);
    }

    /** Verifies that trimming retires an old node after all meshes for the frame have been collected. */
    @Test
    void trim_overBudget_evictsLeastRecentlyUsedNode() {
        var submit = modelSubmit(testModel(), 0);
        var recent = cache.bake(node, submit);
        var eldest = cache.bake(new SceneNode(), submit);
        for (int i = 0; i < 127; i++) cache.bake(new SceneNode(), submit);
        assertTrue(retired.isEmpty());

        assertSame(recent, cache.bake(node, submit));
        cache.trim();
        assertEquals(List.of(eldest.getFirst().mesh()), retired);
        assertSame(recent, cache.bake(node, submit));
    }

    /** @return a simple native cube whose part turns during setupAnim */
    private static Model<Float> testModel() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().addBox(0, 0, 0, 4, 4, 4), PartPose.ZERO);
        return new Model<>(LayerDefinition.create(mesh, 16, 16).bakeRoot(), RenderTypes::entityCutout) {
            /** {@inheritDoc} */
            @Override
            public void setupAnim(Float rotation) {
                super.setupAnim(rotation);
                root().getChild("head").yRot = rotation;
            }
        };
    }

    /**
     * @param model native test model
     * @param rotation head angle in radians
     * @return caller that submits the native model layer
     */
    private static Consumer<SubmitNodeStorage> modelSubmit(Model<Float> model, float rotation) {
        var type = model.renderType(Identifier.withDefaultNamespace("textures/entity/test.png"));
        var pose = new PoseStack();
        return storage -> storage.order(0).solid.submit(modelCommand(model, rotation, type, pose));
    }

    /**
     * @param model native test model
     * @param rotation head angle in radians
     * @param type vanilla layer to bake
     * @param pose node-local transform
     * @return model command for the test storage
     */
    private static ModelFeatureRenderer.Submit<Float> modelCommand(Model<Float> model, float rotation, RenderType type,
                                                                    PoseStack pose) {
        return new ModelFeatureRenderer.Submit<>(type, pose.last().copy(), model, rotation, 0, 0, 0xFFFFFFFF, null, null);
    }

    /**
     * @param width extent of the test quad, in model units
     * @return one baked item quad with a tint layer
     */
    private static ItemQuads itemQuads(float width) {
        var texture = Identifier.withDefaultNamespace("textures/item/test.png");
        var material = new BakedQuad.MaterialInfo(null, null, RenderTypes.itemCutout(texture),
                RenderTypes.itemCutoutGlint(texture), null, 0, Direction.UP, 0);
        return ItemQuads.split(List.of(new BakedQuad(new Vector3f(), new Vector3f(width, 0, 0),
                new Vector3f(width, 0, 1), new Vector3f(0, 0, 1), 0, 0, 0, 0, Direction.UP, material)));
    }

    /**
     * @param quads baked item geometry
     * @param tints ARGB tint layers
     * @param display native placement
     * @param foil native foil pass
     * @return caller that submits the item layer
     */
    private static Consumer<SubmitNodeStorage> itemSubmit(ItemQuads quads, int[] tints,
                                                         ItemDisplayContext display, FoilType foil) {
        var pose = new PoseStack();
        return storage -> storage.submitItem(pose, display, 0, 0, 0, tints, quads, foil);
    }
}
