package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.render.TooltipRenderer;
import dev.sixik.unigui.widgets.render.TooltipState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Minecraft-specific tooltip renderers that delegate visual drawing to vanilla {@link net.minecraft.client.gui.GuiGraphics}.
 *
 * <p>These helpers intentionally live in the Minecraft widgets package so core UniGUI tooltips remain backend-neutral.
 * Use {@link #useVanilla(Tooltip)} or the factory methods when you want the vanilla tooltip background/font/item
 * components instead of the default UniGUI tooltip chrome.</p>
 */
public final class MinecraftTooltipRenderers {
    private static final float VANILLA_TOOLTIP_OFFSET_X = 12.0f;
    private static final float VANILLA_TOOLTIP_OFFSET_Y = -12.0f;

    private MinecraftTooltipRenderers() {
    }

    /**
     * Renders the current UniGUI tooltip text using Minecraft's vanilla tooltip renderer.
     */
    public static WidgetRender vanilla() {
        return WidgetRender.of(Tooltip.class, (draw, tooltip) -> {
            renderComponents(draw, tooltip, stateComponents(draw, tooltip));
        });
    }

    public static WidgetRender vanilla(Component line) {
        return vanilla(line == null ? List.of() : List.of(line));
    }

    public static WidgetRender vanilla(Component... lines) {
        return vanilla(lines == null ? List.of() : Arrays.asList(lines));
    }

    public static WidgetRender vanilla(List<Component> lines) {
        List<Component> fixedLines = sanitizeComponents(lines);
        return WidgetRender.of(Tooltip.class, (draw, tooltip) ->
                renderComponents(draw, tooltip, fixedLines));
    }

    public static WidgetRender vanilla(Supplier<List<Component>> linesSupplier) {
        Objects.requireNonNull(linesSupplier, "linesSupplier");
        return WidgetRender.of(Tooltip.class, (draw, tooltip) ->
                renderComponents(draw, tooltip, sanitizeComponents(linesSupplier.get())));
    }

    /**
     * Renders an ItemStack tooltip through vanilla, preserving modded tooltip components/images where Minecraft exposes them.
     */
    public static WidgetRender item(ItemStack stack) {
        ItemStack fixedStack = stack == null ? ItemStack.EMPTY : stack.copy();
        return item(() -> fixedStack);
    }

    /**
     * Renders a dynamic ItemStack tooltip through vanilla.
     */
    public static WidgetRender item(Supplier<ItemStack> stackSupplier) {
        Objects.requireNonNull(stackSupplier, "stackSupplier");
        return WidgetRender.of(Tooltip.class, (draw, tooltip) -> {
            ItemStack stack = stackSupplier.get();
            if (stack == null || stack.isEmpty()) return;
            ItemStack tooltipStack = stack.copy();
            int mouseX = vanillaMouseX(tooltip);
            int mouseY = vanillaMouseY(tooltip);
            draw.addCallback(backend -> {
                if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                    minecraftBackend.renderVanillaTooltip(tooltipStack, mouseX, mouseY);
                }
            });
        });
    }

    /**
     * Ванильный тултип «Принимает:» со сеткой предметных моделей: заголовок и под
     * ним модели стеков по {@code columns} в ряд. Имена предметов не выводятся —
     * только модели. Модели рисует компонент, переданный через
     * {@code componentSupplier} (маркер
     * {@link TooltipComponent}, который
     * конвертируется клиентской фабрикой мода).
     */
    public static TooltipRenderer acceptsGrid(List<ItemStack> stacks,
                                               Supplier<TooltipComponent> componentSupplier) {
        return acceptsGrid(stacks, null, componentSupplier);
    }

    /**
     * Ванильный тултип «Принимает:» со сеткой предметных моделей (см. {@link #acceptsGrid(List, Supplier)}).
     *
     * @param tag id тега ингредиента без {@code #} ({@code null} — тег неизвестен):
     *            заголовок становится «Принимает любые:», а строка
     *            {@code #namespace:path} рисуется серым сразу под ним, над сеткой
     *            (как в JEI; строку кладёт сам компонент сетки, т.к. ваниль ставит
     *            image-блок строго после первой текстовой строки)
     */
    public static TooltipRenderer acceptsGrid(List<ItemStack> stacks, String tag,
                                               Supplier<TooltipComponent> componentSupplier) {
        List<ItemStack> fixed = stacks == null ? List.of() : List.copyOf(stacks);
        return WidgetRender.of(Tooltip.class, (draw, tooltip) -> {
            if (fixed.isEmpty()) return;
            List<Component> lines = new ObjectArrayList<>();
            if (tag == null || tag.isBlank()) {
                lines.add(Component.translatable("isf.tooltip.accepts"));
            } else {
                lines.add(Component.translatableWithFallback(
                        "isf.tooltip.accepts_tag", "Accepts any:"));
            }
            TooltipComponent component = componentSupplier.get();
            int mouseX = vanillaMouseX(tooltip);
            int mouseY = vanillaMouseY(tooltip);
            draw.addCallback(backend -> {
                if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                    minecraftBackend.renderVanillaTooltipWithComponent(lines, component, mouseX, mouseY);
                }
            });
        });
    }

    /**
     * Applies vanilla tooltip rendering to an existing Tooltip and disables UniGUI's own tooltip chrome.
     */
    public static Tooltip useVanilla(Tooltip tooltip) {
        return useVanilla(tooltip, vanilla());
    }

    /**
     * Applies a Minecraft tooltip renderer to an existing Tooltip and disables UniGUI's own tooltip chrome.
     */
    public static Tooltip useVanilla(Tooltip tooltip, WidgetRender renderer) {
        Objects.requireNonNull(tooltip, "tooltip");
        tooltip.backgroundVisible(false);
        tooltip.borderVisible(false);
        tooltip.themeEnabled(false);
        tooltip.renderer(renderer == null ? vanilla() : renderer);
        return tooltip;
    }

    public static Tooltip tooltip(Widget anchor, Component... lines) {
        return tooltip(anchor, lines == null ? List.of() : Arrays.asList(lines));
    }

    public static Tooltip tooltip(Widget anchor, List<Component> lines) {
        String fallbackText = plainTextFallback(lines);
        Tooltip tooltip = new Tooltip(anchor, fallbackText.isEmpty() ? " " : fallbackText);
        return useVanilla(tooltip, vanilla(lines));
    }

    public static MinecraftItemTooltip itemTooltip(Widget anchor, ItemStack stack) {
        return new MinecraftItemTooltip(anchor, stack);
    }

    public static MinecraftItemTooltip itemTooltip(Widget anchor, Supplier<ItemStack> stackSupplier) {
        return new MinecraftItemTooltip(anchor, stackSupplier);
    }

    private static void renderComponents(DrawScope draw, Tooltip tooltip, List<Component> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> capturedLines = List.copyOf(lines);
        int mouseX = vanillaMouseX(tooltip);
        int mouseY = vanillaMouseY(tooltip);
        draw.addCallback(backend -> {
            if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                minecraftBackend.renderVanillaTooltip(capturedLines, mouseX, mouseY);
            }
        });
    }

    private static List<Component> stateComponents(DrawScope draw, Tooltip tooltip) {
        if (tooltip == null) return List.of();
        List<RichText> lines = tooltip.wrappedLines(draw.context());
        if (lines.isEmpty()) return List.of();
        List<Component> components = new ObjectArrayList<>(lines.size());
        for (RichText line : lines) {
            String text = line == null ? "" : line.plainText();
            if (!text.isEmpty()) {
                components.add(Component.literal(text));
            }
        }
        return components;
    }

    private static List<Component> sanitizeComponents(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> result = new ObjectArrayList<>(lines.size());
        for (Component line : lines) {
            if (line != null) {
                result.add(line);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private static String plainTextFallback(List<Component> lines) {
        List<Component> sanitized = sanitizeComponents(lines);
        if (sanitized.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (Component line : sanitized) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(line.getString());
        }
        return builder.toString();
    }

    private static int vanillaMouseX(Tooltip tooltip) {
        return Math.round(tooltip.layoutBounds().x() - VANILLA_TOOLTIP_OFFSET_X);
    }

    private static int vanillaMouseY(Tooltip tooltip) {
        return Math.round(tooltip.layoutBounds().y() - VANILLA_TOOLTIP_OFFSET_Y);
    }
}
