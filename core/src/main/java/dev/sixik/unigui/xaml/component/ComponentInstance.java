package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Живой экземпляр компонента: значения параметров и обмен событиями.
 *
 * <p>Четыре глагола описывают весь обмен и симметричны:</p>
 * <ul>
 *   <li>{@link #send} — снаружи в компонент (входящее событие, ставится в очередь и выполняется перед кадром);</li>
 *   <li>{@link #handle} — компонент обрабатывает входящее событие (так подключаются граф нод и код-behind);</li>
 *   <li>{@link #emit} — компонент сообщает наружу (исходящее событие, выполняется сразу);</li>
 *   <li>{@link #onEvent} — внешний код слушает исходящее событие.</li>
 * </ul>
 *
 * <p>Объект рассчитан на поток интерфейса и не потокобезопасен: как и остальные виджеты, он живёт в одном потоке.</p>
 */
public final class ComponentInstance {
    /**
     * Предел «цепочек» входящих событий за один сброс очереди: защита от события, которое вечно вызывает себя.
     */
    private static final int MAX_FLUSH_ROUNDS = 100;

    private final ComponentDefinition definition;
    private final ComponentHost widget;
    private final Map<String, XamlMutableObservableValue<Object>> params = new LinkedHashMap<>();
    private final Map<String, List<Consumer<Args>>> handlers = new LinkedHashMap<>();
    private final Map<String, List<Consumer<Object>>> variableListeners = new java.util.HashMap<>();
    private final List<Queued> queue = new ArrayList<>();
    private final List<Consumer<Float>> tickers = new ArrayList<>();
    private boolean constructed;
    private boolean disposed;

    ComponentInstance(ComponentDefinition definition, ComponentHost widget) {
        this.definition = definition;
        this.widget = widget;
        for (Contract.Param param : definition.contract().params()) {
            params.put(param.name(), XamlMutableObservableValue.of(param.name(), Object.class, param.value()));
        }
    }

    /**
     * @return описание компонента
     */
    public ComponentDefinition definition() {
        return definition;
    }

    /**
     * @return виджет-хост, который вставляется в дерево интерфейса
     */
    public Widget widget() {
        return widget;
    }

    // ------------------------------------------------------------------ параметры

    /**
     * Читает значение параметра.
     *
     * @param name имя из контракта
     * @return текущее значение или {@code null}
     * @throws IllegalArgumentException если такого параметра нет
     */
    public Object get(String name) {
        return observable(name).get();
    }

    /**
     * Задаёт значение параметра; виджеты, привязанные через {@code {Param}}, обновляются сразу.
     *
     * @param name  имя из контракта
     * @param value значение; целые расширяются до {@code float} и {@code double}
     * @return этот экземпляр
     * @throws IllegalArgumentException если параметра нет или значение не того типа
     */
    public ComponentInstance set(String name, Object value) {
        XamlMutableObservableValue<Object> observable = observable(name);
        ParamType type = definition.contract().param(name).orElseThrow().type();
        Object coerced;
        try {
            coerced = type.coerce(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Parameter '" + name + "' of " + definition.tag().local() + ": "
                    + failure.getMessage());
        }
        observable.set(coerced);
        return this;
    }

    /**
     * Задаёт несколько параметров; все имена проверяются до первой записи.
     *
     * @param args значения по именам
     * @return этот экземпляр
     */
    public ComponentInstance set(Args args) {
        for (String name : args.asMap().keySet()) observable(name);
        args.asMap().forEach(this::set);
        return this;
    }

    /**
     * Подписывается на изменения параметра.
     *
     * @param name     имя из контракта
     * @param listener получает новое значение
     * @return подписка
     */
    public EventSubscription onParamChanged(String name, Consumer<Object> listener) {
        return observable(name).onChanged(change -> listener.accept(change.newValue()));
    }

    private XamlMutableObservableValue<Object> observable(String name) {
        XamlMutableObservableValue<Object> observable = params.get(name);
        if (observable == null) {
            throw new IllegalArgumentException("Component " + definition.tag().local() + " has no parameter '"
                    + name + "'. Declared: " + definition.contract().paramNames() + ".");
        }
        return observable;
    }

    // ------------------------------------------------------------------ события

    /**
     * Следит за переменной графа: так к ней привязываются свойства тела через {@code {Var Имя}}.
     *
     * @param name     имя переменной
     * @param listener получает новое значение
     * @return подписка
     */
    public EventSubscription onVariableChanged(String name, Consumer<Object> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        variableListeners.computeIfAbsent(name, key -> new ArrayList<>()).add(listener);
        return () -> {
            List<Consumer<Object>> list = variableListeners.get(name);
            if (list != null) list.remove(listener);
        };
    }

    /**
     * Сообщает слушателям новое значение переменной; вызывает среда исполнения графа.
     *
     * @param name  имя переменной
     * @param value значение
     */
    public void publishVariable(String name, Object value) {
        List<Consumer<Object>> list = variableListeners.get(name);
        if (list == null) return;
        for (Consumer<Object> listener : List.copyOf(list)) listener.accept(value);
    }

    /**
     * Слушает исходящее событие.
     *
     * @param name    имя события с {@code Direction="Out"}
     * @param handler получает аргументы
     * @return подписка
     */
    public EventSubscription onEvent(String name, Consumer<Args> handler) {
        requireEvent(name, Contract.Direction.OUT, "listen to");
        return subscribe(name, handler);
    }

    /**
     * Обрабатывает входящее событие или событие жизненного цикла ({@code Construct}, {@code Destruct}).
     *
     * @param name    имя события с {@code Direction="In"} либо {@code Construct}/{@code Destruct}
     * @param handler получает аргументы
     * @return подписка
     */
    public EventSubscription handle(String name, Consumer<Args> handler) {
        if (!isLifecycle(name)) requireEvent(name, Contract.Direction.IN, "handle");
        return subscribe(name, handler);
    }

    /**
     * Отправляет входящее событие. Оно выполняется при ближайшем {@link #flush()} (хост делает это каждый кадр),
     * а события с {@code Coalesce="true"} за кадр сливаются в последнее.
     *
     * @param name имя события с {@code Direction="In"}
     * @param args аргументы по контракту
     */
    public void send(String name, Args args) {
        Contract.Event event = requireEvent(name, Contract.Direction.IN, "send");
        Args checked = check(event, args);
        if (disposed) return;
        if (event.coalesce()) queue.removeIf(queued -> queued.name.equals(name));
        queue.add(new Queued(name, checked));
    }

    /**
     * Сообщает наружу исходящее событие; обработчики вызываются сразу.
     *
     * @param name имя события с {@code Direction="Out"}
     * @param args аргументы по контракту
     */
    public void emit(String name, Args args) {
        Contract.Event event = requireEvent(name, Contract.Direction.OUT, "emit");
        if (disposed) return;
        dispatch(name, check(event, args));
    }

    /**
     * Выполняет накопленные входящие события. Хост вызывает метод каждый кадр перед раскладкой; вызывайте сами
     * в тестах и там, где кадра нет.
     *
     * @throws IllegalStateException если обработчики бесконечно порождают новые события
     */
    public void flush() {
        int rounds = 0;
        while (!queue.isEmpty()) {
            if (++rounds > MAX_FLUSH_ROUNDS) {
                queue.clear();
                throw new IllegalStateException("Component " + definition.tag().local()
                        + " keeps producing incoming events (more than " + MAX_FLUSH_ROUNDS + " rounds); "
                        + "a handler probably sends the event it handles");
            }
            List<Queued> batch = new ArrayList<>(queue);
            queue.clear();
            for (Queued queued : batch) dispatch(queued.name, queued.args);
        }
    }

    /**
     * @return сколько входящих событий ждёт выполнения
     */
    public int pendingEvents() {
        return queue.size();
    }

    // ------------------------------------------------------------------ время

    /**
     * Подписывает действие на ход времени компонента: хост вызывает {@link #advance(float)} каждый кадр. Так
     * работают задержки графа.
     *
     * @param ticker получает прошедшие секунды
     * @return подписка
     */
    public EventSubscription onTick(Consumer<Float> ticker) {
        if (ticker == null) throw new IllegalArgumentException("ticker must not be null");
        tickers.add(ticker);
        return () -> tickers.remove(ticker);
    }

    /**
     * Сообщает компоненту, что прошло время. Хост делает это каждый кадр; в тестах вызывается вручную.
     *
     * @param seconds прошедшее время в секундах
     */
    public void advance(float seconds) {
        if (disposed || seconds <= 0.0f) return;
        for (Consumer<Float> ticker : List.copyOf(tickers)) ticker.accept(seconds);
    }

    // ------------------------------------------------------------------ жизненный цикл

    void construct() {
        if (constructed || disposed) return;
        constructed = true;
        dispatch(Contract.CONSTRUCT, Args.empty());
    }

    /**
     * Освобождает экземпляр: вызывает {@code Destruct}, сбрасывает очередь и обработчики.
     */
    public void dispose() {
        if (disposed) return;
        if (constructed) dispatch(Contract.DESTRUCT, Args.empty());
        disposed = true;
        queue.clear();
        handlers.clear();
        tickers.clear();
    }

    /**
     * @return {@code true} после {@link #dispose()}
     */
    public boolean disposed() {
        return disposed;
    }

    // ------------------------------------------------------------------ внутреннее

    private static boolean isLifecycle(String name) {
        return Contract.CONSTRUCT.equals(name) || Contract.DESTRUCT.equals(name);
    }

    private EventSubscription subscribe(String name, Consumer<Args> handler) {
        if (handler == null) throw new IllegalArgumentException("handler must not be null");
        handlers.computeIfAbsent(name, key -> new ArrayList<>()).add(handler);
        return () -> {
            List<Consumer<Args>> list = handlers.get(name);
            if (list != null) list.remove(handler);
        };
    }

    private void dispatch(String name, Args args) {
        List<Consumer<Args>> list = handlers.get(name);
        if (list == null) return;
        for (Consumer<Args> handler : List.copyOf(list)) handler.accept(args);
    }

    private Contract.Event requireEvent(String name, Contract.Direction expected, String verb) {
        Contract.Event event = definition.contract().event(name).orElseThrow(() -> new IllegalArgumentException(
                "Component " + definition.tag().local() + " has no event '" + name + "'. Declared: "
                        + definition.contract().eventNames() + "."));
        if (event.direction() != expected) {
            throw new IllegalArgumentException("Cannot " + verb + " event '" + name + "' of "
                    + definition.tag().local() + ": it is declared " + describe(event.direction())
                    + " but " + describe(expected) + " is required.");
        }
        return event;
    }

    private static String describe(Contract.Direction direction) {
        return direction == Contract.Direction.IN ? "incoming" : "outgoing";
    }

    /**
     * Проверяет имена и типы аргументов по контракту и приводит значения.
     */
    private Args check(Contract.Event event, Args args) {
        Args given = args == null ? Args.empty() : args;
        Map<String, Object> checked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : given.asMap().entrySet()) {
            Contract.Arg arg = event.arg(entry.getKey()).orElseThrow(() -> new IllegalArgumentException(
                    "Event '" + event.name() + "' of " + definition.tag().local() + " has no argument '"
                            + entry.getKey() + "'."));
            try {
                checked.put(arg.name(), arg.type().coerce(entry.getValue()));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("Event '" + event.name() + "' argument '" + arg.name() + "': "
                        + failure.getMessage());
            }
        }
        return Args.of(checked);
    }

    private record Queued(String name, Args args) {
    }
}
