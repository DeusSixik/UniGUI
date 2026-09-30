package dev.sixik.isf.client;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Иконка живой сущности для GUI: рисует запечённую в бекенде текстуру модели
 * ({@link MinecraftGuiRenderBackend#entityPreviewTexture}) обычной
 * texture-командой — как item-иконки из кэша, поэтому корректно попадает в
 * transform/clip Z-слоёв окна рецептов.
 *
 * <p>Пока запечка не готова (бюджет на кадр), рисуется fallback-предмет —
 * spawn egg сущности.</p>
 */
final class IsfEntityIconWidget extends WidgetBase {
    private EntityType<? extends LivingEntity> entityType;
    private ItemStack fallbackItem;
    private float size;

    IsfEntityIconWidget(EntityType<? extends LivingEntity> entityType, float size) {
        this.entityType = entityType;
        this.size = size;
    }

    IsfEntityIconWidget fallbackItem(ItemStack stack) {
        this.fallbackItem = stack == null || stack.isEmpty() ? null : stack;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, size, size));
    }

    @Override
    public void render(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float side = Math.min(layoutBounds().width(), layoutBounds().height());
        if (side <= 0.0f) return;
        float opacity = context.opacityMultiplier();
        if (entityType != null && context.backend() instanceof MinecraftGuiRenderBackend minecraftBackend) {
            TextureHandle texture = minecraftBackend.entityPreviewTexture(entityType);
            if (texture != null) {
                context.texture(texture, x, y, side, side,
                        Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, opacity)));
                return;
            }
        }
        if (fallbackItem != null && context.backend() instanceof MinecraftGuiRenderBackend minecraftBackend) {
            minecraftBackend.renderItemPreview(fallbackItem, x, y, side, opacity, false);
        }
    }
}
