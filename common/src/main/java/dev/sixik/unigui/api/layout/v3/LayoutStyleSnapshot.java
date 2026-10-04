package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.LayoutStyleLegacyAdapter;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;

/**
 * Неизменяемый снимок стиля компоновки V3.
 *
 * <p>{@code LayoutStyle} остаётся изменяемым пользовательским API. Проходы вычисления V3 работают
 * со снимками, чтобы дерево компоновки не могло измениться под бэкендом в середине прохода.</p>
 */
public record LayoutStyleSnapshot(
        PositionType position,
        SizeValue width,
        SizeValue height,
        SizeValue minWidth,
        SizeValue minHeight,
        SizeValue maxWidth,
        SizeValue maxHeight,
        EdgeInsets margin,
        AutoMargins marginAuto,
        EdgeInsets padding,
        Overflow overflowX,
        Overflow overflowY,
        FlexDirection flexDirection,
        FlexWrap flexWrap,
        float rowGap,
        float columnGap,
        float flexGrow,
        float flexShrink,
        SizeValue flexBasis,
        int order,
        float aspectRatio,
        java.util.List<GridTrack> gridTemplateColumns,
        java.util.List<GridTrack> gridTemplateRows,
        java.util.List<GridTrack> gridAutoColumns,
        java.util.List<GridTrack> gridAutoRows,
        GridAutoFlow gridAutoFlow,
        int gridColumnStart,
        int gridColumnSpan,
        int gridRowStart,
        int gridRowSpan,
        int zIndex,
        Align alignItems,
        Align alignSelf,
        AlignContent alignContent,
        Justify justifyContent,
        SizeValue left,
        SizeValue top,
        SizeValue right,
        SizeValue bottom) {
    public LayoutStyleSnapshot {
        position = position == null ? PositionType.RELATIVE : position;
        width = normalizeSize(width);
        height = normalizeSize(height);
        minWidth = normalizeSize(minWidth);
        minHeight = normalizeSize(minHeight);
        maxWidth = normalizeSize(maxWidth);
        maxHeight = normalizeSize(maxHeight);
        margin = margin == null ? EdgeInsets.ZERO : margin;
        marginAuto = marginAuto == null ? AutoMargins.NONE : marginAuto;
        padding = padding == null ? EdgeInsets.ZERO : padding;
        overflowX = overflowX == null ? Overflow.VISIBLE : overflowX;
        overflowY = overflowY == null ? Overflow.VISIBLE : overflowY;
        flexDirection = flexDirection == null ? FlexDirection.COLUMN : flexDirection;
        flexWrap = flexWrap == null ? FlexWrap.NOWRAP : flexWrap;
        rowGap = sanitize(rowGap);
        columnGap = sanitize(columnGap);
        flexGrow = sanitize(flexGrow);
        flexShrink = sanitize(flexShrink);
        flexBasis = normalizeSize(flexBasis);
        gridTemplateColumns = copyTracks(gridTemplateColumns);
        gridTemplateRows = copyTracks(gridTemplateRows);
        gridAutoColumns = copyTracks(gridAutoColumns);
        gridAutoRows = copyTracks(gridAutoRows);
        gridAutoFlow = gridAutoFlow == null ? GridAutoFlow.ROW : gridAutoFlow;
        gridColumnSpan = Math.max(1, gridColumnSpan);
        gridRowSpan = Math.max(1, gridRowSpan);
        alignItems = alignItems == null ? Align.STRETCH : alignItems;
        alignSelf = alignSelf == null ? Align.AUTO : alignSelf;
        alignContent = alignContent == null ? AlignContent.STRETCH : alignContent;
        justifyContent = justifyContent == null ? Justify.START : justifyContent;
        left = normalizeSize(left);
        top = normalizeSize(top);
        right = normalizeSize(right);
        bottom = normalizeSize(bottom);
    }

    public static LayoutStyleSnapshot defaults() {
        return from(new LayoutStyle());
    }

    public static LayoutStyleSnapshot from(LayoutStyle style) {
        LayoutStyle source = style == null ? new LayoutStyle() : style;
        return new LayoutStyleSnapshot(
                source.position(),
                source.width(),
                source.height(),
                source.minWidth(),
                source.minHeight(),
                source.maxWidth(),
                source.maxHeight(),
                source.margin(),
                source.marginAuto(),
                source.padding(),
                source.overflowX(),
                source.overflowY(),
                source.flexDirection(),
                source.flexWrap(),
                source.rowGap(),
                source.columnGap(),
                source.flexGrow(),
                source.flexShrink(),
                source.flexBasis(),
                source.order(),
                sanitizeAspectRatio(source.aspectRatio()),
                source.gridTemplateColumns(),
                source.gridTemplateRows(),
                source.gridAutoColumns(),
                source.gridAutoRows(),
                source.gridAutoFlow(),
                source.gridColumnStart(),
                source.gridColumnSpan(),
                source.gridRowStart(),
                source.gridRowSpan(),
                source.zIndex(),
                source.alignItems(),
                source.alignSelf(),
                source.alignContent(),
                source.justifyContent(),
                source.left(),
                source.top(),
                source.right(),
                source.bottom());
    }

    public static LayoutStyleSnapshot from(LayoutConstraints constraints) {
        return from(LayoutStyleLegacyAdapter.fromConstraints(constraints));
    }

    private static SizeValue normalizeSize(SizeValue value) {
        return value == null ? SizeValue.auto() : value;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float sanitizeAspectRatio(float value) {
        return Float.isFinite(value) && value > 0.0f ? value : Float.NaN;
    }

    private static java.util.List<GridTrack> copyTracks(java.util.List<GridTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return java.util.List.of();
        }
        return java.util.List.copyOf(tracks);
    }
}
