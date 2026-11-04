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
    /** Предел вложенности вызовов пользовательских событий: защита от события, которое вызывает само себя. */
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
            instance.handle(entry.event(), args -> runtime.fire(entry.nodeId(), args, null));
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

    /** @return отправитель события, которое запустило пользовательское событие: источник подписки или пусто */
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
        variables.put(name, declared.type().coerce(coerced));
    }

    /** @return значение переменной графа; нужно тестам и отладке */
    public Object variableValue(String name) {
        return variables.get(name);
    }

    private void fire(String nodeId, Args args, Object eventSender) {
        Args previousArgs = eventArgs;
        Object previousSender = sender;
        eventArgs = args;
        sender = eventSender;
        try {
            executor.fire(program.model(), nodeId);
        } finally {
            eventArgs = previousArgs;
            sender = previousSender;
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
        if (pending.isEmpty()) return;
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
            for (Contract.Arg arg : custom.args()) if (args.has(arg.name())) values.put(arg.name(), args.get(arg.name()));
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
