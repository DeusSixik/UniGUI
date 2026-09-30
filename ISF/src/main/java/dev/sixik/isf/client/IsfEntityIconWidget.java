package dev.sixik.isf.client;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftEntityBakeCompat;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.world.entity.EntityType;

/**
 * Иконка живой сущности для GUI: рисует запечённую модель моба
 * ({@link MinecraftEntityBakeCompat#bakePreview}) обычной texture-командой, поэтому
 * корректно попадает в transform/clip Z-слоёв окна рецептов.
 *
 * <p>Неживые типы отсекает сама запечка (возвращает {@code null} — рисуем пустоту).</p>
 */
final class IsfEntityIconWidget extends WidgetBase {
    private final EntityType<?> entityType;
    private final float size;

    IsfEntityIconWidget(EntityType<?> entityType, float size) {
        this.entityType = entityType;
        this.size = size;
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
        if (side <= 0.0f || entityType == null) return;
        TextureHandle texture = MinecraftEntityBakeCompat
                .bakePreview(entityType, 256);
        if (texture == null) return;
        context.texture(texture, x, y, side, side,
                Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, context.opacityMultiplier())));
    }
}
