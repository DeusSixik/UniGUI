package dev.sixik.unigui.api.core;

/**
 * Очередь выполнения задач на UI thread.
 *
 * <p>Большая часть UI API должна изменяться с одного потока. Dispatcher даёт backend'у способ
 * безопасно принять задачу из любого места, выполнить её на UI thread и, при необходимости,
 * отложить до следующего кадра. Конкретная реализация решает, синхронно ли выполнить задачу,
 * если вызов уже пришёл с UI thread.</p>
 */
public interface UiDispatcher {
    /**
     * @return {@code true}, если текущий поток является UI thread этого runtime
     */
    boolean isUiThread();

    /**
     * Планирует действие на UI thread.
     *
     * @param action действие; реализация может игнорировать {@code null} или выбросить ошибку
     */
    void execute(Runnable action);

    /**
     * Планирует действие на начало следующего UI-кадра.
     *
     * @param action действие, которое нельзя выполнять в текущей фазе кадра
     */
    void executeNextFrame(Runnable action);

    /**
     * Выполняет накопленные задачи, которые уже можно безопасно применить.
     */
    void drain();
}