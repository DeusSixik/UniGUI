package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphEditsTest {
    private static final String COMPONENT = """
            <Component x:Name="EditedBox" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Param Name="Count" Type="int" Default="0"/>
                <Event Name="Go" Direction="In" Args="n:int"/>
              </Component.Contract>
              <Label id="label" text="x"/>
            </Component>""";

    private EditorSession session = EditorSession.create(COMPONENT);

    @AfterEach
    void cleanUp() {
        Components.unregister("ge:edited");
    }

    private UiNode root() {
        return session.document().root();
    }

    private boolean apply(dev.sixik.unigui.xaml.edit.Edit edit) {
        return session.apply(edit);
    }

    private GraphDefinition graph() {
        return GraphEdits.read(root());
    }

    private String problem() {
        return session.diagnostics(EditorSession.Channel.EDIT).get(0).message();
    }

    private void build() {
        assertTrue(apply(GraphEdits.addNode(root(), "in", "Event.Go", Map.of(), 0, 0)));
        assertTrue(apply(GraphEdits.addNode(root(), "set", "Param.Set", Map.of("Name", "Title"), 200, 0)));
        assertTrue(apply(GraphEdits.connect(root(), "in.then", "set.exec")));
    }

    // ------------------------------------------------------------------ сборка графа правками

    @Test
    void aGraphIsBuiltFromEditsAndEveryStepCanBeUndone() {
        build();
        assertEquals(2, graph().nodes().size());
        assertEquals(1, graph().wires().size());
        assertTrue(session.text().contains("Component.Graph"), session.text());
        assertTrue(session.text().contains("From=\"in.then\""), session.text());

        String full = session.text();
        assertTrue(session.undo());
        assertEquals(0, graph().wires().size());
        assertTrue(session.undo());
        assertTrue(session.undo());
        assertTrue(graph().isEmpty());
        assertFalse(session.text().contains("Component.Graph"), "undoing the first edit removes the graph element again");

        while (session.redo()) {
            // повтор до конца
        }
        assertEquals(full, session.text(), "redo restores exactly what was there");
    }

    @Test
    void theBuiltGraphRunsAfterTheDocumentIsRegistered() {
        build();
        assertTrue(apply(GraphEdits.setAttribute(root(), "set", "value", "done")));
        Components.register("ge:edited", session.text());
        ComponentInstance box = Xaml.component("ge:edited").create();
        box.send("Go", Args.of("n", 1));
        box.flush();
        assertEquals("done", box.get("Title"));
    }

    // ------------------------------------------------------------------ проверка правок

    @Test
    void editsThatBreakTheGraphAreRejectedAndLeaveTheDocumentAlone() {
        build();

        assertFalse(apply(GraphEdits.addNode(root(), "bad", "Nope.Thing", Map.of(), 0, 0)));
        assertTrue(problem().contains("Unknown node type 'Nope.Thing'"), problem());

        assertFalse(apply(GraphEdits.addNode(root(), "in", "Event.Go", Map.of(), 0, 0)));
        assertTrue(problem().contains("already exists"), problem());

        assertFalse(apply(GraphEdits.connect(root(), "in.then", "set.nothing")));
        assertTrue(problem().contains("no input pin 'nothing'"), problem());

        assertTrue(apply(GraphEdits.addNode(root(), "add", "Math.Add", Map.of(), 0, 0)));
        assertFalse(apply(GraphEdits.connect(root(), "in.then", "add.a")));
        assertTrue(problem().contains("execution pin with a data pin"), problem());

        assertFalse(apply(GraphEdits.removeNode(root(), "ghost")));
        assertTrue(problem().contains("No graph node 'ghost'"), problem());

        assertFalse(apply(GraphEdits.setAttribute(root(), "set", "Type", "Param.Get")));
        assertTrue(problem().contains("cannot be changed"), problem());

        assertFalse(apply(GraphEdits.setAttribute(root(), "set", "Name", "Missing")));
        assertTrue(problem().contains("parameter 'Missing'"), problem());

        assertFalse(apply(GraphEdits.disconnect(root(), "in.then", "set.value")));
        assertTrue(problem().contains("No wire"), problem());

        // От всех неудачных правок в документе не осталось ничего, кроме удачно добавленного Math.Add.
        assertEquals(3, graph().nodes().size());
        assertTrue(session.text().contains("Math.Add"));
        assertTrue(session.text().contains("Name=\"Title\""), "the set node kept its settings");
    }

    @Test
    void typeMismatchesBetweenDataPinsAreRejected() {
        build();
        assertTrue(apply(GraphEdits.addNode(root(), "flag", "Logic.Not", Map.of(), 0, 0)));
        assertTrue(apply(GraphEdits.addNode(root(), "cat", "Text.Concat", Map.of(), 0, 0)));
        // string -> bool входит в число допустимых? Нет: bool-пин принимает только bool.
        assertFalse(apply(GraphEdits.connect(root(), "cat.result", "flag.value")));
        assertTrue(problem().contains("joins string to bool"), problem());
    }

    // ------------------------------------------------------------------ провода

    @Test
    void connectingToAnOccupiedDataInputReplacesTheOldWireAsOneStep() {
        build();
        assertTrue(apply(GraphEdits.addNode(root(), "a", "Value.Literal", Map.of("Value", "one"), 0, 0)));
        assertTrue(apply(GraphEdits.addNode(root(), "b", "Value.Literal", Map.of("Value", "two"), 0, 0)));
        assertTrue(apply(GraphEdits.connect(root(), "a.value", "set.value")));
        assertEquals(2, graph().wires().size());

        assertTrue(apply(GraphEdits.connect(root(), "b.value", "set.value")));
        assertEquals(2, graph().wires().size(), "the old data wire left when the new one arrived");
        assertEquals("b.value", graph().wires().get(1).fromNode() + "." + graph().wires().get(1).fromPort());

        assertTrue(session.undo(), "one step undoes both the removal and the new wire");
        assertEquals("a", graph().wires().get(1).fromNode());
    }

    @Test
    void anExecutionInputAcceptsSeveralWires() {
        build();
        assertTrue(apply(GraphEdits.addNode(root(), "other", "Event.Go", Map.of(), 0, 50)));
        // у документа два входа с одним событием не нужны, но проводов в exec-вход может быть несколько
        assertFalse(apply(GraphEdits.addNode(root(), "in", "Event.Go", Map.of(), 0, 0)));
        assertTrue(apply(GraphEdits.connect(root(), "other.then", "set.exec")));
        assertEquals(2, graph().wires().size());
    }

    @Test
    void disconnectRemovesOnlyThatWire() {
        build();
        assertTrue(apply(GraphEdits.disconnect(root(), "in.then", "set.exec")));
        assertEquals(0, graph().wires().size());
        assertEquals(2, graph().nodes().size());
        assertTrue(session.undo());
        assertEquals(1, graph().wires().size());
    }

    @Test
    void removingANodeTakesItsWiresAlongAndUndoBringsThemBack() {
        build();
        assertTrue(apply(GraphEdits.removeNode(root(), "set")));
        assertEquals(1, graph().nodes().size());
        assertEquals(0, graph().wires().size(), "the wire into the removed node is gone");
        assertTrue(session.undo());
        assertEquals(2, graph().nodes().size());
        assertEquals(1, graph().wires().size());
    }

    // ------------------------------------------------------------------ атрибуты и положение

    @Test
    void attributesAreSetAndCleared() {
        build();
        assertTrue(apply(GraphEdits.setAttribute(root(), "set", "value", "hello")));
        assertEquals("hello", graph().nodes().get(1).attributes().get("value"));
        assertTrue(apply(GraphEdits.setAttribute(root(), "set", "value", null)));
        assertFalse(graph().nodes().get(1).attributes().containsKey("value"));
        assertTrue(session.undo());
        assertEquals("hello", graph().nodes().get(1).attributes().get("value"));
    }

    @Test
    void movingANodeMergesIntoOneUndoStepAndIgnoresIncompleteGraphs() {
        build();
        int undoBefore = session.undoCount();
        for (int i = 1; i <= 10; i++) {
            assertTrue(apply(GraphEdits.moveNode(root(), "set", 200 + i * 10, 40)));
        }
        assertEquals(undoBefore + 1, session.undoCount(), "a drag is a single step");
        assertEquals(300.0f, graph().nodes().get(1).x());
        assertEquals(40.0f, graph().nodes().get(1).y());

        assertTrue(session.undo());
        assertEquals(200.0f, graph().nodes().get(1).x(), "undo returns to where the drag started");
    }

    @Test
    void anIncompleteGraphIsAllowedWhileEditingButNotWhenRunning() {
        build(); // Param.Set без значения: обязательный вход не подключён
        ComponentDefinition definition = ComponentDefinition.parse("ge:x", session.text());
        XamlLoadException failure = assertThrows(XamlLoadException.class, definition::program);
        assertTrue(failure.getMessage().contains("Required input"), failure.getMessage());
    }

    // ------------------------------------------------------------------ вспомогательное

    @Test
    void freshIdsAreUnique() {
        assertEquals("Math_Add_1", GraphEdits.freshId(root(), "Math.Add"));
        assertTrue(apply(GraphEdits.addNode(root(), "Math_Add_1", "Math.Add", Map.of(), 0, 0)));
        assertEquals("Math_Add_2", GraphEdits.freshId(root(), "Math.Add"));
    }
}
