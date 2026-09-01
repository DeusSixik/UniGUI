package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
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

import java.util.List;

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
public interface Widget extends EventEmitter {
    /**
     * Возвращает runtime-контекст, к которому привязан виджет.
     *
     * @return UI context с dispatcher, input, theme и debug-сервисами
     */
    UIContext uiContext();

    /**
     * Возвращает родительский виджет в UI-дереве.
     *
     * @return parent или {@code null}, если виджет является root или ещё не добавлен в дерево
     */
    Widget parent();

    /**
     * Возвращает дочерние виджеты.
     *
     * <p>Порядок списка обычно совпадает с layout/render order и используется input pipeline для
     * hit testing. Возвращаемый список не обязан быть изменяемым.</p>
     *
     * @return дети виджета в порядке обхода
     */
    List<Widget> children();

    /**
     * Возвращает runtime/editor id виджета внутри UI-дерева.
     *
     * @return id или пустая строка, если id не задан
     */
    default String id() {
        return "";
    }

    /**
     * Задаёт runtime/editor id виджета внутри UI-дерева.
     *
     * @param id новый id; реализация может нормализовать {@code null} в пустую строку
     * @return этот виджет для fluent-настройки
     */
    default Widget id(String id) {
        return this;
    }

    /**
     * Возвращает явный id стиля, назначенный виджету через атрибут {@code style}.
     *
     * @return style id или пустая строка
     */
    default String styleId() {
        return "";
    }

    /**
     * Задаёт явный id стиля, назначенный виджету через атрибут {@code style}.
     *
     * @param styleId id стиля из Theme/StylePack
     * @return этот виджет для fluent-настройки
     */
    default Widget styleId(String styleId) {
        return this;
    }

    /**
     * Возвращает style classes виджета в порядке назначения.
     *
     * @return read-only или snapshot список классов
     */
    default List<String> styleClasses() {
        return List.of();
    }

    /**
     * Задаёт style classes одной строкой, разделённой пробелами или запятыми.
     *
     * @param styleClasses строка классов, например {@code "primary compact"}
     * @return этот виджет для fluent-настройки
     */
    default Widget styleClass(String styleClasses) {
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
    default Widget addStyleClass(String styleClass) {
        return this;
    }

    /**
     * Удаляет один style class.
     *
     * @param styleClass удаляемый class id
     * @return этот виджет для fluent-настройки
     */
    default Widget removeStyleClass(String styleClass) {
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
     * Возвращает текущие границы виджета после layout.
     *
     * @return arranged bounds в координатах parent/root, принятых текущей реализацией
     */
    RectView layoutBounds();

    /**
     * Возвращает размер, который виджет запросил у системы layout на последнем measure.
     *
     * @return desired size или {@link LayoutSize#ZERO}, если виджет ещё не измерен
     */
    default LayoutSize desiredSize() {
        return LayoutSize.ZERO;
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
    default Visibility visibility() {
        return Visibility.VISIBLE;
    }

    /**
     * Проверяет, должен ли виджет отображаться и участвовать во вводе.
     *
     * @return {@code true} только для {@link Visibility#VISIBLE}
     */
    default boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    /**
     * Проверяет, доступен ли виджет для взаимодействия пользователя.
     *
     * @return {@code true}, если input/focus события не должны игнорироваться disabled-состоянием
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Проверяет, находится ли указатель над виджетом.
     *
     * @return {@code true}, если hover manager считает виджет активным hover target
     */
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
     * Проверяет, может ли виджет получать клавиатурный фокус.
     *
     * @return {@code true}, если focus manager может выбрать этот виджет
     */
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
    void invalidate(int flags);

    /**
     * Снимает указанные флаги инвалидации после успешного обновления.
     *
     * @param flags bitmask, который был обработан runtime'ом
     */
    void clearInvalidation(int flags);

    /**
     * Измеряет желаемый размер виджета с учётом переданного layout context.
     *
     * @param context constraints, scale и служебные данные layout pass
     */
    void measure(LayoutContext context);

    /**
     * Размещает виджет в рассчитанных bounds и обновляет layout state.
     *
     * @param bounds финальный прямоугольник виджета
     */
    void arrange(RectView bounds);

    /**
     * Рисует виджет в текущем render context.
     *
     * @param context render context кадра
     */
    void render(RenderContext context);

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
    void handle(Event event);

    /**
     * Обновляет состояние виджета на каждом кадре.
     *
     * @param frame snapshot текущего кадра
     */
    default void tick(FrameContext frame) {
    }

    /**
     * Освобождает ресурсы виджета перед удалением из дерева UI.
     *
     * <p>Метод должен освобождать только ресурсы, которыми владеет сам виджет. Глобальные renderer,
     * shared texture/font cache и registry очищаются их владельцами.</p>
     */
    default void dispose() {
    }
}
