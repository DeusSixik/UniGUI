package dev.sixik.isf.client;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Кнопка-предмет внутри визуала рецепта.
 *
 * <p>Поддерживает альтернативы (теги/ingredients): показывается один вариант,
 * который автоматически циклически переключается, а тултип перечисляет все.
 * Клики обрабатывает окно рецептов через ручной hit-test
 * ({@link IsfBrowserOverlay#clickDetailControls}), потому что consume нажатия
 * внутри Z-слоя блокирует доставку release и резервный путь IsfClient.
 * Поэтому кнопка не захватывает указатель: она рисует курсор-руку и
 * визуал предмета, а press/release прозрачно проходят мимо.</p>
 */
final class IsfItemButton extends Button {
    private static final long CYCLE_MILLIS = 1000L;

    private final List<ResourceLocation> itemIds;
    private final List<ItemStack> stacks;
    private final IsfItemIconWidget icon;
    private final List<Component> tooltipLines;
    private int shownIndex;
    private long nextCycleMillis;

    IsfItemButton(ResourceLocation itemId, ItemStack stack) {
        this(List.of(itemId), List.of(stack));
    }

    IsfItemButton(List<ResourceLocation> itemIds, List<ItemStack> stacks) {
        this.itemIds = List.copyOf(itemIds);
        this.stacks = List.copyOf(stacks);
        this.icon = new IsfItemIconWidget(this.stacks.get(0));
        this.tooltipLines = stacks.size() > 1 ? buildLines(stacks) : null;
        // Сдвиг фазы, чтобы соседние клетки переключались не синхронно.
        this.nextCycleMillis = System.currentTimeMillis()
                + phaseOffset(itemIds.get(0));
        textPadding(0.0f, 0.0f);
        icon.enabled(false);
        icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
        addChild(icon);
    }

    private static long phaseOffset(ResourceLocation id) {
        return Math.floorMod(id.hashCode(), (int) CYCLE_MILLIS);
    }

    private static List<Component> buildLines(List<ItemStack> stacks) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("isf.tooltip.accepts"));
        for (ItemStack stack : stacks) {
            lines.add(stack.getHoverName());
        }
        return List.copyOf(lines);
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (stacks.size() <= 1) return;
        long now = System.currentTimeMillis();
        if (now < nextCycleMillis) return;
        nextCycleMillis = now + CYCLE_MILLIS;
        shownIndex = (shownIndex + 1) % stacks.size();
        icon.stack(stacks.get(shownIndex));
    }

    @Override
    public void handle(Event event) {
        // Нажатия/отпускания не потребляем: event.cancel() здесь отменил бы
        // Forge-событие и IsfClient никогда не увидел бы клик по предмету.
        if (event instanceof PointerEvent pointer
                && (pointer.phase() == EventPhase.TARGET || pointer.phase() == EventPhase.CAPTURE)
                && (event instanceof PointerPressedEvent || event instanceof PointerReleasedEvent)) {
            return;
        }
        super.handle(event);
    }

    /** Предмет, показанный в клетке прямо сейчас. */
    ResourceLocation itemId() {
        return itemIds.get(shownIndex);
    }

    ItemStack stack() {
        return stacks.get(shownIndex);
    }

    /** Строки тултипа для клеток с несколькими альтернативами; {@code null} — обычный item tooltip. */
    List<Component> tooltipLines() {
        return tooltipLines;
    }
}
