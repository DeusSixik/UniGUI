package dev.sixik.unigui.xaml.logic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Отладчик графа: точки останова на нодах потока и управление остановленным запуском.
 *
 * <p>Запуск останавливается <b>перед</b> нодой с точкой останова. Остановка не блокирует игру: обработчик события
 * возвращается, интерфейс продолжает работать, а запуск ждёт команды {@link #resume()} или {@link #step()}. Пока запуск
 * стоит, время графа (задержки) заморожено, а новые события встают в очередь и выполняются после его завершения.</p>
 *
 * <p>Остановить можно только ноды потока (с пином {@code exec}). Чистые ноды вычисляются внутри шага той ноды, которой
 * нужны их данные. Вложенные запуски (пользовательские события, вызванные из ноды) точек останова не проверяют.</p>
 *
 * <p>Отладчик принадлежит редактору, а не экземпляру: точки останова переживают пересборку предпросмотра.</p>
 */
public final class GraphDebugger {
    /**
     * Что делает отладчик с остановленным запуском: исполняется на стороне среды исполнения графа.
     */
    interface Control {
        void resume();

        void step();
    }

    /**
     * Состояние остановки.
     *
     * @param nodeId нода, перед которой запуск стоит
     * @param entry  нода-вход запуска
     * @param type   тип ноды
     */
    public record Pause(String nodeId, String entry, String type) {
    }

    private final Set<String> breakpoints = new LinkedHashSet<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private Control control;
    private Pause pause;

    // ------------------------------------------------------------------ точки останова

    /**
     * @param nodeId нода
     * @return {@code true}, если на ноде стоит точка останова
     */
    public boolean isBreakpoint(String nodeId) {
        return breakpoints.contains(nodeId);
    }

    /**
     * Ставит или снимает точку останова.
     *
     * @param nodeId нода
     * @return {@code true}, если точка теперь стоит
     */
    public boolean toggle(String nodeId) {
        boolean now = breakpoints.add(nodeId);
        if (!now) breakpoints.remove(nodeId);
        changed();
        return now;
    }

    /**
     * Ставит точку останова.
     *
     * @param nodeId нода
     */
    public void set(String nodeId) {
        if (breakpoints.add(nodeId)) changed();
    }

    /**
     * Снимает все точки останова.
     */
    public void clearBreakpoints() {
        if (breakpoints.isEmpty()) return;
        breakpoints.clear();
        changed();
    }

    /**
     * @return идентификаторы нод с точками останова в порядке постановки
     */
    public Set<String> breakpoints() {
        return Set.copyOf(breakpoints);
    }

    /**
     * Убирает точки на нодах, которых больше нет в графе (после правки документа).
     *
     * @param existing идентификаторы нод, которые есть
     */
    public void retain(Set<String> existing) {
        if (breakpoints.retainAll(existing)) changed();
    }

    // ------------------------------------------------------------------ остановка

    /**
     * @return остановка или пусто, если запуск не стоит
     */
    public Optional<Pause> paused() {
        return Optional.ofNullable(pause);
    }

    /**
     * Продолжает остановленный запуск до следующей точки останова или до конца.
     */
    public void resume() {
        if (control != null && pause != null) control.resume();
    }

    /**
     * Исполняет одну ноду остановленного запуска и останавливается перед следующей.
     */
    public void step() {
        if (control != null && pause != null) control.step();
    }

    /**
     * @param listener вызывается при смене точек останова и при остановке и продолжении запуска
     */
    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    // ------------------------------------------------------------------ для среды исполнения

    void bind(Control next) {
        if (control != null && control != next && pause != null) {
            pause = null; // прежний экземпляр заменён, его остановка больше не действует
        }
        control = next;
        changed();
    }

    void paused(Pause next) {
        pause = next;
        changed();
    }

    void resumed() {
        if (pause == null) return;
        pause = null;
        changed();
    }

    private void changed() {
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }
}
