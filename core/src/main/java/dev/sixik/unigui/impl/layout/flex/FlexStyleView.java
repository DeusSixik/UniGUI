package dev.sixik.unigui.impl.layout.flex;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;

/**
 * Бэкенд-независимое представление flex-свойств стиля.
 *
 * <p>Живой путь виджетов читает {@code LayoutStyle}, путь снапшотов читает
 * {@code LayoutStyleSnapshot}. Оба предоставляют одинаковую поверхность через этот интерфейс,
 * поэтому {@link FlexSolver} реализует flex-алгоритм один раз (фаза 1.1).</p>
 */
public interface FlexStyleView {
    PositionType position();

    SizeValue width();

    SizeValue height();

    SizeValue minWidth();

    SizeValue minHeight();

    SizeValue maxWidth();

    SizeValue maxHeight();

    EdgeInsets margin();

    AutoMargins marginAuto();

    EdgeInsets padding();

    FlexDirection flexDirection();

    FlexWrap flexWrap();

    float rowGap();

    float columnGap();

    float flexGrow();

    float flexShrink();

    SizeValue flexBasis();

    int order();

    float aspectRatio();

    Align alignItems();

    Align alignSelf();

    AlignContent alignContent();

    Justify justifyContent();

    SizeValue left();

    SizeValue top();

    SizeValue right();

    SizeValue bottom();
}
