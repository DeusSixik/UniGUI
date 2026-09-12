package dev.sixik.isf.client;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Кнопка-предмет внутри визуала рецепта.
 *
 * <p>Клики обрабатывает окно рецептов через ручной hit-test
 * ({@link IsfBrowserOverlay#clickDetailControls}), потому что consume нажатия
 * внутри Z-слоя блокирует доставку release и резервный путь IsfClient.
 * Поэтому кнопка не захватывает указатель: она рисует курсор-руку и
 * визуал предмета, а press/release прозрачно проходят мимо.</p>
 */
final class IsfItemButton extends Button {
    private final ResourceLocation itemId;
    private final ItemStack stack;

    IsfItemButton(ResourceLocation itemId, ItemStack stack) {
        this.itemId = itemId;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        textPadding(0.0f, 0.0f);
        // Иконка только рисуется; hit-box принадлежит кнопке.
        IsfItemIconWidget icon = new IsfItemIconWidget(this.stack);
        icon.enabled(false);
        icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
        addChild(icon);
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

    ResourceLocation itemId() {
        return itemId;
    }

    ItemStack stack() {
        return stack;
    }
}
