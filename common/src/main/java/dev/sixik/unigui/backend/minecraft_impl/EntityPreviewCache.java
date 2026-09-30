package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Кэш запечённых иконок живых сущностей для {@link MinecraftGuiRenderBackend}.
 *
 * <p>Прямой entity-рендер (InventoryScreen-путь) рисует в главный фреймбуфер мимо
 * transform/clip Z-слоёв, поэтому для GUI-иконок сущность печётся в offscreen
 * текстуру ({@link MinecraftEntityBakeCompat}) и дальше рисуется как обычная
 * texture-команда. Лимит на кадр не даёт пекарне просадить FPS при первом
 * открытии длинного списка.</p>
 */
final class EntityPreviewCache {
    private static final int CAPACITY = 128;
    /** Максимум запечёк за кадр: как {@code FastItemRenderer#MAX_BAKES_PER_FRAME}. */
    private static final int MAX_BAKES_PER_FRAME = 6;
    private static final int ICON_PIXELS = 64;

    private final Map<EntityType<?>, MinecraftRenderTarget> cache =
            new LinkedHashMap<>(CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<EntityType<?>, MinecraftRenderTarget> eldest) {
                    if (size() <= CAPACITY) return false;
                    eldest.getValue().close();
                    return true;
                }
            };
    private int bakesThisFrame;

    void beginFrame() {
        bakesThisFrame = 0;
    }

    /** @return текстура иконки или {@code null}, если бюджет пекарни на кадр исчерпан. */
    TextureHandle texture(EntityType<? extends LivingEntity> type,
                          Function<EntityType<? extends LivingEntity>, LivingEntity> entityFactory) {
        if (type == null) return null;
        MinecraftRenderTarget existing = cache.get(type);
        if (existing != null) return existing.colorTexture();
        if (bakesThisFrame >= MAX_BAKES_PER_FRAME) return null;
        LivingEntity entity = entityFactory.apply(type);
        if (entity == null) return null;
        bakesThisFrame++;
        try {
            MinecraftRenderTarget target = MinecraftEntityBakeCompat.bake(entity, ICON_PIXELS);
            cache.put(type, target);
            return target.colorTexture();
        } finally {
            entity.discard();
        }
    }

    void clear() {
        for (MinecraftRenderTarget target : cache.values()) {
            target.close();
        }
        cache.clear();
    }
}
