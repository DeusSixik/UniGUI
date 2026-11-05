package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.ValueEditor;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGraphValuesTest {
    private NodeGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph().selectionMode(NodeGraphSelectionMode.MULTIPLE);
        graph.portTypes()
                .register(PortType.data("vec3", 0xFFF5D547))
                .register(PortType.data("string", 0xFFE38BD2))
                .register(PortType.data("number", 0xFF5CD6A9))
                .register(PortType.data("bool", 0xFFE05252))
                .register(PortType.data("color", 0xFFFFFFFF));
        graph.nodeTypes().register(NodeType.of("spawn", "Spawn", "Test", id -> NodeBuilder.node(id)
                .header("Spawn")
                .input("pos", "Position", "vec3", ValueEditors.vector(3, 1, 2, 3))
                .input("name", "Name", "string", ValueEditors.text("Bob"))
                .input("count", "Count", "number", ValueEditors.number(5, 0, 10))
                .input("flag", "Flag", "bool", ValueEditors.bool(false))
                .input("tint", "Tint", "color", ValueEditors.color(0xFF336699))
                .input("mode", "Mode", "string", ValueEditors.choice(List.of("a", "b", "c"), 1))
                .input("plain", "Plain", "number")
                .output("out", "Out", "vec3")
                .build()));
    }

    private static List<String> track(ValueEditor editor) {
        List<String> notifications = new ArrayList<>();
        editor.onChange(() -> notifications.add(editor.serialize()));
        return notifications;
    }

    // ---- редакторы ----

    @Test
    void text_roundTripAndNotifications() {
        ValueEditor editor = ValueEditors.text("hi");
        List<String> seen = track(editor);

        editor.value("hello");
        editor.value("hello");
        editor.silentValue("quiet");
        editor.deserialize("restored");

        assertEquals("restored", editor.value());
        assertEquals("restored", editor.serialize());
        assertEquals(List.of("hello"), seen, "one notification for the real change, none for no-op or silent writes");
    }

    @Test
    void number_isClampedAndSerializedAsDecimal() {
        ValueEditor editor = ValueEditors.number(5, 0, 10);

        editor.value(99.0);
        assertEquals(10.0, (Double) editor.value());
        editor.value(-3);
        assertEquals(0.0, (Double) editor.value());
        editor.value(2.5);
        assertEquals("2.5", editor.serialize());
        editor.deserialize("7.25");
        assertEquals(7.25, (Double) editor.value());
    }

    @Test
    void integer_roundsAndKeepsIntType() {
        ValueEditor editor = ValueEditors.integer(3, 0, 100);

        editor.value(41.6);

        assertEquals(42, editor.value());
        assertEquals("42", editor.serialize());
        editor.deserialize("17");
        assertEquals(17, editor.value());
    }

    @Test
    void bool_togglesAndParsesCaseInsensitively() {
        ValueEditor editor = ValueEditors.bool(false);
        List<String> seen = track(editor);

        editor.value(true);
        editor.deserialize("FALSE");

        assertEquals(false, editor.value());
        assertEquals("false", editor.serialize());
        assertEquals(List.of("true"), seen, "deserialize is silent");
    }

    @Test
    void color_serializesAsArgbHex() {
        ValueEditor editor = ValueEditors.color(0xFF336699);

        assertEquals("#FF336699", editor.serialize());
        editor.deserialize("#80FF0000");
        assertEquals(0x80FF0000, editor.value());
        assertEquals("#80FF0000", editor.serialize());
    }

    @Test
    void choice_storesTheOptionText() {
        ValueEditor editor = ValueEditors.choice(List.of("add", "sub", "mul"), 0);

        editor.value("mul");

        assertEquals("mul", editor.value());
        assertEquals("mul", editor.serialize());
        editor.deserialize("sub");
        assertEquals("sub", editor.value());
        editor.deserialize("nope");
        assertEquals("sub", editor.value(), "unknown option keeps the value");
        assertThrows(IllegalArgumentException.class, () -> editor.value("nope"));
    }

    @Test
    void vector_isDoubleArrayWithSpaceSeparatedForm() {
        ValueEditor editor = ValueEditors.vector(3, 1, 2);

        assertArrayEquals(new double[]{1, 2, 0}, (double[]) editor.value());
        editor.value(new double[]{4, 5.5, -6});

        assertEquals("4.0 5.5 -6.0", editor.serialize());
        editor.deserialize("7 8 9");
        assertArrayEquals(new double[]{7, 8, 9}, (double[]) editor.value());
        editor.deserialize("1 2");
        assertArrayEquals(new double[]{7, 8, 9}, (double[]) editor.value(), "wrong arity is ignored");
        assertThrows(IllegalArgumentException.class, () -> editor.value(new double[]{1, 2}));
    }

    @Test
    void editors_rejectWrongTypesAndMalformedText() {
        ValueEditor number = ValueEditors.number(1);
        ValueEditor bool = ValueEditors.bool(true);

        assertThrows(IllegalArgumentException.class, () -> number.value("abc"));
        assertThrows(IllegalArgumentException.class, () -> bool.value("yes"));
        number.deserialize("not a number");
        bool.deserialize("maybe");
        assertEquals(1.0, (Double) number.value());
        assertEquals(true, bool.value());
        assertThrows(IllegalArgumentException.class, () -> ValueEditors.vector(5));
        assertThrows(IllegalArgumentException.class, () -> ValueEditors.choice(List.of(), 0));
    }

    @Test
    void widgetEdits_notifyLikeUserTyping() {
        ValueEditor editor = ValueEditors.number(1);
        List<String> seen = track(editor);

        ((NumberField) editor.widget()).value(8.0);

        assertEquals(List.of("8.0"), seen);
        assertEquals(8.0, (Double) editor.value());
    }

    // ---- ноды ----

    @Test
    void item_exposesTypedValuesByPort() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);

        assertArrayEquals(new double[]{1, 2, 3}, (double[]) node.value("pos"));
        assertEquals("Bob", node.value("name"));
        assertEquals(5.0, (Double) node.value("count"));
        assertEquals(false, node.value("flag"));
        assertEquals(0xFF336699, node.value("tint"));
        assertEquals("b", node.value("mode"));
        assertNull(node.value("plain"), "an input without an editor has no value");
        assertNull(node.value("missing"));
        assertNotNull(node.editor("pos"));
        assertNull(node.editor("out"));
    }

    @Test
    void item_setValueNotifiesTheGraphOnce() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        List<String> events = new ArrayList<>();
        graph.onValueChanged(event -> events.add(event.itemId() + "." + event.portId() + "=" + event.serialized()));

        node.value("count", 9.0);
        node.value("count", 9.0);
        node.value("name", "Alice");

        assertEquals(List.of("n.count=9.0", "n.name=Alice"), events);
        assertThrows(IllegalArgumentException.class, () -> node.value("plain", 1.0));
        assertThrows(IllegalArgumentException.class, () -> node.value("missing", 1.0));
    }

    @Test
    void graphEvent_carriesTheTypedValue() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        List<Object> values = new ArrayList<>();
        graph.onValueChanged(event -> values.add(event.value()));
        // Дети контейнера добавляются в очередь и появляются при первом measure.
        graph.measure(new LayoutContext(900.0f, 700.0f));

        ((TextField) node.editor("name").widget()).text("typed");
        ((NumberField) ((dev.sixik.unigui.widgets.containers.HBox) node.editor("pos").widget())
                .children().get(1)).value(42.0);

        assertEquals("typed", values.get(0));
        assertArrayEquals(new double[]{1, 42, 3}, (double[]) values.get(1));
    }

    @Test
    void inputEditor_staysReadableWhileHiddenByAConnection() {
        NodeGraphItem source = graph.spawn("spawn", "src", 0.0f, 0.0f);
        NodeGraphItem target = graph.spawn("spawn", "dst", 400.0f, 0.0f);
        target.value("name", "keep me");

        graph.addConnection("w", "src", "out", "dst", "pos");

        assertFalse(target.hostedWidgets().contains(target.editor("pos").widget()));
        assertArrayEquals(new double[]{1, 2, 3}, (double[]) target.value("pos"));
        assertEquals("keep me", target.value("name"));
    }

    @Test
    void saveStateAndLoadState_roundTripEveryEditor() {
        NodeGraphItem source = graph.spawn("spawn", "a", 0.0f, 0.0f);
        source.value("pos", new double[]{9, 8, 7});
        source.value("name", "Zed");
        source.value("count", 3.0);
        source.value("flag", true);
        source.value("tint", 0x7F102030);
        source.value("mode", "c");

        Map<String, String> state = source.saveState();
        NodeGraphItem copy = graph.spawn("spawn", "b", 0.0f, 100.0f);
        copy.loadState(state);

        assertEquals("9.0 8.0 7.0", state.get("row:pos/value"));
        assertArrayEquals(new double[]{9, 8, 7}, (double[]) copy.value("pos"));
        assertEquals("Zed", copy.value("name"));
        assertEquals(3.0, (Double) copy.value("count"));
        assertEquals(true, copy.value("flag"));
        assertEquals(0x7F102030, copy.value("tint"));
        assertEquals("c", copy.value("mode"));
    }

    @Test
    void loadState_isSilentAndIgnoresUnknownKeys() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        List<String> events = new ArrayList<>();
        graph.onValueChanged(event -> events.add(event.portId()));

        node.loadState(Map.of("row:name/value", "loaded", "row:ghost/value", "x", "row:name/unknown", "y", "garbage", "z"));

        assertEquals("loaded", node.value("name"));
        assertTrue(events.isEmpty(), "restoring state must not fire value events");
        node.loadState(null);
        node.loadState(Map.of());
        assertEquals("loaded", node.value("name"));
    }

    @Test
    void contentWidgetNode_hasNoStateOrValues() {
        NodeGraphItem legacy = new NodeGraphItem("legacy", new dev.sixik.unigui.widgets.display.Label("x"), 0.0f, 0.0f);

        assertTrue(legacy.saveState().isEmpty());
        assertNull(legacy.value("anything"));
    }

    // ---- snapshot и буфер ----

    @Test
    void snapshot_restoresValuesWithoutEvents() {
        NodeGraphItem node = graph.spawn("spawn", "n", 10.0f, 20.0f);
        node.value("name", "Persisted");
        node.value("pos", new double[]{4, 5, 6});
        node.value("flag", true);
        NodeGraphSnapshot snapshot = graph.snapshot();
        assertEquals("Persisted", snapshot.items().get(0).state().get("row:name/value"));

        NodeGraph restored = new NodeGraph();
        restored.portTypes().register(PortType.data("vec3", 0xFFF5D547));
        restored.nodeTypes().register(graph.nodeTypes().get("spawn"));
        List<String> events = new ArrayList<>();
        restored.onValueChanged(event -> events.add(event.portId()));
        restored.restoreSnapshot(snapshot, null);

        NodeGraphItem back = restored.item("n");
        assertEquals("Persisted", back.value("name"));
        assertArrayEquals(new double[]{4, 5, 6}, (double[]) back.value("pos"));
        assertEquals(true, back.value("flag"));
        assertTrue(events.isEmpty());
    }

    @Test
    void snapshot_isAnIndependentCopyOfTheValues() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        node.value("name", "before");
        NodeGraphSnapshot snapshot = graph.snapshot();

        node.value("name", "after");

        assertEquals("before", snapshot.items().get(0).state().get("row:name/value"));
    }

    @Test
    void copyPaste_carriesValuesToTheClone() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        node.value("name", "Cloned");
        node.value("count", 8.0);
        node.value("pos", new double[]{0, 64, 0});
        graph.clearSelection();
        graph.selectItem(node, true);

        graph.copySelection();
        NodeGraphItem pasted = graph.paste(300.0f, 0.0f).get(0);

        assertEquals("Cloned", pasted.value("name"));
        assertEquals(8.0, (Double) pasted.value("count"));
        assertArrayEquals(new double[]{0, 64, 0}, (double[]) pasted.value("pos"));
        pasted.value("name", "Edited");
        assertEquals("Cloned", node.value("name"), "the clone owns independent editors");
    }

    @Test
    void duplicate_carriesValues() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        node.value("mode", "a");
        graph.clearSelection();
        graph.selectItem(node, true);

        NodeGraphItem duplicate = graph.duplicateSelection().get(0);

        assertEquals("a", duplicate.value("mode"));
    }

    @Test
    void inlineEditorLayout_fitsTheEditorWidthAndStaysFixed() {
        NodeGraphItem node = graph.spawn("spawn", "n", 0.0f, 0.0f);
        graph.measure(new LayoutContext(900.0f, 700.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));
        float width = node.arrangedWidth();
        float height = node.arrangedHeight();

        node.value("name", "x".repeat(300));
        node.value("pos", new double[]{123456.789, -98765.4321, 1.0e9});
        graph.measure(new LayoutContext(900.0f, 700.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 900.0f, 700.0f));

        assertEquals(width, node.arrangedWidth(), 1.0e-3f);
        assertEquals(height, node.arrangedHeight(), 1.0e-3f);
        assertTrue(node.arrangedWidth() >= 40.0f * 3 + 4.0f, "the vector row is wide enough for three fields");
    }
}
