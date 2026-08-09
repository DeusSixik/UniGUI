package dev.sixik.unigui.api.layout;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Mutable Layout v2 style attached to every {@code WidgetBase}.
 *
 * <p>Pixel overloads are concise defaults; {@link SizeValue} overloads support
 * {@link SizeValue#auto()}, pixels and percentages. Multiple mutations made inside
 * {@link #update(Consumer)} emit a single change notification.</p>
 *
 * <pre>{@code
 * widget.layout(style -> style
 *         .sizePercent(100.0f, 50.0f)
 *         .minSize(80.0f, 24.0f)
 *         .padding(8.0f)
 *         .flex(1.0f, 1.0f, SizeValue.px(120.0f))
 *         .overflowY(Overflow.AUTO));
 * }</pre>
 */
public final class LayoutStyle {
    private Runnable onChanged;
    private int updateDepth;
    private boolean pendingChange;

    private PositionType position = PositionType.RELATIVE;
    private SizeValue width = SizeValue.auto();
    private SizeValue height = SizeValue.auto();
    private SizeValue minWidth = SizeValue.px(0.0f);
    private SizeValue minHeight = SizeValue.px(0.0f);
    private SizeValue maxWidth = SizeValue.auto();
    private SizeValue maxHeight = SizeValue.auto();
    private EdgeInsets margin = EdgeInsets.ZERO;
    private EdgeInsets padding = EdgeInsets.ZERO;
    private Overflow overflowX = Overflow.VISIBLE;
    private Overflow overflowY = Overflow.VISIBLE;
    private FlexDirection flexDirection = FlexDirection.COLUMN;
    private FlexWrap flexWrap = FlexWrap.NOWRAP;
    private float rowGap;
    private float columnGap;
    private float flexGrow;
    private float flexShrink = 1.0f;
    private SizeValue flexBasis = SizeValue.auto();
    private Align alignItems = Align.STRETCH;
    private Align alignSelf = Align.AUTO;
    private Alignment horizontalAlignment = Alignment.STRETCH;
    private Alignment verticalAlignment = Alignment.STRETCH;
    private Justify justifyContent = Justify.START;
    private SizeValue left = SizeValue.auto();
    private SizeValue top = SizeValue.auto();
    private SizeValue right = SizeValue.auto();
    private SizeValue bottom = SizeValue.auto();

    public LayoutStyle() {
    }

    public LayoutStyle(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public LayoutStyle onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public LayoutStyle update(Consumer<LayoutStyle> update) {
        if (update == null) return this;
        updateDepth++;
        try {
            update.accept(this);
        } finally {
            updateDepth--;
            if (updateDepth == 0 && pendingChange) {
                pendingChange = false;
                notifyChanged();
            }
        }
        return this;
    }

    public LayoutStyle copy() {
        return new LayoutStyle().copyFrom(this);
    }

    public LayoutStyle copyFrom(LayoutStyle other) {
        LayoutStyle source = other == null ? new LayoutStyle() : other;
        return update(style -> {
            style.position(source.position);
            style.width(source.width);
            style.height(source.height);
            style.minWidth(source.minWidth);
            style.minHeight(source.minHeight);
            style.maxWidth(source.maxWidth);
            style.maxHeight(source.maxHeight);
            style.margin(source.margin);
            style.padding(source.padding);
            style.overflowX(source.overflowX);
            style.overflowY(source.overflowY);
            style.flexDirection(source.flexDirection);
            style.flexWrap(source.flexWrap);
            style.rowGap(source.rowGap);
            style.columnGap(source.columnGap);
            style.flexGrow(source.flexGrow);
            style.flexShrink(source.flexShrink);
            style.flexBasis(source.flexBasis);
            style.alignItems(source.alignItems);
            style.alignSelf(source.alignSelf);
            style.horizontalAlignment = source.horizontalAlignment;
            style.verticalAlignment = source.verticalAlignment;
            style.justifyContent(source.justifyContent);
            style.left(source.left);
            style.top(source.top);
            style.right(source.right);
            style.bottom(source.bottom);
        });
    }

    public LayoutStyle applyLegacyConstraints(LayoutConstraints constraints) {
        LayoutConstraints source = constraints == null ? LayoutConstraints.DEFAULT : constraints;
        return update(style -> {
            style.width(fromLegacyPreferred(source.preferredWidth()));
            style.height(fromLegacyPreferred(source.preferredHeight()));
            style.minWidth(SizeValue.px(source.minWidth()));
            style.minHeight(SizeValue.px(source.minHeight()));
            style.maxWidth(fromLegacyMaximum(source.maxWidth()));
            style.maxHeight(fromLegacyMaximum(source.maxHeight()));
            style.margin(source.margin());
            style.flexGrow(source.grow());
            style.flexShrink(source.grow() > 0.0f ? 1.0f : 0.0f);
            style.horizontalAlignment = source.horizontalAlignment();
            style.verticalAlignment = source.verticalAlignment();
            style.alignSelf = commonAlignment(source.horizontalAlignment(), source.verticalAlignment());
        });
    }

    public LayoutConstraints toLegacyConstraints(LayoutConstraints fallback) {
        LayoutConstraints source = fallback == null ? LayoutConstraints.DEFAULT : fallback;
        float preferredWidth = legacyPreferred(width, source.preferredWidth());
        float preferredHeight = legacyPreferred(height, source.preferredHeight());
        float resolvedMinWidth = legacyMinimum(minWidth, source.minWidth());
        float resolvedMinHeight = legacyMinimum(minHeight, source.minHeight());
        float resolvedMaxWidth = legacyMaximum(maxWidth, source.maxWidth());
        float resolvedMaxHeight = legacyMaximum(maxHeight, source.maxHeight());
        Alignment horizontal = horizontalAlignment;
        Alignment vertical = verticalAlignment;
        return new LayoutConstraints(
                preferredWidth, preferredHeight,
                resolvedMinWidth, resolvedMinHeight,
                resolvedMaxWidth, resolvedMaxHeight,
                margin,
                horizontal, vertical,
                flexGrow);
    }

    public PositionType position() {
        return position;
    }

    public LayoutStyle position(PositionType position) {
        PositionType normalized = position == null ? PositionType.RELATIVE : position;
        if (this.position == normalized) return this;
        this.position = normalized;
        changed();
        return this;
    }

    public SizeValue width() {
        return width;
    }

    public LayoutStyle width(SizeValue width) {
        return setSize(this.width, width, value -> this.width = value);
    }

    public LayoutStyle width(float pixels) {
        return width(fromLayoutFloat(pixels));
    }

    public LayoutStyle widthPercent(float percent) {
        return width(SizeValue.percent(percent));
    }

    public LayoutStyle size(SizeValue width, SizeValue height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    public LayoutStyle size(float width, float height) {
        return update(style -> {
            style.width(width);
            style.height(height);
        });
    }

    public LayoutStyle sizePercent(float widthPercent, float heightPercent) {
        return size(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    public SizeValue height() {
        return height;
    }

    public LayoutStyle height(SizeValue height) {
        return setSize(this.height, height, value -> this.height = value);
    }

    public LayoutStyle height(float pixels) {
        return height(fromLayoutFloat(pixels));
    }

    public LayoutStyle heightPercent(float percent) {
        return height(SizeValue.percent(percent));
    }

    public SizeValue minWidth() {
        return minWidth;
    }

    public LayoutStyle minWidth(SizeValue minWidth) {
        return setSize(this.minWidth, minWidth, value -> this.minWidth = value);
    }

    public LayoutStyle minWidth(float pixels) {
        return minWidth(SizeValue.px(pixels));
    }

    public LayoutStyle minWidthPercent(float percent) {
        return minWidth(SizeValue.percent(percent));
    }

    public SizeValue minHeight() {
        return minHeight;
    }

    public LayoutStyle minHeight(SizeValue minHeight) {
        return setSize(this.minHeight, minHeight, value -> this.minHeight = value);
    }

    public LayoutStyle minHeight(float pixels) {
        return minHeight(SizeValue.px(pixels));
    }

    public LayoutStyle minHeightPercent(float percent) {
        return minHeight(SizeValue.percent(percent));
    }

    public LayoutStyle minSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.minWidth(width);
            style.minHeight(height);
        });
    }

    public LayoutStyle minSize(float width, float height) {
        return minSize(SizeValue.px(width), SizeValue.px(height));
    }

    public SizeValue maxWidth() {
        return maxWidth;
    }

    public LayoutStyle maxWidth(SizeValue maxWidth) {
        return setSize(this.maxWidth, maxWidth, value -> this.maxWidth = value);
    }

    public LayoutStyle maxWidth(float pixels) {
        return maxWidth(fromMaximumLayoutFloat(pixels));
    }

    public LayoutStyle maxWidthPercent(float percent) {
        return maxWidth(SizeValue.percent(percent));
    }

    public SizeValue maxHeight() {
        return maxHeight;
    }

    public LayoutStyle maxHeight(SizeValue maxHeight) {
        return setSize(this.maxHeight, maxHeight, value -> this.maxHeight = value);
    }

    public LayoutStyle maxHeight(float pixels) {
        return maxHeight(fromMaximumLayoutFloat(pixels));
    }

    public LayoutStyle maxHeightPercent(float percent) {
        return maxHeight(SizeValue.percent(percent));
    }

    public LayoutStyle maxSize(SizeValue width, SizeValue height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    public LayoutStyle maxSize(float width, float height) {
        return update(style -> {
            style.maxWidth(width);
            style.maxHeight(height);
        });
    }

    public LayoutStyle maxSizePercent(float widthPercent, float heightPercent) {
        return maxSize(SizeValue.percent(widthPercent), SizeValue.percent(heightPercent));
    }

    public EdgeInsets margin() {
        return margin;
    }

    public LayoutStyle margin(EdgeInsets margin) {
        EdgeInsets normalized = margin == null ? EdgeInsets.ZERO : margin;
        if (this.margin.equals(normalized)) return this;
        this.margin = normalized;
        changed();
        return this;
    }

    public LayoutStyle margin(float all) {
        return margin(EdgeInsets.all(all));
    }

    public LayoutStyle margin(float horizontal, float vertical) {
        return margin(EdgeInsets.symmetric(horizontal, vertical));
    }

    public LayoutStyle margin(float left, float top, float right, float bottom) {
        return margin(new EdgeInsets(left, top, right, bottom));
    }

    public EdgeInsets padding() {
        return padding;
    }

    public LayoutStyle padding(EdgeInsets padding) {
        EdgeInsets normalized = padding == null ? EdgeInsets.ZERO : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        changed();
        return this;
    }

    public LayoutStyle padding(float all) {
        return padding(EdgeInsets.all(all));
    }

    public LayoutStyle padding(float horizontal, float vertical) {
        return padding(EdgeInsets.symmetric(horizontal, vertical));
    }

    public LayoutStyle padding(float left, float top, float right, float bottom) {
        return padding(new EdgeInsets(left, top, right, bottom));
    }

    public Overflow overflowX() {
        return overflowX;
    }

    public LayoutStyle overflowX(Overflow overflowX) {
        Overflow normalized = overflowX == null ? Overflow.VISIBLE : overflowX;
        if (this.overflowX == normalized) return this;
        this.overflowX = normalized;
        changed();
        return this;
    }

    public Overflow overflowY() {
        return overflowY;
    }

    public LayoutStyle overflowY(Overflow overflowY) {
        Overflow normalized = overflowY == null ? Overflow.VISIBLE : overflowY;
        if (this.overflowY == normalized) return this;
        this.overflowY = normalized;
        changed();
        return this;
    }

    public LayoutStyle overflow(Overflow overflow) {
        return update(style -> {
            style.overflowX(overflow);
            style.overflowY(overflow);
        });
    }

    public FlexDirection flexDirection() {
        return flexDirection;
    }

    public LayoutStyle flexDirection(FlexDirection flexDirection) {
        FlexDirection normalized = flexDirection == null ? FlexDirection.COLUMN : flexDirection;
        if (this.flexDirection == normalized) return this;
        this.flexDirection = normalized;
        changed();
        return this;
    }

    public FlexWrap flexWrap() {
        return flexWrap;
    }

    public LayoutStyle flexWrap(FlexWrap flexWrap) {
        FlexWrap normalized = flexWrap == null ? FlexWrap.NOWRAP : flexWrap;
        if (this.flexWrap == normalized) return this;
        this.flexWrap = normalized;
        changed();
        return this;
    }

    public float rowGap() {
        return rowGap;
    }

    public LayoutStyle rowGap(float rowGap) {
        float normalized = sanitize(rowGap);
        if (this.rowGap == normalized) return this;
        this.rowGap = normalized;
        changed();
        return this;
    }

    public float columnGap() {
        return columnGap;
    }

    public LayoutStyle columnGap(float columnGap) {
        float normalized = sanitize(columnGap);
        if (this.columnGap == normalized) return this;
        this.columnGap = normalized;
        changed();
        return this;
    }

    public LayoutStyle gap(float gap) {
        return update(style -> {
            style.rowGap(gap);
            style.columnGap(gap);
        });
    }

    public float flexGrow() {
        return flexGrow;
    }

    public LayoutStyle flexGrow(float flexGrow) {
        float normalized = sanitize(flexGrow);
        if (this.flexGrow == normalized) return this;
        this.flexGrow = normalized;
        changed();
        return this;
    }

    public float flexShrink() {
        return flexShrink;
    }

    public LayoutStyle flexShrink(float flexShrink) {
        float normalized = sanitize(flexShrink);
        if (this.flexShrink == normalized) return this;
        this.flexShrink = normalized;
        changed();
        return this;
    }

    public SizeValue flexBasis() {
        return flexBasis;
    }

    public LayoutStyle flexBasis(SizeValue flexBasis) {
        return setSize(this.flexBasis, flexBasis, value -> this.flexBasis = value);
    }

    public LayoutStyle flexBasis(float pixels) {
        return flexBasis(fromLayoutFloat(pixels));
    }

    public LayoutStyle flex(float grow, float shrink, SizeValue basis) {
        return update(style -> {
            style.flexGrow(grow);
            style.flexShrink(shrink);
            style.flexBasis(basis);
        });
    }

    public LayoutStyle flex(float grow, float shrink, float basisPixels) {
        return flex(grow, shrink, SizeValue.px(basisPixels));
    }

    public Align alignItems() {
        return alignItems;
    }

    public LayoutStyle alignItems(Align alignItems) {
        Align normalized = alignItems == null ? Align.STRETCH : alignItems;
        if (this.alignItems == normalized) return this;
        this.alignItems = normalized;
        changed();
        return this;
    }

    public Align alignSelf() {
        return alignSelf;
    }

    public LayoutStyle alignSelf(Align alignSelf) {
        Align normalized = alignSelf == null ? Align.AUTO : alignSelf;
        Alignment nextHorizontal;
        Alignment nextVertical;
        if (normalized == Align.AUTO) {
            nextHorizontal = Alignment.STRETCH;
            nextVertical = Alignment.STRETCH;
        } else {
            nextHorizontal = toLegacyAlignment(normalized);
            nextVertical = nextHorizontal;
        }
        if (this.alignSelf == normalized
                && horizontalAlignment == nextHorizontal
                && verticalAlignment == nextVertical) return this;
        this.alignSelf = normalized;
        horizontalAlignment = nextHorizontal;
        verticalAlignment = nextVertical;
        changed();
        return this;
    }

    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    public LayoutStyle align(Alignment horizontal, Alignment vertical) {
        Alignment normalizedHorizontal = horizontal == null ? Alignment.STRETCH : horizontal;
        Alignment normalizedVertical = vertical == null ? Alignment.STRETCH : vertical;
        if (horizontalAlignment == normalizedHorizontal && verticalAlignment == normalizedVertical) return this;
        horizontalAlignment = normalizedHorizontal;
        verticalAlignment = normalizedVertical;
        alignSelf = commonAlignment(normalizedHorizontal, normalizedVertical);
        changed();
        return this;
    }

    public Justify justifyContent() {
        return justifyContent;
    }

    public LayoutStyle justifyContent(Justify justifyContent) {
        Justify normalized = justifyContent == null ? Justify.START : justifyContent;
        if (this.justifyContent == normalized) return this;
        this.justifyContent = normalized;
        changed();
        return this;
    }

    public SizeValue left() {
        return left;
    }

    public LayoutStyle left(SizeValue left) {
        return setSize(this.left, left, value -> this.left = value);
    }

    public LayoutStyle left(float pixels) {
        return left(SizeValue.px(pixels));
    }

    public SizeValue top() {
        return top;
    }

    public LayoutStyle top(SizeValue top) {
        return setSize(this.top, top, value -> this.top = value);
    }

    public LayoutStyle top(float pixels) {
        return top(SizeValue.px(pixels));
    }

    public SizeValue right() {
        return right;
    }

    public LayoutStyle right(SizeValue right) {
        return setSize(this.right, right, value -> this.right = value);
    }

    public LayoutStyle right(float pixels) {
        return right(SizeValue.px(pixels));
    }

    public SizeValue bottom() {
        return bottom;
    }

    public LayoutStyle bottom(SizeValue bottom) {
        return setSize(this.bottom, bottom, value -> this.bottom = value);
    }

    public LayoutStyle bottom(float pixels) {
        return bottom(SizeValue.px(pixels));
    }

    public LayoutStyle inset(SizeValue left, SizeValue top, SizeValue right, SizeValue bottom) {
        return update(style -> {
            style.left(left);
            style.top(top);
            style.right(right);
            style.bottom(bottom);
        });
    }

    public LayoutStyle inset(float all) {
        return inset(all, all, all, all);
    }

    public LayoutStyle inset(float horizontal, float vertical) {
        return inset(horizontal, vertical, horizontal, vertical);
    }

    public LayoutStyle inset(float left, float top, float right, float bottom) {
        return inset(
                SizeValue.px(left), SizeValue.px(top),
                SizeValue.px(right), SizeValue.px(bottom));
    }

    private LayoutStyle setSize(SizeValue current, SizeValue next, Consumer<SizeValue> setter) {
        SizeValue normalized = next == null ? SizeValue.auto() : next;
        if (Objects.equals(current, normalized)) return this;
        setter.accept(normalized);
        changed();
        return this;
    }

    private void changed() {
        if (updateDepth > 0) {
            pendingChange = true;
            return;
        }
        notifyChanged();
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private static SizeValue fromLegacyPreferred(float value) {
        return LayoutConstraints.isAuto(value) ? SizeValue.auto() : SizeValue.px(value);
    }

    private static SizeValue fromLegacyMaximum(float value) {
        return Float.isFinite(value) ? SizeValue.px(value) : SizeValue.auto();
    }

    private static SizeValue fromLayoutFloat(float value) {
        return LayoutConstraints.isAuto(value) ? SizeValue.auto() : SizeValue.px(value);
    }

    private static SizeValue fromMaximumLayoutFloat(float value) {
        return Float.isFinite(value) ? SizeValue.px(value) : SizeValue.auto();
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

    private static Align commonAlignment(Alignment horizontal, Alignment vertical) {
        if (horizontal != vertical) return Align.AUTO;
        return switch (horizontal == null ? Alignment.STRETCH : horizontal) {
            case START -> Align.START;
            case CENTER -> Align.CENTER;
            case END -> Align.END;
            case STRETCH -> Align.AUTO;
        };
    }

    private static Alignment toLegacyAlignment(Align align) {
        return switch (align == null ? Align.AUTO : align) {
            case START -> Alignment.START;
            case CENTER -> Alignment.CENTER;
            case END -> Alignment.END;
            case AUTO, STRETCH -> Alignment.STRETCH;
        };
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
