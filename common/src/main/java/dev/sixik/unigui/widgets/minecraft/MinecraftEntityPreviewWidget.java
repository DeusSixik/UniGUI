package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.backend.minecraft.MinecraftGuiRenderBackend;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public final class MinecraftEntityPreviewWidget extends MinecraftPreviewWidget {
    private EntityType<? extends LivingEntity> entityType;
    private float lookX = 22.0f;
    private float lookY = 12.0f;

    public MinecraftEntityPreviewWidget(String label, EntityType<? extends LivingEntity> entityType) {
        super(label);
        this.entityType = entityType;
    }

    public EntityType<? extends LivingEntity> entityType() {
        return entityType;
    }

    public MinecraftEntityPreviewWidget entityType(EntityType<? extends LivingEntity> entityType) {
        if (this.entityType == entityType) return this;
        this.entityType = entityType;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MinecraftEntityPreviewWidget look(float x, float y) {
        lookX = Float.isFinite(x) ? x : 0.0f;
        lookY = Float.isFinite(y) ? y : 0.0f;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity) {
        if (!backend.renderEntityPreview(entityType, x, y, size, lookX, lookY, opacity)) {
            backend.renderItemPreview(Items.PLAYER_HEAD.getDefaultInstance(), x, y, size, opacity, false);
        }
    }

    @Override
    protected String fallbackText() {
        return "entity";
    }
}
