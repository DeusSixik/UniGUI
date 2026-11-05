package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditorContextTest {
    interface Marker {
    }

    record Ping(int n) implements Marker {
    }

    // ------------------------------------------------------------------ шина

    @Test
    void listenersReceiveTheEventClassItsInterfacesAndAreOrdered() {
        EditorEvents events = new EditorEvents();
        List<String> log = new ArrayList<>();
        events.on(Ping.class, ping -> log.add("class:" + ping.n()));
        events.on(Marker.class, marker -> log.add("interface"));
        events.on(String.class, text -> log.add("other"));

        events.post(new Ping(7));
        assertEquals(List.of("class:7", "interface"), log);
    }

    @Test
    void unsubscribingStopsDeliveryEvenInsideAHandler() {
        EditorEvents events = new EditorEvents();
        AtomicInteger calls = new AtomicInteger();
        EventSubscription[] self = new EventSubscription[1];
        self[0] = events.on(Ping.class, ping -> {
            calls.incrementAndGet();
            self[0].unsubscribe();
        });
        events.post(new Ping(1));
        events.post(new Ping(2));
        assertEquals(1, calls.get());
    }

    @Test
    void aListenerMaySubscribeWhileAnEventIsBeingDelivered() {
        EditorEvents events = new EditorEvents();
        AtomicInteger late = new AtomicInteger();
        events.on(Ping.class, ping -> events.on(Ping.class, again -> late.incrementAndGet()));
        events.post(new Ping(1));
        assertEquals(0, late.get(), "a listener added during delivery waits for the next event");
        events.post(new Ping(2));
        assertEquals(1, late.get());
    }

    // ------------------------------------------------------------------ выбор

    @Test
    void selectionAnnouncesChangesOnlyWhenTheContentDiffers() {
        EditorContext context = new EditorContext();
        List<EditorSelection.Changed> seen = new ArrayList<>();
        context.events().on(EditorSelection.Changed.class, seen::add);

        context.selection().set("a");
        context.selection().set("a");
        context.selection().set(List.of("a", 5));
        context.selection().clear();
        context.selection().clear();

        assertEquals(3, seen.size());
        assertEquals(List.of("a", 5), seen.get(1).items());
        assertTrue(context.selection().isEmpty());
    }

    @Test
    void selectionFindsTheFirstObjectOfAType() {
        EditorContext context = new EditorContext();
        context.selection().set(List.of("text", 42, 7));
        assertEquals(42, context.selection().first(Integer.class).orElseThrow());
        assertEquals("text", context.selection().first().orElseThrow());
        assertTrue(context.selection().first(Double.class).isEmpty());
        assertTrue(context.selection().contains(7));
        assertThrows(NullPointerException.class, () -> context.selection().set(java.util.Arrays.asList("a", null)));
    }

    // ------------------------------------------------------------------ документ

    @Test
    void switchingTheDocumentClearsTheSelectionAndAnnouncesIt() {
        EditorContext context = new EditorContext();
        List<Object> log = new ArrayList<>();
        context.events().on(EditorContext.DocumentChanged.class, log::add);
        XamlEditorDocument first = document("<VBox/>");
        XamlEditorDocument second = document("<HBox/>");

        context.document(first);
        context.selection().set("node");
        context.document(second);

        assertTrue(context.selection().isEmpty(), "selected objects belonged to the old document");
        assertEquals(2, log.size());
        EditorContext.DocumentChanged change = (EditorContext.DocumentChanged) log.get(1);
        assertEquals(first, change.previous());
        assertEquals(second, change.current());

        context.document(second); // повтор не шумит
        assertEquals(2, log.size());
        context.document(null);
        assertTrue(context.document().isEmpty());
    }

    @Test
    void editsOfTheCurrentDocumentAreForwardedAndOldOnesAreNot() {
        EditorContext context = new EditorContext();
        XamlEditorDocument first = document("<VBox/>");
        XamlEditorDocument second = document("<HBox/>");
        List<EditorContext.DocumentEdited> edited = new ArrayList<>();
        context.events().on(EditorContext.DocumentEdited.class, edited::add);

        context.document(first);
        first.session().apply(Edits.setProperty(first.session().document().root(), "id", "one"));
        assertEquals(1, edited.size());
        assertSame(first, edited.get(0).document());

        context.document(second);
        first.session().apply(Edits.setProperty(first.session().document().root(), "id", "two"));
        assertEquals(1, edited.size(), "a document that is no longer current stays quiet");
    }

    private static void assertSame(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }

    // ------------------------------------------------------------------ сервисы

    @Test
    void servicesAreProvidedOnceAndRequiredByType() {
        EditorContext context = new EditorContext();
        context.provide(String.class, "hello");
        assertEquals("hello", context.require(String.class));
        assertEquals("hello", context.service(String.class).orElseThrow());
        assertTrue(context.service(Integer.class).isEmpty());
        assertThrows(IllegalStateException.class, () -> context.require(Integer.class));
        assertThrows(IllegalArgumentException.class, () -> context.provide(String.class, "again"));
    }

    // ------------------------------------------------------------------ адаптер XAML

    @Test
    void xamlDocumentDelegatesUndoSaveAndDirtyToTheSession() {
        XamlEditorDocument document = document("<VBox id=\"a\"/>");
        assertFalse(document.dirty());
        assertFalse(document.canUndo());
        assertEquals("xaml", document.kind());

        document.session().apply(Edits.setProperty(document.session().document().root(), "id", "b"));
        assertTrue(document.dirty());
        assertTrue(document.canUndo());
        assertFalse(document.canRedo());
        assertTrue(document.undoLabel().isPresent());

        assertTrue(document.undo());
        assertTrue(document.canRedo());
        assertTrue(document.redo());
        assertTrue(document.save());
        assertFalse(document.dirty());
    }

    static XamlEditorDocument document(String xaml) {
        String id = "doc-" + xaml.hashCode();
        return new XamlEditorDocument(id, "Doc", EditorSession.open(DocumentSource.memory(id, "Doc", xaml)));
    }
}
