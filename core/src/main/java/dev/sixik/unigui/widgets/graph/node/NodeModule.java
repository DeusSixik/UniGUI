package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;

import java.util.List;

/**
 * Строительный блок ноды: полоса, которая раскладывается сверху вниз внутри тела ноды.
 *
 * <p>Нода состоит из упорядоченных модулей: заголовок, строки портов, поля ввода, превью, разделители.
 * Модуль владеет своими виджетами и портами, сам измеряет и размещает их и может дорисовать фон. Так можно
 * собрать и Blueprint-ноду (exec-строки, data-строки), и Shader Graph (превью-плитка, ряд выходов) из
 * одних и тех же деталей, а собственные детали подключаются реализацией этого интерфейса.</p>
 *
 * <p>Порядок вызовов за кадр: {@link #measureWidth}, затем {@link #measureHeight}, затем {@link #arrange};
 * {@link #paintBackground} вызывается до отрисовки виджетов модуля.</p>
 */
@AiDoc(
        summary = "Pluggable vertical strip of a node: owns widgets and ports, measures and arranges them, optionally paints decor.",
        purpose = "Makes nodes composable so Blueprint or Shader Graph nodes are assembled from reusable parts.",
        useWhen = {
                "Add a custom row (preview tile, slider, color swatch) by implementing NodeModule.",
                "Hide detail when a node is collapsed via `visibleWhenCollapsed()`."
        },
        criticalRules = {
                "All geometry is in world units relative to the node origin; the graph applies zoom.",
                "Only widgets returned by `widgets()` take part in layout, rendering and hit testing.",
                "Register ports through `NodeGraphItem.addPort(port, module)` inside `attach`; remove them in `detach`.",
                "Return a new list from `widgets()` only when it changes: the graph caches the visible widget list until `NodeGraphItem.refreshModules()`."
        },
        related = {"NodeGraphItem", "NodeBuilder", "HeaderModule", "PortRowModule", "WidgetModule", "SeparatorModule"})
public interface NodeModule {
    /**
     * @return id, уникальный внутри ноды
     */
    String id();

    /**
     * @return {@code true}, если модуль остаётся видимым у свёрнутой ноды (заголовок)
     */
    default boolean visibleWhenCollapsed() {
        return false;
    }

    /**
     * @return виджеты, которыми владеет модуль и которые сейчас должны быть видны
     */
    default List<Widget> widgets() {
        return List.of();
    }

    /**
     * @return все виджеты модуля, включая временно скрытые; нужны для dispose и UI-контекста
     */
    default List<Widget> allWidgets() {
        return widgets();
    }

    /**
     * Вызывается при добавлении модуля в ноду. Здесь модуль создаёт порты.
     *
     * @param item нода-владелец
     */
    default void attach(NodeGraphItem item) {
    }

    /**
     * Вызывается при удалении модуля из ноды. Здесь модуль убирает свои порты.
     *
     * @param item нода-владелец
     */
    default void detach(NodeGraphItem item) {
    }

    /**
     * @param context сервисы раскладки; виджеты модуля уже измерены
     * @return минимальная ширина модуля
     */
    float measureWidth(NodeLayoutContext context);

    /**
     * @param context сервисы раскладки
     * @param width   итоговая ширина ноды
     * @return высота модуля при этой ширине
     */
    float measureHeight(NodeLayoutContext context, float width);

    /**
     * Размещает виджеты и якоря портов модуля.
     *
     * @param context сервисы раскладки
     * @param x       левая граница полосы (всегда 0)
     * @param y       верхняя граница полосы
     * @param width   ширина полосы
     * @param height  высота полосы
     */
    void arrange(NodeLayoutContext context, float x, float y, float width, float height);

    /**
     * Рисует фон модуля под его виджетами.
     *
     * @param context контекст рисования
     * @param x       левая граница полосы (node-local)
     * @param y       верхняя граница полосы
     * @param width   ширина полосы
     * @param height  высота полосы
     */
    default void paintBackground(NodePaintContext context, float x, float y, float width, float height) {
    }

    /**
     * Уведомляет, что число соединений порта изменилось. Типичное применение — спрятать inline-поле у
     * подключённого входа.
     *
     * @param port        порт этого модуля
     * @param connections текущее число соединений
     */
    default void onPortConnectionsChanged(NodeGraphPort port, int connections) {
    }

    /**
     * Позволяет модулю перехватить нажатие по своей полосе до начала перетаскивания ноды.
     *
     * @param localX X нажатия относительно ноды
     * @param localY Y нажатия относительно ноды
     * @param x      левая граница полосы
     * @param y      верхняя граница полосы
     * @param width  ширина полосы
     * @param height высота полосы
     * @return {@code true}, если нажатие обработано и ноду двигать не надо
     */
    default boolean onPress(float localX, float localY, float x, float y, float width, float height) {
        return false;
    }
}
