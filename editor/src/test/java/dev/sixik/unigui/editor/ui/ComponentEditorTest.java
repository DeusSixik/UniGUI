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

    /** Поиск виджета там, где живёт всё, что относится к контракту: список, инспектор и панель запуска. */
    private Widget contract(String id) {
        for (String panel : List.of(ComponentEditor.CONTRACT, UiEditor.INSPECTOR, ComponentEditor.RUN)) {
            Widget found = findId(content(panel), id);
            if (found != null) return found;
        }
        return null;
    }

    /** Выбирает запись: её детали появляются в инспекторе. */
    private void select(String kind, String name) {
        button("entry." + kind + "." + name).click();
    }

    private TextField field(String id) {
        return (TextField) requireNonNull(contract(id), id);
    }

    /** Выбирает пункт выпадающего списка панели контракта. */
    private void choose(String id, String item) {
        ComboBox combo = (ComboBox) requireNonNull(contract(id), id);
        combo.selectedIndex(combo.items().indexOf(item));
    }

    /** Правит имя на месте и применяет его, как по Enter. */
    private void commitField(String id, String next) {
        dev.sixik.unigui.editor.style.FlatField name = (dev.sixik.unigui.editor.style.FlatField) requireNonNull(contract(id), id);
        name.text(next);
        name.commit();
    }

    /** Переименование через меню строки; новая запись уже стоит в режиме переименования. */
    private void rename(String kind, String name, String next) {
        if (contract("name." + kind + "." + name) == null) chooseFromMenu(kind, name, "Rename");
        commitField("name." + kind + "." + name, next);
    }

    private Button button(String id) {
        return (Button) requireNonNull(contract(id), id);
    }

    /** Выбирает пункт меню, которое открывается правой кнопкой по записи контракта. */
    private void chooseFromMenu(String kind, String name, String item) {
        content(ComponentEditor.CONTRACT);
        ContractPanel panel = (ContractPanel) shell.panel(ComponentEditor.CONTRACT).orElseThrow();
        panel.menuOf(new ComponentView.Entry(kind, name)).stream().filter(entry -> item.equals(entry.title())).findFirst()
                .orElseThrow(() -> new AssertionError("no menu item '" + item + "'")).action().run();
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
                .map(Label::text).filter(text -> text.contains("already") || text.contains("declared")
                        || text.contains("duplicate") || text.contains("Duplicate")).findFirst().orElse("");
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
        select("Param", "Title");
        assertEquals("string", ((ComboBox) contract("param.Title.Type")).selectedItem());
        assertEquals("idle", field("param.Title.Default").text());
        select("Event", "Buy");
        assertEquals("id", field("arg.Buy.id.name").text());
        assertEquals("string", ((ComboBox) contract("arg.Buy.id.type")).selectedItem());
        assertNotNull(contract("entry.Event.Refresh"));
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
        select("Param", "Title");
        TextField before = field("param.Title.Default");
        before.text("hello");
        assertSame(before, field("param.Title.Default"), "typing does not rebuild the field");
        assertEquals("hello", previewLabel().text());
    }

    @Test
    void aRejectedEditShowsTheReasonAndLeavesTheDocument() {
        rename("Param", "Title", "Buy");
        assertTrue(session.text().contains("<Param Name=\"Title\""), session.text());
        assertTrue(session.text().contains("{Param Title}"), session.text());
        assertFalse(contractProblem().isEmpty(), "the reason is shown under the table");
        assertNotNull(contract("entry.Param.Title"), "the name goes back");
    }

    @Test
    void removingAReferencedParameterKeepsTheLastGoodPreviewAndReportsIt() {
        content(UiEditor.VIEWPORT);
        Widget before = viewport().previewRoot();
        chooseFromMenu("Param", "Title", "Delete");
        assertSame(before, viewport().previewRoot(), "the preview keeps the last good instance");
        assertTrue(all(content(UiEditor.VIEWPORT), Label.class).stream()
                .anyMatch(label -> label.text().startsWith("Preview:") && label.text().contains("Title")));
        assertTrue(session.diagnostics(EditorSession.Channel.VALIDATION).stream()
                .anyMatch(d -> d.message().contains("Title")), "the validator names the missing parameter");
    }

    @Test
    void anEventMovesBetweenIncomingAndOutgoingInTheInspector() {
        select("Event", "Buy");
        choose("event.Buy.Direction", "Incoming");
        assertTrue(session.text().contains("Name=\"Buy\" Direction=\"In\""), session.text());
        assertNotNull(contract("event.Buy.Coalesce"), "an incoming event offers the per-frame option");
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
        viewport().playing(true);
        ComponentHost host = (ComponentHost) viewport().playRoot();
        List<Args> received = new ArrayList<>();
        host.instance().handle("Refresh", received::add);
        button("send.Refresh").click();
        assertEquals(1, received.size());

        host.instance().emit("Buy", Args.of("id", "sword"));
        assertTrue(all(content(ComponentEditor.RUN), Label.class).stream()
                .anyMatch(label -> label.text().equals("Buy(id=sword)")), "the log shows the event");
    }

    // ------------------------------------------------------------------ типы, имена и запуск

    @Test
    void theTypeIsPickedFromTheRegisteredTypes() {
        select("Param", "Title");
        ComboBox type = (ComboBox) contract("param.Title.Type");
        assertTrue(type.items().containsAll(List.of("string", "int", "float", "bool", "color")), type.items().toString());
        choose("param.Title.Type", "int");
        assertTrue(session.text().contains("Name=\"Title\" Type=\"int\""), session.text());
        assertFalse(session.text().contains("Default=\"idle\""), "a default that does not fit the new type is dropped");
    }

    @Test
    void aNameIsEditedInPlaceAndReferencesFollow() {
        rename("Param", "Title", "Caption");
        assertTrue(session.text().contains("{Param Caption}"), session.text());
        assertNotNull(contract("entry.Param.Caption"), "the panel shows the new name");
        document.undo();
        assertTrue(session.text().contains("{Param Title}"), "a rename is one undo step");

        rename("Event", "Buy", "Purchase");
        assertTrue(session.text().contains("Name=\"Purchase\""), session.text());
    }

    @Test
    void entriesAreGroupedAndTheirDetailsLiveInTheInspector() {
        for (String group : List.of("GRAPHS", "PARAMETERS", "VARIABLES", "INCOMING EVENTS", "OUTGOING EVENTS", "CUSTOM EVENTS")) {
            assertNotNull(findId(content(ComponentEditor.CONTRACT), "group." + group), group);
        }
        assertNull(findId(content(ComponentEditor.CONTRACT), "param.Title.Type"), "the list only shows names and types");
        select("Event", "Refresh");
        assertNotNull(findId(content(UiEditor.INSPECTOR), "event.Refresh.Coalesce"));
        select("Event", "Buy");
        assertNull(contract("event.Buy.Coalesce"), "the option only applies to incoming events");
    }

    @Test
    void aGroupCollapsesAndSearchNarrowsTheList() {
        button("group.PARAMETERS").click();
        assertNull(contract("entry.Param.Title"));
        button("group.PARAMETERS").click();
        assertNotNull(contract("entry.Param.Title"));
        field("contract.search").text("ref");
        assertNull(contract("entry.Param.Title"));
        assertNotNull(contract("entry.Event.Refresh"));
    }

    @Test
    void selectingAWidgetReplacesTheEntryInTheInspector() {
        select("Param", "Title");
        assertNotNull(contract("param.Title.Type"));
        session.select(session.document().root().children().get(0));
        assertNull(contract("param.Title.Type"));
    }

    @Test
    void aNewEntryIsSelectedAndReadyToBeNamed() {
        button("add.Var").click();
        assertNotNull(contract("name.Var.Var1"), "the row turns into a name field");
        assertNotNull(contract("var.Var1.Type"), "the inspector shows it");
    }

    @Test
    void theCoalesceOptionIsACheckbox() {
        select("Event", "Refresh");
        ((dev.sixik.unigui.widgets.interaction.Checkbox) contract("event.Refresh.Coalesce")).checked(true);
        assertTrue(session.text().contains("Coalesce=\"true\""), session.text());
    }

    @Test
    void argumentsAreAddedRenamedAndRemovedFromTheList() {
        select("Event", "Buy");
        button("arg.add.Buy").click();
        assertTrue(session.text().contains("id:string, arg1:string"), session.text());
        commitField("arg.Buy.arg1.name", "count");
        choose("arg.Buy.count.type", "int");
        assertTrue(session.text().contains("id:string, count:int"), session.text());
        button("arg.Buy.count.remove").click();
        assertTrue(session.text().contains("Args=\"id:string\""), session.text());
    }

    @Test
    void customEventsAreCreatedAndRenamedInThePanel() {
        button("add.Custom").click();
        assertTrue(session.text().contains("Type=\"Event.Custom\""), session.text());
        rename(ComponentView.CUSTOM, "CustomEvent1", "Ready");
        assertTrue(session.text().contains("Name=\"Ready\""), session.text());
        assertNotNull(contract("arg.add.Ready"), "a custom event has arguments too");
    }

    @Test
    void sendingDoesNothingOutsidePlayMode() {
        content(UiEditor.VIEWPORT);
        ComponentHost preview = (ComponentHost) viewport().previewRoot();
        List<Args> received = new ArrayList<>();
        preview.instance().handle("Refresh", received::add);
        button("send.Refresh").click();
        assertTrue(received.isEmpty(), "the preview stays a blueprint");
    }

    @Test
    void playModeEventsNeverTouchThePreview() {
        content(UiEditor.VIEWPORT);
        ComponentHost preview = (ComponentHost) viewport().previewRoot();
        List<Args> onPreview = new ArrayList<>();
        preview.instance().handle("Refresh", onPreview::add);

        viewport().playing(true);
        ComponentHost running = (ComponentHost) viewport().playRoot();
        assertTrue(running != preview, "Play runs its own instance");
        List<Args> onCopy = new ArrayList<>();
        running.instance().handle("Refresh", onCopy::add);
        button("send.Refresh").click();
        assertEquals(1, onCopy.size());
        assertTrue(onPreview.isEmpty());

        viewport().playing(false);
        button("send.Refresh").click();
        assertEquals(1, onCopy.size(), "stopped means stopped");
    }

    @Test
    void previewValuesCarryIntoPlay() {
        field("sample.Title").text("Sword");
        content(UiEditor.VIEWPORT);
        viewport().playing(true);
        assertEquals("Sword", ((ComponentHost) viewport().playRoot()).instance().get("Title"));
    }

    /** Кадр как на экране: tick, затем раскладка; область прокрутки не тикает то, что лежит вне её видимой части. */
    private void frame() {
        Widget root = shell.root();
        for (int i = 0; i < 2; i++) {
            root.tick(new dev.sixik.unigui.api.core.FrameContext(i, 0.016f, 0.0f,
                    dev.sixik.unigui.api.core.FramePhase.ANIMATION));
            root.measure(new dev.sixik.unigui.api.layout.LayoutContext(1280.0f, 720.0f));
            root.arrange(new dev.sixik.unigui.api.math.MutableRect(0.0f, 0.0f, 1280.0f, 720.0f));
            applyAll(root);
        }
        root.tick(new dev.sixik.unigui.api.core.FrameContext(2L, 0.016f, 0.0f,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
    }

    /** Отрисовка листовых виджетов кэшируется, поэтому доступность обязана обновляться на кадре, а не в paint. */
    @Test
    void sendBecomesAvailableOnTheFrameAfterPlayStarts() {
        content(UiEditor.VIEWPORT);
        Button send = button("send.Refresh");
        shell.dock().selectPane(ComponentEditor.RUN);
        applyAll(shell.root());
        frame();
        assertFalse(send.enabled(), "nothing is running");

        viewport().playing(true);
        applyAll(shell.root());
        frame();
        assertTrue(send.enabled(), "Play makes it clickable without any repaint");

        viewport().playing(false);
        frame();
        assertFalse(send.enabled());
    }

    @Test
    void playBringsTheRunPanelForward() {
        content(UiEditor.VIEWPORT);
        viewport().playing(true);
        assertEquals(ComponentEditor.RUN, shell.dock().findPane(ComponentEditor.RUN).id());
        assertTrue(shell.isOpen(ComponentEditor.RUN));
    }

    @Test
    void anUndoButtonFollowsTheHistoryWithoutARepaint() {
        var undo = (dev.sixik.unigui.editor.style.ToolButton) findId(shell.root(), "toolbar.edit.undo");
        applyAll(shell.root());
        frame();
        assertFalse(undo.enabled());
        session.apply(dev.sixik.unigui.xaml.edit.Edits.setProperty(session.document().root().children().get(0)
                .children().get(0), "id", "renamed"));
        frame();
        assertTrue(undo.enabled());
    }

    // ------------------------------------------------------------------ перетаскивание записей

    private GraphPanel graphPanel() {
        content(ComponentEditor.GRAPH);
        return (GraphPanel) shell.panel(ComponentEditor.GRAPH).orElseThrow();
    }

    private void drop(ComponentView.Entry entry, String item) {
        graphPanel().dropMenu(entry, 40.0f, 60.0f).stream().filter(option -> item.equals(option.title())).findFirst()
                .orElseThrow(() -> new AssertionError("no drop option " + item)).action().run();
    }

    @Test
    void droppingAParameterOnTheGraphOffersGetAndSet() {
        assertEquals(List.of("Get Title", "Set Title"), graphPanel().dropMenu(new ComponentView.Entry("Param", "Title"), 0, 0)
                .stream().map(dev.sixik.unigui.editor.ContextMenus.Entry::title).toList());
        drop(new ComponentView.Entry("Param", "Title"), "Get Title");
        assertTrue(session.text().contains("Type=\"Param.Get\" Name=\"Title\" X=\"40\" Y=\"60\""), session.text());
        assertEquals(1, graphPanel().view().items().size(), "the node is on the canvas");
    }

    @Test
    void droppingAnEventOnTheGraphOffersOnAndRaise() {
        assertEquals(List.of("On Buy", "Raise Buy"), graphPanel().dropMenu(new ComponentView.Entry("Event", "Buy"), 0, 0)
                .stream().map(dev.sixik.unigui.editor.ContextMenus.Entry::title).toList());
        drop(new ComponentView.Entry("Event", "Buy"), "Raise Buy");
        assertTrue(session.text().contains("Type=\"Raise.Buy\""), session.text());
    }

    @Test
    void droppingOnAPropertyBindsIt() {
        UiNode button = session.document().root().children().get(0).children().get(1);
        session.select(button);
        assertTrue(inspector().dropped(new ComponentView.Entry("Param", "Title"), "text"));
        assertTrue(session.text().contains("text=\"{Param Title}\""), session.text());
        assertNotNull(findId(content(UiEditor.INSPECTOR), "inspector.bound.text"));
    }

    @Test
    void droppingAnEventOnAnActionRaisesIt() {
        UiNode button = session.document().root().children().get(0).children().get(1);
        session.select(button);
        assertTrue(inspector().dropped(new ComponentView.Entry("Event", "Buy"), "onClick"));
        assertTrue(session.text().contains("onClick=\"{Raise Buy}\""), session.text());
        assertFalse(inspector().dropped(new ComponentView.Entry("Event", "Refresh"), "onClick"), "only outgoing events");
        assertFalse(inspector().dropped(new ComponentView.Entry("Event", "Buy"), "text"), "an event is not a value");
    }

    // ------------------------------------------------------------------ свойства как значения контракта

    private InspectorPanel inspector() {
        content(UiEditor.INSPECTOR);
        return (InspectorPanel) shell.panel(UiEditor.INSPECTOR).orElseThrow();
    }

    @Test
    void anInspectorRowOffersToPromoteItsProperty() {
        UiNode button = session.document().root().children().get(0).children().get(1);
        session.select(button);
        assertTrue(inspector().menuOf("text").containsAll(List.of("Promote to Variable", "Promote to Parameter")),
                inspector().menuOf("text").toString());
        assertFalse(inspector().menuOf("id").contains("Promote to Variable"));
    }

    @Test
    void promotingAPropertyAddsItToTheContractAndLinksTheRow() {
        UiNode button = session.document().root().children().get(0).children().get(1);
        session.select(button);
        assertTrue(inspector().promote("text", dev.sixik.unigui.xaml.component.Promotions.Kind.VARIABLE));
        assertNotNull(contract("entry.Var.BuyText"), "the new variable is listed");
        assertNotNull(findId(content(UiEditor.INSPECTOR), "inspector.bound.text"), "the row links to it");
        assertTrue(session.text().contains("{Var BuyText}"), session.text());
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

    @Test
    void aDefaultIsNormalizedByTheTypeOfItsParameter() {
        button("add.Param").click();
        choose("param.Param1.Type", "bool");
        ((dev.sixik.unigui.widgets.interaction.Checkbox) contract("param.Param1.Default")).checked(true);
        assertTrue(session.text().contains("Default=\"true\""), session.text());

        button("add.Param").click();
        choose("param.Param2.Type", "int");
        field("param.Param2.Default").text("1.5");
        assertFalse(session.text().contains("Default=\"1.5\""), "a fractional default for an int is refused");
        assertTrue(all(content(UiEditor.INSPECTOR), Label.class).stream()
                .anyMatch(label -> label.text().contains("whole number")), "the reason is shown");
    }
}
