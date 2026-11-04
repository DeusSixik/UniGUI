package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.widgets.graph.node.RerouteModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Исполнитель графа: ходит по exec-проводам и вычисляет data-входы по требованию.
 *
 * <p>Запуск начинается с ноды-события: {@link #fire(GraphModel, String)}. Каждая достигнутая нода
 * выполняется своим {@link NodeBehavior}; поведение читает входы, публикует выходы и выбирает продолжение.
 * Входы с проводом берутся у ноды-источника: чистая нода вычисляется на месте, исполняемая отдаёт выход,
 * опубликованный на предыдущих шагах. Чистые ноды перевычисляются для каждого исполняемого шага, поэтому
 * значения внутри цикла всегда свежие.</p>
 */
@AiDoc(
        summary = "Runs a GraphModel: follows exec wires from an event node and pulls data inputs on demand from pure nodes.",
        purpose = "Turns a node graph into a program for Blueprint-style scripting and data-flow evaluation.",
        useWhen = {
                "Register a NodeBehavior per node type, then `fire(graph.model(), startNodeId)`.",
                "Protect against runaway loops with `maxSteps(...)`."
        },
        criticalRules = {
                "Every node type that can run needs a registered behavior; reroutes are built in.",
                "A data cycle among pure nodes throws GraphExecutionException instead of overflowing the stack.",
                "The step limit counts exec node runs and throws when exceeded, which stops infinite loops.",
                "The executor holds no state between `fire` calls; results are returned in GraphResult."
        },
        related = {"NodeBehavior", "NodeContext", "GraphModel", "GraphValidator"},
        examples = {
                @AiDoc.Example(
                        title = "Run a start event that creates and greets an NPC",
                        order = 10,
                        code = """
                                GraphExecutor executor = new GraphExecutor()
                                        .register("event.start", NodeBehavior.exec(c -> c.continueWith("then")))
                                        .register("npc.say", NodeBehavior.exec(c -> {
                                            log.add(c.text("who") + ": " + c.text("text"));
                                            c.continueWith("then");
                                        }));
                                GraphResult result = executor.fire(graph.model(), "start");
                                """)
        })
@WikiPage(title = "GraphExecutor", slug = "graph-executor", category = "<key:graph.runtime.category>", summary = "<key:graph.executor.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.executor.overview>", order = 10)
@WikiExample(title = "<key:graph.executor.example.title>", code = "GraphExecutor executor = new GraphExecutor()\n        .register(\"event.start\", NodeBehavior.exec(context -> context.continueWith(\"next\")))\n        .register(\"value.add\", NodeBehavior.pure(context ->\n                context.output(\"sum\", context.number(\"left\") + context.number(\"right\"))));\nGraphResult result = executor.fire(graph.model(), \"start\");")
public class GraphExecutor {
    /**
     * Лимит исполняемых шагов по умолчанию.
     */
    public static final int DEFAULT_MAX_STEPS = 10_000;

    private final Map<String, NodeBehavior> behaviors = new HashMap<>();
    private Trace trace;

    /**
     * Наблюдатель за ходом исполнения: отладчик записывает по нему, какие ноды отработали и что они выдали.
     */
    public interface Trace {
        /**
         * Нода отработала.
         *
         * @param node          нода
         * @param pure          {@code true}, если это вычисление чистой ноды по запросу данных
         * @param outputs       значения, которые нода выдала на выходы данных
         * @param continuations выходы потока, по которым исполнение пошло дальше
         */
        void step(GraphModel.Node node, boolean pure, Map<String, Object> outputs, List<String> continuations);

        /**
         * Нода завершилась ошибкой.
         *
         * @param node    нода
         * @param failure причина
         */
        default void failed(GraphModel.Node node, RuntimeException failure) {
        }
    }

    /**
     * @param trace наблюдатель или {@code null}
     * @return этот исполнитель
     */
    public GraphExecutor trace(Trace trace) {
        this.trace = trace;
        return this;
    }
    private int maxSteps = DEFAULT_MAX_STEPS;

    /**
     * Создаёт исполнитель со встроенными поведениями reroute.
     */
    public GraphExecutor() {
        register(RerouteModule.DATA_TYPE_ID, NodeBehavior.pure(
                context -> context.output(RerouteModule.OUTPUT, context.input(RerouteModule.INPUT))));
        register(RerouteModule.FLOW_TYPE_ID, NodeBehavior.exec(
                context -> context.continueWith(RerouteModule.OUTPUT)));
    }

    /**
     * @param typeId   id типа ноды
     * @param behavior поведение
     * @return этот исполнитель
     */
    public GraphExecutor register(String typeId, NodeBehavior behavior) {
        if (typeId != null && behavior != null) behaviors.put(typeId, behavior);
        return this;
    }

    /**
     * @param typeId id типа ноды
     * @return зарегистрированное поведение или {@code null}
     */
    public NodeBehavior behavior(String typeId) {
        return behaviors.get(typeId);
    }

    /**
     * @return лимит исполняемых шагов за один запуск
     */
    public int maxSteps() {
        return maxSteps;
    }

    /**
     * @param maxSteps лимит исполняемых шагов за один запуск, не меньше 1
     * @return этот исполнитель
     */
    public GraphExecutor maxSteps(int maxSteps) {
        this.maxSteps = Math.max(1, maxSteps);
        return this;
    }

    /**
     * Запускает граф с ноды-события.
     *
     * @param model       модель графа
     * @param startNodeId id ноды, с которой начинается поток
     * @return итог запуска
     * @throws GraphExecutionException если ноды нет, нет поведения, найден цикл данных, превышен лимит шагов
     *                                 или поведение ноды выбросило исключение
     */
    public GraphResult fire(GraphModel model, String startNodeId) {
        GraphModel.Node start = model.node(startNodeId);
        if (start == null) {
            throw new GraphExecutionException("No such node: " + startNodeId);
        }
        Execution execution = new Execution(model);
        execution.runChain(start);
        return new GraphResult(execution.steps, execution.copyOutputs());
    }

    /**
     * Проверяет граф: правила {@link GraphValidator} плюс поведения для всех типов нод.
     *
     * @param model модель графа
     * @return проблемы
     */
    public List<GraphIssue> validate(GraphModel model) {
        List<GraphIssue> issues = new ArrayList<>(GraphValidator.validate(model, node -> {
            NodeBehavior behavior = behaviors.get(node.typeId());
            return behavior != null && behavior.pure();
        }));
        for (GraphModel.Node node : model.nodes()) {
            if (node.typeId().isEmpty()) {
                issues.add(new GraphIssue(GraphIssue.Severity.WARNING, node.id(), "",
                        "The node has no type, so it cannot run"));
            } else if (!behaviors.containsKey(node.typeId())) {
                issues.add(new GraphIssue(GraphIssue.Severity.ERROR, node.id(), "",
                        "No behavior registered for node type '" + node.typeId() + "'"));
            }
        }
        return issues;
    }

    /**
     * Состояние одного запуска.
     */
    private final class Execution {
        private final GraphModel model;
        private final Map<String, Map<String, Object>> execOutputs = new LinkedHashMap<>();
        private final Map<String, Map<String, Object>> pureCache = new HashMap<>();
        private final Set<String> evaluating = new HashSet<>();
        private int steps;

        private Execution(GraphModel model) {
            this.model = model;
        }

        Map<String, Map<String, Object>> copyOutputs() {
            Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            execOutputs.forEach((id, outputs) -> copy.put(id, new LinkedHashMap<>(outputs)));
            return copy;
        }

        /**
         * Исполняет цепочку нод по потоку. Последнее продолжение обходится циклом, а не рекурсией, чтобы длинные
         * цепочки не переполняли стек.
         */
        void runChain(GraphModel.Node start) {
            GraphModel.Node current = start;
            while (current != null) {
                Context context = step(current);
                List<GraphModel.Node> targets = new ArrayList<>();
                for (String port : context.continuations) {
                    for (GraphModel.Wire wire : model.outgoing(current.id(), port)) {
                        targets.add(requireNode(wire.toNode()));
                    }
                }
                if (targets.isEmpty()) return;
                for (int i = 0; i < targets.size() - 1; i++) {
                    runChain(targets.get(i));
                }
                current = targets.get(targets.size() - 1);
            }
        }

        private Context step(GraphModel.Node node) {
            if (++steps > maxSteps) {
                throw new GraphExecutionException("Step limit of " + maxSteps + " exceeded at node '" + node.id()
                        + "'; is there an infinite loop?");
            }
            NodeBehavior behavior = behaviorOf(node);
            pureCache.clear();
            Context context = new Context(node, behavior.pure()
                    ? new HashMap<>() : execOutputs.computeIfAbsent(node.id(), id -> new LinkedHashMap<>()));
            invoke(behavior, context, node);
            pureCache.clear();
            if (trace != null) trace.step(node, false, new LinkedHashMap<>(context.outputs), List.copyOf(context.continuations));
            return context;
        }

        Object pull(GraphModel.Node node, String portId) {
            GraphModel.Wire wire = model.incoming(node.id(), portId);
            if (wire == null) return node.values().get(portId);
            return outputOf(requireNode(wire.fromNode()), wire.fromPort());
        }

        private Object outputOf(GraphModel.Node source, String portId) {
            NodeBehavior behavior = behaviorOf(source);
            if (!behavior.pure()) {
                Map<String, Object> published = execOutputs.get(source.id());
                return published == null ? null : published.get(portId);
            }
            Map<String, Object> cached = pureCache.get(source.id());
            if (cached == null) {
                if (!evaluating.add(source.id())) {
                    throw new GraphExecutionException("Data cycle through node '" + source.id() + "'");
                }
                try {
                    Context context = new Context(source, new HashMap<>());
                    invoke(behavior, context, source);
                    cached = context.outputs;
                    pureCache.put(source.id(), cached);
                    if (trace != null) trace.step(source, true, new LinkedHashMap<>(cached), List.of());
                } finally {
                    evaluating.remove(source.id());
                }
            }
            return cached.get(portId);
        }

        private void invoke(NodeBehavior behavior, Context context, GraphModel.Node node) {
            try {
                behavior.run(context);
            } catch (GraphExecutionException failure) {
                if (trace != null) trace.failed(node, failure);
                throw failure;
            } catch (RuntimeException failure) {
                GraphExecutionException wrapped = new GraphExecutionException("Node '" + node.id() + "' ("
                        + node.typeId() + ") failed: " + failure.getMessage(), failure);
                if (trace != null) trace.failed(node, wrapped);
                throw wrapped;
            }
        }

        private NodeBehavior behaviorOf(GraphModel.Node node) {
            NodeBehavior behavior = behaviors.get(node.typeId());
            if (behavior == null) {
                throw new GraphExecutionException("No behavior registered for node type '" + node.typeId()
                        + "' (node '" + node.id() + "')");
            }
            return behavior;
        }

        private GraphModel.Node requireNode(String id) {
            GraphModel.Node node = model.node(id);
            if (node == null) throw new GraphExecutionException("A wire leads to a missing node: " + id);
            return node;
        }

        /**
         * Окружение одной ноды.
         */
        private final class Context implements NodeContext {
            private final GraphModel.Node node;
            private final Map<String, Object> outputs;
            private final List<String> continuations = new ArrayList<>();

            private Context(GraphModel.Node node, Map<String, Object> outputs) {
                this.node = node;
                this.outputs = outputs;
            }

            @Override
            public GraphModel.Node node() {
                return node;
            }

            @Override
            public Object input(String portId) {
                GraphModel.Port port = requirePort(portId);
                if (port.flow() || !port.input()) {
                    throw new GraphExecutionException("Port " + node.id() + "." + portId + " is not a data input");
                }
                return pull(node, portId);
            }

            @Override
            public boolean connected(String portId) {
                GraphModel.Port port = requirePort(portId);
                return port.input()
                        ? model.incoming(node.id(), portId) != null
                        : !model.outgoing(node.id(), portId).isEmpty();
            }

            @Override
            public void output(String portId, Object value) {
                GraphModel.Port port = requirePort(portId);
                if (port.flow() || !port.output()) {
                    throw new GraphExecutionException("Port " + node.id() + "." + portId + " is not a data output");
                }
                outputs.put(portId, value);
            }

            @Override
            public void continueWith(String flowOutputId) {
                requireFlowOutput(flowOutputId);
                continuations.add(flowOutputId);
            }

            @Override
            public void runFlow(String flowOutputId) {
                requireFlowOutput(flowOutputId);
                for (GraphModel.Wire wire : model.outgoing(node.id(), flowOutputId)) {
                    runChain(requireNode(wire.toNode()));
                }
            }

            private GraphModel.Port requirePort(String portId) {
                GraphModel.Port port = node.port(portId);
                if (port == null) {
                    throw new GraphExecutionException("Node '" + node.id() + "' has no port '" + portId + "'");
                }
                return port;
            }

            private void requireFlowOutput(String portId) {
                GraphModel.Port port = requirePort(portId);
                if (!port.flow() || !port.output()) {
                    throw new GraphExecutionException("Port " + node.id() + "." + portId + " is not a flow output");
                }
            }
        }
    }
}
