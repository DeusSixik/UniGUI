package dev.sixik.unigui.xaml.logic;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.graph.runtime.GraphExecutor;
import dev.sixik.unigui.widgets.graph.runtime.GraphModel;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.component.ParamType;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Исполнение графа в одном экземпляре компонента: события компонента запускают ноды-входы, а ноды читают и меняют
 * параметры и переменные, вызывают функции Java, посылают исходящие события, создают другие компоненты и
 * подписываются на их события.
 *
 * <p>Запуск синхронный и укладывается в обработку события: входящее событие ждёт очереди кадра ({@code send} →
 * {@code flush}), а граф отрабатывает целиком внутри {@code flush}. Задержка ({@code Flow.Delay}) прерывает цепочку и
 * продолжает её позже, когда хост сообщит о ходе времени ({@link ComponentInstance#advance}).</p>
 *
 * <p>Важное следствие задержки: после неё выходы исполняемых нод, выполненных до задержки, недоступны (продолжение
 * начинается заново). Значения, которые нужны после паузы, хранятся в переменных графа. Аргументы события, запустившего
 * цепочку, при продолжении сохраняются.</p>
 */
public final class GraphRuntime {
    /**
     * Предел вложенности вызовов пользовательских событий: защита от события, которое вызывает само себя.
     */
    private static final int MAX_CALL_DEPTH = 64;

    private record Pending(double remaining, Args args, Object sender) {
    }

    private record Binding(ComponentInstance target, String event, EventSubscription subscription) {
    }

    private final ComponentInstance instance;
    private final Schema schema;
    private final LoadContext loadContext;
    private final GraphProgram program;
    private final GraphExecutor executor;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final Map<String, Pending> pending = new LinkedHashMap<>();
    private final List<Binding> bindings = new ArrayList<>();
    private final List<ComponentInstance> created = new ArrayList<>();
    private Args eventArgs = Args.empty();
    private Object sender;
    private int depth;
    private GraphTrace trace;

    private GraphRuntime(ComponentInstance instance, GraphProgram program, Schema schema, LoadContext loadContext) {
        this.instance = instance;
        this.schema = schema;
        this.loadContext = loadContext;
        this.program = program;
        for (Contract.Param variable : instance.definition().contract().vars()) {
            variables.put(variable.name(), variable.value());
        }
        this.executor = program.executor(this);
    }

    /**
     * Подключает граф к экземпляру: на каждый вход графа подписывается обработчик события, а ход времени двигает
     * отложенные продолжения.
     *
     * @param instance    экземпляр компонента
     * @param program     скомпилированный граф
     * @param schema      схема, по которой находятся свойства виджетов
     * @param loadContext контекст загрузки для значений-текстур и команд
     * @return среда исполнения
     */
    public static GraphRuntime attach(ComponentInstance instance, GraphProgram program, Schema schema,
                                      LoadContext loadContext) {
        GraphRuntime runtime = new GraphRuntime(instance, program, schema, loadContext);
        for (GraphProgram.Entry entry : program.entries()) {
            // Исходящее событие тоже можно обработать в графе: нода On срабатывает, когда компонент его поднимает.
            boolean outgoing = instance.definition().contract().event(entry.event())
                    .filter(event -> event.direction() == Contract.Direction.OUT).isPresent();
            if (outgoing)
                instance.onEvent(entry.event(), args -> runtime.fireGuarded(entry.nodeId(), entry.event(), args));
            else instance.handle(entry.event(), args -> runtime.fire(entry.nodeId(), args, null));
        }
        instance.onTick(seconds -> runtime.advance(seconds));
        // Последним: к моменту Destruct графа его собственные ноды уже отработали.
        instance.handle(Contract.DESTRUCT, args -> runtime.dispose());
        return runtime;
    }

    // ------------------------------------------------------------------ для поведения нод

    ComponentInstance instance() {
        return instance;
    }

    Args eventArgs() {
        return eventArgs;
    }

    /**
     * @return отправитель события, которое запустило пользовательское событие: источник подписки или пусто
     */
    Object sender() {
        return sender;
    }

    Object variable(String name) {
        return variables.get(name);
    }

    void setVariable(String name, Object value) {
        Contract.Param declared = instance.definition().contract().var(name).orElseThrow();
        Object coerced = value;
        if (value instanceof String text && declared.type().textual() && declared.type() != ParamType.STRING) {
            coerced = declared.type().parse(text, LoadContext.DEFAULT);
        }
        Object stored = declared.type().coerce(coerced);
        variables.put(name, stored);
        instance.publishVariable(name, stored);
    }

    /**
     * @return значение переменной графа; нужно тестам и отладке
     */
    public Object variableValue(String name) {
        return variables.get(name);
    }

    /**
     * Запуск графа с входа: что выполнять и с какими данными события.
     */
    private record Fire(String nodeId, Args args, Object sender) {
    }

    /**
     * Остановленный запуск: ждёт продолжения, данные события нужны, пока он не закончится.
     */
    private record Paused(GraphExecutor.Run run, Fire fire) {
    }

    private Paused paused;
    private boolean running;
    private final java.util.ArrayDeque<Fire> queued = new java.util.ArrayDeque<>();
    private GraphDebugger debugger;

    /**
     * Подключает отладчик: он получает остановки и управляет ими. Без отладчика запуски идут до конца без пауз.
     *
     * @param value отладчик или {@code null}
     */
    public void debugger(GraphDebugger value) {
        this.debugger = value;
        if (value != null) {
            value.bind(new GraphDebugger.Control() {
                @Override
                public void resume() {
                    continuePaused(false);
                }

                @Override
                public void step() {
                    continuePaused(true);
                }
            });
        }
    }

    private void fire(String nodeId, Args args, Object eventSender) {
        Fire request = new Fire(nodeId, args, eventSender);
        if (running) {
            // Вложенный запуск (пользовательское событие из ноды) идёт тут же и без остановок.
            runNested(request);
            return;
        }
        if (paused != null) {
            queued.add(request); // запуск стоит: новые события ждут его конца
            return;
        }
        start(request);
    }

    private void runNested(Fire request) {
        Args previousArgs = eventArgs;
        Object previousSender = sender;
        eventArgs = request.args();
        sender = request.sender();
        if (trace != null) trace.begin(request.nodeId());
        String error = null;
        try {
            executor.fire(program.model(), request.nodeId());
        } catch (RuntimeException failure) {
            error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            throw failure;
        } finally {
            eventArgs = previousArgs;
            sender = previousSender;
            if (trace != null) trace.end(error);
        }
    }

    private void start(Fire request) {
        GraphExecutor.Run run = executor.begin(program.model(), request.nodeId());
        if (trace != null) trace.begin(request.nodeId());
        drive(run, request, true, false);
    }

    /**
     * Ведёт запуск до остановки или конца. Данные события выставляются на время хода и возвращаются после: пока запуск
     * стоит, они не должны подменять данные других вызовов.
     */
    private void drive(GraphExecutor.Run run, Fire request, boolean checkFirst, boolean singleStep) {
        Args previousArgs = eventArgs;
        Object previousSender = sender;
        eventArgs = request.args();
        sender = request.sender();
        running = true;
        String error = null;
        boolean stopped = false;
        try {
            if (singleStep) {
                run.step();
                stopped = !run.finished();
            } else {
                run.resume(node -> debugger != null && debugger.isBreakpoint(node.id()), checkFirst);
                stopped = !run.finished();
            }
        } catch (RuntimeException failure) {
            error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            throw failure;
        } finally {
            running = false;
            eventArgs = previousArgs;
            sender = previousSender;
            if (stopped) {
                paused = new Paused(run, request);
                GraphModel.Node next = run.next();
                if (debugger != null) {
                    debugger.paused(new GraphDebugger.Pause(next.id(), request.nodeId(), typeOf(next)));
                }
            } else {
                paused = null;
                if (debugger != null) debugger.resumed();
                if (trace != null) trace.end(error);
            }
        }
        if (!stopped) drainQueue();
    }

    private static String typeOf(GraphModel.Node node) {
        String key = node.typeId();
        int hash = key.indexOf('#');
        return hash < 0 ? key : key.substring(0, hash);
    }

    private void continuePaused(boolean singleStep) {
        Paused current = paused;
        if (current == null) return;
        paused = null;
        if (debugger != null) debugger.resumed();
        drive(current.run(), current.fire(), false, singleStep);
    }

    private void drainQueue() {
        while (paused == null && !running && !queued.isEmpty()) start(queued.poll());
    }

    /**
     * Включает запись хода графа для отладчика.
     *
     * @param value запись или {@code null}, чтобы выключить
     */
    public void trace(GraphTrace value) {
        this.trace = value;
        executor.trace(value);
    }

    /**
     * Запуск от синхронного события: нода, которая поднимает своё же событие, не должна уйти в бесконечную рекурсию.
     */
    private void fireGuarded(String nodeId, String event, Args args) {
        if (++depth > MAX_CALL_DEPTH) {
            depth = 0;
            throw new IllegalStateException("Event '" + event + "' raises itself more than " + MAX_CALL_DEPTH
                    + " levels deep; does its On node lead back to Raise?");
        }
        try {
            fire(nodeId, args, null);
        } finally {
            if (depth > 0) depth--;
        }
    }

    void callCustom(String name, Args args, Object eventSender) {
        GraphProgram.CustomEvent event = program.customEvents().get(name);
        if (event == null) throw new IllegalStateException("No custom event '" + name + "'");
        if (++depth > MAX_CALL_DEPTH) {
            depth = 0;
            throw new IllegalStateException("Custom events call each other more than " + MAX_CALL_DEPTH
                    + " levels deep at '" + name + "'; is a custom event calling itself?");
        }
        try {
            fire(event.nodeId(), args, eventSender);
        } finally {
            if (depth > 0) depth--;
        }
    }

    // ------------------------------------------------------------------ время

    /**
     * Откладывает продолжение цепочки после ноды-задержки. Повторный запуск той же ноды пока она ждёт заменяет
     * прежнее ожидание (задержка «перезапускаемая»), а не складывает их.
     */
    void schedule(String nodeId, double seconds) {
        pending.put(nodeId, new Pending(Math.max(0.0, seconds), eventArgs, sender));
    }

    void cancel(String nodeId) {
        pending.remove(nodeId);
    }

    private void advance(float seconds) {
        if (pending.isEmpty() || paused != null) return; // пока запуск стоит, время графа заморожено
        List<Map.Entry<String, Pending>> due = new ArrayList<>();
        for (Iterator<Map.Entry<String, Pending>> it = pending.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Pending> entry = it.next();
            double left = entry.getValue().remaining() - seconds;
            if (left <= 0.0) {
                due.add(Map.entry(entry.getKey(), entry.getValue()));
                it.remove();
            } else {
                entry.setValue(new Pending(left, entry.getValue().args(), entry.getValue().sender()));
            }
        }
        for (Map.Entry<String, Pending> entry : due) {
            for (GraphModel.Wire wire : program.model().outgoing(entry.getKey(), "then")) {
                fire(wire.toNode(), entry.getValue().args(), entry.getValue().sender());
            }
        }
    }

    // ------------------------------------------------------------------ компоненты и диспетчеры

    ComponentInstance create(String componentId, Args args) {
        ComponentInstance made = Components.get(componentId).create(args);
        created.add(made);
        return made;
    }

    private static ComponentInstance asInstance(Object reference, String what) {
        if (reference instanceof ComponentInstance found) return found;
        throw new IllegalStateException(what + " needs a component reference, got "
                + (reference == null ? "nothing" : reference.getClass().getSimpleName()));
    }

    private PanelWidget container(String id) {
        Widget found = Xaml.find(instance.widget(), id).orElseThrow(() -> new IllegalStateException(
                "Component " + instance.definition().tag().local() + " has no widget with id '" + id + "'"));
        if (found instanceof PanelWidget panel) return panel;
        throw new IllegalStateException("Widget '" + id + "' (" + found.getClass().getSimpleName()
                + ") cannot hold child widgets");
    }

    void addChild(String targetId, Object child) {
        ComponentInstance made = asInstance(child, "Widget.AddChild");
        PanelWidget panel = container(targetId);
        panel.addChild(made.widget());
        panel.applyQueuedMutations();
    }

    void clearChildren(String targetId) {
        PanelWidget panel = container(targetId);
        created.removeIf(made -> {
            boolean inside = made.widget().parent() == panel;
            if (inside) made.widget().dispose();
            return inside;
        });
        panel.clearChildren();
        panel.applyQueuedMutations();
    }

    /**
     * Подписывает пользовательское событие на исходящее событие другого компонента. Аргументы события передаются
     * обработчику по именам; недостающие остаются пустыми, а {@code sender} — ссылка на компонент-источник.
     */
    void bind(Object target, String event, String handler) {
        ComponentInstance source = asInstance(target, "Dispatcher.Bind");
        GraphProgram.CustomEvent custom = program.customEvents().get(handler);
        EventSubscription subscription = source.onEvent(event, args -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Contract.Arg arg : custom.args())
                if (args.has(arg.name())) values.put(arg.name(), args.get(arg.name()));
            callCustom(handler, Args.of(values), source);
        });
        bindings.add(new Binding(source, event, subscription));
    }

    void unbind(Object target, String event) {
        ComponentInstance source = asInstance(target, "Dispatcher.Unbind");
        bindings.removeIf(binding -> {
            boolean match = binding.target() == source && binding.event().equals(event);
            if (match) binding.subscription().unsubscribe();
            return match;
        });
    }

    private void dispose() {
        pending.clear();
        for (Binding binding : bindings) binding.subscription().unsubscribe();
        bindings.clear();
        for (ComponentInstance made : created) made.widget().dispose();
        created.clear();
    }

    // ------------------------------------------------------------------ виджеты

    /**
     * Задаёт свойство виджета внутри компонента. Строка разбирается конвертером свойства (так же, как в
     * документе), остальные значения подставляются по типу.
     *
     * @param widgetId id виджета в теле компонента
     * @param property имя свойства
     * @param value    значение
     * @throws IllegalStateException если виджета или свойства нет
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setWidgetProperty(String widgetId, String property, Object value) {
        Optional<Widget> found = Xaml.find(instance.widget(), widgetId);
        Widget widget = found.orElseThrow(() -> new IllegalStateException(
                "Component " + instance.definition().tag().local() + " has no widget with id '" + widgetId + "'"));
        TypeInfo<?> type = schema.typeOf(widget.getClass()).orElseThrow(() -> new IllegalStateException(
                "No XAML type is registered for " + widget.getClass().getSimpleName()));
        PropertyInfo info = type.property(property).orElseThrow(() -> new IllegalStateException(
                widget.getClass().getSimpleName() + " has no property '" + property + "'"));
        if (value instanceof String text) {
            info.apply(widget, text, loadContext);
        } else {
            info.set(widget, ParamType.adapt(value, info.valueType()), loadContext);
        }
    }
}
