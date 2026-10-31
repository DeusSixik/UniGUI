package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;

import java.util.List;
import java.util.function.Consumer;

/**
 * Базовый контракт любого элемента UI-дерева.
 *
 * <p>Виджет одновременно участвует в нескольких pipeline: layout, input, focus, style resolving,
 * animation/tick и render. Интерфейс намеренно не диктует конкретную реализацию хранения состояния,
 * но фиксирует точки входа, которые runtime вызывает каждый кадр.</p>
 *
 * <p>Типичный жизненный цикл выглядит так:</p>
 *
 * <ul>
 *     <li>{@link #measure(LayoutContext)} рассчитывает желаемый размер;</li>
 *     <li>{@link #arrange(RectView)} сохраняет финальные bounds;</li>
 *     <li>{@link #handle(Event)} принимает routed/direct input events;</li>
 *     <li>{@link #tick(FrameContext)} обновляет состояние, transition'ы и timers;</li>
 *     <li>{@link #render(RenderContext)} добавляет draw-команды.</li>
 * </ul>
 *
 * <p>Изменения состояния должны помечать нужные {@link InvalidationFlags}. Это позволяет runtime
 * не пересчитывать layout или render cache без необходимости.</p>
 */
@AiDoc(
        summary = "Root interface for all UI tree elements: defines the lifecycle, layout, input, style, and render contracts.",
        purpose = "Defines the pipeline entry points (measure -> arrange -> handle -> tick -> render) that the runtime calls each frame for every widget.",
        useWhen = {
                "Implement a new visual element by extending WidgetBase and overriding pipeline hooks.",
                "Traverse or inspect the UI tree via parent() and children().",
                "Subscribe to routed events through the inherited EventEmitter API.",
                "Programmatically invalidate layout or render state after a property change."
        },
        criticalRules = {
                "State changes must mark InvalidationFlags; the runtime skips recalculation of clean subtrees.",
                "measure() sets desiredSize(); arrange() applies final bounds; calling order matters.",
                "parent() returns null for root or not-yet-attached widgets.",
                "children() order defines layout/render order and hit-testing sequence.",
                "dispose() must release only resources the widget itself owns."
        },
        related = {"WidgetBase", "EventEmitter", "LayoutContext", "RenderContext", "FrameContext", "InvalidationFlags"},
        examples = {
                @AiDoc.Example(
                        title = "Subscribe to a click event",
                        order = 10,
                        code = "Widget button = ...;\nbutton.on(ButtonClickEvent.TYPE, e -> {\n    System.out.println(\"Clicked: \" + e.target().id());\n});"),
                @AiDoc.Example(
                        title = "Custom widget skeleton",
                        order = 20,
                        code = "public class MyWidget extends WidgetBase {\n    @Override\n    public void measure(LayoutContext ctx) {\n        setDesiredSize(100, 50);\n    }\n\n    @Override\n    public void render(RenderContext ctx) {\n        ctx.fillRect(layoutBounds(), 0xFFAABBCC);\n    }\n}")
        })
@WikiPage(
        title = "<key:widget.title>",
        slug = "widget",
        category = "<key:widget.category>",
        summary = "<key:widget.summary>",
        order = 10)
@WikiSection(title = "<key:widget.section.overview.title>", markdown = "<key:widget.section.overview.body>", order = 10)
@WikiSection(title = "<key:widget.section.lifecycle.title>", markdown = "<key:widget.section.lifecycle.body>", order = 20)
@WikiSection(title = "<key:widget.section.tree.title>", markdown = "<key:widget.section.tree.body>", order = 30)
@WikiSection(title = "<key:widget.section.style.title>", markdown = "<key:widget.section.style.body>", order = 40)
@WikiSection(title = "<key:widget.section.focus.title>", markdown = "<key:widget.section.focus.body>", order = 50)
@WikiSection(title = "<key:widget.section.invalidation.title>", markdown = "<key:widget.section.invalidation.body>", order = 60)
@WikiExample(title = "<key:widget.example.events.title>", code = "<key:widget.example.events.code>", order = 10)
@WikiExample(title = "<key:widget.example.skeleton.title>", code = "<key:widget.example.skeleton.code>", order = 20)
@WikiLink(label = "WidgetBase", target = "page:widget-base")
@WikiLink(label = "EventEmitter", target = "page:event-emitter")
@WikiLink(label = "LayoutStyle", target = "page:layout-style")
public interface Widget extends EventEmitter {
    /**
     * Возвращает runtime-контекст, к которому привязан виджет.
     *
     * @return UI context с dispatcher, input, theme и debug-сервисами
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 10,
            title = "<key:widget.method.ui_context.title>", summary = "<key:widget.method.ui_context.summary>")
    @WikiReturn("<key:widget.method.ui_context.return>")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 10, summary = "Returns runtime UIContext with dispatcher, theme, and input subsystems.")
    UIContext uiContext();

    /**
     * Возвращает родительский виджет в UI-дереве.
     *
     * @return parent или {@code null}, если виджет является root или ещё не добавлен в дерево
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 20,
            title = "<key:widget.method.parent.title>", summary = "<key:widget.method.parent.summary>")
    @WikiReturn("<key:widget.method.parent.return>")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 20, summary = "Returns parent widget in UI tree, or null if root/detached.")
    Widget parent();

    /**
     * Возвращает дочерние виджеты.
     *
     * <p>Порядок списка обычно совпадает с layout/render order и используется input pipeline для
     * hit testing. Возвращаемый список не обязан быть изменяемым.</p>
     *
     * @return дети виджета в порядке обхода
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 30,
            title = "<key:widget.method.children.title>", summary = "<key:widget.method.children.summary>")
    @WikiReturn("<key:widget.method.children.return>")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 30, summary = "Returns child widgets in layout/traversal order.")
    List<Widget> children();

    /**
     * Возвращает runtime/editor id виджета внутри UI-дерева.
     *
     * @return id или пустая строка, если id не задан
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 12,
            title = "<key:widget.method.get_id.title>", summary = "<key:widget.method.get_id.summary>")
    @WikiReturn("<key:widget.method.get_id.return>")
    @AiDoc(section = "Identity", sectionOrder = 10, order = 12, summary = "Returns widget identifier string.")
    default String id() {
        return "";
    }

    /**
     * Задаёт runtime/editor id виджета внутри UI-дерева.
     *
     * @param id новый id; реализация может нормализовать {@code null} в пустую строку
     * @return этот виджет для fluent-настройки
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 10,
            title = "<key:widget.method.id.title>", summary = "<key:widget.method.id.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Identity", sectionOrder = 10, order = 10, summary = "Sets runtime identifier for hierarchy lookups and debugging.")
    default Widget id(@WikiParameter(description = "<key:widget.method.id.param.id>") String id) {
        return this;
    }

    /**
     * Возвращает явный id стиля, назначенный виджету через атрибут {@code style}.
     *
     * @return style id или пустая строка
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 22,
            title = "<key:widget.method.get_style_id.title>", summary = "<key:widget.method.get_style_id.summary>")
    @WikiReturn("<key:widget.method.get_style_id.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 22, summary = "Returns style pack id assigned to this widget.")
    default String styleId() {
        return "";
    }

    /**
     * Задаёт явный id стиля, назначенный виджету через атрибут {@code style}.
     *
     * @param styleId id стиля из Theme/StylePack
     * @return этот виджет для fluent-настройки
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 20,
            title = "<key:widget.method.style_id.title>", summary = "<key:widget.method.style_id.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 20, summary = "Sets style pack id for Theme resolution.")
    default Widget styleId(@WikiParameter(description = "<key:widget.method.style_id.param.style_id>") String styleId) {
        return this;
    }

    /**
     * Возвращает style classes виджета в порядке назначения.
     *
     * @return read-only или snapshot список классов
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 32,
            title = "<key:widget.method.get_style_classes.title>", summary = "<key:widget.method.get_style_classes.summary>")
    @WikiReturn("<key:widget.method.get_style_classes.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 32, summary = "Returns list of assigned style classes.")
    default List<String> styleClasses() {
        return List.of();
    }

    /**
     * Задаёт style classes одной строкой, разделённой пробелами или запятыми.
     *
     * @param styleClasses строка классов, например {@code "primary compact"}
     * @return этот виджет для fluent-настройки
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 30,
            title = "<key:widget.method.style_class.title>", summary = "<key:widget.method.style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 30, summary = "Sets space/comma separated style classes.")
    default Widget styleClass(@WikiParameter(description = "<key:widget.method.style_class.param.style_classes>") String styleClasses) {
        return this;
    }

    /**
     * Алиас {@link #styleClass(String)} для API, где имя во множественном числе читается лучше.
     *
     * @param styleClasses строка классов, разделённых пробелами или запятыми
     * @return этот виджет для fluent-настройки
     */
    default Widget styleClasses(String styleClasses) {
        return styleClass(styleClasses);
    }

    /**
     * Добавляет один style class.
     *
     * @param styleClass добавляемый class id
     * @return этот виджет для fluent-настройки
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 34,
            title = "<key:widget.method.add_style_class.title>", summary = "<key:widget.method.add_style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 34, summary = "Adds a style class.")
    default Widget addStyleClass(@WikiParameter(description = "<key:widget.method.add_style_class.param.style_class>") String styleClass) {
        return this;
    }

    /**
     * Удаляет один style class.
     *
     * @param styleClass удаляемый class id
     * @return этот виджет для fluent-настройки
     */
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 36,
            title = "<key:widget.method.remove_style_class.title>", summary = "<key:widget.method.remove_style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Styling", sectionOrder = 20, order = 36, summary = "Removes a style class.")
    default Widget removeStyleClass(@WikiParameter(description = "<key:widget.method.remove_style_class.param.style_class>") String styleClass) {
        return this;
    }

    /**
     * Проверяет наличие style class.
     *
     * @param styleClass class id для проверки
     * @return {@code true}, если class назначен виджету
     */
    default boolean hasStyleClass(String styleClass) {
        return false;
    }

    /**
     * Добавляет встроенные визуальные объявления стиля к этому виджету.
     *
     * <p>Объявления объединяются с уже заданным встроенным стилем и имеют приоритет
     * над значениями темы. Строка без двоеточия считается ссылкой на {@link #styleId(String)}
     * для совместимости с краткой записью ссылок на стили.</p>
     *
     * @param declarations объявления, например {@code "background: #7a1f1f; radius: 8;"}
     * @return этот виджет
     */
    default Widget style(String declarations) {
        return this;
    }

    /**
     * Изменяет тот же набор встроенных стилей с проверкой типизированных ключей компилятором.
     *
     * @param update функция настройки стиля
     * @return этот виджет
     */
    default Widget style(Consumer<MutableStyle> update) {
        return this;
    }

    /**
     * Удаляет inline-объявления, добавленные через {@link #style(String)} и {@link #style(Consumer)}.
     * Глобальная тема, component defaults и назначенные родителем local styles остаются активны.
     *
     * @return этот виджет
     */
    default Widget styleClear() {
        return this;
    }

    /**
     * @return диагностика, собранная при разборе встроенных объявлений стиля
     */
    default List<XmlWidgetDiagnostic> styleDiagnostics() {
        return List.of();
    }

    /**
     * Возвращает текущие границы виджета после layout.
     *
     * @return arranged bounds в координатах parent/root, принятых текущей реализацией
     */
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 10,
            title = "<key:widget.method.layout_bounds.title>", summary = "<key:widget.method.layout_bounds.summary>")
    @WikiReturn("<key:widget.base.method.layout_bounds.return>")
    @AiDoc(section = "Layout", sectionOrder = 30, order = 10, summary = "Returns arranged layout bounds rectangle.")
    RectView layoutBounds();

    /**
     * Возвращает размер, который виджет запросил у системы layout на последнем measure.
     *
     * @return desired size или {@link LayoutSize#ZERO}, если виджет ещё не измерен
     */
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 20,
            title = "<key:widget.method.desired_size.title>", summary = "<key:widget.method.desired_size.summary>")
    @WikiReturn("<key:widget.base.method.desired_size.return>")
    @AiDoc(section = "Layout", sectionOrder = 30, order = 20, summary = "Returns desired size from most recent measure pass.")
    default LayoutSize desiredSize() {
        return LayoutSize.ZERO;
    }

    /**
     * Измеряет минимальный контентный размер (схлопнутый до предела, но без потерь).
     *
     * <p>Дефолт измеряет виджет в нулевом доступном пространстве по главной оси.
     * Виджеты с нетривиальным контентом (текст) переопределяют точнее.
     * Вызов перезаписывает {@link #desiredSize()}, после него нужен повторный
     * обычный {@code measure}.</p>
     *
     * @return минимальный контентный размер
     */
    default LayoutSize minContentSize() {
        measure(new LayoutContext(0.0f, Float.POSITIVE_INFINITY));
        return desiredSize();
    }

    /**
     * Измеряет максимальный контентный размер (без переносов и ужатий).
     *
     * <p>Вызов перезаписывает {@link #desiredSize()}, после него нужен повторный
     * обычный {@code measure}.</p>
     *
     * @return максимальный контентный размер
     */
    default LayoutSize maxContentSize() {
        measure(new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        return desiredSize();
    }

    /**
     * Возвращает baseline контента для baseline-выравнивания.
     *
     * <p>Расстояние от cross-start края border box до baseline первой строки.
     * {@code NaN} означает отсутствие baseline (не участвует в выравнивании).
     * В текущей версии учитывается только row-направлениями, где cross-ось
     * вертикальна.</p>
     *
     * @return baseline в пикселях или {@code NaN}
     */
    default float contentBaseline() {
        return Float.NaN;
    }

    /**
     * Возвращает transform, применяемый при render и hit-testing.
     *
     * @return текущий transform виджета
     */
    Transform transform();

    /**
     * Возвращает флаги инвалидации самого виджета.
     *
     * @return bitmask из {@link InvalidationFlags}
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 40,
            title = "<key:widget.method.invalidation_flags.title>", summary = "<key:widget.method.invalidation_flags.summary>")
    @WikiReturn("<key:widget.base.method.invalidation_flags.return>")
    @AiDoc(section = "Lifecycle", sectionOrder = 50, order = 40, summary = "Returns active bitmask of dirty invalidation flags.")
    int invalidationFlags();

    /**
     * Возвращает legacy layout constraints виджета.
     *
     * @return constraints или {@link LayoutConstraints#DEFAULT}
     */
    default LayoutConstraints layoutConstraints() {
        return LayoutConstraints.DEFAULT;
    }

    /**
     * Возвращает режим видимости виджета.
     *
     * @return visibility mode
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 40, order = 10,
            title = "<key:widget.method.get_visibility.title>", summary = "<key:widget.method.get_visibility.summary>")
    @WikiReturn("<key:widget.base.method.get_visibility.return>")
    @AiDoc(section = "State", sectionOrder = 40, order = 10, summary = "Returns current Visibility mode (VISIBLE, HIDDEN, COLLAPSED).")
    default Visibility visibility() {
        return Visibility.VISIBLE;
    }

    /**
     * Проверяет, должен ли виджет отображаться и участвовать во вводе.
     *
     * @return {@code true} только для {@link Visibility#VISIBLE}
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 40, order = 12,
            title = "<key:widget.method.visible.title>", summary = "<key:widget.method.visible.summary>")
    @WikiReturn("<key:widget.base.method.get_visible.return>")
    @AiDoc(section = "State", sectionOrder = 40, order = 12, summary = "Checks if widget is VISIBLE.")
    default boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    /**
     * Проверяет, доступен ли виджет для взаимодействия пользователя.
     *
     * @return {@code true}, если input/focus события не должны игнорироваться disabled-состоянием
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 40, order = 14,
            title = "<key:widget.method.enabled.title>", summary = "<key:widget.method.enabled.summary>")
    @WikiReturn("<key:widget.base.method.get_enabled.return>")
    @AiDoc(section = "State", sectionOrder = 40, order = 14, summary = "Checks if widget is enabled for interaction.")
    default boolean enabled() {
        return true;
    }

    /**
     * Проверяет, находится ли указатель над виджетом.
     *
     * @return {@code true}, если hover manager считает виджет активным hover target
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 40, order = 16,
            title = "<key:widget.method.hovered.title>", summary = "<key:widget.method.hovered.summary>")
    @WikiReturn("<key:widget.base.method.hovered.return>")
    @AiDoc(section = "State", sectionOrder = 40, order = 16, summary = "Checks if pointer is hovering over this widget.")
    default boolean hovered() {
        return false;
    }

    /**
     * Возвращает курсор, который нужно показать в указанной локальной точке виджета.
     *
     * @param localX X в локальных координатах виджета
     * @param localY Y в локальных координатах виджета
     * @return cursor для текущей точки
     */
    default MouseCursor mouseCursorAt(float localX, float localY) {
        return MouseCursor.DEFAULT;
    }

    /**
     * Курсор, который виджет навязывает дочерним виджетам под указателем.
     *
     * <p>{@link #mouseCursorAt} спрашивают только у самого глубокого виджета под курсором, поэтому
     * контейнер, чья «рамка» перекрыта детьми (край окна под докинг-областью, ручка изменения размера под
     * контентом), не может показать свой курсор. Этот хук позволяет ему это сделать: ближайший к виджету
     * под курсором предок с ненулевым ответом побеждает.</p>
     *
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     * @return курсор-override или {@code null}, если виджет ничего не навязывает
     */
    default MouseCursor mouseCursorOverrideAt(float rootX, float rootY) {
        return null;
    }

    /**
     * Проверяет, может ли виджет получать клавиатурный фокус.
     *
     * @return {@code true}, если focus manager может выбрать этот виджет
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 40, order = 20,
            title = "<key:widget.method.focusable.title>", summary = "<key:widget.method.focusable.summary>")
    @WikiReturn("<key:widget.base.method.get_focusable.return>")
    @AiDoc(section = "State", sectionOrder = 40, order = 20, summary = "Checks if widget can receive keyboard focus.")
    default boolean focusable() {
        return false;
    }

    /**
     * Проверяет, образует ли виджет отдельную область фокусировки.
     *
     * @return {@code true}, если потомки должны обходиться как отдельный focus scope
     */
    default boolean focusScope() {
        return false;
    }

    /**
     * Возвращает порядок обхода фокуса.
     *
     * @return числовой приоритет; меньшие значения обычно идут раньше
     */
    default int focusOrder() {
        return 0;
    }

    /**
     * Проверяет, ограничивает ли виджет область применения локальных стилей.
     *
     * @return {@code true}, если локальные стили не должны протекать за этот subtree
     */
    default boolean styleScope() {
        return false;
    }

    /**
     * Возвращает локальный стиль для указанного типа виджета.
     *
     * <p>Локальные стили позволяют контейнеру переопределить visual preset потомков без изменения
     * глобальной Theme. Если стиль не найден, нужно вернуть {@link Style#EMPTY}.</p>
     *
     * @param widgetType style type виджета, например {@code Button.STYLE_TYPE}
     * @return локальный стиль или {@link Style#EMPTY}
     */
    default Style localStyle(String widgetType) {
        return Style.EMPTY;
    }

    /**
     * Возвращает агрегированные флаги инвалидации виджета и его потомков.
     *
     * @return bitmask текущего subtree
     */
    default int subtreeInvalidationFlags() {
        int flags = invalidationFlags();
        for (Widget child : children()) {
            flags |= child.subtreeInvalidationFlags();
        }
        return flags;
    }

    /**
     * Помечает часть состояния виджета как требующую пересчёта или перерисовки.
     *
     * @param flags bitmask из {@link InvalidationFlags}
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 42,
            title = "<key:widget.method.invalidate.title>", summary = "<key:widget.method.invalidate.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 50, order = 42, summary = "Marks invalidation flags (VISUAL, LAYOUT, HIERARCHY, etc.).")
    void invalidate(@WikiParameter(description = "<key:widget.method.invalidate.param.flags>") int flags);

    /**
     * Снимает указанные флаги инвалидации после успешного обновления.
     *
     * @param flags bitmask, который был обработан runtime'ом
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 44,
            title = "<key:widget.method.clear_invalidation.title>", summary = "<key:widget.method.clear_invalidation.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 50, order = 44, summary = "Clears processed invalidation flags.")
    void clearInvalidation(@WikiParameter(description = "<key:widget.method.clear_invalidation.param.flags>") int flags);

    /**
     * Измеряет желаемый размер виджета с учётом переданного layout context.
     *
     * @param context constraints, scale и служебные данные layout pass
     */
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 30,
            title = "<key:widget.method.measure.title>", summary = "<key:widget.method.measure.summary>")
    @AiDoc(section = "Layout", sectionOrder = 30, order = 30, summary = "Measures widget desired size under given layout constraints.")
    void measure(@WikiParameter(description = "<key:widget.method.measure.param.context>") LayoutContext context);

    /**
     * Измеряет виджет с пропуском повторных вызовов на чистом поддереве.
     *
     * <p>Обычная реализация сохраняет прежнее поведение. {@code WidgetBase}
     * переопределяет метод и пропускает повторный measure, если поддерево
     * чистое и контекст совпадает с последним измерением.</p>
     *
     * @param context constraints, scale и служебные данные layout pass
     */
    default void measureCached(LayoutContext context) {
        measure(context);
    }

    /**
     * Размещает виджет в рассчитанных bounds и обновляет layout state.
     *
     * @param bounds финальный прямоугольник виджета
     */
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 40,
            title = "<key:widget.method.arrange.title>", summary = "<key:widget.method.arrange.summary>")
    @AiDoc(section = "Layout", sectionOrder = 30, order = 40, summary = "Positions and dimensions widget in assigned layout rectangle.")
    void arrange(@WikiParameter(description = "<key:widget.method.arrange.param.bounds>") RectView bounds);

    /**
     * Рисует виджет в текущем render context.
     *
     * @param context render context кадра
     */
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 10,
            title = "<key:widget.method.render.title>", summary = "<key:widget.method.render.summary>")
    @AiDoc(section = "Render", sectionOrder = 60, order = 10, summary = "Draws widget into active render context.")
    void render(@WikiParameter(description = "<key:widget.method.render.param.context>") RenderContext context);

    /**
     * Рендерит виджет через retained-фрагмент, если реализация поддерживает такой кэш.
     *
     * <p>Обычная реализация сохраняет прежнее поведение. {@code WidgetBase}
     * переопределяет метод и переиспользует команды стабильного виджета.</p>
     *
     * @param context render context текущего кадра
     */
    default void renderCached(RenderContext context) {
        render(context);
    }

    /**
     * Обрабатывает входящее событие UI.
     *
     * @param event событие input/runtime pipeline
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 50,
            title = "<key:widget.method.handle.title>", summary = "<key:widget.method.handle.summary>")
    @AiDoc(section = "Events", sectionOrder = 70, order = 10, summary = "Dispatches or consumes incoming UI event.")
    void handle(@WikiParameter(description = "<key:widget.method.handle.param.event>") Event event);

    /**
     * Обновляет состояние виджета на каждом кадре.
     *
     * @param frame snapshot текущего кадра
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 60,
            title = "<key:widget.method.tick.title>", summary = "<key:widget.method.tick.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 50, order = 60, summary = "Updates per-frame state, timers, and animations.")
    default void tick(@WikiParameter(description = "<key:widget.method.tick.param.frame>") FrameContext frame) {
    }

    /**
     * Освобождает ресурсы виджета перед удалением из дерева UI.
     *
     * <p>Метод должен освобождать только ресурсы, которыми владеет сам виджет. Глобальные renderer,
     * shared texture/font cache и registry очищаются их владельцами.</p>
     */
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 70,
            title = "<key:widget.method.dispose.title>", summary = "<key:widget.method.dispose.summary>")
    @AiDoc(section = "Lifecycle", sectionOrder = 50, order = 70, summary = "Releases widget-owned resources prior to removal.")
    default void dispose() {
    }
}
