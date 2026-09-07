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
        context.custom(backend -> {
            if (backend instanceof MinecraftGuiRenderBackend minecraftBackend
                    && !minecraftBackend.renderItemPreviewLazy(stack, x, y, size, opacity)) {
                minecraftBackend.renderItemPreview(stack, x, y, size, opacity, true);
            }
        });
    }
}
