package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.LayoutStyleLegacyAdapter;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;

/**
 * Immutable Layout V3 style snapshot.
 *
 * <p>LayoutStyle remains the user-facing mutable API. V3 compute passes operate
 * on snapshots so a layout tree cannot change underneath a backend mid-pass.</p>
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
        Align alignItems,
        Align alignSelf,
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
        alignItems = alignItems == null ? Align.STRETCH : alignItems;
        alignSelf = alignSelf == null ? Align.AUTO : alignSelf;
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
                source.alignItems(),
                source.alignSelf(),
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
}
