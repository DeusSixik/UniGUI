package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Запись хода графа для отладки: какие ноды отработали, в каком порядке и что выдали.
 *
 * <p>Запись делится на запуски: каждое событие, которое входит в граф (включая продолжение после задержки), открывает
 * новый {@link Run}. Внутри запуска шаги идут в порядке исполнения, чистые ноды записываются в момент, когда у них
 * запросили данные. Запись ограничена ({@link #MAX_RUNS}, {@link #MAX_STEPS}): старые запуски отбрасываются, а
 * бесконечный цикл не раздувает память. Класс не знает ни об экране, ни о редакторе.</p>
 */
public final class GraphTrace implements GraphExecutor.Trace {
    /** Предел хранимых запусков. */
    public static final int MAX_RUNS = 50;
    /** Предел шагов в одном запуске. */
    public static final int MAX_STEPS = 500;

    /**
     * Один шаг.
     *
     * @param nodeId        идентификатор ноды
     * @param type          тип ноды
     * @param pure          {@code true} для чистой ноды, вычисленной по запросу
     * @param outputs       выданные значения данных в виде текста
     * @param continuations выходы потока, по которым пошло исполнение
     */
    public record Step(String nodeId, String type, boolean pure, Map<String, String> outputs,
                       List<String> continuations) {
        public Step {
            outputs = Map.copyOf(outputs);
            continuations = List.copyOf(continuations);
        }
    }

    /**
     * Один запуск графа.
     *
     * @param entry идентификатор ноды, с которой начался запуск
     * @param steps шаги по порядку
     * @param error ошибка запуска или пустая строка
     * @param failedNode нода, на которой запуск упал, или пустая строка
     */
    public record Run(String entry, List<Step> steps, String error, String failedNode) {
        public Run {
            steps = List.copyOf(steps);
            error = error == null ? "" : error;
            failedNode = failedNode == null ? "" : failedNode;
        }
    }

    private final List<Run> runs = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private String entry;
    private List<Step> steps;
    private String failedNode = "";
    private int depth;

    /**
     * Начинает запуск.
     *
     * @param entryNode нода-вход
     */
    void begin(String entryNode) {
        if (depth++ > 0) return; // вложенный вход (пользовательское событие) продолжает текущий запуск
        entry = entryNode;
        steps = new ArrayList<>();
        failedNode = "";
    }

    /**
     * Завершает запуск и сообщает слушателям.
     *
     * @param error ошибка или {@code null}
     */
    void end(String error) {
        if (depth > 0) depth--;
        if (depth > 0 || steps == null) return;
        runs.add(new Run(entry, steps, error, failedNode));
        while (runs.size() > MAX_RUNS) runs.remove(0);
        steps = null;
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    @Override
    public void step(GraphModel.Node node, boolean pure, Map<String, Object> outputs, List<String> continuations) {
        if (steps == null || steps.size() >= MAX_STEPS) return;
        Map<String, String> text = new LinkedHashMap<>();
        outputs.forEach((port, value) -> text.put(port, String.valueOf(value)));
        steps.add(new Step(node.id(), baseType(node.typeId()), pure, text, continuations));
    }

    @Override
    public void failed(GraphModel.Node node, RuntimeException failure) {
        if (failedNode.isEmpty()) failedNode = node.id();
    }

    /** Ключ поведения имеет вид {@code тип#идентификатор}; в записи нужен только тип. */
    private static String baseType(String behaviorKey) {
        int hash = behaviorKey.indexOf('#');
        return hash < 0 ? behaviorKey : behaviorKey.substring(0, hash);
    }

    /** @return запуски от старых к новым */
    public List<Run> runs() {
        return List.copyOf(runs);
    }

    /** @return последний завершённый запуск или {@code null} */
    public Run last() {
        return runs.isEmpty() ? null : runs.get(runs.size() - 1);
    }

    /** Забывает все запуски. */
    public void clear() {
        runs.clear();
        steps = null;
        depth = 0;
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    /**
     * @param listener вызывается после каждого завершённого запуска и после {@link #clear()}
     */
    public void onChange(Runnable listener) {
        listeners.add(listener);
    }
}
