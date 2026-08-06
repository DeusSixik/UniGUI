package dev.sixik.unigui.impl.layout;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.List;

/**
 * Shared resolver for out-of-flow children and host-constrained overlay placement.
 */
public final class AbsoluteLayoutEngine {
    private AbsoluteLayoutEngine() {
    }

    public static boolean isAbsolute(Widget widget) {
        return widget instanceof WidgetBase base
                && base.layoutStyle().position() == PositionType.ABSOLUTE;
    }

    public static void measureChildren(List<Widget> children, LayoutContext context) {
        if (children == null) return;
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && isAbsolute(child)) {
                child.measure(context);
            }
        }
    }

    public static void arrangeChildren(List<Widget> children, RectView hostBounds) {
        if (children == null || hostBounds == null) return;
        for (Widget child : children) {
            if (child != null
                    && child.visibility() != Visibility.COLLAPSED
                    && isAbsolute(child)) {
                arrange(child, hostBounds);
            }
        }
    }

    public static void arrange(Widget child, RectView hostBounds) {
        if (child == null || hostBounds == null) return;
        child.arrange(resolveRect(child, hostBounds));
    }

    public static MutableRect contentBounds(Widget host, RectView bounds) {
        if (bounds == null) return new MutableRect();
        LayoutStyle style = host instanceof WidgetBase base
                ? base.layoutStyle()
                : new LayoutStyle().applyLegacyConstraints(host == null ? null : host.layoutConstraints());
        EdgeInsets padding = style.padding();
        return new MutableRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                Math.max(0.0f, bounds.width() - padding.horizontal()),
                Math.max(0.0f, bounds.height() - padding.vertical()));
    }

    public static LayoutContext contentContext(Widget host, LayoutContext context) {
        if (context == null) return null;
        LayoutStyle style = host instanceof WidgetBase base
                ? base.layoutStyle()
                : new LayoutStyle().applyLegacyConstraints(host == null ? null : host.layoutConstraints());
        EdgeInsets padding = style.padding();
        return new LayoutContext(
                subtractAvailable(context.availableWidth(), padding.horizontal()),
                subtractAvailable(context.availableHeight(), padding.vertical()));
    }

    public static MutableRect resolveRect(Widget child, RectView hostBounds) {
        return resolveRect(child, hostBounds, child.desiredSize().width(), child.desiredSize().height());
    }

    public static MutableRect resolveRect(Widget child,
                                          RectView hostBounds,
                                          float fallbackWidth,
                                          float fallbackHeight) {
        LayoutStyle style = child instanceof WidgetBase base
                ? base.layoutStyle()
                : new LayoutStyle().applyLegacyConstraints(child.layoutConstraints());
        EdgeInsets margin = style.margin();
        float hostWidth = Math.max(0.0f, hostBounds.width());
        float hostHeight = Math.max(0.0f, hostBounds.height());
        float left = resolveInset(style.left(), hostWidth);
        float top = resolveInset(style.top(), hostHeight);
        float right = resolveInset(style.right(), hostWidth);
        float bottom = resolveInset(style.bottom(), hostHeight);

        float width = resolveAxisSize(
                style.width(), style.minWidth(), style.maxWidth(),
                hostWidth, fallbackWidth,
                left, right, margin.horizontal());
        float height = resolveAxisSize(
                style.height(), style.minHeight(), style.maxHeight(),
                hostHeight, fallbackHeight,
                top, bottom, margin.vertical());

        float x = resolveAxisPosition(
                hostBounds.x(), hostWidth, width,
                left, right, margin.left(), margin.right(),
                child.layoutConstraints().horizontalAlignment());
        float y = resolveAxisPosition(
                hostBounds.y(), hostHeight, height,
                top, bottom, margin.top(), margin.bottom(),
                child.layoutConstraints().verticalAlignment());
        return new MutableRect(x, y, width, height);
    }

    public static MutableRect placeBelow(RectView hostBounds,
                                         RectView anchorBounds,
                                         float width,
                                         float height,
                                         float offsetX,
                                         float offsetY,
                                         boolean flipHorizontal,
                                         boolean flipVertical) {
        float normalizedWidth = Math.min(Math.max(0.0f, width), Math.max(0.0f, hostBounds.width()));
        float normalizedHeight = Math.min(Math.max(0.0f, height), Math.max(0.0f, hostBounds.height()));
        float x = anchorBounds.x() + offsetX;
        float y = anchorBounds.y() + anchorBounds.height() + offsetY;

        if (flipHorizontal && x + normalizedWidth > hostBounds.x() + hostBounds.width()) {
            float flippedX = anchorBounds.x() + anchorBounds.width() - normalizedWidth - offsetX;
            float rightSpace = hostBounds.x() + hostBounds.width() - x;
            float leftSpace = anchorBounds.x() + anchorBounds.width() - offsetX - hostBounds.x();
            if (flippedX >= hostBounds.x() || leftSpace > rightSpace) {
                x = flippedX;
            }
        }
        if (flipVertical && y + normalizedHeight > hostBounds.y() + hostBounds.height()) {
            float flippedY = anchorBounds.y() - normalizedHeight - offsetY;
            float belowSpace = hostBounds.y() + hostBounds.height() - y;
            float aboveSpace = anchorBounds.y() - offsetY - hostBounds.y();
            if (flippedY >= hostBounds.y() || aboveSpace > belowSpace) {
                y = flippedY;
            }
        }
        return constrainToHost(hostBounds, x, y, normalizedWidth, normalizedHeight);
    }

    public static MutableRect constrainToHost(RectView hostBounds,
                                               float x,
                                               float y,
                                               float width,
                                               float height) {
        float constrainedWidth = Math.min(Math.max(0.0f, width), Math.max(0.0f, hostBounds.width()));
        float constrainedHeight = Math.min(Math.max(0.0f, height), Math.max(0.0f, hostBounds.height()));
        float maxX = Math.max(hostBounds.x(), hostBounds.x() + hostBounds.width() - constrainedWidth);
        float maxY = Math.max(hostBounds.y(), hostBounds.y() + hostBounds.height() - constrainedHeight);
        return new MutableRect(
                clamp(x, hostBounds.x(), maxX),
                clamp(y, hostBounds.y(), maxY),
                constrainedWidth,
                constrainedHeight);
    }

    public static float resolveInset(SizeValue value, float available) {
        if (value == null || value.isAuto()) return Float.NaN;
        if (value.isPercent()) {
            return Float.isFinite(available) ? Math.max(0.0f, available * value.value() / 100.0f) : 0.0f;
        }
        return value.value();
    }

    private static float resolveAxisSize(SizeValue size,
                                         SizeValue min,
                                         SizeValue max,
                                         float available,
                                         float desired,
                                         float leading,
                                         float trailing,
                                         float totalMargin) {
        float resolved;
        if ((size == null || size.isAuto()) && Float.isFinite(leading) && Float.isFinite(trailing)) {
            resolved = Math.max(0.0f, available - leading - trailing - totalMargin);
        } else {
            resolved = resolveSize(size, available, desired);
        }
        float minimum = resolveSize(min, available, 0.0f);
        float maximum = max == null || max.isAuto()
                ? Float.POSITIVE_INFINITY
                : resolveSize(max, available, Float.POSITIVE_INFINITY);
        return clamp(resolved, minimum, maximum);
    }

    private static float resolveAxisPosition(float hostStart,
                                             float available,
                                             float size,
                                             float leading,
                                             float trailing,
                                             float leadingMargin,
                                             float trailingMargin,
                                             Alignment alignment) {
        if (Float.isFinite(leading)) {
            return hostStart + leading + leadingMargin;
        }
        if (Float.isFinite(trailing)) {
            return hostStart + available - trailing - trailingMargin - size;
        }
        float innerStart = hostStart + leadingMargin;
        float innerAvailable = Math.max(0.0f, available - leadingMargin - trailingMargin);
        return switch (alignment == null ? Alignment.START : alignment) {
            case CENTER -> innerStart + (innerAvailable - size) * 0.5f;
            case END -> innerStart + innerAvailable - size;
            case START, STRETCH -> innerStart;
        };
    }

    private static float resolveSize(SizeValue value, float available, float fallback) {
        if (value == null || value.isAuto()) return Math.max(0.0f, fallback);
        if (value.isPercent()) {
            return Float.isFinite(available) ? Math.max(0.0f, available * value.value() / 100.0f) : Math.max(0.0f, fallback);
        }
        return value.value();
    }

    private static float subtractAvailable(float available, float consumed) {
        return Float.isFinite(available)
                ? Math.max(0.0f, available - Math.max(0.0f, consumed))
                : Float.POSITIVE_INFINITY;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
