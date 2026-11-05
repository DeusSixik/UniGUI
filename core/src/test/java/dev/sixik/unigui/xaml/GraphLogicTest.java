package dev.sixik.unigui.xaml;

import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.logic.GraphFunction;
import dev.sixik.unigui.xaml.logic.GraphFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphLogicTest {
    private final List<String> components = new ArrayList<>();
    private final List<String> functions = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(Components::unregister);
        functions.forEach(GraphFunctions::unregister);
    }

    private void register(String id, String source) {
        Components.register(id, source);
        components.add(id);
    }

    private void function(GraphFunction function) {
        GraphFunctions.register(function);
        functions.add(function.name());
    }

    private static String component(String name, String contract, String graph) {
        return "<Component x:Name=\"" + name + "\" xmlns:x=\"https://unigui.dev/xaml\">\n"
                + "<Component.Contract>\n" + contract + "\n</Component.Contract>\n"
                + "<Label id=\"label\" text=\"body\"/>\n"
                + "<Component.Graph>\n" + graph + "\n</Component.Graph>\n</Component>";
    }

    private static final String COUNTER_CONTRACT = """
            <Param Name="Count" Type="int" Default="0"/>
            <Param Name="Title" Type="string" Default="Start"/>
            <Event Name="Add" Direction="In" Args="amount:int"/>
            <Event Name="Done" Direction="Out" Args="total:int"/>""";

    private static final String COUNTER_GRAPH = """
            <Node Id="onAdd" Type="Event.Add"/>
            <Node Id="get" Type="Param.Get" Name="Count"/>
            <Node Id="sum" Type="Math.Add"/>
            <Node Id="set" Type="Param.Set" Name="Count"/>
            <Node Id="after" Type="Param.Get" Name="Count"/>
            <Node Id="raise" Type="Raise.Done"/>
            <Wire From="onAdd.then" To="set.exec"/>
            <Wire From="get.value" To="sum.a"/>
            <Wire From="onAdd.amount" To="sum.b"/>
            <Wire From="sum.result" To="set.value"/>
            <Wire From="set.then" To="raise.exec"/>
            <Wire From="after.value" To="raise.total"/>""";

    // ------------------------------------------------------------------ исполнение

    @Test
    void anIncomingEventRunsTheGraphAndChangesParametersAndRaisesEvents() {
        register("g:counter", component("CounterBox", COUNTER_CONTRACT, COUNTER_GRAPH));
        ComponentInstance counter = Xaml.component("g:counter").create();
        List<Object> done = new ArrayList<>();
        counter.onEvent("Done", args -> done.add(args.get("total")));

        counter.send("Add", Args.of("amount", 5));
        assertEquals(0, counter.get("Count"), "the graph runs when the frame flushes the queue");
        counter.flush();
        assertEquals(5, counter.get("Count"));

        counter.send("Add", Args.of("amount", 3));
        counter.flush();
        assertEquals(8, counter.get("Count"));
        assertEquals(List.of(5, 8), done, "each run raised the outgoing event with the new total");
    }

    /**
     * Чистые ноды перевычисляются при каждом обращении потребителя, как в Blueprint: после Param.Set значение,
     * прочитанное чистой нодой, уже новое. Поэтому итог берётся вторым Param.Get после записи.
     */
    @Test
    void pureNodesSeeTheStateAtTheMomentTheirConsumerRuns() {
        register("g:pure", component("PureBox", COUNTER_CONTRACT, """
                <Node Id="onAdd" Type="Event.Add"/>
                <Node Id="get" Type="Param.Get" Name="Count"/>
                <Node Id="sum" Type="Math.Add"/>
                <Node Id="set" Type="Param.Set" Name="Count"/>
                <Node Id="raise" Type="Raise.Done"/>
                <Wire From="onAdd.then" To="set.exec"/>
                <Wire From="get.value" To="sum.a"/>
                <Wire From="onAdd.amount" To="sum.b"/>
                <Wire From="sum.result" To="set.value"/>
                <Wire From="set.then" To="raise.exec"/>
                <Wire From="sum.result" To="raise.total"/>"""));
        ComponentInstance counter = Xaml.component("g:pure").create();
        List<Object> done = new ArrayList<>();
        counter.onEvent("Done", args -> done.add(args.get("total")));
        counter.send("Add", Args.of("amount", 5));
        counter.flush();
        assertEquals(5, counter.get("Count"));
        assertEquals(List.of(10), done, "sum was recomputed after Count became 5"); // 5 + 5
    }

    /** Раньше поведение делилось между нодами одного типа, и вторая Param.Set писала в параметр первой. */
    @Test
    void twoNodesOfTheSameTypeKeepTheirOwnSettings() {
        register("g:twins", component("TwinBox", """
                <Param Name="First" Type="string" Default="a"/>
                <Param Name="Second" Type="string" Default="b"/>
                <Event Name="Go" Direction="In"/>""", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="one" Type="Param.Set" Name="First" value="one"/>
                <Node Id="two" Type="Param.Set" Name="Second" value="two"/>
                <Wire From="in.then" To="one.exec"/>
                <Wire From="one.then" To="two.exec"/>"""));
        ComponentInstance box = Xaml.component("g:twins").create();
        box.send("Go", Args.empty());
        box.flush();
        assertEquals("one", box.get("First"));
        assertEquals("two", box.get("Second"));
    }

    @Test
    void constructRunsOnCreationAndCanChangeAWidget() {
        register("g:hello", component("HelloBox", "<Param Name=\"Title\" Type=\"string\" Default=\"x\"/>", """
                <Node Id="start" Type="Event.Construct"/>
                <Node Id="text" Type="Value.Literal" ValueType="string" Value="ready"/>
                <Node Id="show" Type="Widget.Set" Target="label" Property="text"/>
                <Wire From="start.then" To="show.exec"/>
                <Wire From="text.value" To="show.value"/>"""));
        ComponentInstance hello = Xaml.component("g:hello").create();
        assertEquals("ready", ((Label) Xaml.get(hello.widget(), "label")).text());
    }

    @Test
    void branchChoosesThePathByACondition() {
        register("g:branch", component("BranchBox", """
                <Param Name="Title" Type="string" Default="none"/>
                <Event Name="Check" Direction="In" Args="value:int"/>""", """
                <Node Id="in" Type="Event.Check"/>
                <Node Id="cmp" Type="Math.Compare" Op="&gt;" b="10"/>
                <Node Id="branch" Type="Flow.Branch"/>
                <Node Id="big" Type="Param.Set" Name="Title" value="big"/>
                <Node Id="small" Type="Param.Set" Name="Title" value="small"/>
                <Wire From="in.then" To="branch.exec"/>
                <Wire From="in.value" To="cmp.a"/>
                <Wire From="cmp.result" To="branch.condition"/>
                <Wire From="branch.true" To="big.exec"/>
                <Wire From="branch.false" To="small.exec"/>"""));
        ComponentInstance box = Xaml.component("g:branch").create();
        box.send("Check", Args.of("value", 50));
        box.flush();
        assertEquals("big", box.get("Title"));
        box.send("Check", Args.of("value", 3));
        box.flush();
        assertEquals("small", box.get("Title"));
    }

    @Test
    void sequenceRunsItsOutputsInOrder() {
        List<String> log = new ArrayList<>();
        function(GraphFunction.define("t.log").param("message", ParamType.STRING)
                .body(args -> {
                    log.add((String) args.get("message"));
                    return null;
                }));
        register("g:seq", component("SeqBox", "<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="seq" Type="Flow.Sequence" Count="3"/>
                <Node Id="a" Type="Call.t.log" message="first"/>
                <Node Id="b" Type="Call.t.log" message="second"/>
                <Node Id="c" Type="Call.t.log" message="third"/>
                <Wire From="in.then" To="seq.exec"/>
                <Wire From="seq.then0" To="a.exec"/>
                <Wire From="seq.then1" To="b.exec"/>
                <Wire From="seq.then2" To="c.exec"/>"""));
        ComponentInstance box = Xaml.component("g:seq").create();
        box.send("Go", Args.empty());
        box.flush();
        assertEquals(List.of("first", "second", "third"), log);
    }

    @Test
    void javaFunctionsAreCalledByNameWithTypedArguments() {
        function(GraphFunction.define("t.double").param("n", ParamType.INT).returns(ParamType.INT).pure()
                .body(args -> (Integer) args.get("n") * 2));
        List<Object> seen = new ArrayList<>();
        function(GraphFunction.define("t.record").param("value", ParamType.INT).returns(ParamType.INT)
                .body(args -> {
                    seen.add(args.get("value"));
                    return args.get("value");
                }));
        register("g:fn", component("FnBox", "<Event Name=\"Go\" Direction=\"In\" Args=\"n:int\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="twice" Type="Call.t.double"/>
                <Node Id="record" Type="Call.t.record"/>
                <Wire From="in.n" To="twice.n"/>
                <Wire From="twice.result" To="record.value"/>
                <Wire From="in.then" To="record.exec"/>"""));
        ComponentInstance box = Xaml.component("g:fn").create();
        box.send("Go", Args.of("n", 21));
        box.flush();
        assertEquals(List.of(42), seen);
    }

    @Test
    void aFunctionAddedLaterMakesAPreviouslyBrokenGraphCompile() {
        register("g:late", component("LateBox", "<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="call" Type="Call.t.late"/>
                <Wire From="in.then" To="call.exec"/>"""));
        XamlLoadException missing = assertThrows(XamlLoadException.class, () -> Xaml.component("g:late").create());
        assertTrue(missing.getMessage().contains("function 't.late' which is not registered"), missing.getMessage());

        int[] calls = {0};
        function(GraphFunction.define("t.late").body(args -> {
            calls[0]++;
            return null;
        }));
        ComponentInstance box = Xaml.component("g:late").create();
        box.send("Go", Args.empty());
        box.flush();
        assertEquals(1, calls[0]);
    }

    @Test
    void runtimeFailuresNameTheNodeThatFailed() {
        register("g:bad", component("BadBox", "<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="show" Type="Widget.Set" Target="ghost" Property="text" value="x"/>
                <Wire From="in.then" To="show.exec"/>"""));
        ComponentInstance box = Xaml.component("g:bad").create();
        box.send("Go", Args.empty());
        RuntimeException failure = assertThrows(RuntimeException.class, box::flush);
        assertTrue(failure.getMessage().contains("show"), failure.getMessage());
        assertTrue(failure.getMessage().contains("ghost"), failure.getMessage());
    }

    @Test
    void anInfiniteLoopIsStoppedByTheStepLimit() {
        register("g:loop", component("LoopBox", "<Event Name=\"Go\" Direction=\"In\"/>", """
                <Node Id="in" Type="Event.Go"/>
                <Node Id="seq" Type="Flow.Sequence" Count="1"/>
                <Wire From="in.then" To="seq.exec"/>
                <Wire From="seq.then0" To="seq.exec"/>"""));
        ComponentInstance box = Xaml.component("g:loop").create();
        box.send("Go", Args.empty());
        RuntimeException failure = assertThrows(RuntimeException.class, box::flush);
        assertTrue(failure.getMessage().contains("Step limit"), failure.getMessage());
    }

    // ------------------------------------------------------------------ ошибки документа

    private void assertRejected(String contract, String graph, String fragment) {
        ComponentDefinition definition = ComponentDefinition.parse("g:probe", component("ProbeBox", contract, graph));
        XamlLoadException failure = assertThrows(XamlLoadException.class, definition::program, fragment);
        assertTrue(failure.getMessage().contains(fragment), "expected '" + fragment + "' in: " + failure.getMessage());
        assertTrue(failure.diagnostics().get(0).hasLocation(), "the error carries a position: " + failure.getMessage());
    }

    @Test
    void graphMistakesAreReportedWithPositionsBeforeAnythingRuns() {
        String go = "<Event Name=\"Go\" Direction=\"In\" Args=\"n:int\"/><Event Name=\"Out\" Direction=\"Out\"/>"
                + "<Param Name=\"Title\" Type=\"string\"/>";
        assertRejected(go, "<Node Id=\"a\" Type=\"Nope.Thing\"/>", "Unknown node type 'Nope.Thing'");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Missing\"/>", "does not declare");
        assertRejected(go, "<Node Id=\"a\" Type=\"Param.Get\" Name=\"Nope\"/>", "parameter 'Nope'");
        assertRejected(go, "<Node Id=\"a\" Type=\"Param.Get\"/>", "requires Name");
        assertRejected(go, "<Node Id=\"a\" Type=\"Math.Compare\" Op=\"~\"/>", "Op must be one of");
        assertRejected(go, "<Node Id=\"a\" Type=\"Flow.Sequence\" Count=\"99\"/>", "Count must be 1 to 16");
        assertRejected(go, "<Node Id=\"a\" Type=\"Math.Add\" Bogus=\"1\"/>", "Unknown attribute 'Bogus'");
        assertRejected(go, "<Node Id=\"a\" Type=\"Math.Add\" a=\"many\"/>", "not a valid float");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Go\"/><Wire From=\"a.then\" To=\"ghost.exec\"/>", "unknown node 'ghost'");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Go\"/><Node Id=\"b\" Type=\"Math.Add\"/>"
                + "<Wire From=\"a.then\" To=\"b.a\"/>", "execution pin with a data pin");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Go\"/><Node Id=\"b\" Type=\"Flow.Branch\"/>"
                + "<Wire From=\"a.nope\" To=\"b.exec\"/>", "has no output pin 'nope'");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Go\"/><Node Id=\"b\" Type=\"Math.Add\"/>"
                + "<Node Id=\"c\" Type=\"Math.Add\"/><Wire From=\"b.result\" To=\"c.a\"/>"
                + "<Wire From=\"a.n\" To=\"c.a\"/>", "already has a wire");
        assertRejected(go, "<Node Id=\"a\" Type=\"Event.Go\"/><Node Id=\"b\" Type=\"Param.Set\" Name=\"Title\"/>"
                + "<Wire From=\"a.then\" To=\"b.exec\"/>", "value");
    }

    @Test
    void typeMismatchBetweenDataPinsIsReported() {
        assertRejected("<Param Name=\"Flag\" Type=\"bool\"/><Param Name=\"Title\" Type=\"string\"/>",
                "<Node Id=\"flag\" Type=\"Param.Get\" Name=\"Flag\"/><Node Id=\"sum\" Type=\"Math.Add\"/>"
                        + "<Wire From=\"flag.value\" To=\"sum.a\"/>", "joins bool to float");
    }

    @Test
    void malformedGraphMarkupIsRejectedWhenTheDocumentIsRead() {
        String shell = "<Component x:Name=\"BadGraph\" xmlns:x=\"https://unigui.dev/xaml\"><Label/>"
                + "<Component.Graph>%s</Component.Graph></Component>";
        for (String[] bad : new String[][]{
                {"<Node Type=\"Math.Add\"/>", "requires Id"},
                {"<Node Id=\"a\"/>", "requires Type"},
                {"<Node Id=\"a\" Type=\"Math.Add\"/><Node Id=\"a\" Type=\"Math.Add\"/>", "Duplicate graph node id"},
                {"<Node Id=\"bad id\" Type=\"Math.Add\"/>", "letters, digits"},
                {"<Wire From=\"a\" To=\"b.x\"/>", "must look like node.pin"},
                {"<Wire From=\"a.x\"/>", "requires To"},
                {"<Thing/>", "can hold <Node> and <Wire>"},
                {"<Node Id=\"a\" Type=\"Math.Add\" X=\"far\"/>", "not a number"}}) {
            XamlLoadException failure = assertThrows(XamlLoadException.class,
                    () -> ComponentDefinition.parse("g:x", shell.formatted(bad[0])), bad[0]);
            assertTrue(failure.getMessage().contains(bad[1]), bad[0] + " -> " + failure.getMessage());
        }
    }

    @Test
    void aComponentWithoutAGraphStaysCheap() {
        register("g:plain", component("PlainBox", "<Param Name=\"Title\" Type=\"string\"/>", ""));
        ComponentDefinition definition = Components.find("g:plain").orElseThrow();
        assertTrue(definition.graph().isEmpty());
        assertFalse(Xaml.component("g:plain").create().widget() == null);
    }
}
