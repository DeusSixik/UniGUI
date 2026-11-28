package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.SceneNativeLayer.Style;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Converts textured quad models submitted by Minecraft 26.3 into shader-compatible scene meshes.
 */
final class SceneNativeMeshes {
    /**
     * Upper bound on retained node snapshots per renderer.
     */
    private static final int MAX_CACHED_NODES = 128;
    /**
     * Latest captured meshes by node, ordered by access for eviction.
     */
    private final LinkedHashMap<SceneNode, Capture> cache = new LinkedHashMap<>(16, 0.75f, true);
    /**
     * Retires the GPU buffers belonging to a discarded snapshot.
     */
    private final Consumer<Mesh3D> retire;
    /**
     * Resolves the texture bound by a vanilla layer when baking its geometry.
     */
    private final BiFunction<RenderType, Style, SceneNativeLayer> textures;
    /**
     * Model reload generation and client world of the retained snapshots.
     */
    private WeakReference<Object> models = new WeakReference<>(null), level = models;
    /**
     * Commands for the current node, kept in the order emitted by vanilla.
     */
    private final ArrayList<SubmitNode> commands = new ArrayList<>();
    /**
     * Receives drained commands without allocating a new callback for each phase.
     */
    private final FeatureRenderPhase.Output append = (command, sorted) -> commands.add(command);
    /**
     * Drains each vanilla phase into the working command list.
     */
    private final Consumer<FeatureRenderPhase<?>> drain = phase -> phase.sortInto(append);
    /**
     * Isolated vanilla storage drained after every node.
     */
    private SubmitNodeStorage storage = new SubmitNodeStorage();
    /**
     * Pose of the model submit, including vanilla renderer transforms in scene units.
     */
    private final PoseStack pose = new PoseStack();
    /**
     * Reusable per-quad tint, light and overlay values for item geometry.
     */
    private final QuadInstance quadValues = new QuadInstance();
    /**
     * Consecutive native layers in submission order. A trim must follow its matching armor layer.
     */
    private final ArrayList<Layer> layers = new ArrayList<>();
    /**
     * Whether a submission requires the complete node to use the vanilla renderer.
     */
    private boolean unsupported;

    /**
     * Creates a mesh cache scoped to the owning scene renderer.
     *
     * @param retire releases the GPU buffers for discarded geometry
     */
    SceneNativeMeshes(Consumer<Mesh3D> retire) {
        this(retire, SceneNativeMeshes::resolveTextures);
    }

    /**
     * Creates a cache with explicit ownership and texture resolution.
     *
     * @param retire   releases the GPU buffers for discarded geometry
     * @param textures resolves the current texture and sampler for a layer
     */
    SceneNativeMeshes(Consumer<Mesh3D> retire, BiFunction<RenderType, Style, SceneNativeLayer> textures) {
        this.retire = retire;
        this.textures = textures;
    }

    /**
     * Drops retained geometry when its atlas, models or world may have changed.
     *
     * @param models current model reload generation, or null before model loading
     * @param level  current client world, or null outside a world
     */
    void updateResources(Object models, Object level) {
        if (this.models.refersTo(models) && this.level.refersTo(level)) return;
        for (Capture value : cache.values()) release(value);
        cache.clear();
        this.models = new WeakReference<>(models);
        this.level = new WeakReference<>(level);
    }

    /**
     * Reuses geometry for matching submissions or bakes every supported layer as a unit.
     *
     * @param node   identity of the scene node being rendered
     * @param submit sends the model commands through an isolated vanilla storage
     * @return textured local meshes, or an empty list to render the node through vanilla
     */
    List<SceneBlockMeshes.Surface> bake(SceneNode node, Consumer<SubmitNodeStorage> submit) {
        try {
            submit.accept(storage);
            storage.drainPhases(drain);
            Capture previous = cache.get(node);
            if (previous != null && previous.matches(commands)) return previous.surfaces();
            if (previous != null) {
                cache.remove(node);
                release(previous);
            }
            if (commands.isEmpty()) return List.of();

            List<SceneNativeMeshKey> keys = new ArrayList<>(commands.size());
            for (int i = 0; i < commands.size(); i++) {
                SubmitNode command = commands.get(i);
                collect(command);
                if (unsupported) return List.of();
                if (keys != null) {
                    var key = SceneNativeMeshKey.capture(command);
                    if (key == null) keys = null;
                    else keys.add(key);
                }
            }
            List<SceneBlockMeshes.Surface> surfaces = new ArrayList<>(layers.size());
            for (Layer layer : layers) {
                if (layer.vertices().isEmpty()) continue;
                RenderType type = layer.type();
                Style style = layer.style();
                SceneNativeLayer binding = textures.apply(type, style);
                if (binding == null || !binding.isOpen()) return List.of();
                boolean blended = type.hasBlending();
                Material material = new Material(0xFFFFFFFF, null, style.lit ? Material.Shading.LIT : Material.Shading.UNLIT,
                        blended ? Material.AlphaMode.TRANSLUCENT : Material.AlphaMode.CUTOUT,
                        alphaCutoff(type, blended && !style.glintOverlay() ? 0 : 0.1f),
                        type.pipeline().isCull() ? Material.Culling.BACK : Material.Culling.NONE);
                var mesh = layer.vertices().build();
                surfaces.add(new SceneBlockMeshes.Surface(mesh, material, binding));
            }
            if (surfaces.isEmpty()) return List.of();
            cache.put(node, new Capture(keys, surfaces));
            return surfaces;
        } catch (RuntimeException | Error failure) {
            storage = new SubmitNodeStorage();
            throw failure;
        } finally {
            commands.clear();
            layers.clear();
            unsupported = false;
            pose.setIdentity();
        }
    }

    /**
     * Evicts the least recently used nodes after the shadow and color passes have submitted.
     */
    void trim() {
        while (cache.size() > MAX_CACHED_NODES) release(cache.pollFirstEntry().getValue());
    }

    /**
     * Reads the exact atlas or entity skin plus any second glint texture.
     *
     * @param type  layer to resolve
     * @param style whether the layer requires a separate foil binding
     * @return texture bindings, or null if required textures are missing
     */
    private static SceneNativeLayer resolveTextures(RenderType type, Style style) {
        UiTextureBinding base = null;
        UiTextureBinding foil = null;
        for (var texture : type.prepare().textures()) {
            if (texture.name().equals("Sampler0") || (style.integratedGlint() && texture.name().equals("GlintSampler"))) {
                var view = texture.textureView();
                var binding = new UiTextureBinding(view, texture.sampler(), false, false, TextureOptions.defaults(), view);
                if (texture.name().equals("Sampler0")) base = binding;
                else foil = binding;
            }
        }
        return base == null || (style.integratedGlint() && foil == null) ? null : new SceneNativeLayer(base, foil, style);
    }

    /**
     * Reads one vanilla submission. Special render commands and projected foil retain their native pipeline.
     *
     * @param submit native feature command
     */
    private void collect(SubmitNode submit) {
        if (submit instanceof ModelFeatureRenderer.Submit<?> model
                && model.sheetedDecalPose() == null) {
            collectModel(model);
        } else if (submit instanceof ItemFeatureRenderer.Submit item
                && item.foilType() != ItemStackRenderState.FoilType.SPECIAL) {
            collectItem(item);
        } else {
            unsupported = true;
        }
    }

    /**
     * Retires the buffers associated with a snapshot.
     *
     * @param capture discarded geometry
     */
    private void release(Capture capture) {
        for (var surface : capture.surfaces()) retire.accept(surface.mesh());
    }

    /**
     * Stores one fully converted node.
     *
     * @param keys     exact submission snapshots, or null for dynamic UV mappings
     * @param surfaces ordered textured meshes owned by the entry
     */
    private record Capture(List<SceneNativeMeshKey> keys, List<SceneBlockMeshes.Surface> surfaces) {
        /**
         * Checks the texture lifetime and every ordered submission, including the model's current part poses.
         *
         * @param commands newly submitted vanilla model and item layers
         * @return whether the stored local geometry is still current
         */
        boolean matches(List<SubmitNode> commands) {
            if (keys == null || keys.size() != commands.size()) return false;
            for (int i = 0; i < surfaces.size(); i++) {
                if (!surfaces.get(i).nativeLayer().isOpen()) return false;
            }
            for (int i = 0; i < keys.size(); i++) {
                if (!keys.get(i).matches(commands.get(i))) return false;
            }
            return true;
        }
    }

    /**
     * Applies the renderer's animation and texture coordinates to the mesh consumer.
     *
     * @param submit model, animation, material and node-local transform
     * @param <S>    state accepted by the vanilla model
     */
    private <S> void collectModel(ModelFeatureRenderer.Submit<S> submit) {
        Layer layer = layer(submit.renderType());
        if (layer == null) {
            unsupported = true;
            return;
        }
        pose.last().set(submit.pose());
        VertexConsumer output = layer.vertices();
        if (submit.uvMapping() != null) output = submit.uvMapping().wrap(output);
        var model = submit.model();
        model.setupAnim(submit.state());
        model.renderToBuffer(pose, output, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
    }

    /**
     * Emits baked item quads with the vanilla display pose, atlas UVs and tint layers.
     *
     * @param submit resolved item layer and its node-local pose
     */
    private void collectItem(ItemFeatureRenderer.Submit submit) {
        quadValues.setLightCoords(submit.lightCoords());
        quadValues.setOverlayCoords(submit.overlayCoords());
        for (BakedQuad quad : submit.quads()) {
            var material = quad.materialInfo();
            RenderType type = submit.foilType() == ItemStackRenderState.FoilType.STANDARD
                    ? material.itemGlintRenderType() : material.itemRenderType();
            Layer layer = layer(type);
            if (layer == null) {
                unsupported = true;
                return;
            }
            int index = material.tintIndex();
            int[] tints = submit.tintLayers();
            quadValues.setColor(index >= 0 && index < tints.length ? tints[index] : 0xFFFFFFFF);
            layer.vertices().putBakedQuad(submit.pose(), quad, quadValues);
        }
    }

    /**
     * @param type native material and topology
     * @return scene behavior for a supported pipeline, or null to retain vanilla rendering
     */
    private static Style style(RenderType type) {
        if (type == null) return null;
        var pipeline = type.pipeline();
        if (pipeline == RenderPipelines.ENTITY_SOLID || pipeline == RenderPipelines.ENTITY_CUTOUT_CULL
                || pipeline == RenderPipelines.ENTITY_CUTOUT || pipeline == RenderPipelines.ENTITY_TRANSLUCENT
                || pipeline == RenderPipelines.ENTITY_TRANSLUCENT_CULL || pipeline == RenderPipelines.ITEM_CUTOUT
                || pipeline == RenderPipelines.ITEM_TRANSLUCENT) return Style.STANDARD;
        if (pipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL || pipeline == RenderPipelines.ENTITY_CUTOUT_Z_OFFSET
                || pipeline == RenderPipelines.WOLF_ARMOR_CRACKS) return Style.OFFSET;
        if (pipeline == RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD) return Style.OFFSET_FORWARD;
        if (pipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL) return Style.DECAL;
        if (pipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE || pipeline == RenderPipelines.EYES)
            return Style.EMISSIVE;
        if (pipeline == RenderPipelines.ITEM_CUTOUT_GLINT || pipeline == RenderPipelines.ITEM_TRANSLUCENT_GLINT) {
            return Style.ITEM_GLINT;
        }
        if (pipeline == RenderPipelines.ENTITY_SOLID_GLINT) return Style.ENTITY_GLINT;
        if (pipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT) return Style.ARMOR_GLINT;
        if (type == RenderTypes.trimmedArmorGlint()) return Style.ARMOR_GLINT_OVERLAY;
        if (type == RenderTypes.patternedShieldGlint()) return Style.ENTITY_GLINT_OVERLAY;
        return null;
    }

    /**
     * @param type native texture and alpha layer
     * @return accumulated quads for this layer, with zero positional offset
     */
    private Layer layer(RenderType type) {
        if (!layers.isEmpty() && layers.getLast().type() == type) return layers.getLast();
        Style style = style(type);
        if (style == null) return null;
        var layer = new Layer(type, style, new SceneBlockMeshes.MeshBuilder(0, 0, 0));
        layers.add(layer);
        return layer;
    }

    /**
     * @param type     vanilla layer and texture lookup key
     * @param style    scene render behavior
     * @param vertices accumulated consecutive quads
     */
    private record Layer(RenderType type, Style style, SceneBlockMeshes.MeshBuilder vertices) {
    }

    /**
     * @param type     layer whose shader may define an alpha cutoff
     * @param fallback cutoff for shaders without an explicit define
     * @return the normalized alpha cutoff
     */
    private static float alphaCutoff(RenderType type, float fallback) {
        String value = type.pipeline().getShaderDefines().values().get("ALPHA_CUTOUT");
        if (value != null) {
            try {
                float cutoff = Float.parseFloat(value);
                if (cutoff >= 0 && cutoff <= 1) return cutoff;
            } catch (NumberFormatException ignored) {
                // A preprocessor expression cannot be used as a Java float.
            }
        }
        return fallback;
    }
}
