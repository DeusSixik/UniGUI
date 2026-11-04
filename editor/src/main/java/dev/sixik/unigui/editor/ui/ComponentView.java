package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.xaml.preview.ComponentPreview;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общая точка между панелью предпросмотра и панелью контракта: первая создаёт и пересоздаёт
 * {@link ComponentPreview} вместе со сменой документа, вторая показывает его параметры и журнал событий и пишет в него
 * примерные значения. Связывающего объекта не пересоздают, поэтому панели не зависят от порядка своего создания.
 */
final class ComponentView {
    private final List<Runnable> listeners = new ArrayList<>();
    private ComponentPreview preview;

    /** @return предпросмотр компонента или пусто, если открыт не компонент */
    Optional<ComponentPreview> preview() {
        return Optional.ofNullable(preview);
    }

    /**
     * Заменяет предпросмотр и сообщает подписчикам.
     *
     * @param next новый предпросмотр или {@code null}
     */
    void preview(ComponentPreview next) {
        this.preview = next;
        changed();
    }

    /** Сообщает подписчикам, что предпросмотр показал другое состояние или журнал событий вырос. */
    void changed() {
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    /**
     * @param listener вызывается при каждом {@link #changed()}
     */
    void subscribe(Runnable listener) {
        listeners.add(listener);
    }
}
