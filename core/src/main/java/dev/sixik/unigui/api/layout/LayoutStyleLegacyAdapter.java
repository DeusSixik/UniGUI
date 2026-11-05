package dev.sixik.unigui.api.layout;

/**
 * Адаптер совместимости между текущим API {@link LayoutStyle} и неизменяемым
 * контрактом ограничений {@link LayoutConstraints} старого формата.
 *
 * <p>Класс изолирует правила преобразования старого формата от основного API
 * стиля. Новый код должен настраивать {@link LayoutStyle} напрямую.</p>
 */
public final class LayoutStyleLegacyAdapter {
    private LayoutStyleLegacyAdapter() {
    }

    /**
     * Создаёт изменяемый стиль из ограничений старого формата.
     *
     * @param constraints ограничения старого формата или {@code null}
     * @return новый стиль с перенесёнными значениями
     */
    public static LayoutStyle fromConstraints(LayoutConstraints constraints) {
        LayoutConstraints source = constraints == null ? LayoutConstraints.DEFAULT : constraints;
        LayoutStyle style = new LayoutStyle()
                .width(source.preferredWidth())
                .height(source.preferredHeight())
                .minWidth(source.minWidth())
                .minHeight(source.minHeight())
                .maxWidth(source.maxWidth())
                .maxHeight(source.maxHeight())
                .margin(source.margin())
                .flexGrow(source.grow())
                .flexShrink(source.grow() > 0.0f ? 1.0f : 0.0f);
        style.setLegacyAlignmentInternal(source.horizontalAlignment(), source.verticalAlignment());
        return style;
    }

    /**
     * Преобразует текущий стиль в ограничения старого формата.
     *
     * <p>Процентные значения нельзя представить в {@code LayoutConstraints} старого формата;
     * вместо них сохраняется соответствующее значение из {@code fallback}.</p>
     *
     * @param style текущий стиль или {@code null}
     * @param fallback значения для полей, которых нет в контракте старого формата
     * @return ограничения старого формата
     */
    public static LayoutConstraints toConstraints(LayoutStyle style, LayoutConstraints fallback) {
        LayoutStyle sourceStyle = style == null ? new LayoutStyle() : style;
        LayoutConstraints source = fallback == null ? LayoutConstraints.DEFAULT : fallback;
        float preferredWidth = legacyPreferred(sourceStyle.width(), source.preferredWidth());
        float preferredHeight = legacyPreferred(sourceStyle.height(), source.preferredHeight());
        float resolvedMinWidth = legacyMinimum(sourceStyle.minWidth(), source.minWidth());
        float resolvedMinHeight = legacyMinimum(sourceStyle.minHeight(), source.minHeight());
        float resolvedMaxWidth = legacyMaximum(sourceStyle.maxWidth(), source.maxWidth());
        float resolvedMaxHeight = legacyMaximum(sourceStyle.maxHeight(), source.maxHeight());
        return new LayoutConstraints(
                preferredWidth, preferredHeight,
                resolvedMinWidth, resolvedMinHeight,
                resolvedMaxWidth, resolvedMaxHeight,
                sourceStyle.margin(),
                sourceStyle.legacyHorizontalAlignmentInternal(),
                sourceStyle.legacyVerticalAlignmentInternal(),
                sourceStyle.flexGrow());
    }

    private static float legacyPreferred(SizeValue value, float fallback) {
        return value.isAuto() ? LayoutConstraints.AUTO : value.isPixels() ? value.value() : fallback;
    }

    private static float legacyMinimum(SizeValue value, float fallback) {
        return value.isAuto() ? 0.0f : value.isPixels() ? value.value() : fallback;
    }

    private static float legacyMaximum(SizeValue value, float fallback) {
        return value.isAuto() ? Float.POSITIVE_INFINITY : value.isPixels() ? value.value() : fallback;
    }
}
