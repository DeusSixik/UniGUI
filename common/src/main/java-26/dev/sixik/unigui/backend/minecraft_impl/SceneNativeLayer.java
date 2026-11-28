package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Captured native texture bindings and the color pass behavior of their geometry.
 *
 * @param texture base texture with its native sampler
 * @param foil    second texture for integrated glint, or null for ordinary and glint-only layers
 * @param style   lighting, depth and glint behavior
 */
record SceneNativeLayer(UiTextureBinding texture, UiTextureBinding foil, Style style) {
    /**
     * @return whether both borrowed texture views are still usable
     */
    boolean isOpen() {
        return !texture.view().isClosed() && (!style.integratedGlint() || (foil != null && !foil.view().isClosed()));
    }

    /**
     * Selects Minecraft's UV animation for each glint scale.
     */
    enum Glint {
        NONE, ITEM, ENTITY, ARMOR
    }

    /**
     * Describes the supported native draw behavior, independently of the textures and node shader.
     */
    enum Style {
        STANDARD(0, true, false, Glint.NONE),
        OFFSET(1, true, false, Glint.NONE),
        OFFSET_FORWARD(-1, true, false, Glint.NONE),
        DECAL(1, true, true, Glint.NONE),
        EMISSIVE(0, false, false, Glint.NONE),
        ITEM_GLINT(0, true, false, Glint.ITEM),
        ENTITY_GLINT(0, true, false, Glint.ENTITY),
        ARMOR_GLINT(1, true, false, Glint.ARMOR),
        ENTITY_GLINT_OVERLAY(0, false, true, Glint.ENTITY),
        ARMOR_GLINT_OVERLAY(1, false, true, Glint.ARMOR);

        /**
         * Offset passed to Minecraft's view layering transform, or zero for the ordinary depth.
         */
        final int depthLayer;
        /**
         * Whether the surface uses scene light and may receive directional shadows.
         */
        final boolean lit;
        /**
         * Whether color draws require matching depth from a previous layer.
         */
        final boolean depthEqual;
        /**
         * UV animation of the item, entity or armor foil texture.
         */
        final Glint glint;

        /**
         * @param depthLayer signed Minecraft view layering step
         * @param lit        whether ambient and directional lights affect the base surface
         * @param depthEqual whether to require depth written by the parent surface
         * @param glint      native animation scale for the glint
         */
        Style(int depthLayer, boolean lit, boolean depthEqual, Glint glint) {
            this.depthLayer = depthLayer;
            this.lit = lit;
            this.depthEqual = depthEqual;
            this.glint = glint;
        }

        /**
         * @return whether glint is the only texture drawn over a matching base surface
         */
        boolean glintOverlay() {
            return depthEqual && glint != Glint.NONE;
        }

        /**
         * @return whether the texture is sampled in addition to the normal surface texture
         */
        boolean integratedGlint() {
            return !depthEqual && glint != Glint.NONE;
        }
    }
}
