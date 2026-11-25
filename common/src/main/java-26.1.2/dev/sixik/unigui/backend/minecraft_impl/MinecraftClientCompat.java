package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
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
     * @return можно ли создавать {@code ItemStack}: в 26.x компоненты предметов привязываются только после
     * загрузки мира, а до этого {@code new ItemStack} падает с «Components not bound yet».
     */
    /**
     * Создаёт сущность для превью в GUI (в мир она не добавляется). В 26.1 {@code EntityType.create} проверяет
     * только включённость типа, сложность мира не влияет.
     *
     * @return сущность или {@code null}, если тип отключён или фабрика её не создала
     */
    public static <T extends net.minecraft.world.entity.Entity> @Nullable T previewEntity(EntityType<T> type,
                                                                                         net.minecraft.world.level.Level level) {
        return type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
    }

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
