package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorShell;
import dev.sixik.unigui.editor.XamlEditorDocument;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentHost;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentEditorTest {
    private static final String SOURCE = """
            <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Event Name="Buy" Direction="Out" Args="id:string"/>
                <Event Name="Refresh" Direction="In"/>
              </Component.Contract>
              <VBox id="root">
                <Label id="title" text="{Param Title}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </VBox>
            </Component>""";

    private EditorSession session;
    private XamlEditorDocument document;
    private EditorShell shell;

    @BeforeEach
    void setUp() {
        session = EditorSession.open(DocumentSource.memory("card", "Card", SOURCE));
        document = new XamlEditorDocument("card", "Card", session);
        shell = ComponentEditor.shell(document);
    }

    // ------------------------------------------------------------------ вспомогательное

    private Widget content(String panel) {
        Widget widget = shell.dock().findPane(panel).content();
        applyAll(widget);
        return widget;
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private static Widget findId(Widget widget, String id) {
        if (id.equals(widget.id())) return widget;
        for (Widget child : widget.children()) {
            Widget found = findId(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private Widget contract(String id) {
        return findId(content(ComponentEditor.CONTRACT), id);
    }

    private TextField field(String id) {
        return (TextField) requireNonNull(contract(id), id);
    }

    private Button button(String id) {
        return (Button) requireNonNull(contract(id), id);
    }

    private static Widget requireNonNull(Widget widget, String id) {
        if (widget == null) throw new AssertionError("no widget '" + id + "' in the contract panel");
        return widget;
    }

    private static <T extends Widget> List<T> all(Widget root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Widget> void collect(Widget widget, Class<T> type, List<T> into) {
        if (type.isInstance(widget)) into.add(type.cast(widget));
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) collect(child, type, into);
    }

    private ViewportPanel viewport() {
        return (ViewportPanel) shell.panel(UiEditor.VIEWPORT).orElseThrow();
    }

    private Label previewLabel() {
        content(UiEditor.VIEWPORT);
        return (Label) findId(viewport().previewRoot(), "title");
    }

    private String contractProblem() {
        return all(content(ComponentEditor.CONTRACT), Label.class).stream()
                .map(Label::text).filter(text -> text.contains("integer")).findFirst().orElse("");
    }

    // ------------------------------------------------------------------ предпросмотр

    @Test
    void thePreviewShowsTheBodyWithTheDeclaredDefaults() {
        assertEquals("idle", previewLabel().text());
        assertTrue(viewport().previewRoot() instanceof ComponentHost, "a component document is previewed as a component");
    }

    @Test
    void clickingHierarchyNodesOfTheBodyMapsThemToPreviewWidgets() {
        content(UiEditor.VIEWPORT);
        UiNode label = session.document().root().children().get(0).children().get(0);
        session.select(label);
        assertSame(previewLabel(), viewport().selectedWidget());

        session.select(session.document().root());
        assertSame(viewport().previewRoot(), viewport().selectedWidget(), "the component root outlines the whole instance");
    }

    // ------------------------------------------------------------------ контракт

    @Test
    void thePanelListsTheDeclaredParametersAndEvents() {
        assertEquals("Title", field("param.Title.Type").id().isEmpty() ? "" : "Title");
        assertEquals("string", field("param.Title.Type").text());
        assertEquals("idle", field("param.Title.Default").text());
        assertEquals("id:string", field("event.Buy.Args").text());
        assertNotNull(contract("remove.Event.Refresh"));
    }

    @Test
    void addingAParameterWritesTheContractAndTheRowAppears() {
        button("add.Param").click();
        assertTrue(session.text().contains("Name=\"Param1\""), session.text());
        assertNotNull(contract("param.Param1.Type"), "the new row is shown");
        assertTrue(document.canUndo());

        document.undo();
        assertFalse(session.text().contains("Param1"), session.text());
        assertNull(contract("param.Param1.Type"), "the row follows the undo");
    }

    @Test
    void editingADefaultUpdatesThePreviewAndKeepsTheField() {
        TextField before = field("param.Title.Default");
        before.text("hello");
        assertSame(before, field("param.Title.Default"), "typing does not rebuild the field");
        assertEquals("hello", previewLabel().text());
    }

    @Test
    void aRejectedEditShowsTheReasonAndLeavesTheDocument() {
        String before = session.text();
        field("param.Title.Type").text("integer");
        assertEquals(before, session.text());
        assertFalse(contractProblem().isEmpty(), "the reason is shown under the table");
    }

    @Test
    void removingAReferencedParameterKeepsTheLastGoodPreviewAndReportsIt() {
        content(UiEditor.VIEWPORT);
        Widget before = viewport().previewRoot();
        button("remove.Param.Title").click();
        assertSame(before, viewport().previewRoot(), "the preview keeps the last good instance");
        assertTrue(all(content(UiEditor.VIEWPORT), Label.class).stream()
                .anyMatch(label -> label.text().startsWith("Preview:") && label.text().contains("Title")));
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).stream()
                .anyMatch(d -> d.message().contains("Title")), "the validator names the missing parameter");
    }

    @Test
    void theDirectionOfAnEventIsChosenFromAList() {
        ComboBox direction = (ComboBox) contract("event.Buy.Direction");
        direction.selectedIndex(1);
        assertTrue(session.text().contains("Direction=\"In\""), session.text());
    }

    // ------------------------------------------------------------------ примерные значения и события

    @Test
    void aSampleValueChangesThePreviewWithoutTouchingTheDocument() {
        String before = session.text();
        field("sample.Title").text("Sword");
        assertEquals("Sword", previewLabel().text());
        assertEquals(before, session.text(), "samples are preview state, not document state");
    }

    @Test
    void anIncomingEventCanBeSentAndAnOutgoingOneAppearsInTheLog() {
        content(UiEditor.VIEWPORT);
        ComponentHost host = (ComponentHost) viewport().previewRoot();
        List<Args> received = new ArrayList<>();
        host.instance().handle("Refresh", received::add);
        button("send.Refresh").click();
        assertEquals(1, received.size());

        host.instance().emit("Buy", Args.of("id", "sword"));
        assertTrue(all(content(ComponentEditor.CONTRACT), Label.class).stream()
                .anyMatch(label -> label.text().equals("Buy(id=sword)")), "the log shows the event");
    }

    // ------------------------------------------------------------------ палитра

    @Test
    void thePaletteInsertsIntoTheBodyWhenTheComponentRootIsSelected() {
        session.select(session.document().root());
        UiEdits.insert(session, session.schema().type("Slider").orElseThrow());
        UiNode body = session.document().root().children().get(0);
        assertEquals(3, body.children().size(), "the slider joins the body container, not the component root");
        assertEquals(1, session.document().root().children().size());
    }

    @Test
    void anEmptyComponentTakesTheFirstWidgetAsItsBody() {
        EditorSession bare = EditorSession.open(DocumentSource.memory("bare", "Bare", """
                <Component x:Name="Bare" xmlns:x="https://unigui.dev/xaml"/>"""));
        UiEdits.insert(bare, bare.schema().type("VBox").orElseThrow());
        assertEquals("VBox", bare.document().root().children().get(0).type().local());
    }
}
