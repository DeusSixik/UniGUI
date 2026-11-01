package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayDismissable;
import dev.sixik.unigui.widgets.feedback.OverlayHostAware;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.NodeTypeRegistry;
import dev.sixik.unigui.widgets.interaction.SearchField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Всплывающая палитра нод: поле поиска и дерево категорий из {@link NodeTypeRegistry}.
 *
 * <p>Палитра открывается в точке экрана ({@link #openAt(float, float)}) поверх {@link OverlayLayer}, фокус сразу
 * получает поле поиска. Без запроса показано дерево групп: стрелки вправо и влево разворачивают и сворачивают
 * группу, Enter или клик по группе делают то же. Набранный запрос заменяет дерево плоским списком найденного.
 * Стрелки вверх и вниз двигают выбор, Enter или клик по ноде создают её, колесо мыши листает список, Escape и клик
 * вне палитры закрывают её. Что делать с выбранным типом, решает {@link #onPick(Consumer)}; граф подключает её сам
 * (ПКМ, пробел, отпускание провода в пустом месте).</p>
 *
 * <p>Оформление нейтральное тёмное: каждая верхняя категория получает свой цвет акцента, выбранная строка
 * подсвечивается светлой полупрозрачной заливкой.</p>
 */
@AiDoc(
        summary = "Floating searchable node picker opened at a point over an OverlayLayer: a collapsible category tree, or a flat ranked list while typing.",
        purpose = "Lets users add nodes to a graph with the mouse and keyboard instead of code.",
        useWhen = {
                "Open it from a custom button or shortcut with `openAt(x, y)` and react in `onPick`.",
                "Restrict the list with `filter(...)`, for example to nodes that accept a given output type.",
                "Group node types by giving them a category path such as `Math/Vector`."
        },
        criticalRules = {
                "It is an overlay: it only works inside an OverlayLayer and closes itself on outside clicks and Escape.",
                "Opening resets the query and focuses the search field, but remembers which groups were expanded.",
                "Picking closes the palette first and then invokes the callback.",
                "Clicking or pressing Enter on a group toggles it and never closes the palette."
        },
        related = {"NodePaletteModel", "NodeTypeRegistry", "NodeGraph", "OverlayLayer"})
@WikiPage(title = "NodePalette", slug = "node-palette", category = "<key:graph.widget.category>", summary = "<key:node.palette.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.palette.overview>", order = 10)
public class NodePalette extends Box implements OverlayHostAware, OverlayDismissable {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CONTEXT_MENU;

    private static final float WIDTH = 288.0f;
    private static final float ROW_HEIGHT = 22.0f;
    private static final float SEARCH_HEIGHT = 24.0f;
    private static final float INDENT = 14.0f;
    private static final float ROW_LEFT = 16.0f;
    private static final int MAX_VISIBLE = 12;
    private static final String EMPTY_TEXT = "No matching nodes";

    private final NodePaletteModel model;
    private final VBox column = new VBox();
    private final VBox rows = new VBox();
    private final SearchField search = new SearchField();
    private final List<PaletteRow> rowWidgets = new ArrayList<>();
    private Consumer<NodeType> onPick = type -> {
    };
    private final EdgeInsets padding = EdgeInsets.all(5.0f);
    private boolean open;
    private boolean focusPending;
    private float x;
    private float y;

    /**
     * @param registry каталог типов нод, по которому ищет палитра
     */
    public NodePalette(NodeTypeRegistry registry) {
        applyComponentStyleDefaults(WidgetStyles.ContextMenu.DEFAULT);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        background(new MutableColor(0.105f, 0.11f, 0.125f, 0.98f));
        border(new MutableColor(0.30f, 0.31f, 0.36f, 1.0f));
        radius(6.0f);
        visible(false);
        focusable(true);
        model = new NodePaletteModel(registry, MAX_VISIBLE);

        column.spacing(4.0f);
        rows.spacing(1.0f);
        search.placeholder("Search nodes...");
        search.searchChangeDebounceSeconds(0.0f);
        search.layout(style -> style.size(WIDTH, SEARCH_HEIGHT));
        search.onTextChanged(event -> {
            model.query(event.newText());
            refresh();
        });
        for (int i = 0; i < MAX_VISIBLE; i++) {
            int index = i;
            PaletteRow row = new PaletteRow(() -> activateRow(index), () -> hoverRow(index));
            rowWidgets.add(row);
            rows.addChild(row);
        }
        rows.applyQueuedMutations();
        column.addChild(search);
        column.addChild(rows);
        column.applyQueuedMutations();
        addChild(column);
        applyQueuedMutations();
        refresh();
    }

    @Override
    protected Style defaultStyle() {
        return WidgetStyles.ContextMenu.DEFAULT;
    }

    /**
     * @param filter условие показа типа; {@code null} показывает все
     * @return эта палитра
     */
    public NodePalette filter(Predicate<NodeType> filter) {
        model.filter(filter);
        refresh();
        return this;
    }

    /**
     * @param expandAll {@code true}, чтобы показывать все группы развёрнутыми (полезно для коротких отфильтрованных
     *                  списков)
     * @return эта палитра
     */
    public NodePalette expandAll(boolean expandAll) {
        model.expandAll(expandAll);
        refresh();
        return this;
    }

    /**
     * @param onPick что сделать с выбранным типом; вызывается после закрытия палитры
     * @return эта палитра
     */
    public NodePalette onPick(Consumer<NodeType> onPick) {
        this.onPick = onPick == null ? type -> {
        } : onPick;
        return this;
    }

    /**
     * @return поле поиска
     */
    public SearchField searchField() {
        return search;
    }

    /**
     * @return модель палитры: строки, выбор, окно
     */
    public NodePaletteModel model() {
        return model;
    }

    /**
     * @param index номер строки в видимом окне, от нуля
     * @return строка или {@code null}
     */
    public PaletteRow row(int index) {
        return index >= 0 && index < rowWidgets.size() ? rowWidgets.get(index) : null;
    }

    /**
     * @return {@code true}, если палитра открыта
     */
    public boolean opened() {
        return open;
    }

    /**
     * Открывает палитру в точке: сбрасывает запрос и просит фокус для поля поиска. Развёрнутые группы запоминаются
     * между открытиями.
     *
     * @param rootX X точки в root-координатах
     * @param rootY Y точки в root-координатах
     * @return эта палитра
     */
    public NodePalette openAt(float rootX, float rootY) {
        this.x = Float.isFinite(rootX) ? rootX : 0.0f;
        this.y = Float.isFinite(rootY) ? rootY : 0.0f;
        search.clear();
        model.query("");
        if (!open) {
            open = true;
            visible(true);
        }
        refresh();
        focusPending = true;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Закрывает палитру и убирает её из слоя оверлеев.
     *
     * @return эта палитра
     */
    public NodePalette close() {
        if (!open) return this;
        open = false;
        focusPending = false;
        visible(false);
        if (parent() instanceof OverlayLayer layer) {
            layer.removeOverlay(this);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Создаёт выбранную ноду: закрывает палитру и вызывает {@link #onPick(Consumer)}. Если выбрана группа, ничего не
     * делает.
     *
     * @return {@code true}, если нода была выбрана и создана
     */
    public boolean pickSelected() {
        NodeType type = model.selectedType();
        if (type == null) return false;
        close();
        onPick.accept(type);
        return true;
    }

    /**
     * Действие над выбранной строкой: группа разворачивается или сворачивается, нода создаётся.
     *
     * @return {@code true}, если было что выбирать
     */
    public boolean activateSelected() {
        if (model.selected() == null) return false;
        if (model.selectedType() != null) return pickSelected();
        model.activateSelected();
        refresh();
        return true;
    }

    private void activateRow(int rowIndex) {
        int index = model.windowStart() + rowIndex;
        if (index < 0 || index >= model.entries().size()) return;
        model.select(index);
        activateSelected();
    }

    private void hoverRow(int rowIndex) {
        int index = model.windowStart() + rowIndex;
        if (index < 0 || index >= model.entries().size() || index == model.selectedIndex()) return;
        model.select(index);
        refresh();
    }

    /**
     * Переносит модель на экран: подписи, отступы, подсветка выбранной строки.
     */
    private void refresh() {
        List<NodePaletteModel.Entry> visible = model.visible();
        boolean searching = !model.query().isBlank();
        for (int i = 0; i < rowWidgets.size(); i++) {
            PaletteRow row = rowWidgets.get(i);
            if (model.isEmpty() && i == 0) {
                row.showEmpty(EMPTY_TEXT);
            } else if (i < visible.size()) {
                row.show(visible.get(i), model.windowStart() + i == model.selectedIndex(), searching);
            } else {
                row.hide();
            }
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || !open || event instanceof dev.sixik.unigui.api.event.WidgetEvent widgetEvent
                && widgetEvent.phase() == EventPhase.CAPTURE) {
            return;
        }
        if (event instanceof ScrollEvent scroll) {
            if (scroll.deltaY() != 0.0f) {
                model.scroll(scroll.deltaY() < 0.0f ? 1 : -1);
                refresh();
                event.cancel();
            }
            return;
        }
        if (!(event instanceof KeyPressedEvent key)) return;
        int code = key.keyCode();
        if (code == KeyCodes.ESCAPE) {
            close();
            event.cancel();
        } else if (code == KeyCodes.DOWN) {
            model.move(1);
            refresh();
            event.cancel();
        } else if (code == KeyCodes.UP) {
            model.move(-1);
            refresh();
            event.cancel();
        } else if (code == KeyCodes.RIGHT && model.query().isBlank()) {
            model.expandSelected();
            refresh();
            event.cancel();
        } else if (code == KeyCodes.LEFT && model.query().isBlank()) {
            model.collapseSelected();
            refresh();
            event.cancel();
        } else if (code == KeyCodes.ENTER || code == KeyCodes.KEYPAD_ENTER) {
            if (activateSelected()) event.cancel();
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        if (focusPending && uiContext() != null) {
            focusPending = false;
            uiContext().focusManager().requestFocus(search);
        }
        column.measure(context);
        setDesiredSize(resolveDesiredSize(context,
                column.desiredSize().width() + padding.horizontal(),
                column.desiredSize().height() + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(hostBounds.x(), hostBounds.y(), 0.0f, 0.0f);
            return;
        }
        float width = desiredSize().width();
        float height = desiredSize().height();
        float placedX = Math.max(hostBounds.x(), Math.min(x, hostBounds.x() + Math.max(0.0f, hostBounds.width() - width)));
        float placedY = Math.max(hostBounds.y(), Math.min(y, hostBounds.y() + Math.max(0.0f, hostBounds.height() - height)));
        mutableLayoutBounds().set(placedX, placedY, width, height);
        column.arrange(new MutableRect(
                placedX + padding.left(),
                placedY + padding.top(),
                Math.max(0.0f, width - padding.horizontal()),
                Math.max(0.0f, height - padding.vertical())));
    }

    @Override
    public boolean isOpenOverlay() {
        return open;
    }

    @Override
    public boolean dismissOnOutsideClick() {
        return true;
    }

    @Override
    public boolean isInteractionTarget(Widget target) {
        for (Widget current = target; current != null; current = current.parent()) {
            if (current == this) return true;
        }
        return false;
    }

    @Override
    public void dismiss() {
        close();
    }

    /**
     * Цвет акцента категории: стабильный по названию верхней группы, чтобы одна и та же категория всегда имела один
     * цвет; ноды без категории серые.
     *
     * @param category путь категории, например {@code Math/Vector}
     * @return новый цвет акцента
     */
    public static MutableColor accentColor(String category) {
        String top = category == null ? "" : category.split("/")[0].trim().toLowerCase(Locale.ROOT);
        if (top.isEmpty()) return new MutableColor(0.62f, 0.64f, 0.68f, 1.0f);
        float hue = (Math.floorMod(top.hashCode(), 360)) / 360.0f;
        return fromHsv(hue, 0.52f, 0.92f);
    }

    private static MutableColor fromHsv(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) h;
        float fraction = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * fraction);
        float t = value * (1.0f - saturation * (1.0f - fraction));
        return switch (sector) {
            case 0 -> new MutableColor(value, t, p, 1.0f);
            case 1 -> new MutableColor(q, value, p, 1.0f);
            case 2 -> new MutableColor(p, value, t, 1.0f);
            case 3 -> new MutableColor(p, q, value, 1.0f);
            case 4 -> new MutableColor(t, p, value, 1.0f);
            default -> new MutableColor(value, p, q, 1.0f);
        };
    }

    /**
     * Строка палитры: группа с шевроном и счётчиком или нода с цветным акцентом категории. Сама ловит клик и
     * наведение от своих подписей, потому что события всплывают к ней.
     */
    public static final class PaletteRow extends LinearBox {
        private final Label title = new Label("").textAlignment(Alignment.START);
        private final Label hint = new Label("").textAlignment(Alignment.END);
        private final Box spacer = new Box();
        private final MutableColor selectedFill = new MutableColor(1.0f, 1.0f, 1.0f, 0.09f);
        private final MutableColor accent = new MutableColor(0.62f, 0.64f, 0.68f, 1.0f);
        private final Runnable onActivate;
        private final Runnable onHover;
        private boolean category;
        private boolean expanded;
        private boolean inert;
        private int depth;

        private PaletteRow(Runnable onActivate, Runnable onHover) {
            super(Orientation.HORIZONTAL);
            this.onActivate = onActivate;
            this.onHover = onHover;
            layout(style -> style.size(WIDTH, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
            backgroundVisible(false);
            borderVisible(false);
            spacer.backgroundVisible(false);
            spacer.borderVisible(false);
            spacer.layout(style -> style.size(ROW_LEFT, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
            title.layout(style -> style.flexGrow(1).flexShrink(1.0f));
            hint.layout(style -> style.size(96.0f, ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
            addChild(spacer);
            addChild(title);
            addChild(hint);
            applyQueuedMutations();
        }

        /**
         * @return текст названия строки
         */
        public String title() {
            return title.text();
        }

        /**
         * @return текст подсказки справа: количество нод у группы или категория у найденной ноды
         */
        public String hint() {
            return hint.text();
        }

        /**
         * @return {@code true}, если строка — группа категории
         */
        public boolean isCategory() {
            return category;
        }

        /**
         * @return {@code true}, если строка — развёрнутая группа
         */
        public boolean isExpanded() {
            return expanded;
        }

        /**
         * @return глубина вложенности строки
         */
        public int depth() {
            return depth;
        }

        /**
         * @return {@code true}, если строка выбрана (подсвечена)
         */
        public boolean isSelected() {
            return backgroundVisible();
        }

        /**
         * @return текущий цвет акцента строки
         */
        public MutableColor accent() {
            return accent;
        }

        /**
         * Нажимает строку так, как это делает мышь.
         */
        public void click() {
            if (!inert && visibility() == Visibility.VISIBLE) onActivate.run();
        }

        private void show(NodePaletteModel.Entry entry, boolean selected, boolean searching) {
            visibility(Visibility.VISIBLE);
            inert = false;
            depth = entry.depth();
            spacer.layout(style -> style.size(ROW_LEFT + depth * INDENT, ROW_HEIGHT));
            if (entry instanceof NodePaletteModel.CategoryEntry group) {
                category = true;
                expanded = group.expanded();
                accent.set(accentColor(group.path()));
                title.text(group.title());
                title.color(brighter(accent));
                hint.text(Integer.toString(group.count()));
                hint.color(new MutableColor(0.55f, 0.57f, 0.62f, 1.0f));
            } else if (entry instanceof NodePaletteModel.NodeEntry node) {
                category = false;
                expanded = false;
                accent.set(accentColor(node.type().category()));
                title.text(node.type().title());
                title.color(new MutableColor(0.93f, 0.94f, 0.96f, 1.0f));
                hint.text(searching ? node.type().category() : "");
                hint.color(new MutableColor(0.55f, 0.57f, 0.62f, 1.0f));
            }
            background(selectedFill);
            backgroundVisible(selected);
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        private void showEmpty(String text) {
            visibility(Visibility.VISIBLE);
            inert = true;
            category = false;
            expanded = false;
            depth = 0;
            spacer.layout(style -> style.size(ROW_LEFT, ROW_HEIGHT));
            title.text(text);
            title.color(new MutableColor(0.55f, 0.57f, 0.62f, 1.0f));
            hint.text("");
            backgroundVisible(false);
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        private void hide() {
            visibility(Visibility.COLLAPSED);
            inert = true;
            backgroundVisible(false);
        }

        private static MutableColor brighter(MutableColor color) {
            return new MutableColor(Math.min(1.0f, color.r() * 0.9f + 0.1f), Math.min(1.0f, color.g() * 0.9f + 0.1f),
                    Math.min(1.0f, color.b() * 0.9f + 0.1f), 1.0f);
        }

        @Override
        public void handle(Event event) {
            super.handle(event);
            if (event.isCancelled() || inert) return;
            if (event instanceof PointerPressedEvent pressed
                    && pressed.button() == PointerButton.PRIMARY && pressed.phase() != EventPhase.CAPTURE) {
                onActivate.run();
                event.cancel();
            } else if (event instanceof PointerMovedEvent moved && moved.phase() != EventPhase.CAPTURE) {
                onHover.run();
            }
        }

        @Override
        protected void renderContent(RenderContext context) {
            if (!inert && visibility() == Visibility.VISIBLE) {
                DrawScope draw = new DrawScope(context, transform(), layoutBounds());
                float left = layoutBounds().x() + 6.0f + depth * INDENT;
                float centerY = layoutBounds().y() + layoutBounds().height() * 0.5f;
                if (category) {
                    // Шеврон: треугольник вправо у свёрнутой группы, вниз у развёрнутой.
                    draw.pathClear();
                    if (expanded) {
                        draw.pathLineTo(left, centerY - 2.0f);
                        draw.pathLineTo(left + 8.0f, centerY - 2.0f);
                        draw.pathLineTo(left + 4.0f, centerY + 3.0f);
                    } else {
                        draw.pathLineTo(left + 1.0f, centerY - 4.0f);
                        draw.pathLineTo(left + 1.0f, centerY + 4.0f);
                        draw.pathLineTo(left + 6.0f, centerY);
                    }
                    draw.pathFillConvex(accent);
                } else {
                    float barHeight = layoutBounds().height() - 8.0f;
                    draw.rect(left + 2.0f, layoutBounds().y() + 4.0f, 3.0f, barHeight, Paint.fill(accent));
                }
            }
            super.renderContent(context);
        }
    }
}
