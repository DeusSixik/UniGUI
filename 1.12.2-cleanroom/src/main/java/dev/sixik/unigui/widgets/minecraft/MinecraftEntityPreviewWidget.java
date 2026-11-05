package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.cleanroom.backend.CleanroomGuiRenderBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.lang.ref.WeakReference;

/**
 * Shows a living entity the way the inventory screen shows the player: standing, head turned toward a point.
 *
 * <p>The entity is described either by its registry entry ({@code entityType}, a new instance is created in the
 * current world) or by an existing entity ({@link #entity(EntityLivingBase)}, for example the player). If the
 * entity cannot be created or drawn, a player head is shown instead.</p>
 */
public final class MinecraftEntityPreviewWidget extends MinecraftPreviewWidget {
    private EntityEntry entityType;
    private EntityLivingBase explicitEntity;
    private EntityLivingBase created;
    private WeakReference<World> createdIn = new WeakReference<>(null);
    private boolean creationFailed;
    private float lookX = 22.0f;
    private float lookY = 12.0f;

    public MinecraftEntityPreviewWidget(String label, EntityEntry entityType) {
        super(label);
        this.entityType = entityType;
    }

    public EntityEntry entityType() {
        return entityType;
    }

    public MinecraftEntityPreviewWidget entityType(EntityEntry entityType) {
        if (this.entityType == entityType) return this;
        this.entityType = entityType;
        this.created = null;
        this.creationFailed = false;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * @return the entity set with {@link #entity(EntityLivingBase)}, or {@code null}
     */
    public EntityLivingBase entity() {
        return explicitEntity;
    }

    /**
     * Shows an existing entity instead of creating one from the entry.
     *
     * @param entity entity to draw, {@code null} goes back to {@link #entityType()}
     * @return this widget
     */
    public MinecraftEntityPreviewWidget entity(EntityLivingBase entity) {
        if (explicitEntity == entity) return this;
        explicitEntity = entity;
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
    protected void renderMinecraftPreview(CleanroomGuiRenderBackend backend, float x, float y, float size, float opacity) {
        EntityLivingBase entity = resolveEntity();
        if (entity == null || !backend.renderEntityPreview(entity, x, y, size, lookX, lookY, opacity)) {
            // Skull metadata 3 is the player head.
            backend.renderItemPreview(new ItemStack(Items.SKULL, 1, 3), x, y, size, opacity, false);
        }
    }

    private EntityLivingBase resolveEntity() {
        if (explicitEntity != null) return explicitEntity;
        World world = Minecraft.getMinecraft().world;
        if (entityType == null || world == null) return null;
        if (created != null && createdIn.get() == world) return created;
        if (creationFailed && createdIn.get() == world) return null;
        createdIn = new WeakReference<>(world);
        created = null;
        creationFailed = true;
        try {
            Entity entity = entityType.newInstance(world);
            if (entity instanceof EntityLivingBase living) {
                created = living;
                creationFailed = false;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Some entities cannot exist outside their usual setup; the head placeholder is shown.
        }
        return created;
    }

    @Override
    protected String fallbackText() {
        return "entity";
    }
}