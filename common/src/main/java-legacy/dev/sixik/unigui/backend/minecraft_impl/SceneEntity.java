package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Разрешённая сущность {@link dev.sixik.unigui.api.scene.EntityNode} для 1.20.1 и 1.21.1. Начало координат у ног.
 *
 * <p>Источник — готовая сущность или тип для превью. Превью создаётся при появлении клиентского мира и
 * пересоздаётся при его смене.</p>
 */
final class SceneEntity {
    /**
     * Сущность пользователя или превью для текущего мира.
     */
    private Entity entity;
    /**
     * Тип превью или null для сущности пользователя.
     */
    private final EntityType<?> previewType;

    /**
     * Создаёт обёртку для одного вида источника.
     *
     * @param entity      сущность пользователя или null.
     * @param previewType тип превью или null.
     */
    private SceneEntity(Entity entity, EntityType<?> previewType) {
        this.entity = entity;
        this.previewType = previewType;
    }

    /**
     * Принимает нативный объект пользователя.
     *
     * @param source {@link Entity} или {@link EntityType}.
     * @return обёртка.
     * @throws IllegalArgumentException если тип объекта не подходит.
     */
    static SceneEntity of(Object source) {
        if (source instanceof Entity value) return new SceneEntity(value, null);
        if (source instanceof EntityType<?> type) return preview(type);
        throw new IllegalArgumentException("Expected Entity or EntityType, got " + source.getClass().getName());
    }

    /**
     * Создаёт отложенное превью по типу.
     *
     * @param type тип сущности.
     * @return обёртка.
     */
    static SceneEntity preview(EntityType<?> type) {
        return new SceneEntity(null, Objects.requireNonNull(type, "type"));
    }

    /**
     * Возвращает сущность для рендерера.
     *
     * @return сущность или null, если клиентский мир пока недоступен.
     */
    Entity entity() {
        if (previewType == null) return entity;
        Minecraft client = Minecraft.getInstance();
        var level = client == null ? null : client.level;
        if (level == null) entity = null;
        else if (entity == null || entity.level() != level) entity = previewType.create(level);
        return entity;
    }

    /**
     * Вычисляет AABB с основанием в Y=0.
     *
     * @return локальные границы сущности.
     */
    Aabb bounds() {
        float width = previewType != null ? previewType.getWidth() : entity.getBbWidth();
        float height = previewType != null ? previewType.getHeight() : entity.getBbHeight();
        float half = width / 2;
        return new Aabb(-half, 0, -half, half, height, half);
    }
}
