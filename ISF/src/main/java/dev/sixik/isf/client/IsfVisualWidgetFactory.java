package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.definition.IsfExpression;
import dev.sixik.isf.definition.IsfVisualNode;
import dev.sixik.isf.runtime.IsfEvaluationContext;
import dev.sixik.isf.runtime.IsfExpressionEvaluator;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/** Создаёт UniGUI-дерево из декларативного visual ISF-рецепта. */
final class IsfVisualWidgetFactory {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(IsfVisualWidgetFactory.class);
    private static final float CELL = 18.0f;
    static final String SLOT_TEXTURE = "isf:textures/jei/atlas/gui/slot.png";
    static final String ARROW_TEXTURE = "isf:textures/jei/atlas/gui/recipe_arrow.png";

    private final IsfExpressionEvaluator evaluator = new IsfExpressionEvaluator(IsfMod.runtime().functions());
    private final IsfEvaluationContext context;
    private final Button recipePlus = new Button();

    private IsfVisualWidgetFactory(Map<String, JsonElement> parameters) {
        context = new IsfEvaluationContext(parameters);
    }

    static Widget create(IsfVisualNode visual, Map<String, JsonElement> parameters) {
        if (visual == null) return null;
        Widget widget = new IsfVisualWidgetFactory(parameters).createNode(visual);
        if (widget != null) {
            adaptHeightToChildren(widget);
        }
        return widget;
    }

    private Widget createNode(IsfVisualNode node) {
        String widgetId = node.widget().toString().toLowerCase(Locale.ROOT);
        WidgetBase widget = switch (widgetId) {
            case "unigui:box", "box" -> {
                Box box = new Box();
                box.borderVisible(false);
                box.backgroundVisible(false);
                yield box;
            }
            case "unigui:hbox", "hbox" -> new HBox();
            case "unigui:vbox", "vbox" -> new VBox();
            case "unigui:grid", "unigui:gridbox", "grid", "gridbox" -> new GridBox();
            case "unigui:label", "unigui:text", "label", "text" -> new Label();
            case "unigui:item", "unigui:item_preview", "item" -> itemWidget(node);
            case "unigui:texture", "isf:texture", "texture" -> textureWidget(node);
            case "unigui:progress_bar", "unigui:progressbar", "progress_bar" -> new ProgressBar();
            case "isf:ingredient_grid", "ingredient_grid" -> ingredientGrid(node);
            case "isf:loot_grid", "loot_grid" -> lootGrid(node);
            case "isf:loot_scroll", "loot_scroll" -> lootScroll(node);
            case "isf:entity_or_item", "entity_or_item" -> entityOrItem(node);
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
                    if (childWidget != null) {
                        recipePlus.layout(style -> style.size(8,8).alignSelf(Align.END));
                        recipePlus.renderer(new NineSliceButtonRenderer(new MinecraftTextureHandle(ResourceLocation.tryBuild(IsfMod.MOD_ID, "textures/jei/atlas/gui/disabled_plus_button.png"),
                                8, 8, TextureOptions.nearest()),
                                4.0f));
                        panel.addChild(childWidget);
                        panel.addChild(recipePlus);
                    }
                }
            }
        }
        return widget;
    }

    private WidgetBase itemWidget(IsfVisualNode node) {
        JsonElement raw = value(node, "item");
        ItemStack stack = item(raw);
        // Свойство "count" перекрывает суффикс "#count" в id, но только когда оно > 1:
        // fallback 1 не должен затирать количество, уже заданное в записи предмета.
        int count = integer(value(node, "count"), 1);
        if (!stack.isEmpty() && count > 1) stack.setCount(Math.min(count, stack.getMaxStackSize()));
        if (stack.isEmpty()) {
            IsfItemIconWidget icon = new IsfItemIconWidget(stack);
            icon.enabled(false);
            return icon;
        }
        IsfItemButton button = new IsfItemButton(itemId(raw), stack,
                slotTexture(value(node, "slot")),
                Math.max(CELL, integer(value(node, "width"), (int) CELL)));
        attachClickHandler(button);
        return button;
    }

    /** Свойство "slot": true — обычный слот, строка — конкретная текстура, absent — без слота. */
    private static String slotTexture(JsonElement raw) {
        if (raw == null || raw.isJsonNull()) return null;
        if (raw.isJsonPrimitive()) {
            String value = raw.getAsString();
            if (value.isBlank()) return null;
            return value.equalsIgnoreCase("true") ? SLOT_TEXTURE : value;
        }
        return null;
    }

    /** Виджет текстуры: свойство "texture" — ResourceLocation, width/height — размер. */
    private WidgetBase textureWidget(IsfVisualNode node) {
        String id = string(value(node, "texture"), SLOT_TEXTURE);
        int width = Math.max(1, integer(value(node, "width"), 16));
        int height = Math.max(1, integer(value(node, "height"), 16));
        dev.sixik.unigui.widgets.display.TextureWidget texture =
                new dev.sixik.unigui.widgets.display.TextureWidget(
                        new dev.sixik.unigui.api.render.SimpleTextureHandle(id, width, height));
        texture.layout(style -> style.size(width, height).flexNone());
        return texture;
    }

    private static WidgetBase slotBackground(float size, String textureId) {
        Box background = new Box();
        background.layout(style -> style.size(size, size).flexNone());
        dev.sixik.unigui.widgets.display.TextureWidget texture =
                new dev.sixik.unigui.widgets.display.TextureWidget(
                        new dev.sixik.unigui.api.render.SimpleTextureHandle(textureId, 18, 18));
        // Текстура не участвует в hit-test — она декорация.
        texture.enabled(false);
        texture.layout(style -> style.sizePercent(100.0f, 100.0f).flexNone());
        background.addChild(texture);
        return background;
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

    /**
     * Тело окна добычи: сетка дропа (5 колонок, шанс под моделью) в вертикальном
     * скролле. Высота скролла подстраивается под контент: показывается максимум
     * 3 строки, при меньшем числе дропов высота меньше — «пустой» прокрутки нет.
     * Полоса невидима, пока контент влезает, и тонкая при переполнении.
     */
    private WidgetBase lootScroll(IsfVisualNode node) {
        GridBox grid = lootGrid(node);
        // Grid уже имеет точную высоту rows * CELL_ROW, поэтому ScrollView не
        // растягивает строки и не создаёт пустые промежуточные линии.
        grid.applyQueuedMutations();
        grid.measure(new dev.sixik.unigui.api.layout.LayoutContext(140.0f, 4096.0f));
        float contentHeight = Math.max(28.0f, grid.desiredSize().height());
        float scrollHeight = Math.min(84.0f, contentHeight);
        dev.sixik.unigui.widgets.containers.ScrollView scroll =
                new dev.sixik.unigui.widgets.containers.ScrollView(grid);
        scroll.scrollStep(CELL + 10.0f);
        scroll.scrollbarGap(1);
        scroll.scrollbarSize(4.0f);
        scroll.scrollingEnabled(contentHeight > scrollHeight + 0.01f);
        scroll.layout(style -> style
                .widthPercent(100.0f)
                .height(scrollHeight)
                .flexNone()
                .overflowX(dev.sixik.unigui.api.layout.Overflow.HIDDEN)
                .overflowY(dev.sixik.unigui.api.layout.Overflow.AUTO));
        return scroll;
    }

    /**
     * Сетка дропа loot table: ячейка {@code {ids: [...], chance, count_min, count_max}}.
     * В каждой ячейке — модель первого предмета (с числом max-количества) и подпись
     * шанса под ней. Условия (Silk Touch, Fortune, убийство игроком...) уходят в
     * тултип; тег-варианты показываются сеткой «Принимает:».
     */
    private GridBox lootGrid(IsfVisualNode node) {
        GridBox grid = new GridBox();
        // Ячейка шире слота (22px): «12.5%» не налезает на соседнюю колонку.
        float cellColumn = 22.0f;
        grid.columns(5).spacing(0);
        JsonElement raw = value(node, "drops");
        if (raw == null || !raw.isJsonArray()) return grid;
        for (JsonElement element : raw.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            com.google.gson.JsonObject cell = element.getAsJsonObject();
            List<ResourceLocation> ids = new ArrayList<>();
            List<ItemStack> stacks = new ArrayList<>();
            double chance = number(cell.get("chance"), 1.0f);
            int countMax = Math.max(1, (int) Math.ceil(number(cell.get("count_max"), 1.0f)));
            List<net.minecraft.network.chat.Component> conditions = conditionLines(cell);
            if (cell.has("ids") && cell.get("ids").isJsonArray()) {
                for (JsonElement id : cell.getAsJsonArray("ids")) {
                    ResourceLocation itemId = itemId(id);
                    if (itemId == null) continue;
                    ItemStack stack = item(id);
                    if (stack.isEmpty()) continue;
                    ids.add(itemId);
                    stacks.add(stack);
                }
            }
            float rowHeight = CELL + 10.0f;
            if (stacks.isEmpty()) {
                // Пустая ячейка сохраняет позицию в сетке: без placeholder'а индексы
                // смещаются, и ряды «пропадают» (village temple и т.п.).
                Box empty = new Box();
                empty.layout(style -> style.size(cellColumn, rowHeight).flexNone());
                grid.addChild(empty);
                continue;
            }
            IsfItemButton button = new IsfItemButton(ids, stacks, SLOT_TEXTURE, CELL);
            // Количество показываем по максимуму диапазона (min-max нельзя отобразить одним числом).
            ItemStack main = stacks.get(0).copy();
            main.setCount(Math.min(countMax, main.getMaxStackSize()));
            button.icon().stack(main);
            button.extraTooltipLines(conditions);
            button.layout(style -> style.size(CELL, CELL).centerSelf().flexNone());
            attachClickHandler(button);

            VBox slot = new VBox();
            slot.spacing(0);
            slot.layout(style -> style.size(cellColumn, rowHeight).flexNone());
            slot.addChild(button);
            // 100% не подписываем: вероятность по умолчанию очевидна без текста.
            if (chance < 0.999) {
                Label chanceLabel = new Label(formatChance(chance));
                chanceLabel.layout(style -> style.width(cellColumn).height(10.0f).flexNone());
                chanceLabel.textAlignment(Alignment.CENTER);
                // Такой же тёмный цвет, как у ID таблицы добычи.
                chanceLabel.color(MutableColor.fromHex("#5A5A5AFF"));
                slot.addChild(chanceLabel);
            }
            grid.addChild(slot);
        }
        // Grid лежит внутри ScrollView: применяем queued addChild сразу.
        grid.applyQueuedMutations();
        int rows = Math.max(1, (grid.children().size() + 4) / 5);
        // Фиксируем геометрию content: иначе ScrollView передаёт GridBox viewport
        // целиком, и GridBox распределяет строки с пустыми промежутками.
        grid.layout(style -> style.width(5.0f * cellColumn)
                .height(rows * (CELL + 10.0f)).flexNone());
        return grid;
    }

    /**
     * Строки тултипа с условием выпадения: данные пишутся импортёром в ячейку
     * {@code conditions: ["silk_touch", "killed_by_player", ...]} — человекочитаемо
     * раскрываем каждое (например, красная руда без Silk Touch даёт пыль, а не руду).
     */
    private static List<net.minecraft.network.chat.Component> conditionLines(
            com.google.gson.JsonObject cell) {
        if (!cell.has("conditions") || !cell.get("conditions").isJsonArray()) return List.of();
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        for (JsonElement element : cell.getAsJsonArray("conditions")) {
            if (!element.isJsonPrimitive()) continue;
            String id = element.getAsString();
            int colon = id.indexOf(':');
            String path = colon >= 0 ? id.substring(colon + 1) : id;
            String key = "isf.loot_condition." + path;
            lines.add(net.minecraft.network.chat.Component.translatableWithFallback(
                    key, prettifyWords(path)));
        }
        return List.copyOf(lines);
    }

    /** «killed_by_player_or_tamed» → «Killed By Player Or Tamed». */
    private static String prettifyWords(String value) {
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder text = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!text.isEmpty()) text.append(' ');
            text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return text.toString();
    }

    /** @return шанс как «25%» или «12.5%». */
    private static String formatChance(double chance) {
        double percent = Math.max(0.0, Math.min(1.0, chance)) * 100.0;
        if (percent == Math.floor(percent)) return (int) percent + "%";
        return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
    }

    /**
     * Источник окна добычи: если задан параметр {@code entity} (id живой сущности) —
     * рисует запечённую иконку модели моба (texture-путь уважает transform/clip
     * Z-слоёв, в отличие от прямого InventoryScreen-рендера), иначе предмет.
     */
    private WidgetBase entityOrItem(IsfVisualNode node) {
        // Размер иконки моба задаётся отдельным свойством: ячейка предмета (width)
        // остаётся 18, а моделька рисуется крупнее.
        float size = Math.max(CELL, integer(value(node, "entity_size"),
                integer(value(node, "width"), (int) CELL)));
        JsonElement entityRaw = value(node, "entity");
        if (entityRaw != null && entityRaw.isJsonPrimitive() && !entityRaw.getAsString().isBlank()) {
            ResourceLocation entityId = ResourceLocation.tryParse(entityRaw.getAsString());
            net.minecraft.world.entity.EntityType<?> type = entityId == null ? null
                    : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(entityId);
            // «Живость» типа проверяет сама запечка (instanceof на созданной сущности):
            // EntityType.getBaseClass() для этого не подходит — в 1.20.1 он всегда
            // возвращает Entity.class.
            if (type != null) {
                IsfEntityIconWidget icon = new IsfEntityIconWidget(type, size);
                icon.enabled(false);
                icon.layout(style -> style.size(size, size).centerSelf().flexNone());
                return icon;
            }
            LOGGER.debug("entityOrItem: unknown entity '{}', fallback to item", entityRaw.getAsString());
        }
        return itemWidget(node);
    }

    /** @return клетки крафта: pattern даёт строки, плоский список — одну строку. */
    private static List<List<JsonArray>> readCells(JsonElement raw) {        List<List<JsonArray>> rows = new ArrayList<>();
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

    /** Клетка крафта: одна альтернатива или все варианты тега/ingredient'а сразу. */
    private WidgetBase cellWidget(JsonArray alternatives) {
        List<ResourceLocation> ids = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (JsonElement alternative : alternatives) {
            ResourceLocation id = itemId(alternative);
            if (id == null) continue;
            ItemStack stack = item(alternative);
            if (stack.isEmpty()) continue;
            ids.add(id);
            stacks.add(stack);
        }
        if (stacks.isEmpty()) return slotBackground(CELL, SLOT_TEXTURE);
        IsfItemButton button = new IsfItemButton(ids, stacks, SLOT_TEXTURE, CELL);
        button.layout(style -> style.size(CELL, CELL).flexNone());
        attachClickHandler(button);
        return button;
    }

    private static void attachClickHandler(IsfItemButton ignoredButton) {
        // Клик обрабатывается вручную в IsfBrowserOverlay.clickDetailControls:
        // IsfItemButton намеренно не потребляет pointer-события.
    }

    /** @return id из записи {@code "minecraft:iron_ingot#9"} (суффикс {@code #count} отбрасывается). */
    private static ResourceLocation itemId(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return null;
        String raw = value.getAsString();
        int hash = raw.lastIndexOf('#');
        String id = hash < 0 ? raw : raw.substring(0, hash);
        return ResourceLocation.tryParse(id);
    }

    /** @return количество из суффикса {@code #count} записи или {@code 1}. */
    private static int entryCount(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return 1;
        String raw = value.getAsString();
        int hash = raw.lastIndexOf('#');
        if (hash < 0) return 1;
        try {
            return Math.max(1, Integer.parseInt(raw.substring(hash + 1).trim()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private boolean isIngredientGrid(IsfVisualNode node) {
        return node.widget().toString().equalsIgnoreCase("isf:ingredient_grid");
    }

    private void applyProperties(WidgetBase widget, IsfVisualNode node) {
        Map<String, JsonElement> properties = evaluatedProperties(node.properties());
        float width = number(properties.get("width"), Float.NaN);
        float height = number(properties.get("height"), Float.NaN);
        // Иконка моба крупнее ячейки предмета: entity_size перекрывает width/height.
        float iconWidth = width;
        float iconHeight = height;
        if (widget instanceof IsfEntityIconWidget && properties.containsKey("entity_size")) {
            float entitySize = number(properties.get("entity_size"), Float.NaN);
            if (Float.isFinite(entitySize) && entitySize > 0.0f) {
                iconWidth = entitySize;
                iconHeight = entitySize;
            }
        }
        final float layoutWidth = iconWidth;
        final float layoutHeight = iconHeight;
        widget.layout(style -> {
            style.position(PositionType.RELATIVE);
            if (Float.isFinite(layoutWidth)) style.width(layoutWidth);
            if (Float.isFinite(layoutHeight)) style.height(layoutHeight);
            if (properties.containsKey("padding")) style.padding(edgeInsets(properties.get("padding")));
            if (properties.containsKey("alignItems")) style.alignItems(align(properties.get("alignItems")));
            if (properties.containsKey("justifyContent")) style.justifyContent(justify(properties.get("justifyContent")));
            if (properties.containsKey("alignSelf")) style.alignSelf(align(properties.get("alignSelf")));
        });
        if (widget instanceof Label label) {
            if (properties.containsKey("text")) label.text(string(properties.get("text"), ""));
            if (properties.containsKey("color")) label.color(color(properties.get("color")));
        }
        if (widget instanceof Box box) {
            if (properties.containsKey("background")) {
                box.backgroundVisible(false);
            }
            if (properties.containsKey("border")) {
                box.borderVisible(false);
            }
            if (properties.containsKey("radius")) {
                box.radius(number(properties.get("radius"), 0.0f));
            }
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
        ResourceLocation id = itemId(value);
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        int count = entryCount(value);
        if (count > 1) stack.setCount(Math.min(count, stack.getMaxStackSize()));
        return stack;
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

    public static float adaptHeightToChildren(Widget widget) {
        if (widget == null) return 0.0f;
        flushWidgetTree(widget);
        return computeAndApplyAdaptedHeight(widget);
    }

    public static void flushWidgetTree(Widget widget) {
        if (widget instanceof PanelWidget panel) {
            panel.applyQueuedMutations();
            for (Widget child : panel.children()) {
                flushWidgetTree(child);
            }
        }
    }

    private static float computeAndApplyAdaptedHeight(Widget widget) {
        if (widget == null || widget.visibility() == Visibility.COLLAPSED) return 0.0f;

        if (!(widget instanceof PanelWidget panel)) {
            float h = widget.desiredSize().height();
            if (h <= 0.0f && widget instanceof WidgetBase base && !base.layoutStyle().height().isAuto()) {
                h = base.layoutStyle().height().value();
            }
            return Math.max(0.0f, h);
        }

        List<Widget> children = panel.children();
        if (children.isEmpty()) {
            float h = panel.desiredSize().height();
            if (h <= 0.0f && !panel.layoutStyle().height().isAuto()) {
                h = panel.layoutStyle().height().value();
            }
            return Math.max(0.0f, h);
        }

        for (Widget child : children) {
            if (child.visibility() != Visibility.COLLAPSED) {
                computeAndApplyAdaptedHeight(child);
            }
        }

        float containerWidth = panel.layoutStyle().width().value();
        if (containerWidth <= 0.0f) containerWidth = 176.0f;
        float padH = panel.layoutStyle().padding().horizontal();
        float padV = panel.layoutStyle().padding().vertical();
        LayoutContext childContext = new LayoutContext(Math.max(0.0f, containerWidth - padH), 4096.0f);

        float contentHeight;
        if (panel instanceof LinearBox linear) {
            boolean isVertical = linear.orientation() == Orientation.VERTICAL;
            float sum = 0.0f;
            float max = 0.0f;
            int visibleCount = 0;
            for (Widget child : children) {
                if (child.visibility() == Visibility.COLLAPSED) continue;
                try {
                    child.measure(childContext);
                } catch (RuntimeException ignored) {
                }
                float h = child.desiredSize().height() + child.layoutConstraints().margin().vertical();
                sum += h;
                max = Math.max(max, h);
                visibleCount++;
            }
            if (isVertical) {
                float gaps = Math.max(0, visibleCount - 1) * linear.spacing();
                contentHeight = sum + gaps;
            } else {
                contentHeight = max;
            }
        } else if (panel instanceof GridBox grid) {
            int cols = Math.max(1, grid.columns());
            int visibleIndex = 0;
            Map<Integer, Float> rowHeights = new HashMap<>();
            for (Widget child : children) {
                if (child.visibility() == Visibility.COLLAPSED) continue;
                try {
                    child.measure(childContext);
                } catch (RuntimeException ignored) {
                }
                float h = child.desiredSize().height() + child.layoutConstraints().margin().vertical();
                int row = visibleIndex / cols;
                rowHeights.put(row, Math.max(rowHeights.getOrDefault(row, 0.0f), h));
                visibleIndex++;
            }
            int totalRows = (visibleIndex + cols - 1) / cols;
            float sum = 0.0f;
            for (float rh : rowHeights.values()) {
                sum += rh;
            }
            float gaps = Math.max(0, totalRows - 1) * grid.verticalSpacing();
            contentHeight = sum + gaps;
        } else {
            float maxBottom = 0.0f;
            for (Widget child : children) {
                if (child.visibility() == Visibility.COLLAPSED) continue;
                try {
                    child.measure(childContext);
                } catch (RuntimeException ignored) {
                }
                float h = child.desiredSize().height() + child.layoutConstraints().margin().vertical();
                float top = 0.0f;
                if (child instanceof WidgetBase base && !base.layoutStyle().top().isAuto()) {
                    top = base.layoutStyle().top().value();
                }
                maxBottom = Math.max(maxBottom, top + h);
            }
            contentHeight = maxBottom;
        }

        float adapted = contentHeight + padV;
        if (!panel.layoutStyle().minHeight().isAuto()) {
            adapted = Math.max(panel.layoutStyle().minHeight().value(), adapted);
        }
        if (!panel.layoutStyle().maxHeight().isAuto()) {
            adapted = Math.min(panel.layoutStyle().maxHeight().value(), adapted);
        }
        final float targetHeight = adapted;
        if (targetHeight > 0.0f) {
            panel.layout(style -> style.height(targetHeight));
            try {
                panel.measure(new LayoutContext(containerWidth, 4096.0f));
            } catch (RuntimeException ignored) {
            }
        }
        return targetHeight;
    }
}
