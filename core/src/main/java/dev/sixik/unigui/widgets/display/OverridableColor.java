package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Цвет виджета с приоритетом «значение экземпляра выше значения стиля».
 *
 * <p>Виджеты данных ({@code Sparkline}, {@code Chart}, {@code GraphView}) исторически отдают наружу изменяемые
 * {@link MutableColor}, например {@code lineColor().set(...)}. Чтобы это продолжало работать вместе со style-системой,
 * цвет хранит флаг «переопределён»: пока никто не менял значение, виджет берёт цвет из стиля (тема, пакет стилей,
 * локальный стиль); как только значение изменено, оно имеет приоритет над стилем.</p>
 *
 * <p>Стиль никогда не записывается в {@link #value()}: это исключает перезапись пользовательского цвета при
 * смене темы и не порождает циклов уведомлений.</p>
 */
public final class OverridableColor {
    /**
     * Значение экземпляра; до первого изменения хранит значение по умолчанию, которое не используется при отрисовке,
     * если стиль объявляет цвет. Компоненты в диапазоне [0, 1].
     */
    private final MutableColor value;

    /**
     * {@code true}, если значение изменили после создания; такое значение приоритетнее стиля.
     */
    private boolean overridden;

    /**
     * Создаёт цвет и подписывается на его изменения.
     *
     * @param initial  значение по умолчанию; используется, когда стиль не объявляет цвет
     * @param onChange действие, которое нужно выполнить при изменении (обычно перерисовка); не {@code null}
     */
    public OverridableColor(MutableColor initial, Runnable onChange) {
        this.value = initial;
        this.value.onChanged(() -> {
            overridden = true;
            onChange.run();
        });
    }

    /**
     * Возвращает изменяемое значение экземпляра.
     *
     * <p>Любая запись в него помечает цвет переопределённым.</p>
     *
     * @return изменяемый цвет экземпляра; не {@code null}
     */
    public MutableColor value() {
        return value;
    }

    /**
     * Сообщает, переопределён ли цвет на экземпляре.
     *
     * @return {@code true}, если после создания значение изменяли
     */
    public boolean overridden() {
        return overridden;
    }

    /**
     * Выбирает итоговый цвет.
     *
     * <p>Порядок приоритета: значение экземпляра, затем цвет из стиля, затем значение по умолчанию.</p>
     *
     * @param styled цвет, разрешённый style-системой, или {@code null}, если стиль его не объявляет
     * @return цвет для отрисовки; не {@code null}
     */
    public ColorView resolve(ColorView styled) {
        if (overridden || styled == null) return value;
        return styled;
    }
}
