package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.MarkupExtension;
import dev.sixik.unigui.xaml.load.MarkupExtensionRegistry;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.load.XamlLoader;
import dev.sixik.unigui.xaml.logic.GraphDebugger;
import dev.sixik.unigui.xaml.logic.GraphRuntime;
import dev.sixik.unigui.xaml.logic.GraphTrace;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Виджет, в котором живёт один экземпляр компонента.
 *
 * <p>Хост — обычный контейнер с единственным ребёнком (телом компонента), поэтому его можно положить в любой
 * контейнер, задать ему размер и найти по {@code id}. Тело строится не в конструкторе, а когда загрузчик закончил
 * применять свойства-параметры ({@link #build(LoadContext)}), чтобы у тела уже были значения параметров, команды и
 * резолвер текстур вызывающего документа.</p>
 */
public final class ComponentHost extends PanelWidget {
    /** Предел вложенности компонентов друг в друга; защита от компонента, который содержит сам себя. */
    private static final int MAX_NESTING = 32;
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    /** Префикс служебных команд, через которые {@code {Raise}} доходит до события экземпляра. */
    private static final String RAISE_PREFIX = "@raise:";

    private final ComponentDefinition definition;
    private final Schema schema;
    private final ComponentInstance instance;
    private LoadedScreen<Widget> body;
    private String key = "";
    private LoadOptions.NodeObserver observer;
    private GraphTrace trace;
    private GraphDebugger debugger;

    /**
     * Создаёт хост без тела.
     *
     * @param definition определение компонента
     * @param schema     схема, по которой загружается тело (в ней же ищутся вложенные компоненты)
     */
    public ComponentHost(ComponentDefinition definition, Schema schema) {
        if (definition == null || schema == null) throw new IllegalArgumentException("definition and schema are required");
        this.definition = definition;
        this.schema = schema;
        this.instance = new ComponentInstance(definition, this);
    }

    /**
     * Ключ экземпляра: стабильный идентификатор записи, которую он показывает (например, id товара). По ключу
     * экземпляр находит {@link Components#byKey(Widget, String)}, поэтому обновить одну карточку каталога можно, не
     * перебирая остальные.
     *
     * @return ключ или пустая строка
     */
    public String key() {
        return key;
    }

    /**
     * Задаёт ключ экземпляра.
     *
     * @param value ключ; {@code null} означает пустой
     * @return этот хост
     */
    public ComponentHost key(String value) {
        this.key = value == null ? "" : value;
        return this;
    }

    /**
     * Подписывает наблюдателя на создание виджетов тела: предпросмотр редактора по нему сопоставляет узлы документа
     * компонента с виджетами. Вызывается до {@link #build}.
     *
     * @param value наблюдатель или {@code null}
     * @return этот хост
     */
    public ComponentHost observe(LoadOptions.NodeObserver value) {
        this.observer = value;
        return this;
    }

    /**
     * Включает запись хода графа. Вызывается до {@link #build}, чтобы в записи оказался и запуск от {@code Construct}.
     *
     * @param value запись или {@code null}
     * @return этот хост
     */
    public ComponentHost traceTo(GraphTrace value) {
        this.trace = value;
        return this;
    }

    /**
     * Подключает отладчик графа. Вызывается до {@link #build}.
     *
     * @param value отладчик или {@code null}
     * @return этот хост
     */
    public ComponentHost debugTo(GraphDebugger value) {
        this.debugger = value;
        return this;
    }

    /** @return экземпляр: параметры и события */
    public ComponentInstance instance() {
        return instance;
    }

    /** @return определение компонента */
    public ComponentDefinition definition() {
        return definition;
    }

    /** @return {@code true}, если тело уже построено */
    public boolean built() {
        return body != null;
    }

    /**
     * Строит тело из документа компонента. Повторный вызов ничего не делает.
     *
     * @param context текстуры и команды документа, в который вставлен компонент
     * @throws XamlLoadException если тело не загружается; сообщение содержит имя компонента
     * @throws IllegalStateException если компоненты вложены глубже {@value #MAX_NESTING} уровней
     */
    public void build(LoadContext context) {
        if (body != null) return;
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_NESTING) {
            throw new IllegalStateException("Components are nested more than " + MAX_NESTING
                    + " levels deep at '" + definition.id() + "'; a component probably contains itself.");
        }
        depth[0]++;
        LoadedScreen<Widget> loaded;
        try {
            MarkupExtensionRegistry extensions = MarkupExtensionRegistry.builtIns()
                    .register("Param", paramExtension())
                    .register("Var", varExtension())
                    .register("Raise", raiseExtension());
            LoadOptions options = LoadOptions.DEFAULT
                    .textureResolver(context.textures())
                    .commands(commandsFor(context.commands()))
                    .extensions(extensions);
            if (observer != null) options = options.nodeObserver(observer);
            loaded =new XamlLoader(schema, options).loadScreen(UiDocument.of(definition.body()));
        } catch (XamlLoadException failure) {
            var location = failure.diagnostics().isEmpty()
                    ? definition.location() : failure.diagnostics().get(0).location();
            throw new XamlLoadException("In component '" + definition.id() + "': " + failure.getMessage(),
                    location, failure);
        } finally {
            depth[0]--;
        }
        body = loaded;
        addChild(loaded.root());
        applyQueuedMutations();
        definition.applyBehaviors(instance);
        if (!definition.graph().isEmpty()) {
            GraphRuntime runtime = GraphRuntime.attach(instance, definition.program(), schema, context);
            if (trace != null) runtime.trace(trace);
            if (debugger != null) runtime.debugger(debugger);
        }
        instance.construct();
    }

    // ------------------------------------------------------------------ расширения тела

    /** {@code {Param Name}}: подставляет значение параметра и следит за его изменениями. */
    private MarkupExtension paramExtension() {
        return (context, use, target) -> {
            requireOnlyName(use, "Param");
            String name = literal(use.argument("Name", 0));
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Param requires a parameter name, for example {Param Title}");
            }
            Contract.Param declared = definition.contract().param(name.trim()).orElseThrow(
                    () -> new IllegalArgumentException("Component " + definition.tag().local()
                            + " has no parameter '" + name.trim() + "'. Declared: "
                            + definition.contract().paramNames() + "."));
            Consumer<Object> apply = binder(declared, target, "Parameter");
            apply.accept(instance.get(declared.name()));
            instance.onParamChanged(declared.name(), apply);
        };
    }

    /** {@code {Var Name}}: подставляет значение переменной графа и следит за её изменениями. */
    private MarkupExtension varExtension() {
        return (context, use, target) -> {
            requireOnlyName(use, "Var");
            String name = literal(use.argument("Name", 0));
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Var requires a variable name, for example {Var Count}");
            }
            Contract.Param declared = definition.contract().var(name.trim()).orElseThrow(
                    () -> new IllegalArgumentException("Component " + definition.tag().local()
                            + " has no variable '" + name.trim() + "'."));
            Consumer<Object> apply = binder(declared, target, "Variable");
            apply.accept(declared.value());
            instance.onVariableChanged(declared.name(), apply);
        };
    }

    /** Проверяет совместимость типов и возвращает запись значения в свойство. */
    private static Consumer<Object> binder(Contract.Param declared, dev.sixik.unigui.xaml.load.PropertyTarget target, String what) {
        Class<?> wanted = target.valueType();
        if (!declared.type().fits(wanted)) {
            throw new IllegalArgumentException(what + " '" + declared.name() + "' is "
                    + declared.type().name() + " but '" + target.name() + "' expects "
                    + wanted.getSimpleName());
        }
        return value -> {
            if (value != null) target.setValue(ParamType.adapt(value, wanted));
        };
    }

    /** {@code {Raise Event}}: превращает действие свойства (например, клик) в исходящее событие компонента. */
    private MarkupExtension raiseExtension() {
        return (context, use, target) -> {
            requireOnlyName(use, "Raise");
            String name = literal(use.argument("Name", 0));
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Raise requires an event name, for example {Raise Buy}");
            }
            Contract.Event event = definition.contract().event(name.trim()).orElseThrow(
                    () -> new IllegalArgumentException("Component " + definition.tag().local()
                            + " has no event '" + name.trim() + "'. Declared: "
                            + definition.contract().eventNames() + "."));
            if (event.direction() != Contract.Direction.OUT) {
                throw new IllegalArgumentException("Event '" + event.name() + "' is incoming and cannot be raised "
                        + "from the component body");
            }
            target.setText(RAISE_PREFIX + event.name());
        };
    }

    private static void requireOnlyName(UiValue.Extension use, String extension) {
        if (use.positional().size() > 1 || !use.named().isEmpty()) {
            throw new IllegalArgumentException(extension + " takes a single name, for example {"
                    + extension + " Name}");
        }
    }

    private static String literal(UiValue value) {
        if (value == null) return null;
        if (value instanceof UiValue.Literal literal) return literal.text();
        throw new IllegalArgumentException("The name must be plain text, not a nested expression");
    }

    /**
     * Команды тела: все команды документа-владельца плюс служебные команды {@code @raise:Событие}. Аргументы
     * события берутся из параметров с теми же именами.
     */
    private XamlCommandRegistry commandsFor(XamlCommandRegistry outer) {
        XamlCommandRegistry registry = XamlCommandRegistry.empty();
        // Оригинальные объекты, а не обёртки: иначе параметризованная команда перестала бы быть параметризованной.
        for (String name : outer.names()) outer.command(name).ifPresent(command -> registry.register(name, command));
        for (Contract.Event event : definition.contract().events()) {
            if (event.direction() != Contract.Direction.OUT) continue;
            registry.register(RAISE_PREFIX + event.name(), (source, ignored) -> instance.emit(event.name(), argsFor(event)));
        }
        return registry;
    }

    private Args argsFor(Contract.Event event) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Contract.Arg arg : event.args()) {
            if (definition.contract().param(arg.name()).isEmpty()) continue;
            try {
                values.put(arg.name(), arg.type().coerce(instance.get(arg.name())));
            } catch (IllegalArgumentException mismatch) {
                // параметр с таким именем другого типа: аргумент остаётся пустым, а не ломает клик
            }
        }
        return Args.of(values);
    }

    // ------------------------------------------------------------------ раскладка и кадр

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutSize measured = LayoutV3StackAdapter.measure(children(), context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), bounds, layoutStyle());
    }

    @Override
    public void tick(FrameContext frame) {
        instance.flush();
        instance.advance(frame.deltaSeconds());
        super.tick(frame);
    }

    @Override
    public void dispose() {
        instance.dispose();
        if (body != null) body.close();
        super.dispose();
    }
}
