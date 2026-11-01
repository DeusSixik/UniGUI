package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Поведение типа ноды при исполнении графа.
 *
 * <p>Есть два вида. <b>Чистая</b> нода ({@link #pure()}) — вычисление без побочных эффектов: она запускается,
 * когда потребитель запрашивает её выход, и пишет результат через {@link NodeContext#output}. <b>Исполняемая</b>
 * нода запускается потоком выполнения (exec-проводами), может иметь побочные эффекты, публикует выходы и
 * решает, куда идти дальше ({@link NodeContext#continueWith}, {@link NodeContext#runFlow}).</p>
 */
@AiDoc(
        summary = "Runtime behavior of a node type: either a pure computation or an exec-flow step with side effects.",
        purpose = "Lets users implement what each node does without touching the editor widgets.",
        useWhen = {
                "Register a behavior per node type with `GraphExecutor.register(typeId, behavior)`.",
                "Use `NodeBehavior.pure(...)` for math/logic nodes and `NodeBehavior.exec(...)` for events and actions."
        },
        criticalRules = {
                "A pure behavior is evaluated on demand and must be deterministic for the same inputs.",
                "An exec behavior is run once per arrival of the flow; its outputs stay readable for later nodes.",
                "Throwing from a behavior aborts the run with GraphExecutionException naming the node."
        },
        related = {"NodeContext", "GraphExecutor"})
@WikiPage(title = "NodeBehavior", slug = "node-behavior", category = "<key:graph.category>", summary = "<key:node.behavior.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.behavior.overview>", order = 10)
@FunctionalInterface
public interface NodeBehavior {
    /**
     * Выполняет ноду.
     *
     * @param context окружение исполнения
     */
    void run(NodeContext context);

    /**
     * @return {@code true} для чистых нод: они вычисляются по запросу потребителя, а не потоком выполнения
     */
    default boolean pure() {
        return false;
    }

    /**
     * @param behavior вычисление
     * @return чистое поведение
     */
    static NodeBehavior pure(NodeBehavior behavior) {
        return new NodeBehavior() {
            @Override
            public void run(NodeContext context) {
                behavior.run(context);
            }

            @Override
            public boolean pure() {
                return true;
            }
        };
    }

    /**
     * @param behavior действие
     * @return исполняемое поведение
     */
    static NodeBehavior exec(NodeBehavior behavior) {
        return behavior;
    }
}
