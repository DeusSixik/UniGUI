package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.definition.IsfExpression;
import dev.sixik.isf.definition.IsfVisualNode;
import dev.sixik.isf.runtime.IsfEvaluationContext;
import dev.sixik.isf.runtime.IsfExpressionEvaluator;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/** Создаёт UniGUI-дерево из декларативного visual ISF-рецепта. */
final class IsfVisualWidgetFactory {
    private static final float CELL = 18.0f;

    private final IsfExpressionEvaluator evaluator = new IsfExpressionEvaluator(IsfMod.runtime().functions());
    private final IsfEvaluationContext context;

    private IsfVisualWidgetFactory(Map<String, JsonElement> parameters) {
        context = new IsfEvaluationContext(parameters);
    }

    static Widget create(IsfVisualNode visual, Map<String, JsonElement> parameters) {
        if (visual == null) return null;
        return new IsfVisualWidgetFactory(parameters).createNode(visual);
    }

    private Widget createNode(IsfVisualNode node) {
        String widgetId = node.widget().toString().toLowerCase(Locale.ROOT);
        WidgetBase widget = switch (widgetId) {
            case "unigui:box", "box" -> new Box();
            case "unigui:hbox", "hbox" -> new HBox();
            case "unigui:vbox", "vbox" -> new VBox();
            case "unigui:grid", "unigui:gridbox", "grid", "gridbox" -> new GridBox();
            case "unigui:label", "unigui:text", "label", "text" -> new Label();
            case "unigui:item", "unigui:item_preview", "item" -> itemWidget(node);
            case "unigui:progress_bar", "unigui:progressbar", "progress_bar" -> new ProgressBar();
            case "isf:ingredient_grid", "ingredient_grid" -> ingredientGrid(node);
            default -> {
                Label unsupported = new Label("Unknown visual widget: " + node.widget());
                unsupported.color(MutableColor.fromHex("#FF6B6BFF"));
                yield unsupported;
            }
        };

        applyProperties(widget, node);
        if (!(widget instanceof IsfItemIconWidget) && !isIngredientGrid(node)) {
            if (widget instanceof PanelWidget panel) {
                for (IsfVisualNode child : node.children()) {
                    Widget childWidget = createNode(child);
                    if (childWidget != null) panel.addChild(childWidget);
                }
            }
        }
        return widget;
    }

    private WidgetBase itemWidget(IsfVisualNode node) {
        JsonElement raw = value(node, "item");
        ItemStack stack = item(raw);
        int count = integer(value(node, "count"), 1);
        if (!stack.isEmpty() && count > 0) stack.setCount(Math.min(count, stack.getMaxStackSize()));
        if (stack.isEmpty()) {
            IsfItemIconWidget icon = new IsfItemIconWidget(stack);
            icon.enabled(false);
            return icon;
        }
        IsfItemButton button = new IsfItemButton(itemId(raw), stack);
        attachClickHandler(button);
        return button;
    }

    /**
     * Сетка крафта. Поддерживает два формата "items":
     * pattern (строки → клетки → альтернативы) — рендерит реальную форму крафта,
     * и плоский список клеток (альтернативы → id) — колонки из свойства "columns".
     */
    private WidgetBase ingredientGrid(IsfVisualNode node) {
        GridBox grid = new GridBox();
        JsonElement raw = value(node, "items");
        int columns = Math.max(1, integer(value(node, "columns"), 3));
        List<List<JsonArray>> rows = readCells(raw);
        if (!rows.isEmpty()) {
            columns = rows.stream().mapToInt(List::size).max().orElse(columns);
        }
        grid.columns(columns).spacing(0);
        if (rows.isEmpty()) return grid;
        for (List<JsonArray> row : rows) {
            for (int column = 0; column < columns; column++) {
                JsonArray cell = column < row.size() ? row.get(column) : new JsonArray();
                grid.addChild(cellWidget(cell));
            }
        }
        return grid;
    }

    /** @return клетки крафта: pattern даёт строки, плоский список — одну строку. */
    private static List<List<JsonArray>> readCells(JsonElement raw) {
        List<List<JsonArray>> rows = new ArrayList<>();
        if (raw == null || !raw.isJsonArray()) return rows;
        List<JsonArray> flat = new ArrayList<>();
        boolean flatMode = true;
        for (JsonElement element : raw.getAsJsonArray()) {
            if (!element.isJsonArray()) continue;
            JsonArray outer = element.getAsJsonArray();
            if (!outer.isEmpty() && outer.get(0).isJsonArray()) {
                flatMode = false;
                List<JsonArray> row = new ArrayList<>();
                for (JsonElement cell : outer) {
                    row.add(cell.isJsonArray() ? cell.getAsJsonArray() : new JsonArray());
                }
                rows.add(row);
            } else {
                flat.add(outer);
            }
        }
        if (flatMode && !flat.isEmpty()) rows.add(flat);
        return rows;
    }

    private WidgetBase cellWidget(JsonArray alternatives) {
        JsonElement first = firstAlternative(alternatives);
        ItemStack stack = item(first);
        if (stack.isEmpty()) {
            Box empty = new Box();
            empty.layout(style -> style.size(CELL, CELL).flexNone());
            return empty;
        }
        IsfItemButton button = new IsfItemButton(itemId(first), stack);
        button.layout(style -> style.size(CELL, CELL).flexNone());
        attachClickHandler(button);
        return button;
    }

    private static void attachClickHandler(IsfItemButton ignoredButton) {
        // Клик обрабатывается вручную в IsfBrowserOverlay.clickDetailControls:
        // IsfItemButton намеренно не потребляет pointer-события.
    }

    private static ResourceLocation itemId(JsonElement value) {
        return value == null || !value.isJsonPrimitive()
                ? null : ResourceLocation.tryParse(value.getAsString());
    }

    private boolean isIngredientGrid(IsfVisualNode node) {
        return node.widget().toString().equalsIgnoreCase("isf:ingredient_grid");
    }

    private void applyProperties(WidgetBase widget, IsfVisualNode node) {
        Map<String, JsonElement> properties = evaluatedProperties(node.properties());
        float width = number(properties.get("width"), Float.NaN);
        float height = number(properties.get("height"), Float.NaN);
        widget.layout(style -> {
            style.position(PositionType.RELATIVE);
            if (Float.isFinite(width)) style.width(width);
            if (Float.isFinite(height)) style.height(height);
            if (properties.containsKey("padding")) style.padding(edgeInsets(properties.get("padding")));
            if (properties.containsKey("alignItems")) style.alignItems(align(properties.get("alignItems")));
            if (properties.containsKey("justifyContent")) style.justifyContent(justify(properties.get("justifyContent")));
            if (properties.containsKey("alignSelf")) style.alignSelf(align(properties.get("alignSelf")));
        });

        if (widget instanceof Box box) {
            if (properties.containsKey("background")) box.background(color(properties.get("background")));
            if (properties.containsKey("border")) box.border(color(properties.get("border")));
            if (properties.containsKey("radius")) box.radius(number(properties.get("radius"), 0.0f));
        }
        if (widget instanceof Label label) {
            if (properties.containsKey("text")) label.text(string(properties.get("text"), ""));
            if (properties.containsKey("color")) label.color(color(properties.get("color")));
        }
        if (widget instanceof HBox hbox && properties.containsKey("spacing")) {
            hbox.spacing(number(properties.get("spacing"), 0.0f));
        } else if (widget instanceof VBox vbox && properties.containsKey("spacing")) {
            vbox.spacing(number(properties.get("spacing"), 0.0f));
        } else if (widget instanceof GridBox grid && properties.containsKey("columns")) {
            grid.columns(integer(properties.get("columns"), 1));
            if (properties.containsKey("spacing")) grid.spacing(number(properties.get("spacing"), 0.0f));
        }
        if (widget instanceof ProgressBar progress) {
            if (properties.containsKey("min")) progress.min(number(properties.get("min"), 0.0f));
            if (properties.containsKey("max")) progress.max(number(properties.get("max"), 1.0f));
            if (properties.containsKey("value")) progress.value(number(properties.get("value"), 0.0f));
        }
    }

    private Map<String, JsonElement> evaluatedProperties(Map<String, IsfExpression> properties) {
        Map<String, JsonElement> values = new java.util.LinkedHashMap<>();
        properties.forEach((name, expression) -> values.put(name, evaluator.evaluate(expression, context)));
        return values;
    }

    private JsonElement value(IsfVisualNode node, String key) {
        IsfExpression expression = node.properties().get(key);
        return expression == null ? null : evaluator.evaluate(expression, context);
    }

    private static JsonElement firstAlternative(JsonElement value) {
        if (value == null) return null;
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            return array.isEmpty() ? null : array.get(0);
        }
        return value;
    }

    private static ItemStack item(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static int integer(JsonElement value, int fallback) {
        return value == null || !value.isJsonPrimitive() ? fallback : (int) number(value, fallback);
    }

    private static float number(JsonElement value, float fallback) {
        try {
            return value == null || value.isJsonNull() ? fallback : value.getAsFloat();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String string(JsonElement value, String fallback) {
        return value == null || !value.isJsonPrimitive() ? fallback : value.getAsString();
    }

    private static MutableColor color(JsonElement value) {
        try {
            return MutableColor.fromHex(string(value, "#FFFFFFFF"));
        } catch (RuntimeException ignored) {
            return MutableColor.fromHex("#FFFFFFFF");
        }
    }

    private static EdgeInsets edgeInsets(JsonElement value) {
        if (value != null && value.isJsonArray()) {
            JsonArray values = value.getAsJsonArray();
            if (values.size() == 2) return EdgeInsets.css(number(values.get(0), 0), number(values.get(1), 0));
            if (values.size() >= 4) return EdgeInsets.css(number(values.get(0), 0), number(values.get(1), 0),
                    number(values.get(2), 0), number(values.get(3), 0));
        }
        return EdgeInsets.all(number(value, 0));
    }

    private static Align align(JsonElement value) {
        try {
            return Align.valueOf(string(value, "AUTO").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Align.AUTO;
        }
    }

    private static Justify justify(JsonElement value) {
        try {
            return Justify.valueOf(string(value, "START").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Justify.START;
        }
    }
}
