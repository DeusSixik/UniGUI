package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Клиентские API Minecraft, которые отличаются между версиями (26.2: экраны в Minecraft.gui, типы сущностей в EntityTypes, шерсть в Blocks.WOOL).
 *
 * <p>Общий код (демо, тестовые команды, рантайм UniGUI) обращается к экрану, оверлею загрузки и ванильным
 * типам сущностей только через этот класс; у каждой версии своя реализация.</p>
 */
public final class MinecraftClientCompat {
    public static final EntityType<? extends LivingEntity> ZOMBIE = EntityTypes.ZOMBIE;
    public static final EntityType<? extends LivingEntity> SKELETON = EntityTypes.SKELETON;
    public static final EntityType<? extends LivingEntity> CREEPER = EntityTypes.CREEPER;
    public static final EntityType<? extends LivingEntity> VILLAGER = EntityTypes.VILLAGER;
    public static final EntityType<? extends LivingEntity> PIG = EntityTypes.PIG;
    public static final EntityType<? extends LivingEntity> COW = EntityTypes.COW;
    public static final EntityType<? extends LivingEntity> SHEEP = EntityTypes.SHEEP;
    public static final EntityType<? extends LivingEntity> CHICKEN = EntityTypes.CHICKEN;

    private MinecraftClientCompat() {
    }

    /**
     * @return открытый экран или {@code null}
     */
    public static @Nullable Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    /**
     * Открывает экран ({@code null} закрывает текущий).
     */
    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    /**
     * @return текущий оверлей (экран загрузки ресурсов) или {@code null}
     */
    public static @Nullable Overlay overlay() {
        return Minecraft.getInstance().gui.overlay();
    }

    /**
     * Ставит оверлей ({@code null} убирает текущий).
     */
    public static void setOverlay(@Nullable Overlay overlay) {
        Minecraft.getInstance().gui.setOverlay(overlay);
    }

    /**
     * @return блок красной шерсти (для демо)
     */
    public static Block redWool() {
        return Blocks.WOOL.red();
    }

    /**
     * @return буферы отрисовки клиента
     */
    public static RenderBuffers renderBuffers() {
        return Minecraft.getInstance().gameRenderer.renderBuffers();
    }

    /**
     * Создаёт сущность для превью в GUI (в мир она не добавляется).
     *
     * <p>В 26.2 {@code EntityType.create} проверяет, может ли сущность появиться в мире, и на мирной сложности
     * возвращает {@code null} для монстров. Для превью эти проверки не нужны, поэтому они пропускаются.</p>
     *
     * @param type  тип сущности.
     * @param level уровень для разрешения её данных.
     * @param <T>   класс сущности.
     * @return сущность с локальным ID или {@code null}, если тип отключён или фабрика её не создала.
     */
    public static <T extends net.minecraft.world.entity.Entity> @Nullable T previewEntity(EntityType<T> type,
                                                                                          net.minecraft.world.level.Level level) {
        if (!type.isEnabled(level.enabledFeatures())) return null;
        return PreviewEntityIds.assign(type.create(level, new EntitySpawnRequest(EntitySpawnReason.LOAD, true)));
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
