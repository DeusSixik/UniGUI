package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Окружение, в котором работает {@link NodeBehavior}: входы, выходы и управление потоком выполнения.
 */
@AiDoc(
        summary = "Execution environment handed to a NodeBehavior: reads inputs, writes outputs and steers the exec flow.",
        purpose = "Gives node implementations a small, UI-free API for Blueprint-style and data-flow execution.",
        useWhen = {
                "Read a wired or default input value with `input`, `number`, `text`, `bool` or `vector`.",
                "Publish results with `output` and continue the flow with `continueWith` or `runFlow`."
        },
        criticalRules = {
                "`input` returns the upstream output when the input is wired, otherwise the editor default, otherwise null.",
                "`continueWith` queues a continuation that runs after the behavior returns, in call order.",
                "`runFlow` runs the chain behind a flow output immediately, which is how loops and branches are written.",
                "Pure nodes are re-evaluated for every consuming exec step, so they must not keep state."
        },
        related = {"NodeBehavior", "GraphExecutor", "GraphModel"})
@WikiPage(title = "NodeContext", slug = "node-context", category = "<key:graph.runtime.category>", summary = "<key:node.context.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.context.overview>", order = 10)
public interface NodeContext {
    /**
     * @return нода, которую исполняют
     */
    GraphModel.Node node();

    /**
     * @param portId id входного data-порта
     * @return значение: выход подключённой ноды, иначе значение редактора, иначе {@code null}
     * @throws GraphExecutionException если порта нет или это порт потока
     */
    Object input(String portId);

    /**
     * @param portId id входного порта
     * @return значение как число; {@code 0}, если значения нет
     * @throws GraphExecutionException если значение не число
     */
    default double number(String portId) {
        Object value = input(portId);
        if (value == null) return 0.0;
        if (value instanceof Number number) return number.doubleValue();
        throw new GraphExecutionException("Input " + node().id() + "." + portId + " is not a number: " + value);
    }

    /**
     * @param portId id входного порта
     * @return значение как текст; пустая строка, если значения нет
     */
    default String text(String portId) {
        Object value = input(portId);
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * @param portId id входного порта
     * @return значение как флаг; {@code false}, если значения нет
     * @throws GraphExecutionException если значение не Boolean
     */
    default boolean bool(String portId) {
        Object value = input(portId);
        if (value == null) return false;
        if (value instanceof Boolean flag) return flag;
        throw new GraphExecutionException("Input " + node().id() + "." + portId + " is not a boolean: " + value);
    }

    /**
     * @param portId id входного порта
     * @return значение как вектор; {@code null}, если значения нет
     * @throws GraphExecutionException если значение не {@code double[]}
     */
    default double[] vector(String portId) {
        Object value = input(portId);
        if (value == null) return null;
        if (value instanceof double[] vector) return vector;
        throw new GraphExecutionException("Input " + node().id() + "." + portId + " is not a vector: " + value);
    }

    /**
     * @param portId id порта этой ноды
     * @return {@code true}, если к входу подключён провод или из выхода выходит хотя бы один
     */
    boolean connected(String portId);

    /**
     * Публикует значение выходного data-порта.
     *
     * @param portId id выходного порта
     * @param value  значение для потребителей
     */
    void output(String portId, Object value);

    /**
     * Ставит в очередь продолжение потока через выходной порт; выполнится после возврата из поведения.
     *
     * @param flowOutputId id выходного порта потока
     */
    void continueWith(String flowOutputId);

    /**
     * Немедленно исполняет цепочку за выходным портом потока. Годится для тел циклов и ветвей.
     *
     * @param flowOutputId id выходного порта потока
     */
    void runFlow(String flowOutputId);
}
