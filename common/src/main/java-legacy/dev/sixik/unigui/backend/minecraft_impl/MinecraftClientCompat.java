package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.EntityActionClip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Клиентские API Minecraft, которые отличаются между версиями (1.20.1, 1.21.1).
 *
 * <p>Общий код (демо, тестовые команды, рантайм UniGUI) обращается к экрану, оверлею загрузки и ванильным
 * типам сущностей только через этот класс; у каждой версии своя реализация.</p>
 */
public final class MinecraftClientCompat {
    /**
     * @return the native six-tick main-hand swing preset
     */
    static EntityActionClip<?> mainHandSwing() {
        return LegacySceneAnimation.SWING;
    }

    /**
     * Whether native item and entity models can use scene surface effects.
     */
    public static final boolean NATIVE_SCENE_MATERIALS = true;
    /**
     * Whether the backend can draw vanilla particle emitters in Scene3D.
     */
    public static final boolean NATIVE_SCENE_PARTICLES = true;
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
     * @return можно ли создавать {@code ItemStack}. В 1.20/1.21 всегда можно.
     */
    public static boolean itemsReady() {
        return true;
    }
}
