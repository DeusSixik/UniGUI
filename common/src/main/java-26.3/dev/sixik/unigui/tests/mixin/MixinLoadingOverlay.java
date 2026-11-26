package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/** Сохраняет оригинальные параметры LoadingOverlay Minecraft 26.2 (оверлей в 26.2 ставится через {@code Gui}). */
@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {
    @Redirect(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"
            )
    )
    private void unigui$delayOverlayRemoval(Gui gui, Overlay overlay) {
        if (overlay == null && LoadingScreenRender.shouldKeepOverlay((LoadingOverlay) (Object) this)) {
            return;
        }
        gui.setOverlay(overlay);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Z)V",
            at = @At("TAIL")
    )
    private void unigui$captureLoadingContext(Minecraft minecraft, ReloadInstance reloadInstance,
                                               Consumer<Optional<Throwable>> consumer,
                                               boolean fadeIn, CallbackInfo callback) {
        LoadingScreenRender.onLoadingOverlayCreated(
                (LoadingOverlay) (Object) this,
                minecraft,
                reloadInstance,
                consumer,
                fadeIn
        );
    }
}
