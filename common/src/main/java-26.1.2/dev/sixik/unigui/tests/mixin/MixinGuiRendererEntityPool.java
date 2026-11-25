package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Несколько сущностей в GUI за один кадр (Minecraft 26.1).
 *
 * <p>{@code GuiRenderer} держит по одному picture-in-picture рендереру на класс состояния, а у рендерера одна
 * текстура. Каждая следующая сущность кадра перерисовывает эту текстуру, и все превью показывают последнюю
 * сущность. Здесь первая сущность кадра идёт в ванильный рендерер, а остальные — в собственные рендереры из пула,
 * у каждого своя текстура. Порядок наложения и обрезка остаются ванильными.</p>
 */
@Mixin(GuiRenderer.class)
public abstract class MixinGuiRendererEntityPool {
    @Unique
    private final List<GuiEntityRenderer> unigui$entityRenderers = new ArrayList<>();
    @Unique
    private int unigui$entityIndex;

    @Inject(method = "preparePictureInPicture", at = @At("HEAD"))
    private void unigui$resetEntityRenderers(CallbackInfo callback) {
        unigui$entityIndex = 0;
    }

    @Redirect(method = "preparePictureInPictureState",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object unigui$pickRenderer(Map<?, ?> renderers, Object stateClass) {
        if (stateClass != GuiEntityRenderState.class) return renderers.get(stateClass);
        int index = unigui$entityIndex++;
        if (index == 0) return renderers.get(stateClass);
        while (unigui$entityRenderers.size() < index) {
            Minecraft minecraft = Minecraft.getInstance();
            unigui$entityRenderers.add(new GuiEntityRenderer(minecraft.renderBuffers().bufferSource(),
                    minecraft.getEntityRenderDispatcher()));
        }
        return unigui$entityRenderers.get(index - 1);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void unigui$closeEntityRenderers(CallbackInfo callback) {
        unigui$entityRenderers.forEach(GuiEntityRenderer::close);
        unigui$entityRenderers.clear();
    }
}
