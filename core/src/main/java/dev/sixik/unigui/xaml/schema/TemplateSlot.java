package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Слот, который принимает не готовый виджет, а шаблон ({@code <VirtualListView.ItemTemplate>}).
 *
 * @param <T> тип родителя
 */
public interface TemplateSlot<T extends Widget> {
    /**
     * Принимает шаблон при загрузке документа.
     *
     * @param parent   виджет-владелец
     * @param template шаблон
     */
    void accept(T parent, Template template);

    /**
     * Возвращает принятый шаблон: так сохранение записывает слот обратно в документ.
     *
     * @param parent виджет-владелец
     * @return шаблон или {@code null}, если слот пуст
     */
    Template current(T parent);
}
