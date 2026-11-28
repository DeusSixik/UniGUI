package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Unity's Add Component popup: a search field over the list of components that can be added to the selection.
 * Typing filters by name or category, Enter adds the first match and Escape closes the window.
 */
final class UnityAddComponentWindow {
    /**
     * Popup width in UI units.
     */
    static final float WIDTH = 240.0f;
    /**
     * Popup height in UI units.
     */
    static final float HEIGHT = 260.0f;

    /**
     * One component the popup offers.
     *
     * @param name      component name
     * @param category  menu category, also searched
     * @param available whether the component can be added to the current selection
     * @param add       adds the component to the selection
     */
    record Entry(String name, String category, BooleanSupplier available, Runnable add) {
        /**
         * @param query lower-case search text
         * @return whether the name or category contains the text
         */
        boolean matches(String query) {
            return query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)
                    || category.toLowerCase(Locale.ROOT).contains(query);
        }
    }

    /**
     * Search field focused when the popup opens.
     */
    private final TextInput search = new TextInput("");
    /**
     * Rows of the visible entries.
     */
    private final VBox list = new VBox();
    /**
     * Shown when nothing matches.
     */
    private final Label empty = new Label("No matching components");
    /**
     * Offered components.
     */
    private final List<Entry> entries;
    /**
     * Entries visible for the current search text, in list order.
     */
    private final List<Entry> visible = new ArrayList<>();
    /**
     * The popup window.
     */
    private final WindowWidget window;

    /**
     * @param entries offered components
     */
    private UnityAddComponentWindow(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        VBox content = new VBox();
        content.spacing(4);
        content.layout(style -> style.sizePercent(100, 100).padding(6));
        UnityStyles.styleTextInput(search);
        search.placeholder("Search...");
        search.layout(style -> style.widthPercent(100).height(22).flexShrink(0));
        search.onTextChanged(event -> filter());
        search.on(KeyPressedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.BUBBLE && event.keyCode() == KeyCodes.ENTER && !visible.isEmpty()) {
                pick(visible.get(0));
                event.cancel();
            }
        });
        content.addChild(search);
        list.spacing(1);
        list.layout(style -> style.widthPercent(100).flexGrow(1).flexShrink(1).alignItems(Align.STRETCH));
        content.addChild(list);
        empty.color(UnityStyles.TEXT_MUTED);
        empty.layout(style -> style.widthPercent(100).height(22));
        content.addChild(empty);
        window = new WindowWidget("Add Component", content).dockable(false)
                .collapseButtonVisible(false).resizable(false).fixedModal(true);
        window.layout(style -> style.size(WIDTH, HEIGHT));
        window.on(KeyPressedEvent.TYPE, event -> {
            if (event.phase() != EventPhase.BUBBLE && event.keyCode() == KeyCodes.ESCAPE) {
                window.close();
                event.cancel();
            }
        });
        filter();
    }

    /**
     * Opens the popup below an anchor, kept inside the overlay.
     *
     * @param overlay editor window host
     * @param anchor  bounds of the Add Component button in overlay coordinates
     * @param entries offered components
     * @return the opened popup
     */
    static UnityAddComponentWindow open(OverlayLayer overlay, RectView anchor, List<Entry> entries) {
        UnityAddComponentWindow popup = new UnityAddComponentWindow(entries);
        WindowWidget window = popup.window;
        RectView bounds = overlay.layoutBounds();
        float x = anchor.x() + (anchor.width() - WIDTH) / 2 - bounds.x();
        float y = anchor.y() + anchor.height() + 2 - bounds.y();
        if (y + HEIGHT > bounds.height()) y = Math.max(0, anchor.y() - bounds.y() - HEIGHT - 2);
        window.position(Math.max(0, Math.min(x, bounds.width() - WIDTH)), y);
        window.onClosed(event -> {
            overlay.removeOverlay(window);
            if (overlay.uiContext() != null) overlay.uiContext().dispatcher().executeNextFrame(window::dispose);
            else window.dispose();
        });
        overlay.addOverlay(window);
        window.openModal();
        if (overlay.uiContext() != null) {
            overlay.uiContext().dispatcher().executeNextFrame(() -> {
                if (window.opened() && popup.search.uiContext() != null) {
                    popup.search.uiContext().focusManager().requestFocus(popup.search);
                }
            });
        }
        return popup;
    }

    /**
     * Rebuilds the rows for the current search text.
     */
    private void filter() {
        String query = search.text().trim().toLowerCase(Locale.ROOT);
        visible.clear();
        list.applyQueuedMutations();
        for (var child : List.copyOf(list.children())) list.removeChild(child);
        for (Entry entry : entries) {
            // Components every selected object already has are left out, as Unity lists only what can be added.
            if (!entry.matches(query) || !entry.available().getAsBoolean()) continue;
            visible.add(entry);
            Button row = new Button(entry.category() + " / " + entry.name());
            UnityStyles.styleButton(row);
            row.layout(style -> style.widthPercent(100).height(22).flexShrink(0));
            row.onClick(event -> pick(entry));
            list.addChild(row);
        }
        list.applyQueuedMutations();
        empty.visibility(list.children().isEmpty() ? Visibility.VISIBLE : Visibility.COLLAPSED);
    }

    /**
     * Adds a component and closes the popup.
     *
     * @param entry chosen component
     */
    private void pick(Entry entry) {
        if (!entry.available().getAsBoolean()) return;
        entry.add().run();
        window.close();
    }

    /**
     * @return the search field
     */
    TextInput search() {
        return search;
    }

    /**
     * @return rows of the visible components
     */
    List<Button> rows() {
        list.applyQueuedMutations();
        List<Button> rows = new ArrayList<>();
        for (var child : list.children()) if (child instanceof Button button) rows.add(button);
        return rows;
    }

    /**
     * @return the popup window
     */
    WindowWidget window() {
        return window;
    }
}
