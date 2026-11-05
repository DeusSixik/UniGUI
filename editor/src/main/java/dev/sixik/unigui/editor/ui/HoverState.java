package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.xaml.UiNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Узел документа, над которым сейчас находится указатель: строка иерархии или виджет в предпросмотре. Панели
 * подсвечивают его у себя, поэтому наведение в одной панели видно во всех.
 */
final class HoverState {
    private final List<Runnable> listeners = new ArrayList<>();
    private UiNode node;

    /** @return узел под указателем или {@code null} */
    UiNode node() {
        return node;
    }

    /**
     * @param next новый узел или {@code null}, если указатель ушёл
     */
    void set(UiNode next) {
        if (next == node) return;
        node = next;
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    /**
     * @param listener вызывается при каждой смене узла
     */
    void subscribe(Runnable listener) {
        listeners.add(listener);
    }
}
