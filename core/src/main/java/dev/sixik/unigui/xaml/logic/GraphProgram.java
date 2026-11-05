package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphIssue;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import dev.sixik.unigui.widgets.graph.runtime.NodeBehavior;

import java.util.List;
import java.util.Map;

/**
 * Скомпилированный граф компонента: проверенная модель, точки входа и фабрики поведения нод.
 *
 * <p>Программа не привязана к экземпляру: один раз скомпилированная по определению компонента, она создаёт
 * исполнитель для каждого экземпляра ({@link #executor(GraphRuntime)}), потому что поведение нод работает с
 * параметрами и событиями именно этого экземпляра.</p>
 */
public final class GraphProgram {
    /**
     * Создаёт поведение ноды для конкретного экземпляра.
     */
    @FunctionalInterface
    interface BehaviorFactory {
        NodeBehavior create(GraphRuntime runtime);
    }

    /**
     * Точка входа: нода-событие и имя события компонента, которое её запускает.
     *
     * @param nodeId идентификатор ноды
     * @param event  имя входного события либо {@code Construct}/{@code Destruct}
     */
    public record Entry(String nodeId, String event) {
    }

    /**
     * Пользовательское событие графа: подпрограмма, которую вызывает нода {@code Event.Call} или подписка
     * {@code Dispatcher.Bind}.
     *
     * @param nodeId идентификатор ноды {@code Event.Custom}
     * @param name   имя события
     * @param args   аргументы
     */
    public record CustomEvent(String nodeId, String name, List<dev.sixik.unigui.xaml.component.Contract.Arg> args) {
        public CustomEvent {
            args = List.copyOf(args);
        }
    }

    private final GraphModel model;
    private final List<Entry> entries;
    private final Map<String, CustomEvent> customEvents;
    private final Map<String, BehaviorFactory> factories;
    private final List<GraphIssue> warnings;
    private final long stamp;

    GraphProgram(GraphModel model, List<Entry> entries, Map<String, BehaviorFactory> factories,
                 List<GraphIssue> warnings, long stamp, Map<String, CustomEvent> customEvents) {
        this.customEvents = Map.copyOf(customEvents);
        this.model = model;
        this.entries = List.copyOf(entries);
        this.factories = Map.copyOf(factories);
        this.warnings = List.copyOf(warnings);
        this.stamp = stamp;
    }

    /**
     * @return проверенная модель графа
     */
    public GraphModel model() {
        return model;
    }

    /**
     * @return точки входа в порядке документа
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * @return предупреждения проверки (например, нода, до которой не доходит ни одно событие)
     */
    public List<GraphIssue> warnings() {
        return warnings;
    }

    /**
     * @return пользовательские события графа по имени
     */
    public Map<String, CustomEvent> customEvents() {
        return customEvents;
    }

    /**
     * @return состояние реестров функций и компонентов, на котором собрана программа
     */
    public long stamp() {
        return stamp;
    }

    /**
     * Создаёт исполнитель с поведением, привязанным к экземпляру.
     *
     * @param runtime среда экземпляра
     * @return исполнитель
     */
    GraphExecutor executor(GraphRuntime runtime) {
        GraphExecutor executor = new GraphExecutor();
        factories.forEach((typeId, factory) -> executor.register(typeId, factory.create(runtime)));
        return executor;
    }
}
