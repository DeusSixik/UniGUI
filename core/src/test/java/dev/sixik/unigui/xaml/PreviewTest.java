package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.preview.HotReload;
import dev.sixik.unigui.xaml.preview.Preview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreviewTest {
    private static final String SOURCE =
            "<VBox id=\"root\" padding=\"10\" spacing=\"6\"><Label id=\"title\" text=\"Hello\"/>"
                    + "<Button id=\"ok\" text=\"OK\"/></VBox>";

    private static Preview preview() {
        return new Preview(Xaml.defaultSchema(), LoadOptions.DEFAULT);
    }

    private static UiDocument read(String xaml) {
        return XamlFormat.INSTANCE.read(xaml);
    }

    private static void layout(Widget root) {
        apply(root);
        root.measure(new LayoutContext(400.0f, 300.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));
        apply(root);
    }

    private static void apply(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) apply(child);
    }

    private static Widget find(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    // ------------------------------------------------------------------ показ и устойчивость

    @Test
    void showsADocumentAndMapsNodesToWidgets() {
        UiDocument document = read(SOURCE);
        try (Preview preview = preview()) {
            assertTrue(preview.show(document).shown());
            Widget root = preview.root().orElseThrow();
            UiNode label = document.root().children().get(0);
            assertSame(find(root, "title"), preview.widgetOf(label).orElseThrow());
            assertEquals("Hello", ((Label) preview.widgetOf(label).orElseThrow()).text());
            assertEquals("", preview.error());
        }
    }

    @Test
    void aBrokenDocumentKeepsTheLastGoodTree() {
        try (Preview preview = preview()) {
            preview.show(read(SOURCE));
            Widget before = preview.root().orElseThrow();

            Preview.Result result = preview.show(read("<VBox><NoSuchWidget/></VBox>"));
            assertFalse(result.shown());
            assertTrue(result.message().contains("NoSuchWidget"), result.message());
            assertSame(before, preview.root().orElseThrow(), "the previous tree stays");
            assertEquals(result.message(), preview.error());

            assertTrue(preview.show(read(SOURCE)).shown());
            assertEquals("", preview.error(), "a good document clears the error");
        }
    }

    @Test
    void unknownAttributesDoNotSpoilThePreview() {
        try (Preview preview = preview()) {
            assertTrue(preview.show(read("<VBox><Label textt=\"typo\"/></VBox>")).shown(),
                    "an unfinished attribute is tolerated while typing");
        }
    }

    @Test
    void replacingTheTreeReleasesTheOldBindings() {
        XamlBindingContext data = XamlBindingContext.empty();
        var name = data.mutable("name", String.class, "a");
        try (Preview preview = new Preview(Xaml.defaultSchema(), LoadOptions.DEFAULT.bindingContext(data))) {
            preview.show(read("<Label id=\"l\" text=\"{Binding name}\"/>"));
            Label first = (Label) preview.root().orElseThrow();
            assertEquals(1, preview.bindings().size());

            preview.show(read("<Label id=\"l\" text=\"static\"/>"));
            name.set("changed");
            assertEquals("a", first.text(), "the replaced tree no longer follows the source");
            assertTrue(preview.bindings().isEmpty());
        }
    }

    @Test
    void bindingStatusesExplainMissingSources() {
        try (Preview preview = preview()) {
            preview.show(read("<Label text=\"{Binding player.health}\"/>"));
            assertEquals(1, preview.bindings().size());
            assertTrue(preview.bindings().get(0).hasDiagnostics(), "the missing source is reported");
        }
    }

    // ------------------------------------------------------------------ выбор по точке

    @Test
    void pickReturnsTheDeepestNodeUnderThePoint() {
        UiDocument document = read(SOURCE);
        try (Preview preview = preview()) {
            preview.show(document);
            Widget root = preview.root().orElseThrow();
            layout(root);
            Widget ok = find(root, "ok");
            float x = ok.layoutBounds().x() + 2.0f;
            float y = ok.layoutBounds().y() + 2.0f;
            assertSame(document.root().children().get(1), preview.pick(document, x, y).orElseThrow());

            // правый нижний угол контейнера: там только сам контейнер
            float cornerX = root.layoutBounds().x() + root.layoutBounds().width() - 1.0f;
            float cornerY = root.layoutBounds().y() + root.layoutBounds().height() - 1.0f;
            assertSame(document.root(), preview.pick(document, cornerX, cornerY).orElseThrow());

            assertTrue(preview.pick(document, -50.0f, -50.0f).isEmpty());
        }
    }

    @Test
    void pickingARowSelectsTheTemplateThatProducedIt() {
        UiDocument document = read("""
                <VBox id="root">
                  <VirtualListView id="list" itemHeight="20" items="{Binding rows}">
                    <VirtualListView.ItemTemplate><Label text="{Item}"/></VirtualListView.ItemTemplate>
                  </VirtualListView>
                </VBox>""");
        XamlBindingContext data = XamlBindingContext.empty().list("rows", java.util.List.of("a", "b"));
        try (Preview preview = new Preview(Xaml.defaultSchema(), LoadOptions.DEFAULT.bindingContext(data))) {
            assertTrue(preview.show(document).shown());
            Widget root = preview.root().orElseThrow();
            layout(root);
            var list = find(root, "list");
            float x = list.layoutBounds().x() + 2.0f;
            float y = list.layoutBounds().y() + 2.0f;
            UiNode picked = preview.pick(document, x, y).orElseThrow();
            // Строка показывает данные, но правится шаблон: выбранным оказывается узел шаблона, а не строка.
            assertEquals("Label", picked.type().local());
            assertTrue(document.root().children().get(0).children().isEmpty(),
                    "the template node lives in the slot, not among the ordinary children");
        }
    }

    // ------------------------------------------------------------------ закрытие

    @Test
    void closingReleasesEverythingAndRefusesFurtherUse() {
        Preview preview = preview();
        preview.show(read(SOURCE));
        preview.close();
        assertTrue(preview.closed());
        assertTrue(preview.root().isEmpty());
        assertThrows(IllegalStateException.class, () -> preview.show(read(SOURCE)));
        preview.close(); // повторное закрытие безопасно
    }

    // ------------------------------------------------------------------ горячая перезагрузка

    @Test
    void hotReloadShowsTheSourceAndFollowsItsChanges() {
        DocumentSource source = DocumentSource.memory("d", "d.xaml", SOURCE);
        try (Preview preview = preview()) {
            HotReload reload = new HotReload(source, preview);
            assertEquals("Hello", ((Label) find(preview.root().orElseThrow(), "title")).text());
            assertTrue(reload.poll().isEmpty(), "nothing changed");

            source.writeText(SOURCE.replace("Hello", "Updated"));
            Preview.Result result = reload.poll().orElseThrow();
            assertTrue(result.shown());
            assertEquals("Updated", ((Label) find(preview.root().orElseThrow(), "title")).text());
            assertTrue(reload.poll().isEmpty());
        }
    }

    @Test
    void hotReloadSurvivesBrokenFilesAndRecovers() {
        DocumentSource source = DocumentSource.memory("d", "d.xaml", SOURCE);
        try (Preview preview = preview()) {
            HotReload reload = new HotReload(source, preview);
            Widget good = preview.root().orElseThrow();

            source.writeText("<VBox><Label");
            Preview.Result syntax = reload.poll().orElseThrow();
            assertFalse(syntax.shown());
            assertTrue(syntax.message().startsWith("Cannot parse"), syntax.message());
            assertSame(good, preview.root().orElseThrow());
            assertTrue(reload.poll().isEmpty(), "the same broken text is reported once");

            source.writeText("<VBox><NoSuchWidget/></VBox>");
            assertFalse(reload.poll().orElseThrow().shown());
            assertSame(good, preview.root().orElseThrow());

            source.writeText(SOURCE.replace("Hello", "Fixed"));
            assertTrue(reload.poll().orElseThrow().shown());
            assertEquals("Fixed", ((Label) find(preview.root().orElseThrow(), "title")).text());
        }
    }

    @Test
    void hotReloadReportsAnUnreadableSourceInsteadOfThrowing() {
        DocumentSource failing = new DocumentSource() {
            private int reads;

            @Override
            public String id() {
                return "x";
            }

            @Override
            public String title() {
                return "x.xaml";
            }

            @Override
            public String readText() {
                if (reads++ > 0) throw new IllegalStateException("locked");
                return SOURCE;
            }

            @Override
            public boolean writable() {
                return false;
            }

            @Override
            public void writeText(String text) {
                throw new UnsupportedOperationException();
            }
        };
        try (Preview preview = preview()) {
            HotReload reload = new HotReload(failing, preview);
            Widget before = preview.root().orElseThrow();
            Preview.Result result = reload.poll().orElseThrow();
            assertFalse(result.shown());
            assertTrue(result.message().contains("locked"), result.message());
            assertSame(before, preview.root().orElseThrow());
        }
    }
}
