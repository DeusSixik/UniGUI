package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SceneElementKind;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Searchable registry browser for adding Minecraft models to the scene.
 */
final class UnityMinecraftPicker {
    /**
     * Desired window size in UI units.
     */
    private static final float WIDTH = 590, HEIGHT = 350;
    /**
     * Platform that owns the registries, or null in a standalone editor.
     */
    private final DemoMinecraftHost host;
    /**
     * Registry contents loaded once per category for the current dialog.
     */
    private final Map<SceneElementKind, List<Entry>> catalog = new EnumMap<>(SceneElementKind.class);
    /**
     * Buttons in the category strip.
     */
    private final Map<SceneElementKind, Button> tabs = new EnumMap<>(SceneElementKind.class);
    /**
     * User's filter for the active category.
     */
    private final TextInput search = new TextInput("");
    /**
     * Virtual list that only creates rows within the visible scroll range.
     */
    private final VirtualListView results = new VirtualListView();
    /**
     * Status shown when the query has no matching entries.
     */
    private final Label empty;
    /**
     * Independent viewport for the currently selected registry entry.
     */
    private final UnityScenePreview preview = new UnityScenePreview();
    /**
     * Confirms that the previewed model should be added to the editor scene.
     */
    private final Button addButton = new Button("Add to Scene");
    /**
     * Modal window containing the browser.
     */
    private final WindowWidget window;
    /**
     * Category shown in the list.
     */
    private SceneElementKind category = SceneElementKind.BLOCK;
    /**
     * Entry currently shown in the preview, or null.
     */
    private Entry selected;
    /**
     * Most recently realized button belonging to the selected entry, or null.
     */
    private Button selectedRow;
    /**
     * Receives the selected registry entry before the dialog closes.
     */
    private final BiConsumer<SceneElementKind, Entry> onPick;

    /**
     * An entry in a Minecraft registry.
     *
     * @param id    exact ID accepted by the scene backend
     * @param label human-readable name generated from the ID
     */
    record Entry(String id, String label) {
        /**
         * Formats the ID path for display, including legacy camel-case names.
         *
         * @param id registry ID
         * @return entry ready to display and search
         */
        static Entry fromId(String id) {
            String words = id.substring(id.indexOf(':') + 1).replace('_', ' ')
                    .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            return new Entry(id, words.isEmpty() ? id : Character.toUpperCase(words.charAt(0)) + words.substring(1));
        }

        /**
         * Checks a normalized search against the name and the fully qualified ID.
         *
         * @param query lower-case search text
         * @return true if the entry should be shown
         */
        boolean matches(String query) {
            return id.toLowerCase(Locale.ROOT).contains(query) || label.toLowerCase(Locale.ROOT).contains(query);
        }
    }

    /**
     * Shows the picker over the editor and attaches its selection and close callbacks.
     *
     * @param overlay editor's window host
     * @param host    registry provider, or null if unavailable
     * @param onPick  receives the category and selected entry
     * @param onClose notifies the editor when the dialog is dismissed
     * @return the active window
     */
    static WindowWidget open(OverlayLayer overlay, DemoMinecraftHost host, BiConsumer<SceneElementKind, Entry> onPick,
                             Runnable onClose) {
        UnityMinecraftPicker picker = new UnityMinecraftPicker(host, onPick);
        WindowWidget window = picker.window;
        var bounds = overlay.layoutBounds();
        float width = Math.min(WIDTH, bounds.width() * 0.94f);
        float height = Math.min(HEIGHT, bounds.height() * 0.92f);
        window.position((bounds.width() - width) / 2, (bounds.height() - height) / 2);
        window.onClosed(event -> {
            overlay.removeOverlay(window);
            if (overlay.uiContext() != null) {
                overlay.uiContext().dispatcher().executeNextFrame(window::dispose);
            } else {
                window.dispose();
            }
            onClose.run();
        });
        overlay.addOverlay(window);
        window.openModal();
        if (overlay.uiContext() != null) {
            overlay.uiContext().dispatcher().executeNextFrame(() -> {
                if (window.opened() && picker.search.uiContext() != null) {
                    picker.search.uiContext().focusManager().requestFocus(picker.search);
                }
            });
        }
        return window;
    }

    /**
     * Builds the registry dialog.
     *
     * @param host   platform supplying the entries, or null
     * @param onPick action invoked with the selected registry entry
     */
    private UnityMinecraftPicker(DemoMinecraftHost host, BiConsumer<SceneElementKind, Entry> onPick) {
        this.host = host;
        this.onPick = onPick;
        VBox content = new VBox();
        content.spacing(6);
        content.layout(style -> style.sizePercent(100, 100).padding(6));

        HBox categories = new HBox();
        categories.spacing(4);
        categories.layout(style -> style.widthPercent(100).height(24).alignItems(Align.CENTER));
        addTab(categories, SceneElementKind.BLOCK, "Blocks");
        addTab(categories, SceneElementKind.ITEM, "Items");
        addTab(categories, SceneElementKind.ENTITY, "Entities");
        content.addChild(categories);

        UnityStyles.styleTextInput(search);
        search.placeholder("Search by name or ID...");
        search.layout(style -> style.widthPercent(100).height(22).flexShrink(0));
        search.onTextChanged(event -> filter());
        content.addChild(search);

        HBox browser = new HBox();
        browser.spacing(6);
        browser.layout(style -> style.widthPercent(100).height(0).flexGrow(1).flexShrink(1).alignItems(Align.STRETCH));
        content.addChild(browser);

        VBox list = new VBox();
        list.layout(style -> style.width(0).flexGrow(1).flexShrink(1));
        browser.addChild(list);
        results.itemHeight(23);
        results.layout(style -> style.widthPercent(100).height(0).flexGrow(1).flexShrink(1));
        list.addChild(results);
        empty = new Label(host == null ? "Minecraft registries are unavailable" : "No matching entries");
        empty.color(UnityStyles.TEXT_MUTED);
        empty.wrap(true);
        list.addChild(empty);

        VBox details = new VBox();
        details.spacing(6);
        details.layout(style -> style.widthPercent(42).flexShrink(0));
        details.addChild(preview);
        UnityStyles.styleButton(addButton);
        addButton.layout(style -> style.widthPercent(100).height(23).flexShrink(0));
        addButton.onClick(event -> addSelected());
        details.addChild(addButton);
        browser.addChild(details);

        window = new WindowWidget("Create Minecraft Element", content).dockable(false)
                .collapseButtonVisible(false).resizable(false).fixedModal(true);
        window.layout(style -> style.size(WIDTH, HEIGHT).maxWidthPercent(94).maxHeightPercent(92));
        window.on(KeyPressedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.BUBBLE && event.keyCode() == KeyCodes.ESCAPE) {
                window.close();
                event.cancel();
            }
        });
        selectEntry(null);
        select(SceneElementKind.BLOCK);
    }

    /**
     * Adds a category button to the tab strip.
     *
     * @param bar   horizontal category strip
     * @param kind  registry category
     * @param label caption shown to the user
     */
    private void addTab(HBox bar, SceneElementKind kind, String label) {
        Button button = new Button(label);
        UnityStyles.styleButton(button);
        button.layout(style -> style.height(22).flexGrow(1));
        button.onClick(event -> select(kind));
        tabs.put(kind, button);
        bar.addChild(button);
    }

    /**
     * Highlights the active category and applies the current search.
     *
     * @param kind registry to show
     */
    private void select(SceneElementKind kind) {
        if (category != kind) selectEntry(null);
        category = kind;
        tabs.forEach((tab, button) -> button.background().set(tab == kind ? UnityStyles.SELECTION_BLUE : UnityStyles.BG_BUTTON));
        filter();
    }

    /**
     * Rebuilds the visible results for the active registry and filter text.
     */
    private void filter() {
        String query = search.text().strip().toLowerCase(Locale.ROOT);
        SceneElementKind kind = category;
        List<Entry> entries = catalog.computeIfAbsent(kind, key -> host == null ? List.of()
                : host.sceneElementIds(key).stream().filter(id -> id != null && !id.isBlank())
                .distinct().map(Entry::fromId)
                .sorted(Comparator.comparing(Entry::label, String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id))
                .toList());
        List<Entry> visible = query.isEmpty() ? entries : entries.stream().filter(entry -> entry.matches(query)).toList();
        if (selected != null && !visible.contains(selected)) selectEntry(null);
        selectedRow = null;
        results.itemCount(visible.size()).itemFactory(index -> row(visible.get(index))).scrollTo(0);
        empty.visibility(visible.isEmpty() ? Visibility.VISIBLE : Visibility.COLLAPSED);
    }

    /**
     * Builds a result row that opens the model in the preview.
     *
     * @param entry registry source and display name
     * @return selectable row
     */
    private Button row(Entry entry) {
        Button button = new Button(entry.label() + "  ·  " + entry.id());
        UnityStyles.styleButton(button);
        button.textAlignment(Alignment.START);
        if (entry.equals(selected)) highlightRow(button);
        button.onClick(event -> {
            if (!window.opened() || entry.equals(selected)) return;
            highlightRow(button);
            selectEntry(entry);
        });
        return button;
    }

    /**
     * Moves the selection highlight to a newly selected or realized row.
     *
     * @param button row for the selected entry
     */
    private void highlightRow(Button button) {
        if (selectedRow != null && selectedRow.parent() == results) {
            selectedRow.background().set(UnityStyles.BG_BUTTON);
            selectedRow.hoverBackground().set(UnityStyles.BG_BUTTON_HOVER);
        }
        selectedRow = button;
        button.background().set(UnityStyles.SELECTION_BLUE);
        button.hoverBackground().set(UnityStyles.SELECTION_BLUE);
    }

    /**
     * Shows a model in isolation and updates whether it can be added.
     *
     * @param entry active registry entry, or null to clear the preview
     */
    private void selectEntry(Entry entry) {
        selected = entry;
        UnitySceneObject model = entry == null ? null : UnitySceneObject.minecraft(category, entry.label(), entry.id());
        preview.setContent(model, entry == null ? "Preview" : entry.id());
        addButton.enabled(entry != null);
    }

    /**
     * Adds the previewed registry entry and dismisses the picker.
     */
    private void addSelected() {
        if (!window.opened() || selected == null) return;
        onPick.accept(category, selected);
        window.close();
    }
}
