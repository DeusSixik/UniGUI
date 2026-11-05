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
    /** Вид записи пользовательского события; остальные совпадают с именами элементов контракта. */
    static final String CUSTOM = "Custom";

    /**
     * Запись панели контракта.
     *
     * @param kind {@code Param}, {@code Var}, {@code Event} или {@link #CUSTOM}
     * @param name имя
     */
    record Entry(String kind, String name) {
    }

    private final List<Runnable> listeners = new ArrayList<>();
    private final List<Runnable> entryListeners = new ArrayList<>();
    private Entry entry;
    private String problem = "";
    private ComponentPreview preview;
    private ComponentPreview running;

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

    /** @return запись контракта, показанная в инспекторе */
    Optional<Entry> entry() {
        return Optional.ofNullable(entry);
    }

    /**
     * Выбирает запись контракта; прежняя причина отказа забывается.
     *
     * @param next запись или {@code null}
     */
    void entry(Entry next) {
        this.entry = next;
        this.problem = "";
        for (Runnable listener : List.copyOf(entryListeners)) listener.run();
    }

    /** @return причина последней отклонённой правки контракта или пустая строка */
    String problem() {
        return problem;
    }

    /**
     * Показывает причину отказа в панели контракта.
     *
     * @param reason причина
     */
    void problem(String reason) {
        this.problem = reason == null ? "" : reason;
        for (Runnable listener : List.copyOf(entryListeners)) listener.run();
    }

    /**
     * @param listener вызывается при смене выбранной записи
     */
    void onEntry(Runnable listener) {
        entryListeners.add(listener);
    }

    /** @return копия, запущенная по Play, или пусто: только в неё отправляются события */
    Optional<ComponentPreview> running() {
        return Optional.ofNullable(running);
    }

    /**
     * Запоминает копию, запущенную по Play.
     *
     * @param next копия или {@code null}, когда запуск остановлен
     */
    void running(ComponentPreview next) {
        this.running = next;
        changed();
    }

    /** @return то, что сейчас исполняется: запущенная копия, а без неё предпросмотр */
    Optional<ComponentPreview> active() {
        return running != null ? running() : preview();
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
