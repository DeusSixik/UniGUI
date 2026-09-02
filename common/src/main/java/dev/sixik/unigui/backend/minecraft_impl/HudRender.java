package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.widget.Widget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Predicate;

/**
 * Глобальная точка регистрации UniGUI-слоёв поверх игрового HUD.
 *
 * <pre>{@code
 * MinecraftRenderLayerRegistration<Minecraft> hud = HudRender.register(root);
 * hud.visibleWhen(client -> client.screen == null);
 * // hud.close() удаляет слой и освобождает его render-ресурсы.
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class HudRender {
    private static final MinecraftRenderLayerRegistry<Minecraft> REGISTRY =
            new MinecraftRenderLayerRegistry<>();

    private HudRender() {
    }

    public static MinecraftRenderLayerRegistration<Minecraft> register(Widget root) {
        return register(new MinecraftWidgetRenderLayer(root));
    }

    public static MinecraftRenderLayerRegistration<Minecraft> register(Widget root, UIContext context) {
        return register(new MinecraftWidgetRenderLayer(root, context));
    }

    public static MinecraftRenderLayerRegistration<Minecraft> register(MinecraftWidgetRenderLayer layer) {
        return register(layer, client -> true, 0);
    }

    public static MinecraftRenderLayerRegistration<Minecraft> register(
            MinecraftWidgetRenderLayer layer,
            Predicate<? super Minecraft> visibility,
            int priority) {
        return REGISTRY.add(layer, visibility, priority);
    }

    /** Удаляет все HUD-слои и освобождает принадлежащие им ресурсы. */
    public static void clear() {
        REGISTRY.clear();
    }

    /** Внутренняя точка вызова из Minecraft HUD hook. */
    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        REGISTRY.render(client, graphics, width, height, 0, 0, partialTick);
    }
}
