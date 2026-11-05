package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentPreviewTest {
    private static final String CARD = """
            <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Event Name="Buy" Direction="Out" Args="id:string"/>
                <Event Name="Refresh" Direction="In"/>
              </Component.Contract>
              <VBox id="root" padding="4">
                <Label id="title" text="{Param Title}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </VBox>
            </Component>""";

    private static UiDocument read(String xaml) {
        return XamlFormat.INSTANCE.read(xaml);
    }

    private static ComponentPreview preview() {
        return new ComponentPreview(Xaml.defaultSchema(), LoadOptions.DEFAULT);
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

    @Test
    void showsTheBodyWithContractDefaultsAndMapsNodesToWidgets() {
        UiDocument document = read(CARD);
        try (ComponentPreview preview = preview()) {
            assertTrue(preview.show(document).shown(), preview.error());
            Widget root = preview.root().orElseThrow();
            Label title = (Label) find(root, "title");
            assertEquals("idle", title.text());

            UiNode body = document.root().children().get(0);
            assertSame(find(root, "root"), preview.widgetOf(body).orElseThrow());
            assertSame(title, preview.widgetOf(body.children().get(0)).orElseThrow());
        }
    }

    @Test
    void samplesUpdateTheShownInstanceAndSurviveARebuild() {
        try (ComponentPreview preview = preview()) {
            preview.show(read(CARD));
            preview.sample("Title", "Sword");
            assertEquals("Sword", ((Label) find(preview.root().orElseThrow(), "title")).text());

            preview.show(read(CARD.replace("padding=\"4\"", "padding=\"8\"")));
            assertEquals("Sword", ((Label) find(preview.root().orElseThrow(), "title")).text(),
                    "the sample value is applied to the rebuilt instance");

            preview.resetSamples();
            preview.show(read(CARD));
            assertEquals("idle", ((Label) find(preview.root().orElseThrow(), "title")).text());
        }
    }

    @Test
    void aSampleOfAParameterThatBecameAnotherTypeIsDroppedInsteadOfBreakingTheRebuild() {
        try (ComponentPreview preview = preview()) {
            preview.show(read(CARD));
            preview.sample("Title", "Sword");
            String retyped = CARD.replace("<Param Name=\"Title\" Type=\"string\" Default=\"idle\"/>",
                    "<Param Name=\"Title\" Type=\"int\" Default=\"3\"/>").replace("{Param Title}", "1");
            assertTrue(preview.show(read(retyped)).shown(), preview.error());
            assertFalse(preview.samples().containsKey("Title"));
        }
    }

    @Test
    void aBrokenDocumentKeepsTheLastGoodInstance() {
        try (ComponentPreview preview = preview()) {
            preview.show(read(CARD));
            Widget before = preview.root().orElseThrow();

            var result = preview.show(read(CARD.replace("{Param Title}", "{Param Missing}")));
            assertFalse(result.shown());
            assertTrue(preview.error().contains("Missing"), preview.error());
            assertSame(before, preview.root().orElseThrow());

            assertTrue(preview.show(read(CARD)).shown());
            assertEquals("", preview.error());
        }
    }

    @Test
    void outgoingEventsAreLoggedAndIncomingOnesCanBeSent() {
        try (ComponentPreview preview = preview()) {
            preview.show(read(CARD));
            preview.host().orElseThrow().instance().emit("Buy", Args.of("id", "sword"));
            assertEquals("Buy(id=sword)", preview.events().get(0));

            java.util.List<Args> received = new java.util.ArrayList<>();
            preview.host().orElseThrow().instance().handle("Refresh", received::add);
            preview.send("Refresh", Args.empty());
            assertEquals(1, received.size());

            preview.clearEvents();
            assertTrue(preview.events().isEmpty());
        }
    }

    @Test
    void pickFindsTheDeepestBodyNodeUnderThePoint() {
        UiDocument document = read(CARD);
        try (ComponentPreview preview = preview()) {
            preview.show(document);
            Widget root = preview.root().orElseThrow();
            layout(root);
            Widget buy = find(root, "buy");
            UiNode picked = preview.pick(document, buy.layoutBounds().x() + 2.0f, buy.layoutBounds().y() + 2.0f)
                    .orElseThrow();
            assertEquals("Button", picked.type().local());
            assertTrue(preview.pick(document, -50.0f, -50.0f).isEmpty());
        }
    }

    @Test
    void closingReleasesTheInstance() {
        ComponentPreview preview = preview();
        preview.show(read(CARD));
        var instance = preview.host().orElseThrow().instance();
        preview.close();
        assertTrue(preview.closed());
        assertTrue(instance.disposed());
        assertTrue(preview.root().isEmpty());
    }
}
