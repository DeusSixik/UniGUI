package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Статическая проверка графа без исполнения.
 *
 * <p>Правила: обязательный вход без провода и без значения по умолчанию — ошибка; цикл по data-проводам между
 * чистыми нодами — ошибка (вычислить такие ноды невозможно); нода с входом потока, до которой не доходит ни
 * одна точка входа, — предупреждение. Циклы потока выполнения — это обычные циклы и ошибкой не считаются, как и
 * цикл данных через исполняемую ноду: её выходы кэшируются между шагами.</p>
 */
@AiDoc(
        summary = "Static graph checks: missing required inputs, data cycles among pure nodes and unreachable exec nodes.",
        purpose = "Finds graphs that cannot run correctly before running them, so the editor can mark broken nodes.",
        useWhen = {
                "Call `GraphExecutor.validate(model)` to include missing-behavior checks.",
                "Call `NodeGraph.validate(executor)` to mark nodes with a red or amber border."
        },
        criticalRules = {
                "Only edges leaving a pure node count as data dependencies; exec outputs are cached state.",
                "An entry is a node with a flow output and no flow input.",
                "A required input is satisfied by a wire or by an editor default."
        },
        related = {"GraphExecutor", "GraphModel", "GraphIssue"})
@WikiPage(title = "GraphValidator", slug = "graph-validator", category = "<key:graph.runtime.category>", summary = "<key:graph.validator.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.validator.overview>", order = 10)
public final class GraphValidator {
    private GraphValidator() {
    }

    /**
     * Проверяет граф.
     *
     * @param model модель
     * @param pure  определяет, чистая ли нода (её выходы вычисляются по запросу)
     * @return проблемы в порядке нод модели
     */
    public static List<GraphIssue> validate(GraphModel model, Predicate<GraphModel.Node> pure) {
        List<GraphIssue> issues = new ArrayList<>();
        requiredInputs(model, issues);
        dataCycles(model, pure, issues);
        unreachable(model, issues);
        return issues;
    }

    private static void requiredInputs(GraphModel model, List<GraphIssue> issues) {
        for (GraphModel.Node node : model.nodes()) {
            for (GraphModel.Port port : node.ports()) {
                if (!port.required() || port.flow() || !port.input()) continue;
                boolean wired = model.incoming(node.id(), port.id()) != null;
                if (!wired && !node.values().containsKey(port.id())) {
                    issues.add(new GraphIssue(GraphIssue.Severity.ERROR, node.id(), port.id(),
                            "Required input '" + port.id() + "' is not connected"));
                }
            }
        }
    }

    /**
     * Находит ноды на циклах двумя проходами Кана: сначала с входов, потом с выходов; остаток лежит на циклах.
     */
    private static void dataCycles(GraphModel model, Predicate<GraphModel.Node> pure, List<GraphIssue> issues) {
        Map<String, Set<String>> next = new HashMap<>();
        Map<String, Set<String>> previous = new HashMap<>();
        for (GraphModel.Node node : model.nodes()) {
            next.put(node.id(), new LinkedHashSet<>());
            previous.put(node.id(), new LinkedHashSet<>());
        }
        for (GraphModel.Wire wire : model.wires()) {
            GraphModel.Node from = model.node(wire.fromNode());
            GraphModel.Node to = model.node(wire.toNode());
            if (from == null || to == null || !pure.test(from)) continue;
            GraphModel.Port port = from.port(wire.fromPort());
            if (port == null || port.flow()) continue;
            next.get(from.id()).add(to.id());
            previous.get(to.id()).add(from.id());
        }
        Set<String> remaining = new LinkedHashSet<>(next.keySet());
        peel(remaining, previous);
        peel(remaining, next);
        for (GraphModel.Node node : model.nodes()) {
            if (remaining.contains(node.id())) {
                issues.add(new GraphIssue(GraphIssue.Severity.ERROR, node.id(), "",
                        "Data cycle: this node depends on itself through data wires"));
            }
        }
    }

    /**
     * Убирает из множества ноды без зависимостей внутри множества, пока такие есть.
     */
    private static void peel(Set<String> remaining, Map<String, Set<String>> dependencies) {
        Map<String, Integer> degree = new HashMap<>();
        for (String id : remaining) {
            int count = 0;
            for (String dependency : dependencies.get(id)) {
                if (remaining.contains(dependency)) count++;
            }
            degree.put(id, count);
        }
        ArrayDeque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : degree.entrySet()) {
            if (entry.getValue() == 0) ready.add(entry.getKey());
        }
        Map<String, Set<String>> dependents = new HashMap<>();
        for (String id : remaining) {
            for (String dependency : dependencies.get(id)) {
                dependents.computeIfAbsent(dependency, key -> new HashSet<>()).add(id);
            }
        }
        while (!ready.isEmpty()) {
            String id = ready.poll();
            if (!remaining.remove(id)) continue;
            for (String dependent : dependents.getOrDefault(id, Set.of())) {
                if (!remaining.contains(dependent)) continue;
                int left = degree.merge(dependent, -1, Integer::sum);
                if (left == 0) ready.add(dependent);
            }
        }
    }

    private static void unreachable(GraphModel model, List<GraphIssue> issues) {
        ArrayDeque<GraphModel.Node> queue = new ArrayDeque<>();
        Set<String> reached = new HashSet<>();
        for (GraphModel.Node node : model.nodes()) {
            if (node.hasFlowOutput() && !node.hasFlowInput() && reached.add(node.id())) queue.add(node);
        }
        while (!queue.isEmpty()) {
            GraphModel.Node node = queue.poll();
            for (GraphModel.Port port : node.ports()) {
                if (!port.flow() || !port.output()) continue;
                for (GraphModel.Wire wire : model.outgoing(node.id(), port.id())) {
                    GraphModel.Node target = model.node(wire.toNode());
                    if (target != null && reached.add(target.id())) queue.add(target);
                }
            }
        }
        for (GraphModel.Node node : model.nodes()) {
            if (node.hasFlowInput() && !reached.contains(node.id())) {
                issues.add(new GraphIssue(GraphIssue.Severity.WARNING, node.id(), "",
                        "Never reached by the execution flow"));
            }
        }
    }
}
