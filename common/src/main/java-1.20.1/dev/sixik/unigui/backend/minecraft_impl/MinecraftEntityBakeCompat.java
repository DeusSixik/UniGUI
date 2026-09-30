package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Version-specific пекер иконок живых сущностей.
 *
 * <p>Рендерит модель моба в offscreen render target (по образцу
 * {@code InventoryScreen.renderEntityInInventory}, но без привязки
 * к текущему фреймбуферу), после чего иконку можно рисовать обычной
 * texture-командой, уважающей transform/clip Z-слоёв UniGUI.</p>
 */
public final class MinecraftEntityBakeCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftEntityBakeCompat.class);
    private static final int ICON_PIXELS = 64;
    private static final int CACHE_CAPACITY = 64;
    /** Доля иконки, которую занимает модель по большей стороне (остальное — поля). */
    private static final float MODEL_FILL = 0.78f;
    /** Фиксированный лёгкий поворот модели: классический вид «куклы» из инвентаря. */
    private static final float LOOK_X = 12.0f;
    private static final float LOOK_Y = 6.0f;

    private static final Map<EntityType<?>, MinecraftRenderTarget> CACHE =
            new LinkedHashMap<>(CACHE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<EntityType<?>, MinecraftRenderTarget> eldest) {
                    if (size() <= CACHE_CAPACITY) return false;
                    eldest.getValue().close();
                    return true;
                }
            };
    /**
     * Типы, которые точно не являются живыми сущностями (запечка невозможна).
     * Запоминаем отрицательный результат, чтобы не создавать сущность каждый кадр.
     */
    private static final Set<EntityType<?>> UNSUPPORTED =
            Collections.newSetFromMap(new LinkedHashMap<>(CACHE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<EntityType<?>, Boolean> eldest) {
                    return size() > CACHE_CAPACITY;
                }
            });

    private MinecraftEntityBakeCompat() {
    }

    /**
     * Запечённая иконка живой сущности (кэшируется по типу).
     *
     * <p>Проверка «живости» делается через {@code instanceof} на созданной сущности:
     * {@code EntityType.getBaseClass()} для этого не подходит — в 1.20.1 он всегда
     * возвращает {@code Entity.class} (это базовый класс для {@code EntityTypeTest}).</p>
     *
     * @return handle текстуры или {@code null}, если сущность не удалось создать/запечь.
     */
    public static TextureHandle bakePreview(EntityType<?> type, int pixels) {
        if (type == null || UNSUPPORTED.contains(type)) return null;
        MinecraftRenderTarget cached = CACHE.get(type);
        if (cached != null) return cached.colorTexture();
        Minecraft minecraft = Minecraft.getInstance();
        // EntityType.create обращается к level.enabledFeatures(): без клиентского мира
        // будет NPE, поэтому вне мира сразу возвращаем null (не кэшируем — попробуем позже).
        if (minecraft.level == null) {
            return null;
        }
        LivingEntity entity;
        try {
            if (!(type.create(minecraft.level) instanceof LivingEntity living)) {
                UNSUPPORTED.add(type);
                LOGGER.debug("Entity type {} is not a living entity, preview skipped", type);
                return null;
            }
            entity = living;
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to create entity preview for {}", type, e);
            return null;
        }
        // Сущность никуда не спавним (живёт только на время запечки), поэтому discard()
        // не вызываем: он помечает её удалённой безо всякой пользы.
        int clampedPixels = Math.max(16, Math.min(256, pixels));
        MinecraftRenderTarget target;
        try {
            target = bake(entity, clampedPixels);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to bake entity preview for {}", type, e);
            return null;
        }
        CACHE.put(type, target);
        return target.colorTexture();
    }

    /** Рендерит сущность в новый offscreen-таргет {@code pixels x pixels}. */
    private static MinecraftRenderTarget bake(LivingEntity entity, int pixels) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftRenderTarget target = new MinecraftRenderTarget(
                pixels, pixels, new RenderTargetOptions(true, true, "entity_preview_" + pixels));
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer scissorBox = BufferUtils.createIntBuffer(16);
        if (scissorEnabled) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        // Копия RenderPassState.capture() из FastItemRenderer: GL-состояния,
        // изменённые entity-рендером, восстанавливаются после прохода.
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableScissor();
        target.bindWrite();
        RenderSystem.backupProjectionMatrix();
        Object modelView = RenderSystem.getModelViewStack();
        MinecraftFastItemCompat.pushModelView(modelView);
        try {
            MinecraftFastItemCompat.resetModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            float depth = Math.max(1000.0f, pixels * 32.0f);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0f, pixels, pixels, 0.0f, -depth, depth),
                    VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            // Entity-шейдеры требуют 3D-освещения (как setupFor3DItems у предметов).
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();

            GuiGraphics graphics = new GuiGraphics(minecraft, MinecraftBufferCompat.immediate(256));
            // Геометрия как в ванильном inventory-превью: центр по X, ноги у нижней кромки.
            // Масштаб подбирается под габариты сущности: фиксированный 0.72 подходил только
            // мобам ростом ~1 блок, а высокие (эндермен, гаст, голем) целиком уходили за
            // пределы текстуры и иконка оставалась пустой.
            int centerX = pixels / 2;
            int bottomY = Math.round(pixels * 0.92f);
            float bounds = Math.max(entity.getBbHeight(), entity.getBbWidth());
            int entityScale = bounds <= 0.0f
                    ? Math.max(1, Math.round(pixels * 0.72f))
                    : Math.max(1, Math.round(pixels * MODEL_FILL / bounds));
            MinecraftEntityPreviewCompat.render(graphics, centerX, bottomY, entityScale,
                    pixels, LOOK_X, LOOK_Y, entity);
            graphics.flush();
        } finally {
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(depthMask);
            if (!depthTest) RenderSystem.disableDepthTest();
            MinecraftFastItemCompat.popModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.unbindWrite();
            minecraft.getMainRenderTarget().bindWrite(true);
            if (scissorEnabled) {
                RenderSystem.enableScissor(scissorBox.get(0), scissorBox.get(1),
                        scissorBox.get(2), scissorBox.get(3));
            }
        }
        return target;
    }
}
