package dev.sixik.unigui.xaml.preview;

import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.ComponentHost;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.edit.NodePath;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.logic.GraphTrace;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Живой предпросмотр компонента: строит экземпляр по документу {@code <Component>} и пересобирает его при каждой
 * правке, как {@link Preview} делает для экрана.
 *
 * <p>Экземпляр показывается с примерными значениями параметров: сначала значения по умолчанию из контракта, поверх
 * них то, что автор задал через {@link #sample}. Примерные значения переживают пересборку, поэтому правка тела не
 * сбрасывает то, что автор набрал в панели параметров. Исходящие события экземпляра записываются в журнал
 * ({@link #events()}), так что клик по кнопке в предпросмотре виден как {@code Buy(id=sword)}. Входящие события можно
 * отправить вручную ({@link #send}).</p>
 *
 * <p>Как и у {@link Preview}, при ошибке документа прежний экземпляр остаётся на месте, а причина доступна через
 * {@link #error()}.</p>
 */
public final class ComponentPreview implements AutoCloseable {
    /** Предел записей журнала событий: старые отбрасываются. */
    public static final int MAX_EVENTS = 200;

    private final Schema schema;
    private final LoadContext context;
    private final Map<String, Object> samples = new LinkedHashMap<>();
    private final List<String> events = new ArrayList<>();
    private final GraphTrace trace = new GraphTrace();
    private ComponentHost host;
    private UiNode shownRoot;
    private java.util.function.Consumer<String> eventListener;
    private Map<UiNode, Widget> widgets = new IdentityHashMap<>();
    private List<Map.Entry<UiNode, Widget>> order = List.of();
    private String error = "";
    private boolean closed;

    /**
     * Создаёт предпросмотр.
     *
     * @param schema  схема типов
     * @param options настройки загрузки: резолвер текстур и команды окружающего документа
     */
    public ComponentPreview(Schema schema, LoadOptions options) {
        if (schema == null) throw new IllegalArgumentException("schema must not be null");
        this.schema = schema;
        LoadOptions effective = options == null ? LoadOptions.DEFAULT : options;
        this.context = new LoadContext(effective.textureResolver(), effective.commands());
    }

    /**
     * Пытается показать документ компонента.
     *
     * @param document документ с корнем {@code <Component>}
     * @return успех или причина, по которой осталось прежнее дерево
     */
    public Preview.Result show(UiDocument document) {
        if (closed) throw new IllegalStateException("The preview is closed");
        if (document == null) {
            clear();
            return new Preview.Result(true, "");
        }
        Map<UiNode, Widget> created = new IdentityHashMap<>();
        List<Map.Entry<UiNode, Widget>> createdInOrder = new ArrayList<>();
        try {
            ComponentDefinition definition = ComponentDefinition.parse("preview", document);
            ComponentHost next = new ComponentHost(definition, schema).observe((node, widget) -> {
                created.put(node, widget);
                createdInOrder.add(Map.entry(node, widget));
            });
            trace.clear();
            next.traceTo(trace);
            applySamples(next, definition.contract());
            subscribe(next, definition.contract());
            next.build(context);
            if (host != null) host.dispose();
            host = next;
            shownRoot = document.root();
            widgets = created;
            order = createdInOrder;
            error = "";
            return new Preview.Result(true, "");
        } catch (RuntimeException failure) {
            error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            return new Preview.Result(false, error);
        }
    }

    /** Заданные значения подставляются в параметры, которые ещё есть в контракте и подходят по типу. */
    private void applySamples(ComponentHost target, Contract contract) {
        for (Map.Entry<String, Object> sample : new ArrayList<>(samples.entrySet())) {
            if (contract.param(sample.getKey()).isEmpty()) continue;
            try {
                target.instance().set(sample.getKey(), sample.getValue());
            } catch (IllegalArgumentException staleType) {
                // Тип параметра изменили: прежнее примерное значение больше не подходит.
                samples.remove(sample.getKey());
            }
        }
    }

    private void subscribe(ComponentHost target, Contract contract) {
        for (Contract.Event event : contract.events()) {
            if (event.direction() != Contract.Direction.OUT) continue;
            target.instance().onEvent(event.name(), args -> log(event.name() + format(args)));
        }
    }

    private static String format(Args args) {
        if (args.isEmpty()) return "";
        StringBuilder text = new StringBuilder("(");
        args.asMap().forEach((name, value) -> {
            if (text.length() > 1) text.append(", ");
            text.append(name).append('=').append(value);
        });
        return text.append(')').toString();
    }

    private void log(String line) {
        events.add(line);
        while (events.size() > MAX_EVENTS) events.remove(0);
        if (eventListener != null) eventListener.accept(line);
    }

    /**
     * Подписывает наблюдателя на записи журнала событий (панель редактора обновляет по нему список).
     *
     * @param listener получает каждую новую запись или {@code null}
     */
    public void onEvent(java.util.function.Consumer<String> listener) {
        this.eventListener = listener;
    }

    /**
     * Задаёт примерное значение параметра; показанный экземпляр обновляется сразу.
     *
     * @param param имя параметра
     * @param value значение подходящего типа
     * @throws IllegalArgumentException если параметра нет или значение не того типа
     */
    public void sample(String param, Object value) {
        if (host != null) host.instance().set(param, value);
        samples.put(param, value);
    }

    /** Забывает все примерные значения: параметры возвращаются к значениям по умолчанию при следующей сборке. */
    public void resetSamples() {
        samples.clear();
    }

    /** @return примерные значения, заданные автором */
    public Map<String, Object> samples() {
        return Map.copyOf(samples);
    }

    /**
     * Отправляет показанному экземпляру входящее событие и сразу обрабатывает его.
     *
     * @param event имя события с {@code Direction="In"}
     * @param args  аргументы по контракту
     * @throws IllegalStateException    если ничего не показано
     * @throws IllegalArgumentException если события нет или аргументы не подходят
     */
    public void send(String event, Args args) {
        if (host == null) throw new IllegalStateException("Nothing is shown");
        host.instance().send(event, args);
        host.instance().flush();
    }

    /**
     * Продвигает время экземпляра: срабатывают задержки графа и таймеры.
     *
     * @param seconds прошедшее время
     */
    public void advance(float seconds) {
        if (host != null) host.instance().advance(seconds);
    }

    /** @return запись хода графа показанного экземпляра; очищается при каждой пересборке */
    public GraphTrace trace() {
        return trace;
    }

    /** @return журнал исходящих событий, старые записи первыми */
    public List<String> events() {
        return List.copyOf(events);
    }

    /** Очищает журнал событий. */
    public void clearEvents() {
        events.clear();
    }

    /** Убирает показанный экземпляр. Примерные значения и журнал остаются. */
    public void clear() {
        if (host != null) host.dispose();
        host = null;
        shownRoot = null;
        widgets = new IdentityHashMap<>();
        order = List.of();
        error = "";
    }

    /** @return хост показанного компонента или пусто, если ничего не показано */
    public Optional<ComponentHost> host() {
        return Optional.ofNullable(host);
    }

    /** @return корневой виджет показанного компонента (его хост) */
    public Optional<Widget> root() {
        return Optional.ofNullable(host);
    }

    /** @return определение показанного компонента */
    public Optional<ComponentDefinition> definition() {
        return host == null ? Optional.empty() : Optional.of(host.definition());
    }

    /** @return причина последней неудачи или пустая строка */
    public String error() {
        return error;
    }

    /**
     * @param node узел тела компонента в документе
     * @return виджет, созданный из узла
     */
    public Optional<Widget> widgetOf(UiNode node) {
        if (node != null && node == shownRoot) return Optional.ofNullable(host);
        return Optional.ofNullable(widgets.get(node));
    }

    /**
     * Выбирает самый глубокий узел документа компонента, чей виджет накрывает точку.
     *
     * @param document документ, узлы которого ищутся
     * @param x        координата точки в системе окна
     * @param y        координата точки в системе окна
     * @return узел или пусто
     */
    public Optional<UiNode> pick(UiDocument document, float x, float y) {
        UiNode hit = null;
        for (Map.Entry<UiNode, Widget> entry : order) {
            RectView bounds = entry.getValue().layoutBounds();
            boolean inside = x >= bounds.x() && x < bounds.x() + bounds.width()
                    && y >= bounds.y() && y < bounds.y() + bounds.height();
            if (inside && NodePath.find(document, entry.getKey()).isPresent()) hit = entry.getKey();
        }
        return Optional.ofNullable(hit);
    }

    /** @return {@code true} после {@link #close()} */
    public boolean closed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) return;
        clear();
        closed = true;
    }
}
