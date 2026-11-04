package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphCompiler;
import dev.sixik.unigui.xaml.logic.GraphDefinition;
import dev.sixik.unigui.xaml.logic.GraphEdits;
import dev.sixik.unigui.xaml.logic.GraphFunction;
import dev.sixik.unigui.xaml.logic.GraphFunctions;
import dev.sixik.unigui.xaml.logic.NodeCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NodeCatalogTest {
    private static final String COMPONENT = """
            <Component x:Name="Cat" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Var Name="Seen" Type="int"/>
                <Event Name="Buy" Direction="Out" Args="id:string"/>
                <Event Name="Refresh" Direction="In" Args="n:int"/>
              </Component.Contract>
              <Label text="x"/>
            </Component>""";

    private static GraphDefinition.Node node(String type, Map<String, String> attributes) {
        return new GraphDefinition.Node("n", type, attributes, 0, 0, SourceLocation.UNKNOWN);
    }

    @Test
    void theCatalogFollowsTheContractAndTheRegisteredFunctions() throws Exception {
        ComponentDefinition definition = ComponentDefinition.parse("t", COMPONENT);
        try (AutoCloseable ignored = GraphFunctions.register(GraphFunction.define("cat.log")
                .param("text", ParamType.STRING).body(args -> null))) {
            List<String> types = NodeCatalog.entries(definition.contract()).stream()
                    .map(entry -> entry.type() + (entry.attributes().containsKey("Name")
                            ? ":" + entry.attributes().get("Name") : "")).toList();
            assertTrue(types.containsAll(List.of("Event.Construct", "Event.Refresh", "Raise.Buy", "Param.Get:Title",
                    "Param.Set:Title", "Var.Get:Seen", "Var.Set:Seen", "Call.cat.log", "Flow.Branch", "Math.Add")), types.toString());
            assertTrue(!types.contains("Event.Buy"), "an outgoing event has no entry node");
            assertTrue(!types.contains("Raise.Refresh"), "an incoming event cannot be raised");
        }
    }

    @Test
    void everyCatalogEntryCanBeAddedToTheGraph() throws Exception {
        try (AutoCloseable ignored = GraphFunctions.register(GraphFunction.define("cat.log")
                .param("text", ParamType.STRING).body(args -> null))) {
            EditorSession session = EditorSession.create(COMPONENT);
            ComponentDefinition definition = ComponentDefinition.parse("t", COMPONENT);
            int added = 0;
            for (NodeCatalog.Entry entry : NodeCatalog.entries(definition.contract())) {
                String id = GraphEdits.freshId(session.document().root(), entry.type());
                assertTrue(session.apply(GraphEdits.addNode(session.document().root(), id, entry.type(),
                        entry.attributes(), added * 10, 0)),
                        entry.type() + " " + entry.attributes() + ": "
                                + session.diagnostics(EditorSession.Channel.EDIT));
                added++;
            }
            assertEquals(added, GraphEdits.read(session.document().root()).nodes().size());
        }
    }

    @Test
    void aShapeListsPinsAndAttributes() {
        ComponentDefinition definition = ComponentDefinition.parse("t", COMPONENT);
        GraphCompiler.Shape set = GraphCompiler.shape(definition.contract(), GraphDefinition.empty(),
                node("Param.Set", Map.of("Name", "Title")));
        assertEquals(List.of("exec", "value", "then"), set.pins().stream().map(GraphCompiler.Pin::id).toList());
        assertTrue(set.pins().get(0).flow());
        assertTrue(set.pins().get(1).required());
        assertEquals("string", set.pins().get(1).type());
        assertEquals(java.util.Set.of("Name"), set.attributes());

        GraphCompiler.Shape entry = GraphCompiler.shape(definition.contract(), GraphDefinition.empty(),
                node("Event.Refresh", Map.of()));
        assertEquals(List.of("then", "n"), entry.pins().stream().map(GraphCompiler.Pin::id).toList(),
                "an entry exposes the event arguments as outputs");

        GraphCompiler.Shape sequence = GraphCompiler.shape(definition.contract(), GraphDefinition.empty(),
                node("Flow.Sequence", Map.of("Count", "3")));
        assertEquals(4, sequence.pins().size(), "the number of outputs follows Count");
    }

    @Test
    void aBrokenNodeHasNoShapeAndSaysWhy() {
        ComponentDefinition definition = ComponentDefinition.parse("t", COMPONENT);
        XamlLoadException unknown = assertThrows(XamlLoadException.class, () -> GraphCompiler.shape(
                definition.contract(), GraphDefinition.empty(), node("Nope.Type", Map.of())));
        assertTrue(unknown.getMessage().contains("Unknown node type"), unknown.getMessage());
        XamlLoadException missing = assertThrows(XamlLoadException.class, () -> GraphCompiler.shape(
                definition.contract(), GraphDefinition.empty(), node("Param.Get", Map.of("Name", "Gone"))));
        assertTrue(missing.getMessage().contains("Gone"), missing.getMessage());
    }

    @Test
    void titlesComeFromTheCatalog() {
        ComponentDefinition definition = ComponentDefinition.parse("t", COMPONENT);
        assertEquals("Set Title", NodeCatalog.titleOf(definition.contract(), "Param.Set", Map.of("Name", "Title")));
        assertEquals("Add", NodeCatalog.titleOf(definition.contract(), "Math.Add", Map.of()));
        assertEquals("Param.Set Gone", NodeCatalog.titleOf(definition.contract(), "Param.Set", Map.of("Name", "Gone")));
    }
}
