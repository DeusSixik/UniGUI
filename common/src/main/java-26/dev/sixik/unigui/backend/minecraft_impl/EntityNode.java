package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Ванильная сущность. Начало локальных координат находится у ног.
 */
public final class EntityNode extends SceneNode {
    /**
     * Сущность, из которой бэкенд извлекает состояние с текущим partialTick.
     */
    private final Entity entity;
    /**
     * Готовое состояние для статичного превью, в том числе без загруженного мира.
     */
    private final EntityRenderState fixedState;

    /**
     * Использует текущую позу и анимацию сущности. Владельцу принадлежит обновление её состояния.
     * Если сущности ещё не выдан ID, узел назначает локальный ID для рендера.
     *
     * @param entity сущность Minecraft.
     */
    public EntityNode(Entity entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
        PreviewEntityIds.ensureAssigned(this.entity);
        fixedState = null;
        setBounds(bounds(entity.getBbWidth(), entity.getBbHeight()));
    }

    /**
     * Показывает заранее подготовленное состояние ванильного рендерера.
     * После редактирования этого состояния вызовите {@link #invalidateContent()}.
     *
     * @param state состояние с заданным типом сущности и размерами.
     */
    public EntityNode(EntityRenderState state) {
        entity = null;
        fixedState = Objects.requireNonNull(state, "state");
        Objects.requireNonNull(state.entityType, "state.entityType");
        setBounds(bounds(state.boundingBoxWidth, state.boundingBoxHeight));
    }

    /**
     * Создаёт сущность для превью без добавления в мир.
     *
     * @param type ванильный тип сущности.
     * @return узел превью.
     * @throws IllegalArgumentException если тип недоступен в текущем уровне.
     */
    public static EntityNode preview(EntityType<?> type) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            Entity entity = MinecraftClientCompat.previewEntity(type, client.level);
            if (entity != null) return new EntityNode(entity);
        }
        if (type == MinecraftClientCompat.ZOMBIE) {
            ZombieRenderState state = new ZombieRenderState();
            state.entityType = type;
            state.boundingBoxWidth = type.getWidth();
            state.boundingBoxHeight = type.getHeight();
            return new EntityNode(state);
        }
        throw new IllegalArgumentException("Entity type is unavailable in the current client level: " + type);
    }

    /**
     * Читает источник анимации живого узла.
     *
     * @return сущность или null для готового состояния.
     */
    public Entity entity() {
        return entity;
    }

    /**
     * Читает готовое состояние узла.
     *
     * @return состояние или null для узла, привязанного к сущности.
     */
    public EntityRenderState fixedState() {
        return fixedState;
    }

    /**
     * Считает локальные границы от уровня ног.
     *
     * @param width  ширина сущности в блоках.
     * @param height высота сущности в блоках.
     * @return параллелепипед для отсечения и выбора.
     */
    private static Aabb bounds(float width, float height) {
        float half = width / 2;
        return new Aabb(-half, 0, -half, half, height, half);
    }
}
