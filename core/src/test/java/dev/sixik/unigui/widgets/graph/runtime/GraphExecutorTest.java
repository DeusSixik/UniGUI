package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphExecutorTest {
    private NodeGraph graph;
    private GraphExecutor executor;
    private final List<String> log = new ArrayList<>();
    private int counter;

    @BeforeEach
    void setUp() {
        graph = new NodeGraph();
        graph.portTypes()
                .register(PortType.data("number", 0xFF5CD6A9))
                .register(PortType.data("bool", 0xFFE05252));
        registerType("start", id -> NodeBuilder.node(id).header("Start").flowRow(null, "then").build());
        registerType("log", id -> NodeBuilder.node(id).header("Log").flowRow("exec", "then")
                .input("text", "Text", "any", ValueEditors.text("")).build());
        registerType("add", id -> NodeBuilder.node(id).header("Add")
                .input("a", "A", "number", ValueEditors.number(0))
                .input("b", "B", "number", ValueEditors.number(0))
                .output("sum", "Sum", "number").build());
        registerType("branch", id -> NodeBuilder.node(id).header("Branch")
                .flowRow("exec", "true").flowRow(null, "false")
                .input("cond", "Cond", "bool", ValueEditors.bool(false)).build());
        registerType("foreach", id -> NodeBuilder.node(id).header("For each")
                .flowRow("exec", "body").flowRow(null, "done")
                .input("count", "Count", "number", ValueEditors.number(3))
                .output("index", "Index", "number").build());
        registerType("sequence", id -> NodeBuilder.node(id).header("Sequence")
                .flowRow("exec", "a").flowRow(null, "b").build());
        registerType("counter", id -> NodeBuilder.node(id).header("Counter").flowRow("exec", "then")
                .output("value", "Value", "number").build());
        registerType("accum", id -> NodeBuilder.node(id).header("Accumulate").flowRow("exec", "then")
                .input("in", "In", "number", ValueEditors.number(0))
                .output("out", "Out", "number").build());
        registerType("boom", id -> NodeBuilder.node(id).header("Boom").flowRow("exec", "then").build());
        registerType("need", id -> NodeBuilder.node(id).header("Need")
                .requiredInput("x", "X", "number").output("y", "Y", "number").build());
        registerType("ghost", id -> NodeBuilder.node(id).header("Ghost").flowRow("exec", "then").build());

        executor = new GraphExecutor()
                .register("start", NodeBehavior.exec(c -> c.continueWith("then")))
                .register("log", NodeBehavior.exec(c -> {
                    log.add(c.text("text"));
                    c.continueWith("then");
                }))
                .register("add", NodeBehavior.pure(c -> c.output("sum", c.number("a") + c.number("b"))))
                .register("branch", NodeBehavior.exec(c -> c.continueWith(c.bool("cond") ? "true" : "false")))
                .register("foreach", NodeBehavior.exec(c -> {
                    int count = (int) c.number("count");
                    for (int i = 0; i < count; i++) {
                        c.output("index", (double) i);
                        c.runFlow("body");
                    }
                    c.continueWith("done");
                }))
                .register("sequence", NodeBehavior.exec(c -> {
                    c.continueWith("a");
                    c.continueWith("b");
                }))
                .register("counter", NodeBehavior.exec(c -> {
                    counter++;
                    c.output("value", (double) counter);
                    c.continueWith("then");
                }))
                .register("accum", NodeBehavior.exec(c -> {
                    c.output("out", c.number("in") + 1.0);
                    c.continueWith("then");
                }))
                .register("boom", NodeBehavior.exec(c -> {
                    throw new IllegalStateException("kaboom");
                }));
    }

    private void registerType(String id, java.util.function.Function<String, NodeGraphItem> factory) {
        graph.nodeTypes().register(NodeType.of(id, id, "Test", factory));
    }

    private NodeGraphItem node(String type, String id) {
        return graph.spawn(type, id, 0.0f, 0.0f);
    }

    private void wire(String fromNode, String fromPort, String toNode, String toPort) {
        graph.addConnection("", fromNode, fromPort, toNode, toPort);
    }

    private GraphResult fire(String startId) {
        return executor.fire(graph.model(), startId);
    }

    // ---- поток выполнения ----

    @Test
    void linearFlow_runsNodesInWireOrder() {
        node("start", "s");
        node("log", "a").value("text", "hello");
        node("log", "b").value("text", "world");
        wire("s", "then", "a", "exec");
        wire("a", "then", "b", "exec");

        GraphResult result = fire("s");

        assertEquals(List.of("hello", "world"), log);
        assertEquals(3, result.steps());
    }

    @Test
    void flowStopsWhereThereIsNoWire() {
        node("start", "s");
        node("log", "a").value("text", "only");
        node("log", "unreached").value("text", "never");
        wire("s", "then", "a", "exec");

        fire("s");

        assertEquals(List.of("only"), log);
    }

    @Test
    void firingAnUnknownNodeFails() {
        assertThrows(GraphExecutionException.class, () -> fire("missing"));
    }

    @Test
    void sequence_runsContinuationsInCallOrder() {
        node("start", "s");
        node("sequence", "seq");
        node("log", "first").value("text", "1");
        node("log", "second").value("text", "2");
        node("log", "after").value("text", "1b");
        wire("s", "then", "seq", "exec");
        wire("seq", "a", "first", "exec");
        wire("seq", "b", "second", "exec");
        wire("first", "then", "after", "exec");

        fire("s");

        assertEquals(List.of("1", "1b", "2"), log, "branch a runs to its end before branch b starts");
    }

    @Test
    void branch_followsTheConditionEditorAndTheWire() {
        node("start", "s");
        node("branch", "br");
        node("log", "yes").value("text", "yes");
        node("log", "no").value("text", "no");
        wire("s", "then", "br", "exec");
        wire("br", "true", "yes", "exec");
        wire("br", "false", "no", "exec");

        fire("s");
        graph.item("br").value("cond", true);
        fire("s");

        assertEquals(List.of("no", "yes"), log);
    }

    @Test
    void branch_conditionRejectsANumberWire() {
        node("branch", "br");
        node("add", "calc");

        // Число не может питать bool-вход: тип проверяется при соединении, а не при исполнении.
        assertThrows(IllegalArgumentException.class, () -> wire("calc", "sum", "br", "cond"));
    }

    // ---- данные ----

    @Test
    void wiredInputBeatsTheEditorDefault() {
        node("start", "s");
        node("add", "sum").value("a", 2.0);
        graph.item("sum").value("b", 3.0);
        node("log", "out").value("text", "default");
        wire("s", "then", "out", "exec");

        fire("s");
        wire("sum", "sum", "out", "text");
        fire("s");

        assertEquals(List.of("default", "5.0"), log);
    }

    @Test
    void pureChain_isEvaluatedOnDemand() {
        node("start", "s");
        node("add", "first").value("a", 1.0);
        graph.item("first").value("b", 2.0);
        node("add", "second").value("b", 3.0);
        node("log", "out");
        wire("first", "sum", "second", "a");
        wire("second", "sum", "out", "text");
        wire("s", "then", "out", "exec");

        fire("s");

        assertEquals(List.of("6.0"), log);
    }

    @Test
    void execOutputsStayReadableForLaterNodes() {
        node("start", "s");
        node("counter", "count");
        node("log", "out");
        wire("s", "then", "count", "exec");
        wire("count", "then", "out", "exec");
        wire("count", "value", "out", "text");

        GraphResult result = fire("s");

        assertEquals(List.of("1.0"), log);
        assertEquals(1.0, (Double) result.output("count", "value"));
        assertNull(result.output("count", "missing"));
        assertNull(result.output("nobody", "value"));
    }

    @Test
    void readingAnExecOutputBeforeItRanGivesNull() {
        node("start", "s");
        node("counter", "count");
        node("log", "out").value("text", "x");
        wire("count", "value", "out", "text");
        wire("s", "then", "out", "exec");

        fire("s");

        assertEquals(List.of(""), log);
    }

    // ---- циклы ----

    @Test
    void forEach_runsTheBodyPerIterationWithFreshPureValues() {
        node("start", "s");
        node("foreach", "loop");
        node("add", "shift").value("b", 10.0);
        node("log", "body");
        node("log", "done").value("text", "done");
        wire("s", "then", "loop", "exec");
        wire("loop", "body", "body", "exec");
        wire("loop", "done", "done", "exec");
        wire("loop", "index", "shift", "a");
        wire("shift", "sum", "body", "text");

        fire("s");

        assertEquals(List.of("10.0", "11.0", "12.0", "done"), log,
                "the pure add node is recomputed for every iteration");
    }

    @Test
    void forEach_countComesFromTheEditorAndHandlesZero() {
        node("start", "s");
        node("foreach", "loop").value("count", 0.0);
        node("log", "body").value("text", "body");
        node("log", "done").value("text", "done");
        wire("s", "then", "loop", "exec");
        wire("loop", "body", "body", "exec");
        wire("loop", "done", "done", "exec");

        fire("s");

        assertEquals(List.of("done"), log);
    }

    @Test
    void infiniteFlowLoop_isStoppedByTheStepLimit() {
        node("start", "s");
        node("log", "spin").value("text", "x");
        wire("s", "then", "spin", "exec");
        wire("spin", "then", "spin", "exec");
        executor.maxSteps(50);

        GraphExecutionException failure = assertThrows(GraphExecutionException.class, () -> fire("s"));

        assertTrue(failure.getMessage().contains("Step limit of 50"), failure.getMessage());
        assertEquals(49, log.size());
    }

    @Test
    void longChains_doNotOverflowTheStack() {
        int length = 5000;
        List<GraphModel.Node> nodes = new ArrayList<>();
        List<GraphModel.Wire> wires = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            nodes.add(flowNode("n" + i));
            if (i > 0) wires.add(new GraphModel.Wire("w" + i, "n" + (i - 1), "then", "n" + i, "exec"));
        }
        GraphExecutor chain = new GraphExecutor().maxSteps(length)
                .register("flow", NodeBehavior.exec(c -> c.continueWith("then")));

        GraphResult result = chain.fire(GraphModel.of(nodes, wires), "n0");

        assertEquals(length, result.steps());
    }

    private static GraphModel.Node flowNode(String id) {
        return new GraphModel.Node(id, "flow", Map.of(), List.of(
                new GraphModel.Port("exec", NodeGraphPortKind.INPUT, "flow", true, false),
                new GraphModel.Port("then", NodeGraphPortKind.OUTPUT, "flow", true, false)));
    }

    @Test
    void dataCycle_failsInsteadOfOverflowing() {
        node("start", "s");
        node("add", "one");
        node("add", "two");
        node("log", "out");
        wire("one", "sum", "two", "a");
        wire("two", "sum", "one", "a");
        wire("two", "sum", "out", "text");
        wire("s", "then", "out", "exec");

        GraphExecutionException failure = assertThrows(GraphExecutionException.class, () -> fire("s"));

        assertTrue(failure.getMessage().contains("Data cycle"), failure.getMessage());
    }

    // ---- ошибки ----

    @Test
    void missingBehavior_namesTheTypeAndNode() {
        node("start", "s");
        node("ghost", "g");
        wire("s", "then", "g", "exec");

        GraphExecutionException failure = assertThrows(GraphExecutionException.class, () -> fire("s"));

        assertTrue(failure.getMessage().contains("ghost") && failure.getMessage().contains("'g'"), failure.getMessage());
    }

    @Test
    void behaviorFailure_isWrappedWithTheNodeId() {
        node("start", "s");
        node("boom", "b");
        wire("s", "then", "b", "exec");

        GraphExecutionException failure = assertThrows(GraphExecutionException.class, () -> fire("s"));

        assertTrue(failure.getMessage().contains("'b'") && failure.getMessage().contains("kaboom"), failure.getMessage());
        assertTrue(failure.getCause() instanceof IllegalStateException);
    }

    @Test
    void contextRejectsWrongPortUse() {
        List<String> errors = new ArrayList<>();
        executor.register("log", NodeBehavior.exec(c -> {
            try {
                c.input("exec");
            } catch (GraphExecutionException e) {
                errors.add("flow-as-data");
            }
            try {
                c.input("nope");
            } catch (GraphExecutionException e) {
                errors.add("unknown-port");
            }
            try {
                c.output("text", 1);
            } catch (GraphExecutionException e) {
                errors.add("output-on-input");
            }
            try {
                c.continueWith("text");
            } catch (GraphExecutionException e) {
                errors.add("continue-on-data");
            }
        }));
        node("start", "s");
        node("log", "a");
        wire("s", "then", "a", "exec");

        fire("s");

        assertEquals(List.of("flow-as-data", "unknown-port", "output-on-input", "continue-on-data"), errors);
    }

    @Test
    void typedAccessors_rejectMismatchedValues() {
        List<String> failures = new ArrayList<>();
        executor.register("log", NodeBehavior.exec(c -> {
            try {
                c.number("text");
            } catch (GraphExecutionException e) {
                failures.add("number");
            }
            try {
                c.bool("text");
            } catch (GraphExecutionException e) {
                failures.add("bool");
            }
            try {
                c.vector("text");
            } catch (GraphExecutionException e) {
                failures.add("vector");
            }
        }));
        node("start", "s");
        node("log", "a").value("text", "words");
        wire("s", "then", "a", "exec");

        fire("s");

        assertEquals(List.of("number", "bool", "vector"), failures);
    }

    @Test
    void connectedReportsWires() {
        List<Boolean> seen = new ArrayList<>();
        executor.register("log", NodeBehavior.exec(c -> {
            seen.add(c.connected("text"));
            seen.add(c.connected("then"));
        }));
        node("start", "s");
        node("log", "a");
        node("add", "sum");
        wire("s", "then", "a", "exec");

        fire("s");
        wire("sum", "sum", "a", "text");
        fire("s");

        assertEquals(List.of(false, false, true, false), seen);
    }

    // ---- reroute ----

    @Test
    void dataReroute_isTransparent() {
        node("start", "s");
        node("add", "sum").value("a", 4.0);
        graph.item("sum").value("b", 5.0);
        node("log", "out");
        wire("sum", "sum", "out", "text");
        wire("s", "then", "out", "exec");
        String wireId = graph.connections().stream()
                .filter(c -> c.fromItemId().equals("sum")).findFirst().orElseThrow().id();

        assertTrue(graph.insertReroute(wireId, 100.0f, 0.0f) != null);
        fire("s");

        assertEquals(List.of("9.0"), log);
    }

    @Test
    void flowReroute_keepsTheFlowGoing() {
        node("start", "s");
        node("log", "out").value("text", "through");
        wire("s", "then", "out", "exec");
        String wireId = graph.connections().get(0).id();

        assertTrue(graph.insertReroute(wireId, 100.0f, 0.0f) != null);
        fire("s");

        assertEquals(List.of("through"), log);
    }

    // ---- модель ----

    @Test
    void model_isASnapshotOfValuesAndWires() {
        node("start", "s");
        node("log", "a").value("text", "before");
        wire("s", "then", "a", "exec");

        GraphModel model = graph.model();
        graph.item("a").value("text", "after");
        graph.removeConnection(graph.connections().get(0));

        assertEquals("before", model.node("a").values().get("text"));
        assertEquals(1, model.wires().size());
        assertTrue(model.node("s").hasFlowOutput() && !model.node("s").hasFlowInput());
        assertTrue(model.node("a").hasFlowInput());
        assertEquals("a", model.outgoing("s", "then").get(0).toNode());
        assertEquals("s", model.incoming("a", "exec").fromNode());
        assertNull(model.incoming("a", "text"));
        assertTrue(model.outgoing("a", "none").isEmpty());
    }

    @Test
    void model_skipsDisabledWires() {
        node("start", "s");
        node("log", "a");
        wire("s", "then", "a", "exec");
        graph.connections().get(0).enabled(false);

        fire("s");

        assertTrue(graph.model().wires().isEmpty());
        assertTrue(log.isEmpty());
    }

    // ---- проверка ----

    private List<GraphIssue> issuesOf(String nodeId, List<GraphIssue> all) {
        return all.stream().filter(issue -> issue.itemId().equals(nodeId)).toList();
    }

    @Test
    void validator_requiredInputNeedsAWireOrADefault() {
        node("need", "n");
        node("add", "sum");

        executor.register("need", NodeBehavior.pure(c -> { }));
        List<GraphIssue> unsatisfied = executor.validate(graph.model());
        wire("sum", "sum", "n", "x");
        List<GraphIssue> wired = executor.validate(graph.model());

        assertEquals(1, issuesOf("n", unsatisfied).size());
        assertEquals(GraphIssue.Severity.ERROR, issuesOf("n", unsatisfied).get(0).severity());
        assertEquals("x", issuesOf("n", unsatisfied).get(0).portId());
        assertTrue(issuesOf("n", wired).stream().noneMatch(issue -> issue.portId().equals("x")));
    }

    @Test
    void validator_dataCycleAmongPureNodesIsAnError() {
        node("add", "one");
        node("add", "two");
        node("add", "bystander");
        wire("one", "sum", "two", "a");
        wire("two", "sum", "one", "a");
        wire("two", "sum", "bystander", "a");

        List<GraphIssue> issues = executor.validate(graph.model());

        assertEquals(1, issuesOf("one", issues).size());
        assertEquals(1, issuesOf("two", issues).size());
        assertTrue(issuesOf("one", issues).get(0).message().contains("Data cycle"));
        assertTrue(issuesOf("bystander", issues).isEmpty(), "a node merely downstream of a cycle is not on it");
    }

    @Test
    void validator_selfLoopIsACycle() {
        node("add", "loop");
        wire("loop", "sum", "loop", "a");

        assertEquals(1, issuesOf("loop", executor.validate(graph.model())).size());
    }

    @Test
    void validator_cycleThroughAnExecNodeIsFine() {
        node("accum", "acc");
        node("add", "plus");
        wire("acc", "out", "plus", "a");
        wire("plus", "sum", "acc", "in");

        List<GraphIssue> issues = executor.validate(graph.model());

        assertTrue(issues.stream().noneMatch(issue -> issue.message().contains("Data cycle")), issues.toString());
    }

    @Test
    void validator_warnsAboutNodesNoEventReaches() {
        node("start", "s");
        node("log", "reached");
        node("log", "orphan");
        wire("s", "then", "reached", "exec");

        List<GraphIssue> issues = executor.validate(graph.model());

        assertTrue(issuesOf("reached", issues).isEmpty());
        assertTrue(issuesOf("s", issues).isEmpty(), "the event itself is the entry");
        assertEquals(GraphIssue.Severity.WARNING, issuesOf("orphan", issues).get(0).severity());
    }

    @Test
    void validator_flagsMissingBehaviorsAndUntypedNodes() {
        node("ghost", "g");
        graph.addItem(NodeBuilder.node("plain").header("Plain").build());

        List<GraphIssue> issues = executor.validate(graph.model());

        assertTrue(issuesOf("g", issues).stream().anyMatch(issue ->
                issue.severity() == GraphIssue.Severity.ERROR && issue.message().contains("ghost")));
        assertEquals(GraphIssue.Severity.WARNING, issuesOf("plain", issues).get(0).severity());
    }

    @Test
    void validator_cleanGraphHasNoIssues() {
        node("start", "s");
        node("log", "a");
        wire("s", "then", "a", "exec");

        assertTrue(executor.validate(graph.model()).isEmpty());
    }

    // ---- подсветка на нодах ----

    @Test
    void validate_marksNodesAndClearsThemAgain() {
        node("add", "one");
        node("add", "two");
        node("log", "orphan");
        node("start", "s");
        wire("one", "sum", "two", "a");
        wire("two", "sum", "one", "a");

        List<GraphIssue> issues = graph.validate(executor);

        assertFalse(issues.isEmpty());
        assertEquals(2, graph.item("one").issueLevel());
        assertEquals(2, graph.item("two").issueLevel());
        assertEquals(1, graph.item("orphan").issueLevel());
        assertEquals(0, graph.item("s").issueLevel());
        assertTrue(graph.item("one").issueMessages().get(0).contains("Data cycle"));

        graph.clearIssues();

        assertEquals(0, graph.item("one").issueLevel());
        assertTrue(graph.item("orphan").issueMessages().isEmpty());
    }

    @Test
    void validate_errorsOutrankWarningsOnTheSameNode() {
        node("ghost", "g");

        graph.validate(executor);

        // Нода одновременно недостижима (предупреждение) и без поведения (ошибка): рамка берёт худшее.
        assertEquals(2, graph.item("g").issueLevel());
        List<String> messages = graph.item("g").issueMessages();
        assertEquals(2, messages.size(), messages.toString());
        assertTrue(messages.stream().anyMatch(message -> message.contains("No behavior")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Never reached")));
    }
}
