package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.core.FrameContext;
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

public interface Widget extends EventEmitter {
    /**
     * Возвращает контекст интерфейса, к которому привязан виджет.
     */
    UIContext uiContext();

    /**
     * Возвращает родительский виджет в дереве интерфейса.
     */
    Widget parent();

    /**
     * Возвращает дочерние виджеты, участвующие в компоновке, вводе и отрисовке.
     */
    List<Widget> children();

    /**
     * Возвращает runtime/editor id виджета внутри UI-дерева.
     */
    default String id() {
        return "";
    }

    /**
     * Задаёт runtime/editor id виджета внутри UI-дерева.
     */
    default Widget id(String id) {
        return this;
    }
    /**
     * Возвращает явный id стиля, назначенный виджету через атрибут style.
     */
    default String styleId() {
        return "";
    }

    /**
     * Задаёт явный id стиля, назначенный виджету через атрибут style.
     */
    default Widget styleId(String styleId) {
        return this;
    }

    /**
     * Возвращает style classes виджета в порядке назначения.
     */
    default List<String> styleClasses() {
        return List.of();
    }

    /**
     * Задаёт style classes одной строкой, разделённой пробелами или запятыми.
     */
    default Widget styleClass(String styleClasses) {
        return this;
    }

    /**
     * Задаёт style classes одной строкой, разделённой пробелами или запятыми.
     */
    default Widget styleClasses(String styleClasses) {
        return styleClass(styleClasses);
    }

    /**
     * Добавляет один style class.
     */
    default Widget addStyleClass(String styleClass) {
        return this;
    }

    /**
     * Удаляет один style class.
     */
    default Widget removeStyleClass(String styleClass) {
        return this;
    }

    /**
     * Проверяет наличие style class.
     */
    default boolean hasStyleClass(String styleClass) {
        return false;
    }

    /**
     * Возвращает текущие границы виджета после компоновки.
     */
    RectView layoutBounds();

    /**
     * Возвращает размер, который виджет запрашивает у системы компоновки.
     */
    default LayoutSize desiredSize() {
        return LayoutSize.ZERO;
    }

    /**
     * Возвращает трансформацию, применяемую к виджету при отрисовке.
     */
    Transform transform();

    /**
     * Возвращает флаги инвалидации самого виджета.
     */
    int invalidationFlags();

    /**
     * Возвращает ограничения компоновки, применяемые к виджету.
     */
    default LayoutConstraints layoutConstraints() {
        return LayoutConstraints.DEFAULT;
    }

    /**
     * Возвращает режим видимости виджета.
     */
    default Visibility visibility() {
        return Visibility.VISIBLE;
    }

    /**
     * Проверяет, должен ли виджет отображаться и участвовать во вводе.
     */
    default boolean visible() {
        return visibility() == Visibility.VISIBLE;
    }

    /**
     * Проверяет, доступен ли виджет для взаимодействия пользователя.
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Проверяет, находится ли указатель над виджетом.
     */
    default boolean hovered() {
        return false;
    }

    /**
     * Возвращает курсор, который нужно показать в указанной локальной точке виджета.
     */
    default MouseCursor mouseCursorAt(float localX, float localY) {
        return MouseCursor.DEFAULT;
    }

    /**
     * Проверяет, может ли виджет получать клавиатурный фокус.
     */
    default boolean focusable() {
        return false;
    }

    /**
     * Проверяет, образует ли виджет отдельную область фокусировки.
     */
    default boolean focusScope() {
        return false;
    }

    /**
     * Возвращает порядок обхода фокуса для этого виджета.
     */
    default int focusOrder() {
        return 0;
    }

    /**
     * Проверяет, ограничивает ли виджет область применения локальных стилей.
     */
    default boolean styleScope() {
        return false;
    }

    /**
     * Возвращает локальный стиль для указанного типа виджета.
     */
    default Style localStyle(String widgetType) {
        return Style.EMPTY;
    }

    /**
     * Возвращает агрегированные флаги инвалидации виджета и его потомков.
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
     */
    void invalidate(int flags);

    /**
     * Снимает указанные флаги инвалидации после успешного обновления.
     */
    void clearInvalidation(int flags);

    /**
     * Измеряет желаемый размер виджета с учётом переданного контекста компоновки.
     */
    void measure(LayoutContext context);

    /**
     * Размещает виджет в рассчитанных границах и обновляет состояние компоновки.
     */
    void arrange(RectView bounds);

    /**
     * Отрисовывает виджет в текущем контексте отрисовки.
     */
    void render(RenderContext context);

    /**
     * Обрабатывает входящее событие интерфейса и обновляет состояние виджета при необходимости.
     */
    void handle(Event event);

    /**
     * Обновляет состояние виджета на каждом кадре.
     */
    default void tick(FrameContext frame) {
    }

    /**
     * Освобождает ресурсы виджета перед удалением из дерева интерфейса.
     */
    default void dispose() {
    }
}
