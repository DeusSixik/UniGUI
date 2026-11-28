package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Arrays;
import java.util.List;

/**
 * Identifies the inputs used to generate a single native scene submission's baked vertices.
 */
sealed interface SceneNativeMeshKey {
    /**
     * Snapshots a command after its model has been posed for capture.
     *
     * @param command supported vanilla submission
     * @return the snapshot, or null if the command's UV mapping has no stable snapshot
     */
    static SceneNativeMeshKey capture(SubmitNode command) {
        if (command instanceof ItemFeatureRenderer.Submit item) {
            return new ItemKey(item.pose().copy(), item.displayContext(), item.foilType(),
                    List.copyOf(item.quads()), item.tintLayers().clone());
        }
        if (command instanceof ModelFeatureRenderer.Submit<?> model) {
            UvMapping mapping = model.uvMapping();
            if (mapping != null && !(mapping instanceof PalettedTextureManager.Handle)
                    && mapping.getClass() != TextureAtlasSprite.class) return null;
            return new ModelKey(model.pose().copy(), model.model(), model.renderType(), model.tintedColor(),
                    PartPose.capture(model.model().allParts()), mapping == null ? null : UvKey.capture(mapping));
        }
        return null;
    }

    /**
     * @param command current vanilla submission
     * @return whether the baked vertices and layer can be reused for the command
     */
    boolean matches(SubmitNode command);

    /**
     * Compares the transform applied to vertex positions and normals.
     *
     * @param cached  pose copied when the mesh was baked
     * @param current pose of the new submission
     * @return whether both matrices match
     */
    private static boolean samePose(PoseStack.Pose cached, PoseStack.Pose current) {
        return cached.pose().equals(current.pose()) && cached.normal().equals(current.normal());
    }

    /**
     * Baked item layer inputs after resolving the stack and its model overrides.
     *
     * @param pose    node-local transform applied by the item layer
     * @param display item placement context
     * @param foil    native foil mode that selects the layer pipeline
     * @param quads   immutable list of resolved baked quads
     * @param tints   copied ARGB tint layers
     */
    record ItemKey(PoseStack.Pose pose, ItemDisplayContext display, ItemStackRenderState.FoilType foil,
                   List<BakedQuad> quads,
                   int[] tints) implements SceneNativeMeshKey {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(SubmitNode command) {
            if (!(command instanceof ItemFeatureRenderer.Submit item)
                    || item.foilType() != foil
                    || item.displayContext() != display || !samePose(pose, item.pose())
                    || !Arrays.equals(tints, item.tintLayers())) return false;
            var next = item.quads();
            if (quads.size() != next.size()) return false;
            for (int i = 0; i < quads.size(); i++) {
                if (!quads.get(i).equals(next.get(i))) return false;
            }
            return true;
        }
    }

    /**
     * Entity layer inputs after applying the live renderer's animations.
     *
     * @param pose  node-local transform applied by the renderer
     * @param model model instance supplying the immutable hierarchy and cubes
     * @param type  native texture and render layer
     * @param tint  ARGB model tint
     * @param parts snapshot of animated part transforms and visibility
     * @param uv    native atlas region, or null when the model uses its own texture coordinates
     */
    record ModelKey(PoseStack.Pose pose, Model<?> model, RenderType type, int tint,
                    PartPose parts, UvKey uv) implements SceneNativeMeshKey {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(SubmitNode command) {
            if (!(command instanceof ModelFeatureRenderer.Submit<?> next) || next.model() != model
                    || next.renderType() != type || next.tintedColor() != tint || next.sheetedDecalPose() != null
                    || (uv == null ? next.uvMapping() != null : !uv.matches(next.uvMapping()))
                    || !samePose(pose, next.pose())) return false;
            return matchesAnimatedPose(next);
        }

        /**
         * Checks the actual rendered pose so animated state and equipment layers can change normally.
         *
         * @param submit current model and state
         * @param <S>    native state accepted by this model
         * @return whether its posed parts match the baked snapshot
         */
        private <S> boolean matchesAnimatedPose(ModelFeatureRenderer.Submit<S> submit) {
            submit.model().setupAnim(submit.state());
            return parts.matches(model.allParts());
        }
    }

    /**
     * Snapshots the endpoints of a native atlas remap. The remap is affine from each input U and V axis to
     * its atlas interval, so the endpoints determine its translation and scale.
     *
     * @param mapping native sprite or palette slot identity
     * @param u0      atlas U for input 0
     * @param u1      atlas U for input 1
     * @param v0      atlas V for input 0
     * @param v1      atlas V for input 1
     */
    record UvKey(UvMapping mapping, float u0, float u1, float v0, float v1) {
        /**
         * @param mapping non-null native affine remap
         * @return the current atlas region
         */
        static UvKey capture(UvMapping mapping) {
            return new UvKey(mapping, mapping.getU(0), mapping.getU(1), mapping.getV(0), mapping.getV(1));
        }

        /**
         * @param current new model's remap, possibly null
         * @return whether the palette slot still has the same atlas coordinates
         */
        boolean matches(UvMapping current) {
            return current == mapping && current.getU(0) == u0 && current.getU(1) == u1
                    && current.getV(0) == v0 && current.getV(1) == v1;
        }
    }

    /**
     * Retains the per-part translation, rotation, scale and drawing flags without retaining render state.
     */
    final class PartPose {
        /**
         * Number of scalars per model part.
         */
        private static final int STRIDE = 11;
        /**
         * Packed pose, using model units for translation and radians for rotation.
         */
        private final float[] values;

        /**
         * @param values packed poses owned by this snapshot
         */
        private PartPose(float[] values) {
            this.values = values;
        }

        /**
         * @param parts the model's ordered list of already posed parts
         * @return a copy of all per-part vertex inputs
         */
        static PartPose capture(List<ModelPart> parts) {
            float[] values = new float[parts.size() * STRIDE];
            for (int i = 0, offset = 0; i < parts.size(); i++, offset += STRIDE) {
                ModelPart part = parts.get(i);
                values[offset] = part.x;
                values[offset + 1] = part.y;
                values[offset + 2] = part.z;
                values[offset + 3] = part.xRot;
                values[offset + 4] = part.yRot;
                values[offset + 5] = part.zRot;
                values[offset + 6] = part.xScale;
                values[offset + 7] = part.yScale;
                values[offset + 8] = part.zScale;
                values[offset + 9] = part.visible ? 1 : 0;
                values[offset + 10] = part.skipDraw ? 1 : 0;
            }
            return new PartPose(values);
        }

        /**
         * @param parts freshly posed parts in the same model order
         * @return whether every part retains its captured transform and drawing flags
         */
        boolean matches(List<ModelPart> parts) {
            if (parts.size() * STRIDE != values.length) return false;
            for (int i = 0, offset = 0; i < parts.size(); i++, offset += STRIDE) {
                ModelPart part = parts.get(i);
                if (values[offset] != part.x || values[offset + 1] != part.y || values[offset + 2] != part.z
                        || values[offset + 3] != part.xRot || values[offset + 4] != part.yRot || values[offset + 5] != part.zRot
                        || values[offset + 6] != part.xScale || values[offset + 7] != part.yScale || values[offset + 8] != part.zScale
                        || values[offset + 9] != (part.visible ? 1 : 0) || values[offset + 10] != (part.skipDraw ? 1 : 0))
                    return false;
            }
            return true;
        }
    }
}
