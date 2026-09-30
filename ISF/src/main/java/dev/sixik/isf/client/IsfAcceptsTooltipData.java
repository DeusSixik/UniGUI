package dev.sixik.isf.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Tooltip-данные «Принимает:» со списком вариантов тега-ингредиента.
 *
 * <p>Маркер реализует ванильный {@link TooltipComponent} — так его принимает
 * {@code GuiGraphics.renderTooltip(font, lines, Optional<TooltipComponent>, x, y)}.
 * Forge конвертирует его в {@link ClientTooltipComponent} через зарегистрированную
 * фабрику ({@code RegisterClientTooltipComponentFactoriesEvent}); текст заголовка
 * и имена предметов рисуются как текстовые строки компонента, ниже — сетка
 * предметных моделей (как image-блок тултипа bundle).</p>
 */
public final class IsfAcceptsTooltipData implements TooltipComponent {
    /** Максимум моделей в одной строке сетки. */
    public static final int GRID_COLUMNS = 5;

    private final List<ItemStack> stacks;

    public IsfAcceptsTooltipData(List<ItemStack> stacks) {
        this.stacks = List.copyOf(stacks);
    }

    public List<ItemStack> stacks() {
        return stacks;
    }

    /** Только заголовок «Принимает:»: имена предметов не выводим, только модели. */
    public List<FormattedCharSequence> textLines(Font font) {
        return List.copyOf(font.split(Component.translatable("isf.tooltip.accepts"), 220));
    }

    /** Внутренний клиентский рендер: текстовые строки + сетка моделей. */
    public static final class GridComponent implements ClientTooltipComponent {
        private static final int CELL = 18;
        private static final int PADDING = 2;

        private final List<FormattedCharSequence> textLines;
        private final List<ItemStack> stacks;

        public GridComponent(IsfAcceptsTooltipData data, Font font) {
//            this.textLines = data.textLines(font);
            this.textLines = new ObjectArrayList<>();
            this.stacks = data.stacks();
        }

        @Override
        public int getHeight() {
            return textLines.size() * 10 + PADDING + gridHeight();
        }

        private int gridHeight() {
            int columns = Math.min(IsfAcceptsTooltipData.GRID_COLUMNS, Math.max(1, stacks.size()));
            return ((stacks.size() + columns - 1) / columns) * CELL;
        }

        @Override
        public int getWidth(Font font) {
            int textWidth = 0;
            for (FormattedCharSequence line : textLines) {
                textWidth = Math.max(textWidth, font.width(line));
            }
            int columns = Math.min(IsfAcceptsTooltipData.GRID_COLUMNS, stacks.size());
            return Math.max(textWidth, columns * CELL + PADDING * 2) + 8;
        }

        @Override
        public void renderText(Font font, int x, int y, org.joml.Matrix4f matrix,
                               net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
            for (int index = 0; index < textLines.size(); index++) {
                font.drawInBatch(textLines.get(index), x, y + index * 10, 0xFFA0A0A0, false,
                        matrix, buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int gridY = y + textLines.size() * 10 + PADDING;
            int columns = Math.min(IsfAcceptsTooltipData.GRID_COLUMNS, stacks.size());
            for (int index = 0; index < stacks.size(); index++) {
                ItemStack stack = stacks.get(index);
                int px = x + PADDING + (index % columns) * CELL;
                int py = gridY + (index / columns) * CELL;
                graphics.renderItem(stack, px, py);
                graphics.renderItemDecorations(font, stack, px, py);
            }
        }
    }
}
