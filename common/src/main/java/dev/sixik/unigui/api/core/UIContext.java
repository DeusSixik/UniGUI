package dev.sixik.unigui.api.core;

import dev.sixik.unigui.api.debug.UiProfiler;
import dev.sixik.unigui.api.debug.UiDebugCounters;
import dev.sixik.unigui.api.debug.DebugOverlaySettings;
import dev.sixik.unigui.api.event.EventEmitter;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.input.ClipboardService;
import dev.sixik.unigui.api.input.FocusManager;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.input.HoverManager;
import dev.sixik.unigui.api.input.KeyboardState;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Runtime-контекст одного UI дерева.
 *
 * <p>{@code UIContext} собирает сервисы, которые нужны виджетам, но не должны храниться внутри
 * каждого виджета напрямую: dispatcher, scale provider, routed events, hit testing, focus,
 * clipboard, theme и debug/profiling инфраструктура. Благодаря этому виджеты остаются переносимыми
 * между Minecraft runtime, editor preview и тестовым окружением.</p>
 *
 * <p>Большинство методов имеют безопасные default-реализации. Минимальный backend обязан предоставить
 * только {@link #dispatcher()}, {@link #scaleProvider()} и {@link #events()}. Остальные сервисы можно
 * подключать постепенно по мере возможностей runtime.</p>
 *
 * @see Widget#uiContext()
 */
public interface UIContext {
    /**
     * Возвращает состояние клавиатуры этого UI context'а.
     *
     * <p>Виджеты, которым нужно игровое управление или polling удерживаемых клавиш, читают этот сервис
     * в {@code tick(...)} через {@link KeyboardState#isDown(int)} и edge-методы
     * {@link KeyboardState#wasPressed(int)} / {@link KeyboardState#wasReleased(int)}.</p>
     *
     * @return keyboard state или {@link KeyboardState#NONE}, если runtime не поддерживает клавиатуру
     */
    default KeyboardState keyboard() {
        return KeyboardState.NONE;
    }

    /**
     * Возвращает dispatcher UI thread'а.
     *
     * @return очередь выполнения задач UI runtime
     */
    UiDispatcher dispatcher();

    /**
     * Возвращает provider масштаба между logical UI pixels и backend pixels.
     *
     * @return активный scale provider
     */
    UIScaleProvider scaleProvider();

    /**
     * Возвращает глобальный event emitter этого UI context.
     *
     * @return emitter для runtime/editor событий
     */
    EventEmitter events();

    /**
     * Возвращает dispatcher routed events.
     *
     * @return routed dispatcher или {@link RoutedEventDispatcher#DIRECT} для прямой доставки
     */
    default RoutedEventDispatcher routedEvents() {
        return RoutedEventDispatcher.DIRECT;
    }

    /**
     * Возвращает сервис hit testing.
     *
     * @return hit tester или {@link HitTester#NONE}, если runtime не поддерживает hit path
     */
    default HitTester hitTester() {
        return HitTester.NONE;
    }

    /**
     * Возвращает менеджер клавиатурного фокуса.
     *
     * @return focus manager или {@link FocusManager#NONE}
     */
    default FocusManager focusManager() {
        return FocusManager.NONE;
    }

    /**
     * Возвращает менеджер hover-состояния.
     *
     * @return hover manager или {@link HoverManager#NONE}
     */
    default HoverManager hoverManager() {
        return HoverManager.NONE;
    }

    /**
     * Возвращает виджет, который захватил указатель.
     *
     * <p>Pointer capture нужен для drag/resize/slider сценариев: после нажатия события движения
     * должны приходить исходному виджету даже тогда, когда курсор вышел за его bounds.</p>
     *
     * @param pointerId id указателя или мыши
     * @return виджет-владелец capture или {@code null}
     */
    default Widget capturedPointer(int pointerId) {
        return null;
    }

    /**
     * Захватывает указатель за виджетом.
     *
     * @param pointerId id указателя или мыши
     * @param widget виджет, который должен получать дальнейшие события этого указателя
     */
    default void capturePointer(int pointerId, Widget widget) {
    }

    /**
     * Освобождает указатель, если он всё ещё принадлежит указанному виджету.
     *
     * @param pointerId id указателя или мыши
     * @param widget виджет, который отпускает capture
     */
    default void releasePointer(int pointerId, Widget widget) {
    }

    /**
     * Принудительно очищает capture указателя.
     *
     * @param pointerId id указателя или мыши
     */
    default void clearPointerCapture(int pointerId) {
    }

    /**
     * Возвращает clipboard service.
     *
     * @return clipboard runtime'а или {@link ClipboardService#EMPTY}
     */
    default ClipboardService clipboard() {
        return ClipboardService.EMPTY;
    }

    /**
     * Возвращает активную тему UI.
     *
     * @return theme или {@link Theme#EMPTY}, если style system не подключена
     */
    default Theme theme() {
        return Theme.EMPTY;
    }

    /**
     * Возвращает версию style/theme данных.
     *
     * <p>Виджеты могут кешировать resolved styles и сравнивать version, чтобы не резолвить стили
     * каждый кадр. При изменении Theme версия должна увеличиваться.</p>
     *
     * @return монотонная версия style-состояния
     */
    default long styleVersion() {
        return theme().version();
    }

    /**
     * Инвалидирует визуальное состояние всего subtree после изменения стилей.
     *
     * @param root корень subtree, который должен перечитать style/theme данные
     */
    default void invalidateStyles(Widget root) {
        if (root == null) return;
        root.invalidate(InvalidationFlags.VISUAL);
        for (Widget child : root.children()) {
            invalidateStyles(child);
        }
    }

    /**
     * Возвращает profiler UI pipeline.
     *
     * @return profiler или {@link UiProfiler#NOOP}
     */
    default UiProfiler profiler() {
        return UiProfiler.NOOP;
    }

    /**
     * Возвращает debug counters текущего UI runtime.
     *
     * @return counters или {@link UiDebugCounters#NOOP}
     */
    default UiDebugCounters debugCounters() {
        return UiDebugCounters.NOOP;
    }

    /**
     * Возвращает настройки debug overlay.
     *
     * @return mutable или snapshot settings debug overlay
     */
    default DebugOverlaySettings debugOverlaySettings() {
        return new DebugOverlaySettings();
    }

    /**
     * Возвращает backend-specific debug flags.
     *
     * @return bitmask debug флагов; {@code 0}, если debug режим выключен
     */
    default int debugFlags() {
        return 0;
    }
}