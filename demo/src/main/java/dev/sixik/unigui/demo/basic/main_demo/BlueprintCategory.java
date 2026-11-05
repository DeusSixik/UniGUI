package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.graph.node.NodeBuilder;
import dev.sixik.unigui.widgets.graph.node.NodeType;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.graph.node.ValueEditors;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutionException;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphIssue;
import dev.sixik.unigui.widgets.graph.runtime.GraphResult;
import dev.sixik.unigui.widgets.graph.runtime.NodeBehavior;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BlueprintCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("blueprint-graph", "Blueprint graph", "Configure modular node types, typed ports, regions, and runtime actions."));

    @Override
    String title() {
        return "Blueprint";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        if (!"blueprint-graph".equals(controlId)) {
            root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
            return;
        }
        addBlueprintExamples(root);
    }

    private void addBlueprintExamples(VBox root) {
        Label status = label("Blueprint: ready", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        NodeGraph graph = new NodeGraph()
                .selectionMode(NodeGraphSelectionMode.MULTIPLE)
                .minimapEnabled(true);
        graph.layout(style -> style.widthPercent(100.0f).height(410.0f));

        graph.portTypes()
                .register(PortType.data("bool", 0xFFE05252))
                .register(PortType.data("number", 0xFF5CD6A9))
                .register(PortType.data("vec3", 0xFFF5D547))
                .register(PortType.data("string", 0xFFE08BD0))
                .register(PortType.data("entity", 0xFF5B8DEF))
                .allowConversion("number", "vec3");

        graph.nodeTypes()
                .register(NodeType.of("event.start", "Start", "Events", id -> NodeBuilder.node(id)
                        .color(0xFFD9534F)
                        .header("Start")
                        .flowRow(null, "then")
                        .build()))
                .register(NodeType.of("npc.create", "Create NPC", "NPC", id -> NodeBuilder.node(id)
                        .color(0xFF4F9D4F)
                        .header("Create NPC")
                        .flowRow("exec", "then")
                        .input("pos", "Position", "vec3", ValueEditors.vector(3, 0, 64, 0))
                        .input("name", "Name", "string", ValueEditors.text("Villager"))
                        .output("npc", "Result", "entity")
                        .build()))
                .register(NodeType.of("logic.compare", "Less or equal", "Logic", id -> NodeBuilder.node(id)
                        .color(0xFF8E44AD)
                        .header("Less or equal")
                        .input("a", "A", "number", ValueEditors.number(3))
                        .input("b", "B", "number", ValueEditors.number(5))
                        .output("result", "Result", "bool")
                        .build()))
                .register(NodeType.of("npc.say", "Say", "NPC/Dialogue", id -> NodeBuilder.node(id)
                        .color(0xFF2E8B57)
                        .header("Say")
                        .flowRow("exec", "then")
                        .input("who", "Who", "entity")
                        .input("text", "Text", "string", ValueEditors.text("Hello!"))
                        .input("loud", "Shout", "bool", ValueEditors.bool(false))
                        .build()));

        graph.addRegion("setup", "Spawn setup", 20.0f, 20.0f, 520.0f, 250.0f)
                .comment("Creates the NPC and greets the player")
                .color(0xFF3B6EA5);
        graph.addRegion("dialogue", "Dialogue", 345.0f, 44.0f, 190.0f, 216.0f)
                .comment("Nested region: it moves with Spawn setup")
                .color(0xFFA5703B);
        graph.spawn("event.start", "start", 40.0f, 70.0f);
        graph.spawn("npc.create", "create", 190.0f, 60.0f);
        graph.spawn("npc.say", "say", 360.0f, 60.0f);
        graph.spawn("logic.compare", "compare", 190.0f, 330.0f);
        graph.addConnection("w-start", "start", "then", "create", "exec");
        graph.addConnection("w-then", "create", "then", "say", "exec");
        graph.addConnection("w-npc", "create", "npc", "say", "who");

        graph.onItemCollapsed(event -> status.text("Blueprint: " + event.itemId()
                + (event.collapsed() ? " collapsed" : " expanded")));
        graph.onRegionChanged(event -> status.text("Blueprint: region " + event.regionId() + " " + event.kind()));
        graph.onValueChanged(event -> status.text("Blueprint: " + event.itemId() + "." + event.portId()
                + " = " + event.serialized()));
        graph.onConnectionCreated(event -> status.text("Blueprint: connected "
                + event.from().itemId() + "." + event.from().portId() + " -> "
                + event.to().itemId() + "." + event.to().portId()));
        graph.onSelectionChanged(event -> {
            if (!event.newSelection().isEmpty()) {
                status.text("Blueprint: selected " + String.join(", ", event.newSelection()));
            }
        });

        HBox tools = new HBox();
        tools.spacing(6.0f);
        tools.layout(style -> style.widthPercent(100.0f));
        tools.addChild(graphToolButton("Frame all", () -> {
            graph.frameAll();
            status.text("Blueprint: framed all");
        }));
        Button snap = graphToolButton("Snap: Ctrl", null);
        snap.onClick(event -> {
            graph.snapToGrid(!graph.snapToGrid());
            snap.text(graph.snapToGrid() ? "Snap: always" : "Snap: Ctrl");
        });
        tools.addChild(snap);

        List<String> sayLog = new ArrayList<>();
        GraphExecutor executor = new GraphExecutor()
                .register("event.start", NodeBehavior.exec(context -> context.continueWith("then")))
                .register("npc.create", NodeBehavior.exec(context -> {
                    double[] position = context.vector("pos");
                    String coordinates = position == null ? "?" : String.format(Locale.ROOT,
                            "%.0f, %.0f, %.0f", position[0], position[1], position[2]);
                    context.output("npc", context.text("name") + "@" + coordinates);
                    context.continueWith("then");
                }))
                .register("logic.compare", NodeBehavior.pure(context ->
                        context.output("result", context.number("a") <= context.number("b"))))
                .register("npc.say", NodeBehavior.exec(context -> {
                    String line = context.text("who") + " says " + context.text("text");
                    sayLog.add(context.bool("loud") ? line.toUpperCase(Locale.ROOT) : line);
                    context.continueWith("then");
                }));
        tools.addChild(graphToolButton("Run", () -> {
            sayLog.clear();
            try {
                GraphResult result = executor.fire(graph.model(), "start");
                status.text("Run: " + result.steps() + " steps | " + String.join(" | ", sayLog));
            } catch (GraphExecutionException failure) {
                status.text("Run failed: " + failure.getMessage());
            }
        }));
        tools.addChild(graphToolButton("Validate", () -> {
            List<GraphIssue> issues = graph.validate(executor);
            status.text(issues.isEmpty()
                    ? "Blueprint: graph is valid"
                    : "Blueprint: " + issues.size() + " issue(s), first: " + issues.get(0).message());
        }));

        VBox workspace = new VBox();
        workspace.spacing(9.0f);
        workspace.layout(style -> style.widthPercent(100.0f));
        workspace.addChild(status);
        workspace.addChild(tools);
        workspace.addChild(graph);
        root.addChild(samplePanel("Blueprint editor", "Registered node types form the add-node palette. The graph combines typed pins, editable defaults, regions, comments, a minimap, execution and validation.", workspace,
                "graph.portTypes()\n        .register(PortType.data(\"number\", NUMBER_COLOR))\n        .register(PortType.data(\"vec3\", VECTOR_COLOR))\n        .allowConversion(\"number\", \"vec3\");\ngraph.nodeTypes().register(NodeType.of(\"npc.create\", \"Create NPC\", \"NPC\", id ->\n        NodeBuilder.node(id).color(GREEN).header(\"Create NPC\")\n                .flowRow(\"exec\", \"then\")\n                .input(\"pos\", \"Position\", \"vec3\", ValueEditors.vector(3, 0, 64, 0))\n                .output(\"npc\", \"Result\", \"entity\").build()));\ngraph.spawn(\"npc.create\", \"create\", 190, 60);"));

        VBox modules = new VBox();
        modules.spacing(8.0f);
        modules.layout(style -> style.widthPercent(100.0f));
        modules.addChild(label("HeaderModule sets the node title and category color.", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        modules.addChild(label("FlowRowModule creates execution pins; PortRowModule creates typed data inputs and outputs.", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        modules.addChild(label("ValueEditors attach live text, number, boolean, choice, color, or vector editors to unconnected inputs.", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("NodeBuilder modules", "NodeBuilder composes a node from ordered modules; the call order becomes the visual row order.", modules,
                "NodeGraphItem node = NodeBuilder.node(\"create\")\n        .color(0xFF4F9D4F).header(\"Create NPC\")\n        .flowRow(\"exec\", \"then\")\n        .input(\"pos\", \"Position\", \"vec3\", ValueEditors.vector(3, 0, 64, 0))\n        .input(\"name\", \"Name\", \"string\", ValueEditors.text(\"Villager\"))\n        .output(\"npc\", \"Result\", \"entity\")\n        .build();"));

        VBox runtime = new VBox();
        runtime.spacing(8.0f);
        runtime.layout(style -> style.widthPercent(100.0f));
        runtime.addChild(label("GraphExecutor follows execution wires and evaluates connected data inputs on demand.", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        runtime.addChild(label("GraphValidator checks required inputs, node behaviors, and connection types before execution.", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        root.addChild(samplePanel("Execution and validation", "Use the Run and Validate actions above to exercise the same graph model.", runtime,
                "GraphExecutor executor = new GraphExecutor()\n        .register(\"event.start\", NodeBehavior.exec(c -> c.continueWith(\"then\")))\n        .register(\"npc.say\", NodeBehavior.exec(c -> {\n            log.add(c.text(\"who\") + \": \" + c.text(\"text\"));\n            c.continueWith(\"then\");\n        }));\nGraphResult result = executor.fire(graph.model(), \"start\");\nList<GraphIssue> issues = graph.validate(executor);"));
    }

    private Button graphToolButton(String text, Runnable action) {
        Button button = new Button(text);
        button.layout(style -> style.size(86.0f, 26.0f).flexGrow(0.0f).flexShrink(0.0f));
        if (action != null) button.onClick(event -> action.run());
        return button;
    }
}
