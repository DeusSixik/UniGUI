package dev.sixik.unigui.gtnh.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import javax.annotation.Nonnull;

/**
 * Every UniGUI mixin, all client side and early (they patch vanilla classes).
 */
public enum Mixins implements IMixins {

    /**
     * Overlay layers see mouse and keyboard events first; vanilla tooltips are kept above the overlay.
     */
    GUI_SCREEN_OVERLAY(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addClientMixins("MixinGuiScreenUniGuiOverlay")),

    /**
     * Silences the vanilla slot under an overlay panel.
     */
    GUI_CONTAINER_OVERLAY(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addClientMixins("MixinGuiContainerUniGuiOverlay")),

    /**
     * Tracks the "Loading level" screens and draws loading layers over them.
     */
    LOADING_SCREEN(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addClientMixins("MixinLoadingScreenRenderer"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}