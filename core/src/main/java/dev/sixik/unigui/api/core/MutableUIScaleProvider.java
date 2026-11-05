package dev.sixik.unigui.api.core;

/**
 * Изменяемый поставщик UI scale для экранов с ручным управлением масштабом.
 *
 * <p>Класс удобен для настроек, editor preview и тестов, где scale должен меняться без создания
 * нового {@link UIContext}. Значение всегда проходит через {@link UIScaleProvider#sanitize(float)},
 * поэтому внешний код может безопасно передавать пользовательский ввод.</p>
 */
public final class MutableUIScaleProvider implements UIScaleProvider {
    private float scale;

    /**
     * Создаёт provider со scale {@code 1.0f}.
     */
    public MutableUIScaleProvider() {
        this(1.0f);
    }

    /**
     * Создаёт provider с указанным scale.
     *
     * @param scale начальный scale; невалидные значения заменяются на {@code 1.0f}
     */
    public MutableUIScaleProvider(float scale) {
        this.scale = UIScaleProvider.sanitize(scale);
    }

    /**
     * @return текущий нормализованный scale
     */
    @Override
    public float scale() {
        return scale;
    }

    /**
     * Изменяет scale provider'а.
     *
     * @param scale новый scale; невалидные значения заменяются на {@code 1.0f}
     * @return этот provider для fluent-настройки
     */
    public MutableUIScaleProvider scale(float scale) {
        this.scale = UIScaleProvider.sanitize(scale);
        return this;
    }
}