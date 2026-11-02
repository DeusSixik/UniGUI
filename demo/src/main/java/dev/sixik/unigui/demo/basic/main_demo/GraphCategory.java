package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.graph.NodeGraphPortSide;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.graph.node.PortType;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;

final class GraphCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("node-graph", "NodeGraph", "Move nodes, connect typed ports, and navigate a graph canvas."));

    @Override
    String title() {
        return "Graph";
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
        if ("node-graph".equals(controlId)) addNodeGraph(root);
        else root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
    }

    private void addNodeGraph(VBox root) {
        Label status = label("Select nodes, drag to move, or connect matching ports.", UniGuiDemo.GalleryTemplate.TEXT_DARK);
        NodeGraph graph = new NodeGraph()
                .selectionMode(NodeGraphSelectionMode.MULTIPLE)
                .gridSize(24.0f)
                .minimapEnabled(true);
        graph.portTypes()
                .register(PortType.data("item", 0xFF75B7E8))
                .register(PortType.data("fluid", 0xFF42C8B8));
        graph.layout(style -> style.widthPercent(100.0f).height(360.0f));

        var source = graph.addItem("source", graphNode("Source", "Two input values"), 36.0f, 52.0f)
                .size(176.0f, 104.0f);
        source.addPort("item-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.40f).type("item");
        source.addPort("fluid-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.72f).type("fluid");
        var processor = graph.addItem("processor", graphNode("Processor", "Combines the inputs"), 300.0f, 92.0f)
                .size(200.0f, 120.0f);
        processor.addPort("item-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.32f).type("item");
        processor.addPort("fluid-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.68f).type("fluid");
        processor.addPort("result", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f).type("item");
        var result = graph.addItem("result", graphNode("Result", "Output slot"), 610.0f, 60.0f)
                .size(160.0f, 96.0f);
        result.addPort("item-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f).type("item");
        graph.addConnection("item-link", new NodeGraphPortRef("source", "item-out"),
                new NodeGraphPortRef("processor", "item-in")).type("item");
        graph.addConnection("fluid-link", new NodeGraphPortRef("source", "fluid-out"),
                new NodeGraphPortRef("processor", "fluid-in")).type("fluid");
        graph.addConnection("result-link", new NodeGraphPortRef("processor", "result"),
                new NodeGraphPortRef("result", "item-in")).type("item");

        Button frame = new Button("Frame all");
        frame.onClick(event -> graph.frameAll());
        Button clear = new Button("Clear selection");
        clear.onClick(event -> graph.clearSelection());
        HBox tools = new HBox();
        tools.spacing(8.0f);
        tools.addChild(frame);
        tools.addChild(clear);
        graph.onSelectionChanged(event -> status.text(event.newSelection().isEmpty()
                ? "Selection cleared"
                : "Selected: " + String.join(", ", event.newSelection())));
        graph.onConnectionCreated(event -> status.text("Connected " + event.from().itemId()
                + " to " + event.to().itemId()));
        graph.onItemMoveEnded(event -> status.text("Moved " + event.itemId()));

        VBox sample = new VBox();
        sample.spacing(10.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(status);
        sample.addChild(tools);
        sample.addChild(graph);
        root.addChild(samplePanel("Typed node graph", "Ports expose typed inputs and outputs. Drag nodes and wires; use the minimap or Frame all to navigate.", sample,
                "NodeGraph graph = new NodeGraph()\n        .selectionMode(NodeGraphSelectionMode.MULTIPLE)\n        .minimapEnabled(true);\nNodeGraphItem source = graph.addItem(\"source\", sourceView, 36, 52);\nsource.addPort(\"out\", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f)\n        .type(\"item\");\ngraph.addConnection(\"link\", new NodeGraphPortRef(\"source\", \"out\"),\n        new NodeGraphPortRef(\"target\", \"in\"));"));
    }

    private VBox graphNode(String title, String description) {
        VBox content = new VBox();
        content.spacing(5.0f);
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).padding(8.0f, 6.0f));
        content.addChild(label(title, UniGuiDemo.GalleryTemplate.color(239, 245, 250)));
        content.addChild(label(description, UniGuiDemo.GalleryTemplate.color(162, 181, 197)));
        return content;
    }
}
