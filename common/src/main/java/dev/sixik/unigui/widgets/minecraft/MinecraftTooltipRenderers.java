package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftGuiRenderBackend;
import dev.sixik.unigui.widgets.Tooltip;
import dev.sixik.unigui.widgets.render.TooltipRenderer;
import dev.sixik.unigui.widgets.render.TooltipState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
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
    public static TooltipRenderer vanilla() {
        return (draw, state) -> renderComponents(draw, state, stateComponents(state));
    }

    public static TooltipRenderer vanilla(Component line) {
        return vanilla(line == null ? List.of() : List.of(line));
    }

    public static TooltipRenderer vanilla(Component... lines) {
        return vanilla(lines == null ? List.of() : Arrays.asList(lines));
    }

    public static TooltipRenderer vanilla(List<Component> lines) {
        List<Component> fixedLines = sanitizeComponents(lines);
        return (draw, state) -> renderComponents(draw, state, fixedLines);
    }

    public static TooltipRenderer vanilla(Supplier<List<Component>> linesSupplier) {
        Objects.requireNonNull(linesSupplier, "linesSupplier");
        return (draw, state) -> renderComponents(draw, state, sanitizeComponents(linesSupplier.get()));
    }

    /**
     * Renders an ItemStack tooltip through vanilla, preserving modded tooltip components/images where Minecraft exposes them.
     */
    public static TooltipRenderer item(ItemStack stack) {
        ItemStack fixedStack = stack == null ? ItemStack.EMPTY : stack.copy();
        return item(() -> fixedStack);
    }

    /**
     * Renders a dynamic ItemStack tooltip through vanilla.
     */
    public static TooltipRenderer item(Supplier<ItemStack> stackSupplier) {
        Objects.requireNonNull(stackSupplier, "stackSupplier");
        return (draw, state) -> {
            ItemStack stack = stackSupplier.get();
            if (stack == null || stack.isEmpty()) return;
            ItemStack tooltipStack = stack.copy();
            int mouseX = vanillaMouseX(state);
            int mouseY = vanillaMouseY(state);
            draw.addCallback(backend -> {
                if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                    minecraftBackend.renderVanillaTooltip(tooltipStack, mouseX, mouseY);
                }
            });
        };
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
    public static Tooltip useVanilla(Tooltip tooltip, TooltipRenderer renderer) {
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

    private static void renderComponents(DrawScope draw, TooltipState state, List<Component> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> capturedLines = List.copyOf(lines);
        int mouseX = vanillaMouseX(state);
        int mouseY = vanillaMouseY(state);
        draw.addCallback(backend -> {
            if (backend instanceof MinecraftGuiRenderBackend minecraftBackend) {
                minecraftBackend.renderVanillaTooltip(capturedLines, mouseX, mouseY);
            }
        });
    }

    private static List<Component> stateComponents(TooltipState state) {
        if (state == null || state.lines().isEmpty()) return List.of();
        List<Component> components = new ArrayList<>(state.lines().size());
        for (RichText line : state.lines()) {
            String text = line == null ? "" : line.plainText();
            if (!text.isEmpty()) {
                components.add(Component.literal(text));
            }
        }
        return components;
    }

    private static List<Component> sanitizeComponents(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<Component> result = new ArrayList<>(lines.size());
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

    private static int vanillaMouseX(TooltipState state) {
        return Math.round(state.x() - VANILLA_TOOLTIP_OFFSET_X);
    }

    private static int vanillaMouseY(TooltipState state) {
        return Math.round(state.y() - VANILLA_TOOLTIP_OFFSET_Y);
    }
}
