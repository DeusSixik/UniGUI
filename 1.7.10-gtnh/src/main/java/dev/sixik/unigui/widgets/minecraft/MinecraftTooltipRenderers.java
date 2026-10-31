package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.gtnh.backend.GtnhGuiRenderBackend;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import dev.sixik.unigui.gtnh.backend.GtnhItemStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tooltip renderers that hand the drawing to the vanilla tooltip code (background, border, fonts, mod hooks).
 *
 * <p>They live next to the Minecraft widgets so core tooltips stay backend-neutral. Use {@link #useVanilla(Tooltip)}
 * or the factory methods when you want the vanilla box instead of the UniGUI tooltip chrome. Lines are plain strings
 * with {@code §} formatting codes, which is what the 1.7.10 tooltip code takes; {@link ITextComponent}s are
 * converted with {@code getFormattedText()}.</p>
 *
 * <p>There is no counterpart of the 1.20 {@code TooltipComponent} grid (it does not exist in 1.7.10).</p>
 */
public final class MinecraftTooltipRenderers {
    private static final float VANILLA_TOOLTIP_OFFSET_X = 12.0f;
    private static final float VANILLA_TOOLTIP_OFFSET_Y = -12.0f;

    private MinecraftTooltipRenderers() {
    }

    /**
     * Draws the current UniGUI tooltip text with the vanilla tooltip renderer.
     */
    public static WidgetRender vanilla() {
        return WidgetRender.of(Tooltip.class, (draw, tooltip) ->
                renderLines(draw, tooltip, stateLines(draw, tooltip)));
    }

    public static WidgetRender vanilla(String line) {
        return vanilla(line == null ? List.of() : List.of(line));
    }

    public static WidgetRender vanilla(String... lines) {
        return vanilla(lines == null ? List.of() : Arrays.asList(lines));
    }

    public static WidgetRender vanilla(List<String> lines) {
        List<String> fixedLines = sanitize(lines);
        return WidgetRender.of(Tooltip.class, (draw, tooltip) -> renderLines(draw, tooltip, fixedLines));
    }

    public static WidgetRender vanilla(Supplier<List<String>> linesSupplier) {
        Objects.requireNonNull(linesSupplier, "linesSupplier");
        return WidgetRender.of(Tooltip.class, (draw, tooltip) ->
                renderLines(draw, tooltip, sanitize(linesSupplier.get())));
    }

    /**
     * Draws vanilla text components; formatting (colors, bold) is kept.
     */
    public static WidgetRender vanillaComponents(List<? extends IChatComponent> lines) {
        return vanilla(formatted(lines));
    }

    /**
     * Draws the tooltip of an item stack through vanilla, including what other mods add to it.
     */
    public static WidgetRender item(ItemStack stack) {
        ItemStack fixedStack = GtnhItemStacks.copyOrEmpty(stack);
        return item(() -> fixedStack);
    }

    /**
     * Draws the tooltip of a dynamic item stack through vanilla.
     */
    public static WidgetRender item(Supplier<ItemStack> stackSupplier) {
        Objects.requireNonNull(stackSupplier, "stackSupplier");
        return WidgetRender.of(Tooltip.class, (draw, tooltip) -> {
            ItemStack stack = stackSupplier.get();
            if (GtnhItemStacks.isEmpty(stack)) return;
            ItemStack tooltipStack = stack.copy();
            int mouseX = vanillaMouseX(tooltip);
            int mouseY = vanillaMouseY(tooltip);
            draw.addCallback(backend -> {
                if (backend instanceof GtnhGuiRenderBackend minecraftBackend) {
                    minecraftBackend.renderVanillaTooltip(tooltipStack, mouseX, mouseY);
                }
            });
        });
    }

    /**
     * Switches an existing tooltip to vanilla rendering and turns off the UniGUI chrome.
     */
    public static Tooltip useVanilla(Tooltip tooltip) {
        return useVanilla(tooltip, vanilla());
    }

    /**
     * Applies a vanilla renderer to an existing tooltip and turns off the UniGUI chrome.
     */
    public static Tooltip useVanilla(Tooltip tooltip, WidgetRender renderer) {
        Objects.requireNonNull(tooltip, "tooltip");
        tooltip.backgroundVisible(false);
        tooltip.borderVisible(false);
        tooltip.themeEnabled(false);
        tooltip.renderer(renderer == null ? vanilla() : renderer);
        return tooltip;
    }

    public static Tooltip tooltip(Widget anchor, String... lines) {
        return tooltip(anchor, lines == null ? List.of() : Arrays.asList(lines));
    }

    public static Tooltip tooltip(Widget anchor, List<String> lines) {
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

    private static void renderLines(DrawScope draw, Tooltip tooltip, List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<String> capturedLines = List.copyOf(lines);
        int mouseX = vanillaMouseX(tooltip);
        int mouseY = vanillaMouseY(tooltip);
        draw.addCallback(backend -> {
            if (backend instanceof GtnhGuiRenderBackend minecraftBackend) {
                minecraftBackend.renderVanillaTooltip(capturedLines, mouseX, mouseY);
            }
        });
    }

    private static List<String> stateLines(DrawScope draw, Tooltip tooltip) {
        if (tooltip == null) return List.of();
        List<RichText> lines = tooltip.wrappedLines(draw.context());
        if (lines.isEmpty()) return List.of();
        List<String> result = new ObjectArrayList<>(lines.size());
        for (RichText line : lines) {
            String text = line == null ? "" : line.plainText();
            if (!text.isEmpty()) result.add(text);
        }
        return result;
    }

    private static List<String> formatted(List<? extends IChatComponent> components) {
        if (components == null || components.isEmpty()) return List.of();
        List<String> result = new ObjectArrayList<>(components.size());
        for (IChatComponent component : components) {
            if (component != null) result.add(component.getFormattedText());
        }
        return result;
    }

    private static List<String> sanitize(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<String> result = new ObjectArrayList<>(lines.size());
        for (String line : lines) {
            if (line != null) result.add(line);
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    /**
     * @return the lines joined by newlines with {@code §} codes removed: a plain layout anchor for the tooltip
     */
    static String plainTextFallback(List<String> lines) {
        List<String> sanitized = sanitize(lines);
        if (sanitized.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : sanitized) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(stripFormatting(line));
        }
        return builder.toString();
    }

    /**
     * @return {@code text} without {@code §x} formatting codes
     */
    static String stripFormatting(String text) {
        if (text == null || text.indexOf('\u00a7') < 0) return text == null ? "" : text;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) i++;
            else out.append(text.charAt(i));
        }
        return out.toString();
    }

    private static int vanillaMouseX(Tooltip tooltip) {
        return Math.round(tooltip.layoutBounds().x() - VANILLA_TOOLTIP_OFFSET_X);
    }

    private static int vanillaMouseY(Tooltip tooltip) {
        return Math.round(tooltip.layoutBounds().y() - VANILLA_TOOLTIP_OFFSET_Y);
    }
}