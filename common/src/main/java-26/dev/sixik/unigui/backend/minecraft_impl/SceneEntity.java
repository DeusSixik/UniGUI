package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.EntityNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Resolved {@link dev.sixik.unigui.api.scene.EntityNode} entity for 26.x. The origin is at the feet.
 *
 * <p>The source is a live entity, a supplied render state, or a type for previews. Type-based previews
 * are created when a client world appears and recreated if it changes. Without a world, a static model
 * is available for certain types.</p>
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
     * Caller-supplied render state or static preview when no world is loaded.
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
     * Resolves a render state when the node has no live entity. Animated previews get a state
     * owned by the current frame, so disabling a callback also restores the default pose.
     *
     * @param animated whether the caller will override the preview pose
     * @return caller-supplied state, a default preview state, or null for unsupported previews
     */
    EntityRenderState fixedState(boolean animated) {
        if (previewType == null) return fixedState;
        if (animated) return createPreviewState();
        if (fixedState == null) fixedState = createPreviewState();
        return fixedState;
    }

    /**
     * @return whether the caller owns the supplied state and its animation
     */
    boolean hasSuppliedState() {
        return previewType == null && fixedState != null;
    }

    /**
     * @return a fresh full-bright fallback for a preview that works outside a world, or null
     */
    private EntityRenderState createPreviewState() {
        EntityRenderState state;
        if (previewType == MinecraftClientCompat.ZOMBIE) {
            state = new ZombieRenderState();
        } else if (MinecraftClientCompat.NATIVE_SCENE_MATERIALS && previewType == MinecraftClientCompat.CREEPER) {
            state = new CreeperRenderState();
        } else {
            return null;
        }
        state.entityType = previewType;
        state.boundingBoxWidth = previewType.getWidth();
        state.boundingBoxHeight = previewType.getHeight();
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        return state;
    }

    /**
     * Applies animation to the frame's native state. The vanilla age is {@code 20 * seconds}
     * in ticks. Living models also receive the accumulated native walk phase and stride strength.
     *
     * @param node  scene animation inputs
     * @param state freshly extracted entity or preview state
     */
    static void sampleAnimation(EntityNode node, EntityRenderState state) {
        if (node.hasAnimation()) {
            state.ageInTicks = (float) Math.min(20.0 * node.animationSeconds(), Float.MAX_VALUE);
            if (state instanceof LivingEntityRenderState living) {
                living.walkAnimationPos = node.walkPhase();
                living.walkAnimationSpeed = node.walkStrength();
            }
        }
        node.sampleNativeAnimation(state);
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
