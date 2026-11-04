package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragGhost;
import dev.sixik.unigui.widgets.dnd.DragPayload;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoader;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Типы виджетов схемы по категориям. В режиме плиток у каждого типа живая миниатюра (настоящий виджет, созданный из
 * схемы), в режиме списка только название. Тип можно перетащить на предпросмотр или на строку иерархии, а щелчок
 * добавляет его в выбранный контейнер.
 */
final class PalettePanel extends SessionPanel {
    /** Тип данных перетаскивания: в {@link DragPayload#data} лежит {@link TypeInfo}. */
    static final String PAYLOAD_TYPE = "unigui.widget";

    private static final float TILE_WIDTH = 74.0f;
    private static final float PREVIEW_HEIGHT = 40.0f;

    private final VBox entries = new VBox();
    private final TextField filter = new TextField();
    private final Button mode = new Button("List");
    private final Map<TypeInfo<?>, Widget> previews = new IdentityHashMap<>();
    private final List<LoadedScreen<Widget>> screens = new ArrayList<>();
    private boolean tileMode = true;

    @Override
    public String id() {
        return UiEditor.PALETTE;
    }

    @Override
    public String title() {
        return "Palette";
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.LEFT;
    }

    @Override
    public float share() {
        return 0.22f;
    }

    @Override
    protected Widget build(EditorContext editorContext) {
        VBox root = new VBox();
        root.spacing(4.0f);
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        filter.placeholder("Search widgets");
        filter.onTextChanged(event -> refresh());
        mode.id("palette.mode");
        mode.onClick(click -> {
            tileMode = !tileMode;
            mode.text(tileMode ? "List" : "Tiles");
            refresh();
        });
        HBox bar = new HBox();
        bar.spacing(4.0f);
        bar.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
        filter.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        bar.addChild(filter);
        bar.addChild(mode);
        bar.applyQueuedMutations();

        entries.spacing(1.0f);
        entries.layout(style -> style.widthPercent(100.0f));
        ScrollView scroll = new ScrollView(entries);
        scroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        root.addChild(bar);
        root.addChild(scroll);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        for (LoadedScreen<Widget> screen : screens) screen.close();
        screens.clear();
        previews.clear();
        refresh();
    }

    private void refresh() {
        // Миниатюры живут до следующего обновления: виджет нельзя держать в двух плитках, поэтому создаются заново.
        for (LoadedScreen<Widget> screen : screens) screen.close();
        screens.clear();
        previews.clear();
        entries.clearChildren();
        EditorSession session = session().orElse(null);
        if (session == null) {
            entries.applyQueuedMutations();
            return;
        }
        String query = filter.text().trim().toLowerCase(Locale.ROOT);
        List<TypeInfo<?>> types = new ArrayList<>(session.schema().types());
        types.sort(Comparator.comparing((TypeInfo<?> type) -> type.category().toLowerCase(Locale.ROOT))
                .thenComparing(type -> type.name().local().toLowerCase(Locale.ROOT)));
        String category = null;
        WrapPanel group = null;
        for (TypeInfo<?> type : types) {
            String name = type.name().local();
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (!type.category().equals(category)) {
                category = type.category();
                entries.addChild(new Label(category));
                if (tileMode) {
                    group = new WrapPanel();
                    group.spacing(4.0f);
                    group.lineSpacing(4.0f);
                    group.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f));
                    entries.addChild(group);
                }
            }
            if (tileMode) group.addChild(tile(session, type));
            else entries.addChild(listEntry(session, type));
        }
        if (tileMode) {
            // Плитки добавляются в группы уже после того, как группы попали в список.
            for (Widget child : entries.children()) if (child instanceof WrapPanel panel) panel.applyQueuedMutations();
        }
        entries.applyQueuedMutations();
    }

    private Widget listEntry(EditorSession session, TypeInfo<?> type) {
        Button button = new Button("  " + type.name().local());
        button.id("palette." + type.name().local());
        button.onClick(event -> UiEdits.insert(session, type));
        draggable(button, button, type);
        return button;
    }

    private Widget tile(EditorSession session, TypeInfo<?> type) {
        VBox tile = new VBox();
        tile.spacing(2.0f);
        tile.layout(style -> style.width(TILE_WIDTH).flexShrink(0.0f));

        StackPanel frame = new StackPanel();
        frame.layout(style -> style.width(TILE_WIDTH).height(PREVIEW_HEIGHT).flexShrink(0.0f)
                .overflow(dev.sixik.unigui.api.layout.Overflow.HIDDEN));
        Widget preview = previewOf(session, type);
        if (preview != null) frame.addChild(preview);
        frame.applyQueuedMutations();

        Button name = new Button(type.name().local());
        name.id("palette." + type.name().local());
        name.layout(style -> style.width(TILE_WIDTH).flexShrink(0.0f));
        name.onClick(event -> UiEdits.insert(session, type));
        tile.addChild(frame);
        tile.addChild(name);
        tile.applyQueuedMutations();
        draggable(name, tile, type);
        draggable(frame, tile, type);
        return tile;
    }

    private void draggable(Widget handle, Widget moving, TypeInfo<?> type) {
        String name = type.name().local();
        DragController.attach(handle)
                .dragged(moving)
                .threshold(4.0f)
                .payload(() -> DragPayload.of(name, PAYLOAD_TYPE, name, type))
                .ghost(DragGhost.text());
    }

    /** Миниатюра типа: настоящий виджет по схеме; {@code null}, если тип без аргументов не создаётся. */
    private Widget previewOf(EditorSession session, TypeInfo<?> type) {
        try {
            UiNode sample = new UiNode(type.name());
            if (type.property("text").isPresent()) sample.set("text", type.name().local());
            LoadedScreen<Widget> screen = new XamlLoader(session.schema(), LoadOptions.DEFAULT.strictAttributes(false))
                    .loadScreen(UiDocument.of(sample));
            screens.add(screen);
            Widget widget = screen.root();
            previews.put(type, widget);
            return widget;
        } catch (RuntimeException failure) {
            return null;
        }
    }
}
