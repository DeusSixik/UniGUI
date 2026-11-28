package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.EntityActionClip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Клиентские API Minecraft, которые отличаются между версиями (26.1).
 *
 * <p>Общий код (демо, тестовые команды, рантайм UniGUI) обращается к экрану, оверлею загрузки и ванильным
 * типам сущностей только через этот класс; у каждой версии своя реализация.</p>
 */
public final class MinecraftClientCompat {
    /**
     * @return the native main-hand swing preset
     */
    static EntityActionClip<?> mainHandSwing() {
        return MainHandSwing.CLIP;
    }

    /**
     * Loads the native swing state when a caller first requests the clip.
     */
    private static final class MainHandSwing {
        /**
         * Uses the native swing's duration converted from ticks to seconds.
         */
        private static final EntityActionClip<ArmedEntityRenderState> CLIP = new EntityActionClip<>(
                SwingAnimation.DEFAULT.duration() / 20f, ArmedEntityRenderState.class, (state, phase) -> {
            state.attackArm = state.mainArm;
            state.swingAnimationType = SwingAnimation.DEFAULT.type();
            state.attackTime = phase;
        });
    }

    /**
     * Whether native item and entity models can use scene surface effects.
     */
    public static final boolean NATIVE_SCENE_MATERIALS = UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS;
    /**
     * Whether the backend can draw vanilla particle emitters in Scene3D.
     */
    public static final boolean NATIVE_SCENE_PARTICLES = false;
    public static final EntityType<? extends LivingEntity> ZOMBIE = EntityType.ZOMBIE;
    public static final EntityType<? extends LivingEntity> SKELETON = EntityType.SKELETON;
    public static final EntityType<? extends LivingEntity> CREEPER = EntityType.CREEPER;
    public static final EntityType<? extends LivingEntity> VILLAGER = EntityType.VILLAGER;
    public static final EntityType<? extends LivingEntity> PIG = EntityType.PIG;
    public static final EntityType<? extends LivingEntity> COW = EntityType.COW;
    public static final EntityType<? extends LivingEntity> SHEEP = EntityType.SHEEP;
    public static final EntityType<? extends LivingEntity> CHICKEN = EntityType.CHICKEN;

    private MinecraftClientCompat() {
    }

    /**
     * @return открытый экран или {@code null}
     */
    public static @Nullable Screen screen() {
        return Minecraft.getInstance().screen;
    }

    /**
     * Открывает экран ({@code null} закрывает текущий).
     */
    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * @return текущий оверлей (экран загрузки ресурсов) или {@code null}
     */
    public static @Nullable Overlay overlay() {
        return Minecraft.getInstance().getOverlay();
    }

    /**
     * Ставит оверлей ({@code null} убирает текущий).
     */
    public static void setOverlay(@Nullable Overlay overlay) {
        Minecraft.getInstance().setOverlay(overlay);
    }

    /**
     * @return блок красной шерсти (для демо)
     */
    public static Block redWool() {
        return Blocks.RED_WOOL;
    }

    /**
     * @return буферы отрисовки клиента
     */
    public static RenderBuffers renderBuffers() {
        return Minecraft.getInstance().renderBuffers();
    }

    /**
     * Создаёт сущность для превью в GUI (в мир она не добавляется). В 26.1 {@code EntityType.create} проверяет
     * только включённость типа, сложность мира не влияет.
     *
     * @param type  тип сущности.
     * @param level уровень для разрешения её данных.
     * @param <T>   класс сущности.
     * @return сущность с локальным ID или {@code null}, если тип отключён или фабрика её не создала.
     */
    public static <T extends net.minecraft.world.entity.Entity> @Nullable T previewEntity(EntityType<T> type,
                                                                                          net.minecraft.world.level.Level level) {
        return PreviewEntityIds.assign(type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD));
    }

    /**
     * @return можно ли создавать {@code ItemStack}: в 26.x компоненты предметов привязываются только после
     * загрузки мира, а до этого {@code new ItemStack} падает с «Components not bound yet».
     */
    public static boolean itemsReady() {
        return Items.STONE.builtInRegistryHolder().areComponentsBound();
    }

    /**
     * Рисует подсказку из готовых компонентов в текущем кадре GUI.
     */
    public static void tooltip(net.minecraft.client.gui.GuiGraphicsExtractor extractor, net.minecraft.client.gui.Font font,
                               java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> components,
                               int x, int y,
                               net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,
                               net.minecraft.resources.@Nullable Identifier style) {
        extractor.tooltip(font, components, x, y, positioner, style);
    }
}
