package dev.sixik.unigui.impl.layout.flex;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.layout.v3.LayoutStyleSnapshot;

/**
 * Фабрики {@link FlexStyleView} для изменяемой модели стилей и модели снапшотов.
 */
public final class FlexStyleViews {
    private FlexStyleViews() {
    }

    /**
     * Оборачивает живой изменяемый стиль. Стиль {@code null} читается как значения по умолчанию.
     *
     * @param style живой стиль или {@code null}
     * @return представление стиля
     */
    public static FlexStyleView of(LayoutStyle style) {
        return new LayoutStyleView(style);
    }

    /**
     * Оборачивает неизменяемый снапшот. Снапшот {@code null} читается как значения по умолчанию.
     *
     * @param snapshot снапшот или {@code null}
     * @return представление снапшота
     */
    public static FlexStyleView of(LayoutStyleSnapshot snapshot) {
        return new SnapshotStyleView(snapshot);
    }

    private static final class LayoutStyleView implements FlexStyleView {
        private final LayoutStyle style;

        private LayoutStyleView(LayoutStyle style) {
            this.style = style == null ? new LayoutStyle() : style;
        }

        @Override
        public PositionType position() {
            return style.position();
        }

        @Override
        public SizeValue width() {
            return style.width();
        }

        @Override
        public SizeValue height() {
            return style.height();
        }

        @Override
        public SizeValue minWidth() {
            return style.minWidth();
        }

        @Override
        public SizeValue minHeight() {
            return style.minHeight();
        }

        @Override
        public SizeValue maxWidth() {
            return style.maxWidth();
        }

        @Override
        public SizeValue maxHeight() {
            return style.maxHeight();
        }

        @Override
        public EdgeInsets margin() {
            return style.margin();
        }

        @Override
        public AutoMargins marginAuto() {
            return style.marginAuto();
        }

        @Override
        public EdgeInsets padding() {
            return style.padding();
        }

        @Override
        public FlexDirection flexDirection() {
            return style.flexDirection();
        }

        @Override
        public FlexWrap flexWrap() {
            return style.flexWrap();
        }

        @Override
        public float rowGap() {
            return style.rowGap();
        }

        @Override
        public float columnGap() {
            return style.columnGap();
        }

        @Override
        public float flexGrow() {
            return style.flexGrow();
        }

        @Override
        public float flexShrink() {
            return style.flexShrink();
        }

        @Override
        public SizeValue flexBasis() {
            return style.flexBasis();
        }

        @Override
        public int order() {
            return style.order();
        }

        @Override
        public float aspectRatio() {
            return style.aspectRatio();
        }

        @Override
        public Align alignItems() {
            return style.alignItems();
        }

        @Override
        public Align alignSelf() {
            return style.alignSelf();
        }

        @Override
        public AlignContent alignContent() {
            return style.alignContent();
        }

        @Override
        public Justify justifyContent() {
            return style.justifyContent();
        }

        @Override
        public SizeValue left() {
            return style.left();
        }

        @Override
        public SizeValue top() {
            return style.top();
        }

        @Override
        public SizeValue right() {
            return style.right();
        }

        @Override
        public SizeValue bottom() {
            return style.bottom();
        }
    }

    private static final class SnapshotStyleView implements FlexStyleView {
        private final LayoutStyleSnapshot snapshot;

        private SnapshotStyleView(LayoutStyleSnapshot snapshot) {
            this.snapshot = snapshot == null ? LayoutStyleSnapshot.defaults() : snapshot;
        }

        @Override
        public PositionType position() {
            return snapshot.position();
        }

        @Override
        public SizeValue width() {
            return snapshot.width();
        }

        @Override
        public SizeValue height() {
            return snapshot.height();
        }

        @Override
        public SizeValue minWidth() {
            return snapshot.minWidth();
        }

        @Override
        public SizeValue minHeight() {
            return snapshot.minHeight();
        }

        @Override
        public SizeValue maxWidth() {
            return snapshot.maxWidth();
        }

        @Override
        public SizeValue maxHeight() {
            return snapshot.maxHeight();
        }

        @Override
        public EdgeInsets margin() {
            return snapshot.margin();
        }

        @Override
        public AutoMargins marginAuto() {
            return snapshot.marginAuto();
        }

        @Override
        public EdgeInsets padding() {
            return snapshot.padding();
        }

        @Override
        public FlexDirection flexDirection() {
            return snapshot.flexDirection();
        }

        @Override
        public FlexWrap flexWrap() {
            return snapshot.flexWrap();
        }

        @Override
        public float rowGap() {
            return snapshot.rowGap();
        }

        @Override
        public float columnGap() {
            return snapshot.columnGap();
        }

        @Override
        public float flexGrow() {
            return snapshot.flexGrow();
        }

        @Override
        public float flexShrink() {
            return snapshot.flexShrink();
        }

        @Override
        public SizeValue flexBasis() {
            return snapshot.flexBasis();
        }

        @Override
        public int order() {
            return snapshot.order();
        }

        @Override
        public float aspectRatio() {
            return snapshot.aspectRatio();
        }

        @Override
        public Align alignItems() {
            return snapshot.alignItems();
        }

        @Override
        public Align alignSelf() {
            return snapshot.alignSelf();
        }

        @Override
        public AlignContent alignContent() {
            return snapshot.alignContent();
        }

        @Override
        public Justify justifyContent() {
            return snapshot.justifyContent();
        }

        @Override
        public SizeValue left() {
            return snapshot.left();
        }

        @Override
        public SizeValue top() {
            return snapshot.top();
        }

        @Override
        public SizeValue right() {
            return snapshot.right();
        }

        @Override
        public SizeValue bottom() {
            return snapshot.bottom();
        }
    }
}
