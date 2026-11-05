package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.ContextMenus;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.editor.style.SectionHeader;
import dev.sixik.unigui.editor.style.ToolButton;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.component.Promotions;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import dev.sixik.unigui.xaml.edit.Inspector;
import dev.sixik.unigui.xaml.edit.ResourceCatalog;
import dev.sixik.unigui.xaml.edit.ValueEditing;
import dev.sixik.unigui.xaml.edit.SessionChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Свойства выбранного узла. Пустое поле означает «не задано» (действует значение по умолчанию), любое другое
 * значение записывается в документ правкой с историей отмены. Некорректное значение подсвечивается под полем.
 */
final class InspectorPanel extends SessionPanel {
    /** Свойства, которые показываются парой в одной строке: запись, заголовок и короткие подписи полей. */
    private record Pair(String first, String second, String title, String firstMark, String secondMark) {
    }

    private static final List<Pair> PAIRS = List.of(
            new Pair("x", "y", "Position", "X", "Y"),
            new Pair("width", "height", "Size", "W", "H"),
            new Pair("minWidth", "minHeight", "Min Size", "W", "H"),
            new Pair("maxWidth", "maxHeight", "Max Size", "W", "H"));
    private static final float LABEL_WIDTH = 100.0f;

    private static final String NOT_SET = "(default)";

    private final VBox content = new VBox();
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Map<String, ComboBox> combos = new LinkedHashMap<>();
    private final Map<String, Label> problems = new LinkedHashMap<>();
    /** Группа Grid нужна редко и длинная, поэтому изначально свёрнута. */
    private final java.util.Set<String> collapsed = new java.util.HashSet<>(List.of("Grid"));
    private final TextField search = EditorStyles.searchField();
    /** Строки свойств с текстом, по которому их ищет поле поиска. */
    private final Map<Widget, String> searchable = new LinkedHashMap<>();
    private UiNode shown;
    private boolean syncing;
    private boolean applying;

    @Override
    public String id() {
        return UiEditor.INSPECTOR;
    }

    @Override
    public String title() {
        return "Inspector";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.INSPECTOR;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.RIGHT;
    }

    @Override
    public float share() {
        return 0.28f;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        content.spacing(1.0f);
        content.layout(style -> style.widthPercent(100.0f).padding(6.0f, 2.0f, 8.0f, 10.0f));
        ScrollView scroll = new ScrollView(content);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        search.id("inspector.search");
        search.placeholder("Search properties");
        search.onTextChanged(event -> applySearch());
        VBox root = new VBox();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        root.addChild(EditorStyles.searchBar(search));
        root.addChild(scroll);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        rebuild();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case SELECTION_CHANGED, REVERTED -> rebuild();
            case DOCUMENT_CHANGED -> {
                // Свои же правки поля не пересобирают: иначе поле теряло бы фокус на каждой букве.
                if (applying) refreshProblems();
                else rebuild();
            }
            default -> { }
        }
    }

    private void rebuild() {
        content.clearChildren();
        fields.clear();
        combos.clear();
        problems.clear();
        searchable.clear();
        EditorSession session = session().orElse(null);
        shown = session == null ? null : session.selected().orElse(null);
        if (shown == null) {
            Label empty = EditorStyles.caption(session == null ? "No document" : "Select a widget");
            empty.layout(style -> style.margin(2.0f, 6.0f));
            content.addChild(empty);
            content.applyQueuedMutations();
            EditorStyles.apply(content);
            return;
        }

        Inspector.Inspection inspection = Inspector.inspect(session.schema(), shown);
        content.addChild(heading(inspection));

        syncing = true;
        try {
            for (Map.Entry<String, List<Inspector.PropertyView>> category : groups(inspection).entrySet()) {
                addGroup(session, category.getKey(), category.getValue());
            }
        } finally {
            syncing = false;
        }
        if (Anchors.supported(session, shown)) {
            SectionHeader header = new SectionHeader("▾ Anchor");
            header.id("inspector.group.Anchor");
            content.addChild(header);
            content.addChild(Anchors.picker(session, shown, this::rebuild));
        }
        for (UiProperty unknown : inspection.unknown()) {
            content.addChild(EditorStyles.caption(unknown.displayName() + " (not in schema)"));
        }
        content.applyQueuedMutations();
        EditorStyles.apply(content);
        applySearch();
    }

    /** Заголовок панели: значок типа, имя и описание. */
    private Widget heading(Inspector.Inspection inspection) {
        HBox head = new HBox();
        head.spacing(6.0f);
        head.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).margin(0.0f, 0.0f, 0.0f, 4.0f)
                .alignItems(dev.sixik.unigui.api.layout.Align.CENTER));
        dev.sixik.unigui.widgets.display.CanvasWidget tile = new dev.sixik.unigui.widgets.display.CanvasWidget();
        tile.layout(style -> style.size(24.0f, 24.0f).flexShrink(0.0f));
        var icon = EditorIcons.forWidget(inspection.typeName());
        tile.onDraw(draw -> {
            var b = tile.layoutBounds();
            draw.addRectFilled(b.x(), b.y(), b.width(), b.height(), 4.0f, EditorStyles.ACCENT_SOFT);
            icon.draw(EditorStyles.scope(draw, tile), b.x() + 5.0f, b.y() + 5.0f, 14.0f, EditorStyles.INFO_BLUE);
        });
        Label name = new Label(inspection.typeName() + (inspection.known() ? "" : " (unknown type)"));
        name.color(EditorStyles.TEXT_BOLD);
        VBox text = new VBox();
        text.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).minWidth(0.0f));
        text.addChild(name);
        if (!inspection.description().isBlank()) {
            Label description = EditorStyles.caption(inspection.description());
            description.layout(style -> style.widthPercent(100.0f));
            text.addChild(description);
        }
        text.applyQueuedMutations();
        head.addChild(tile);
        head.addChild(text);
        head.applyQueuedMutations();
        return head;
    }

    /** Категории схемы; длинная Layout делится, чтобы редкие свойства сетки не мешали частым. */
    private static Map<String, List<Inspector.PropertyView>> groups(Inspector.Inspection inspection) {
        Map<String, List<Inspector.PropertyView>> groups = new LinkedHashMap<>();
        for (Map.Entry<String, List<Inspector.PropertyView>> category : inspection.byCategory().entrySet()) {
            for (Inspector.PropertyView property : category.getValue()) {
                String group = category.getKey().equals("Layout") && property.name().toLowerCase().startsWith("grid")
                        ? "Grid" : category.getKey();
                groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(property);
            }
        }
        return groups;
    }

    /** Показывает только строки, подходящие под поиск. */
    private void applySearch() {
        String query = search.text().trim().toLowerCase();
        for (Map.Entry<Widget, String> row : searchable.entrySet()) {
            if (row.getKey() instanceof dev.sixik.unigui.impl.widget.WidgetBase widget) {
                widget.visibility(query.isEmpty() || row.getValue().contains(query)
                        ? Visibility.VISIBLE : Visibility.COLLAPSED);
            }
        }
    }

    private static void show(Label label, String text) {
        label.text(text == null ? "" : text);
        label.visibility(text == null || text.isEmpty() ? Visibility.COLLAPSED : Visibility.VISIBLE);
    }

    /** Группа свойств с заголовком-переключателем; свёрнутые группы запоминаются между перестроениями. */
    private void addGroup(EditorSession session, String category, List<Inspector.PropertyView> properties) {
        VBox body = new VBox();
        body.spacing(3.0f);
        body.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(0.0f, 1.0f, 0.0f, 6.0f));
        SectionHeader header = new SectionHeader((collapsed.contains(category) ? "▸ " : "▾ ") + category);
        header.id("inspector.group." + category);
        header.onClick(click -> {
            boolean now = collapsed.add(category);
            if (!now) collapsed.remove(category);
            header.text((now ? "▸ " : "▾ ") + category);
            body.visibility(now ? Visibility.COLLAPSED : Visibility.VISIBLE);
        });
        content.addChild(header);
        Map<String, Inspector.PropertyView> byName = new LinkedHashMap<>();
        for (Inspector.PropertyView property : properties) byName.put(property.name(), property);
        java.util.Set<String> done = new java.util.HashSet<>();
        for (Inspector.PropertyView property : properties) {
            if (!done.add(property.name())) continue;
            Pair pair = PAIRS.stream().filter(candidate -> candidate.first().equals(property.name())
                    && byName.containsKey(candidate.second())).findFirst().orElse(null);
            if (pair == null) {
                addRow(session, property, body);
            } else {
                done.add(pair.second());
                addPair(session, pair, property, byName.get(pair.second()), body);
            }
        }
        body.visibility(collapsed.contains(category) ? Visibility.COLLAPSED : Visibility.VISIBLE);
        body.applyQueuedMutations();
        content.addChild(body);
    }

    private static HBox propertyRow() {
        HBox row = new HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).minHeight(EditorStyles.ROW_HEIGHT));
        return row;
    }

    private static Label rowLabel(String text, boolean set) {
        Label name = new Label(text);
        name.color(set ? EditorStyles.TEXT_PRIMARY : EditorStyles.TEXT_LABEL);
        name.layout(style -> style.width(LABEL_WIDTH).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
        return name;
    }

    private void addRow(EditorSession session, Inspector.PropertyView property, VBox into) {
        HBox row = propertyRow();
        row.addChild(rowLabel(property.displayName(), property.valueText() != null));
        row.addChild(editor(session, property, row));
        row.applyQueuedMutations();
        into.addChild(row);
        searchable.put(row, property.displayName().toLowerCase());
        into.addChild(problemLabel(property));
    }

    /** Две связанные величины в одной строке: X и Y, ширина и высота. */
    private void addPair(EditorSession session, Pair pair, Inspector.PropertyView first, Inspector.PropertyView second,
                         VBox into) {
        HBox row = propertyRow();
        row.addChild(rowLabel(pair.title(), first.valueText() != null || second.valueText() != null));
        row.addChild(cell(session, first, pair.firstMark()));
        row.addChild(cell(session, second, pair.secondMark()));
        row.applyQueuedMutations();
        into.addChild(row);
        searchable.put(row, (pair.title() + " " + first.displayName() + " " + second.displayName()).toLowerCase());
        into.addChild(problemLabel(first));
        into.addChild(problemLabel(second));
    }

    private HBox cell(EditorSession session, Inspector.PropertyView property, String mark) {
        HBox cell = new HBox();
        cell.spacing(3.0f);
        cell.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        Label prefix = EditorStyles.caption(mark);
        prefix.layout(style -> style.width(9.0f).height(EditorStyles.ROW_HEIGHT).flexShrink(0.0f));
        cell.addChild(prefix);
        cell.addChild(editor(session, property, cell));
        cell.applyQueuedMutations();
        return cell;
    }

    private Label problemLabel(Inspector.PropertyView property) {
        Label problem = new Label("");
        problem.color(EditorStyles.ERROR_RED);
        problem.layout(style -> style.margin(LABEL_WIDTH + 4.0f, 0.0f, 0.0f, 0.0f));
        show(problem, property.problem());
        problems.put(property.name(), problem);
        return problem;
    }

    /** Поле значения по виду свойства; «Очистить» живёт в контекстном меню строки. */
    private Widget editor(EditorSession session, Inspector.PropertyView property, HBox row) {
        String bound = shown == null ? null : Promotions.binding(shown, property.name()).orElse(null);
        if (bound != null) return boundEditor(session, property, row, bound);
        ValueEditing.Editor spec = ValueEditing.editorFor(property,
                context().service(ResourceCatalog.class).orElse(ResourceCatalog.NONE));
        Widget editor;
        Runnable clear;
        if (!property.options().isEmpty()) {
            // Перечисления и логические свойства выбираются из списка, а не печатаются: опечатка невозможна.
            // У списка всегда есть выбранный пункт, поэтому «не задано» — отдельный первый пункт.
            ComboBox combo = choice(session, property, property.options());
            combos.put(property.name(), combo);
            editor = combo;
            clear = () -> {
                combo.silentSelectedIndex(0);
                commit(session, property.name(), "");
            };
        } else if (spec instanceof ValueEditing.Resource resource && !resource.candidates().isEmpty()) {
            ComboBox combo = choice(session, property, resource.candidates());
            combo.id("inspector.resource." + property.name());
            combos.put(property.name(), combo);
            editor = combo;
            clear = () -> {
                combo.silentSelectedIndex(0);
                commit(session, property.name(), "");
            };
        } else if (spec instanceof ValueEditing.Color) {
            VBox color = colorEditor(session, property, spec);
            editor = color;
            clear = () -> commit(session, property.name(), "");
        } else if (spec instanceof ValueEditing.Insets) {
            HBox insets = insetsEditor(session, property, spec);
            editor = insets;
            clear = () -> commit(session, property.name(), "");
        } else {
            TextField field = new TextField(property.valueText() == null ? "" : property.valueText());
            field.placeholder(property.defaultValue());
            field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
            field.onTextChanged(event -> {
                if (!syncing) commitTyped(session, property, spec, field.text());
            });
            if (spec instanceof ValueEditing.Number) field.id("inspector.number." + property.name());
            fields.put(property.name(), field);
            editor = field;
            clear = () -> field.text("");
        }
        ContextMenus.onSecondaryClick(row, context(), () -> rowMenu(session, property, clear));
        return editor;
    }

    /** Меню строки: сброс значения и, в компоненте, превращение свойства в значение контракта. */
    private List<ContextMenus.Entry> rowMenu(EditorSession session, Inspector.PropertyView property, Runnable clear) {
        List<ContextMenus.Entry> entries = new ArrayList<>();
        entries.add(ContextMenus.Entry.of("Clear", shown != null && shown.attributeText(property.name()).isPresent(), clear));
        if (shown != null && Promotions.supported(session, shown, property.name())) {
            entries.add(ContextMenus.Entry.separator());
            entries.add(ContextMenus.Entry.of("Promote to Variable", () -> promote(property.name(), Promotions.Kind.VARIABLE)));
            entries.add(ContextMenus.Entry.of("Promote to Parameter", () -> promote(property.name(), Promotions.Kind.PARAM)));
        }
        return entries;
    }

    /** Значение теперь приходит из контракта: вместо поля показывается ссылка на него. */
    private Widget boundEditor(EditorSession session, Inspector.PropertyView property, HBox row, String name) {
        ToolButton link = new ToolButton(EditorIcons.GRAPH, name, ToolButton.Variant.SOLID).alignStart()
                .iconColor(EditorStyles.INFO_BLUE);
        link.id("inspector.bound." + property.name());
        link.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f)
                .height(EditorStyles.ROW_HEIGHT));
        Runnable unbind = () -> {
            if (shown != null) Promotions.unbind(session, shown, property.name());
        };
        ContextMenus.onSecondaryClick(row, context(), () -> List.of(ContextMenus.Entry.of("Remove binding", unbind)));
        return link;
    }

    /**
     * Превращает свойство выбранного узла в значение контракта.
     *
     * @param property имя свойства
     * @param kind     параметр или переменная
     * @return {@code true}, если превращено
     */
    boolean promote(String property, Promotions.Kind kind) {
        EditorSession session = session().orElse(null);
        return session != null && shown != null && Promotions.promote(session, shown, property, kind).isPresent();
    }

    /**
     * @param property имя свойства
     * @return подписи пунктов меню строки; нужно тестам
     */
    List<String> menuOf(String property) {
        EditorSession session = session().orElse(null);
        Inspector.PropertyView view = Inspector.inspect(session.schema(), shown).properties().stream()
                .filter(candidate -> candidate.name().equals(property)).findFirst().orElseThrow();
        return rowMenu(session, view, () -> { }).stream().map(ContextMenus.Entry::title)
                .filter(java.util.Objects::nonNull).toList();
    }

    /** Список значений с первым пунктом «не задано». */
    private ComboBox choice(EditorSession session, Inspector.PropertyView property, List<String> options) {
        ComboBox combo = new ComboBox();
        List<String> items = new ArrayList<>();
        items.add(NOT_SET);
        items.addAll(options);
        combo.items(items);
        combo.silentSelectedIndex(property.valueText() == null ? 0 : Math.max(0, items.indexOf(property.valueText())));
        combo.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        combo.onSelectionChanged(event -> {
            int index = combo.selectedIndex();
            if (syncing || index < 0) return;
            commit(session, property.name(), index == 0 ? "" : items.get(index));
        });
        return combo;
    }

    /** Цвет: шестнадцатеричная запись, образец и четыре канала 0–255. */
    private VBox colorEditor(EditorSession session, Inspector.PropertyView property, ValueEditing.Editor spec) {
        VBox box = new VBox();
        box.spacing(2.0f);
        box.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        ValueEditing.ColorValue start = null;
        try {
            if (property.valueText() != null) start = ValueEditing.ColorValue.parse(property.valueText());
        } catch (IllegalArgumentException notAColor) {
            start = null; // привязка или нестандартная запись: редактируется как текст
        }
        TextField hex = new TextField(property.valueText() == null ? "" : property.valueText());
        hex.id("inspector.color." + property.name());
        hex.placeholder(property.defaultValue());
        hex.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        fields.put(property.name(), hex);

        dev.sixik.unigui.widgets.containers.Box swatch = new dev.sixik.unigui.widgets.containers.Box();
        swatch.themeEnabled(false);
        swatch.backgroundVisible(true);
        swatch.borderVisible(true);
        swatch.borderColor().set(EditorStyles.BORDER_BUTTON);
        swatch.radius(EditorStyles.RADIUS);
        swatch.layout(style -> style.size(18.0f, 18.0f).flexShrink(0.0f));
        paintSwatch(swatch, start);

        HBox channels = new HBox();
        channels.spacing(2.0f);
        TextField[] fieldsRgba = new TextField[4];
        String[] names = {"r", "g", "b", "a"};
        for (int i = 0; i < 4; i++) {
            int value = start == null ? (i == 3 ? 255 : 0) : new int[]{start.red(), start.green(), start.blue(), start.alpha()}[i];
            TextField channel = new TextField(Integer.toString(value));
            channel.id("inspector.color." + property.name() + "." + names[i]);
            channel.layout(style -> style.width(10.0f).flexGrow(1.0f).flexShrink(1.0f).minWidth(0.0f));
            fieldsRgba[i] = channel;
            channels.addChild(channel);
        }
        channels.applyQueuedMutations();

        hex.onTextChanged(event -> {
            if (syncing) return;
            commitTyped(session, property, spec, hex.text());
            try {
                ValueEditing.ColorValue parsed = ValueEditing.ColorValue.parse(hex.text());
                paintSwatch(swatch, parsed);
                syncing = true;
                try {
                    fieldsRgba[0].text(Integer.toString(parsed.red()));
                    fieldsRgba[1].text(Integer.toString(parsed.green()));
                    fieldsRgba[2].text(Integer.toString(parsed.blue()));
                    fieldsRgba[3].text(Integer.toString(parsed.alpha()));
                } finally {
                    syncing = false;
                }
            } catch (IllegalArgumentException invalid) {
                paintSwatch(swatch, null);
            }
        });
        for (int i = 0; i < 4; i++) {
            char channel = names[i].charAt(0);
            TextField source = fieldsRgba[i];
            source.onTextChanged(event -> {
                if (syncing) return;
                int number;
                try {
                    number = Integer.parseInt(source.text().trim());
                } catch (NumberFormatException failure) {
                    return;
                }
                ValueEditing.ColorValue base;
                try {
                    base = hex.text().isBlank() ? ValueEditing.ColorValue.parse("#000000")
                            : ValueEditing.ColorValue.parse(hex.text());
                } catch (IllegalArgumentException failure) {
                    base = ValueEditing.ColorValue.parse("#000000");
                }
                String next = base.with(channel, number).format();
                syncing = true;
                try {
                    hex.text(next);
                } finally {
                    syncing = false;
                }
                paintSwatch(swatch, ValueEditing.ColorValue.parse(next));
                commitTyped(session, property, spec, next);
            });
        }
        HBox top = new HBox();
        top.spacing(4.0f);
        top.addChild(swatch);
        top.addChild(hex);
        top.applyQueuedMutations();
        box.addChild(top);
        box.addChild(channels);
        box.applyQueuedMutations();
        return box;
    }

    private static void paintSwatch(dev.sixik.unigui.widgets.containers.Box swatch, ValueEditing.ColorValue color) {
        if (color == null) {
            swatch.background().set(EditorStyles.BG_INPUT);
        } else {
            swatch.background().set(MutableColor.rgba255(color.red(), color.green(), color.blue(), color.alpha()));
        }
    }

    /** Отступы: четыре поля — верх, право, низ, лево. */
    private HBox insetsEditor(EditorSession session, Inspector.PropertyView property, ValueEditing.Editor spec) {
        HBox box = new HBox();
        box.spacing(2.0f);
        box.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f).minWidth(0.0f));
        double[] values = {0, 0, 0, 0};
        if (property.valueText() != null) {
            try {
                ValueEditing.InsetsValue parsed = ValueEditing.InsetsValue.parse(property.valueText());
                values = new double[]{parsed.top(), parsed.right(), parsed.bottom(), parsed.left()};
            } catch (IllegalArgumentException custom) {
                values = new double[]{0, 0, 0, 0};
            }
        }
        String[] names = {"top", "right", "bottom", "left"};
        TextField[] cells = new TextField[4];
        for (int i = 0; i < 4; i++) {
            TextField cell = new TextField(ValueEditing.normalize(new ValueEditing.Number(false), Double.toString(values[i])));
            cell.id("inspector.insets." + property.name() + "." + names[i]);
            cell.placeholder(names[i].substring(0, 1).toUpperCase());
            cell.layout(style -> style.width(10.0f).flexGrow(1.0f).flexShrink(1.0f).minWidth(0.0f));
            cells[i] = cell;
            box.addChild(cell);
        }
        for (TextField cell : cells) {
            cell.onTextChanged(event -> {
                if (syncing) return;
                double[] parsed = new double[4];
                try {
                    for (int i = 0; i < 4; i++) {
                        parsed[i] = cells[i].text().isBlank() ? 0.0 : Double.parseDouble(cells[i].text().trim());
                    }
                } catch (NumberFormatException failure) {
                    Label message = problems.get(property.name());
                    if (message != null) show(message, "Insets are four numbers.");
                    return;
                }
                commitTyped(session, property, spec,
                        new ValueEditing.InsetsValue(parsed[0], parsed[1], parsed[2], parsed[3]).format());
            });
        }
        box.applyQueuedMutations();
        return box;
    }

    /** Значение приводится к канонической записи; если оно не подходит, причина показывается под полем. */
    private void commitTyped(EditorSession session, Inspector.PropertyView property, ValueEditing.Editor spec, String text) {
        String normalized;
        try {
            normalized = ValueEditing.normalize(spec, text);
        } catch (IllegalArgumentException failure) {
            Label message = problems.get(property.name());
            if (message != null) show(message, failure.getMessage());
            return;
        }
        commit(session, property.name(), normalized);
    }

    private void commit(EditorSession session, String name, String text) {
        UiNode node = shown;
        if (node == null) return;
        Edit edit;
        if (text.isBlank()) {
            if (node.attributeText(name).isEmpty()) return;
            edit = Edits.removeProperty(node, name);
        } else {
            edit = Edits.setProperty(node, name, text);
        }
        applying = true;
        try {
            session.apply(edit);
        } finally {
            applying = false;
        }
        refreshProblems();
    }

    /** Обновляет подписи ошибок под полями, не пересоздавая сами поля. */
    private void refreshProblems() {
        EditorSession session = session().orElse(null);
        if (session == null || shown == null) return;
        Inspector.Inspection inspection = Inspector.inspect(session.schema(), shown);
        for (Inspector.PropertyView property : inspection.properties()) {
            Label label = problems.get(property.name());
            if (label != null) show(label, property.problem());
        }
    }
}
