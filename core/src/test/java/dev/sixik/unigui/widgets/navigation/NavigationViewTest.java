package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.interaction.Button;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Боковая навигация: группы, пункты, выбор, сворачивание и отрисовка.
 */
class NavigationViewTest {
    private NavigationView nav;
    private NavigationCategory input;
    private NavigationCategory text;

    @BeforeEach
    void setUp() {
        nav = new NavigationView();
        input = nav.addCategory("Basic Input");
        input.addItem("button", "Button");
        input.addItem("slider", "Slider");
        text = nav.addCategory("Text", false);
        text.addItem("textbox", "TextBox");
        layout();
    }

    private void layout() {
        nav.measure(new LayoutContext(240.0f, 600.0f));
        nav.arrange(new MutableRect(0.0f, 0.0f, 240.0f, 600.0f));
    }

    private static void click(TreeViewNode node) {
        Button row = node.rowButton();
        assertNotNull(row, "row for " + node.text() + " must be visible");
        row.click();
    }

    @Test
    void categoriesAndItems_areRegisteredInOrder() {
        assertEquals(2, nav.categories().size());
        assertEquals("Basic Input", nav.categories().get(0).title());
        assertEquals(List.of("button", "slider", "textbox"), nav.items().stream().map(NavigationItem::id).toList());
        assertSame(input, nav.item("slider").category());
        assertNull(nav.item("missing"));
    }

    @Test
    void duplicateOrEmptyIds_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> text.addItem("button", "Again"));
        assertThrows(IllegalArgumentException.class, () -> input.addItem("", "Empty"));
    }

    @Test
    void clickingAnItem_selectsItAndNotifiesListeners() {
        List<String> picked = new ArrayList<>();
        nav.onItemSelected(item -> picked.add(item.id()));

        click(nav.item("slider").node());

        assertEquals("slider", nav.selectedItem().id());
        assertTrue(nav.item("slider").selected());
        assertEquals(List.of("slider"), picked);
    }

    @Test
    void clickingACategoryHeader_togglesItAndKeepsTheSelection() {
        nav.selectItem("slider");
        assertTrue(input.expanded());

        click(input.node());

        assertFalse(input.expanded());
        assertEquals("slider", nav.selectedItem().id(), "header click must not clear the selected item");
        click(input.node());
        assertTrue(input.expanded());
        assertEquals("slider", nav.selectedItem().id());
    }

    @Test
    void aCategoryIsNeverSelectable() {
        nav.select(input.node());

        assertNull(nav.selectedItem());
        assertNull(nav.selectedNode());
    }

    @Test
    void selectItem_expandsACollapsedCategory() {
        assertFalse(text.expanded());

        nav.selectItem("textbox");

        assertTrue(text.expanded());
        assertEquals("textbox", nav.selectedItem().id());
    }

    @Test
    void selectItem_ignoresUnknownAndDisabledItems() {
        nav.selectItem("button");
        nav.item("slider").enabled(false);

        nav.selectItem("slider");
        nav.selectItem("nope");

        assertEquals("button", nav.selectedItem().id());
    }

    @Test
    void disablingTheSelectedItem_clearsTheSelection() {
        nav.selectItem("button");

        nav.item("button").enabled(false);

        assertNull(nav.selectedItem());
    }

    @Test
    void unsubscribe_stopsNotifications() {
        List<String> picked = new ArrayList<>();
        var subscription = nav.onItemSelected(item -> picked.add(item.id()));

        nav.selectItem("button");
        subscription.unsubscribe();
        nav.selectItem("slider");

        assertEquals(List.of("button"), picked);
    }

    @Test
    void titleChanges_reachTheTree() {
        nav.item("button").title("Push");
        input.title("Input");

        assertEquals("Push", nav.item("button").node().text());
        assertEquals("Input", input.node().text());
        assertEquals("button", nav.item("button").id(), "id stays stable when the title changes");
    }

    @Test
    void clear_removesEverything() {
        nav.selectItem("button");

        nav.clear();

        assertTrue(nav.categories().isEmpty());
        assertTrue(nav.items().isEmpty());
        assertNull(nav.selectedItem());
    }

    @Test
    void rendering_paintsCategoryPlatesAndTheSelection() {
        nav.selectItem("slider");
        layout();
        DrawList plain = new DrawList();
        new NavigationView().render(new DefaultRenderContext(plain));
        DrawList list = new DrawList();

        nav.render(new DefaultRenderContext(list));

        assertTrue(list.size() > plain.size(), "rows, plates and chevrons produce draw commands");
    }

    @Test
    void theViewCanBeSubclassed() {
        class Custom extends NavigationView {
        }
        Custom custom = new Custom();
        custom.addCategory("A").addItem("a", "A");

        assertEquals("a", custom.item("a").id());
    }
}
