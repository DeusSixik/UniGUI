package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Типизированный редактор значения входа ноды: виджет плюс чтение, запись и стабильная текстовая форма.
 *
 * <p>Редактор — источник истины для значения по умолчанию входа. Виджет остаётся деталью реализации: код,
 * исполняющий граф, читает {@link #value()}, а сохранение и вставка работают через {@link #serialize()} и
 * {@link #deserialize(String)}. Готовые редакторы собираются через {@link ValueEditors}.</p>
 */
@AiDoc(
        summary = "Typed default-value editor of a node input: owns a widget, exposes the value and a stable text form.",
        purpose = "Separates the value of a node input from its widget so graphs can be executed, saved, copied and restored.",
        useWhen = {
                "Give an input a default value editor via `PortRowModule.editor(...)` or `NodeBuilder.input(..., editor)`.",
                "Read or write an input value with `NodeGraphItem.value(portId)` and `value(portId, value)`.",
                "Implement a custom editor (curve, gradient) by implementing this interface."
        },
        criticalRules = {
                "`silentValue` and `deserialize` never notify listeners; `value(Object)` notifies once if the value changed.",
                "`deserialize` ignores malformed text and keeps the current value.",
                "`serialize` must round-trip: `deserialize(serialize())` restores an equal value."
        },
        related = {"ValueEditors", "PortRowModule", "NodeGraphItem"})
@WikiPage(title = "ValueEditor", slug = "value-editor", category = "<key:graph.category>", summary = "<key:value.editor.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:value.editor.overview>", order = 10)
public interface ValueEditor {
    /**
     * @return виджет редактора, который размещается в строке порта
     */
    Widget widget();

    /**
     * @return текущее значение: {@code String}, {@code Double}, {@code Integer}, {@code Boolean},
     * {@code double[]} или другой тип пользовательского редактора
     */
    Object value();

    /**
     * Меняет значение и уведомляет слушателей, если оно изменилось.
     *
     * @param value новое значение подходящего редактору типа
     * @throws IllegalArgumentException если тип значения редактору не подходит
     */
    void value(Object value);

    /**
     * Меняет значение без уведомлений; нужен при восстановлении, чтобы не порождать лишних событий.
     *
     * @param value новое значение подходящего редактору типа
     * @throws IllegalArgumentException если тип значения редактору не подходит
     */
    void silentValue(Object value);

    /**
     * @return стабильная текстовая форма значения для сохранения
     */
    String serialize();

    /**
     * Восстанавливает значение из {@link #serialize()} без уведомлений.
     *
     * @param text текстовая форма; некорректный текст игнорируется
     */
    void deserialize(String text);

    /**
     * @param listener вызывается, когда значение изменили пользователь или код через {@link #value(Object)}
     */
    void onChange(Runnable listener);

    /**
     * @return желательная ширина виджета в world-единицах; используется как ширина inline-поля строки
     */
    default float preferredWidth() {
        return 84.0f;
    }
}
