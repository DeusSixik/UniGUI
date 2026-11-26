package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Разрешённая сущность {@link dev.sixik.unigui.api.scene.EntityNode} для 26.x. Начало координат у ног.
 *
 * <p>Источник — готовая сущность, готовое состояние рендера или тип для превью. Превью по типу создаётся при
 * появлении клиентского мира и пересоздаётся при его смене. Без мира зомби показывается через статичное
 * состояние рендера, а остальные типы не рисуются.</p>
 */
final class SceneEntity {
    /**
     * Сущность пользователя или превью для текущего мира.
     */
    private Entity entity;
    /**
     * Тип превью или null для сущности и состояния пользователя.
     */
    private final EntityType<?> previewType;
    /**
     * Готовое состояние рендера пользователя или статичный зомби без мира.
     */
    private EntityRenderState fixedState;

    /**
     * Создаёт обёртку для одного вида источника.
     *
     * @param entity      сущность пользователя или null.
     * @param previewType тип превью или null.
     * @param fixedState  состояние пользователя или null.
     */
    private SceneEntity(Entity entity, EntityType<?> previewType, EntityRenderState fixedState) {
        this.entity = entity;
        this.previewType = previewType;
        this.fixedState = fixedState;
    }

    /**
     * Принимает нативный объект пользователя.
     *
     * @param source {@link Entity}, {@link EntityType} или {@link EntityRenderState} с заданным типом.
     * @return обёртка.
     * @throws IllegalArgumentException если тип объекта не подходит.
     */
    static SceneEntity of(Object source) {
        if (source instanceof Entity value) {
            PreviewEntityIds.ensureAssigned(value);
            return new SceneEntity(value, null, null);
        }
        if (source instanceof EntityType<?> type) return preview(type);
        if (source instanceof EntityRenderState state) {
            Objects.requireNonNull(state.entityType, "state.entityType");
            return new SceneEntity(null, null, state);
        }
        throw new IllegalArgumentException("Expected Entity, EntityType or EntityRenderState, got "
                + source.getClass().getName());
    }

    /**
     * Создаёт отложенное превью по типу.
     *
     * @param type тип сущности.
     * @return обёртка.
     */
    static SceneEntity preview(EntityType<?> type) {
        return new SceneEntity(null, Objects.requireNonNull(type, "type"), null);
    }

    /**
     * Возвращает сущность для извлечения позы текущего кадра.
     *
     * @return сущность или null, если превью ждёт мир либо источник — готовое состояние.
     */
    Entity entity() {
        if (previewType == null) return entity;
        Minecraft client = Minecraft.getInstance();
        var level = client == null ? null : client.level;
        if (level == null) {
            entity = null;
        } else if (entity == null || entity.level() != level) {
            entity = MinecraftClientCompat.previewEntity(previewType, level);
        }
        return entity;
    }

    /**
     * Возвращает готовое состояние рендера, если сущности нет.
     *
     * @return состояние пользователя, статичный зомби без мира или null.
     */
    EntityRenderState fixedState() {
        if (previewType == null) return fixedState;
        if (previewType != MinecraftClientCompat.ZOMBIE) return null;
        if (fixedState == null) {
            ZombieRenderState state = new ZombieRenderState();
            state.entityType = previewType;
            state.boundingBoxWidth = previewType.getWidth();
            state.boundingBoxHeight = previewType.getHeight();
            fixedState = state;
        }
        return fixedState;
    }

    /**
     * Вычисляет AABB с основанием в Y=0.
     *
     * @return локальные границы сущности.
     */
    Aabb bounds() {
        if (previewType != null) return bounds(previewType.getWidth(), previewType.getHeight());
        if (entity != null) return bounds(entity.getBbWidth(), entity.getBbHeight());
        return bounds(fixedState.boundingBoxWidth, fixedState.boundingBoxHeight);
    }

    /**
     * Строит AABB с горизонтальным полуразмером width / 2.
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
