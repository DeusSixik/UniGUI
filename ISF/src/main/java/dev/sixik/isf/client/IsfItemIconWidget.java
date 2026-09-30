package dev.sixik.isf.client;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.world.item.ItemStack;

/** Рисует ItemStack как retained custom-команду внутри UniGUI pipeline. */
final class IsfItemIconWidget extends WidgetBase {
    private ItemStack stack;

    IsfItemIconWidget(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    IsfItemIconWidget stack(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        setDesiredSize(resolveDesiredSize(context, 16.0f, 16.0f));
    }

    @Override
    public void render(RenderContext context) {
        if (stack.isEmpty()) return;
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float size = Math.min(layoutBounds().width(), layoutBounds().height());
        float opacity = context.opacityMultiplier();
        // Fast-путь кэширует только иконку предмета, поэтому число (count) и прочие
        // ванильные декорации видны только через renderItemPreview(..., decorations=true).
        // Для стэков с count > 1 идём по полному vanilla-пути, иначе — по кэшированному.
        boolean decorations = stack.getCount() > 1;
        context.custom(backend -> {
            if (!(backend instanceof MinecraftGuiRenderBackend minecraftBackend)) return;
            if (decorations) {
                minecraftBackend.renderItemPreview(stack, x, y, size, opacity, true);
            } else if (!minecraftBackend.renderItemPreviewLazy(stack, x, y, size, opacity)) {
                minecraftBackend.renderItemPreview(stack, x, y, size, opacity, true);
            }
        });
    }
}
