package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Ванильная сущность, локальное начало которой находится у ног.
 */
public final class EntityNode extends SceneNode {
    /**
     * Готовая сущность или null для превью, ожидающего клиентский уровень.
     */
    private Entity entity;
    /**
     * Тип отложенного превью либо null у переданной вызывающим кодом сущности.
     */
    private final EntityType<?> previewType;

    /**
     * Использует позу указанной сущности. Её тики выполняет владелец.
     *
     * @param entity источник ванильной модели и анимации.
     */
    public EntityNode(Entity entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
        previewType = null;
        setBounds(bounds(entity.getBbWidth(), entity.getBbHeight()));
    }

    /**
     * Запоминает тип до появления клиентского уровня.
     *
     * @param type тип ванильной сущности.
     */
    private EntityNode(EntityType<?> type) {
        previewType = Objects.requireNonNull(type, "type");
        setBounds(bounds(type.getWidth(), type.getHeight()));
    }

    /**
     * Создаёт превью сущности, не добавляя её в мир.
     *
     * @param type тип модели.
     * @return узел, который создаст модель при появлении клиентского уровня.
     */
    public static EntityNode preview(EntityType<?> type) {
        return new EntityNode(type);
    }

    /**
     * Возвращает сущность для рендерера.
     *
     * @return сущность или null, если клиентский уровень или тип пока недоступен.
     */
    public Entity entity() {
        if (previewType == null) return entity;
        Minecraft client = Minecraft.getInstance();
        var level = client == null ? null : client.level;
        if (level == null) entity = null;
        else if (entity == null || entity.level() != level) entity = previewType.create(level);
        return entity;
    }

    /**
     * Вычисляет AABB с основанием в Y=0 и горизонтальным полуразмером width / 2.
     *
     * @param width  ширина сущности в блоках.
     * @param height высота в блоках.
     * @return локальный AABB.
     */
    private static Aabb bounds(float width, float height) {
        float half = width / 2;
        return new Aabb(-half, 0, -half, half, height, half);
    }
}
