package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutResult;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Backend-neutral resolver for Layout V3 overlay portals.
 *
 * <p>The resolver intentionally does not mutate widgets. It converts a list of
 * anchored overlay requests into synthetic portal layout results with explicit
 * draw and hit-test ordering. Runtime integration can then apply those results
 * to Popup, ComboBox, DropDownBox, Tooltip and similar floating widgets.</p>
 */
public final class OverlayLayoutResolver {
    public static final int DEFAULT_DRAW_ORDER_BASE = 10_000;
    public static final int DEFAULT_HIT_TEST_PRIORITY_BASE = 10_000;
    public static final ClippingPolicy DEFAULT_CLIPPING_POLICY = ClippingPolicy.CLIP_TO_ROOT;

    private final ClippingPolicy defaultClippingPolicy;

    public OverlayLayoutResolver() {
        this(DEFAULT_CLIPPING_POLICY);
    }

    public OverlayLayoutResolver(ClippingPolicy defaultClippingPolicy) {
        this.defaultClippingPolicy = defaultClippingPolicy == null
                ? DEFAULT_CLIPPING_POLICY
                : defaultClippingPolicy;
    }

    public ClippingPolicy defaultClippingPolicy() {
        return defaultClippingPolicy;
    }

    public Host resolveHost(List<Host> hostChain) {
        return resolveHost(hostChain, null);
    }

    public Host resolveHost(List<Host> hostChain, Host fallback) {
        Host resolved = null;
        if (hostChain != null) {
            for (Host host : hostChain) {
                if (host != null && host.acceptsPortals()) {
                    resolved = host;
                }
            }
        }
        return resolved == null ? fallback : resolved;
    }

    public List<ResolvedOverlay> resolve(Host host, List<Request> requests) {
        if (host == null || requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<ResolvedOverlay> resolved = new ArrayList<>(requests.size());
        int portalIndex = 0;
        for (Request request : requests) {
            if (request == null) {
                continue;
            }
            resolved.add(resolve(host, request, portalIndex));
            portalIndex++;
        }
        return Collections.unmodifiableList(resolved);
    }

    public ResolvedOverlay resolve(Host host, Request request, int portalIndex) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(request, "request");
        int normalizedIndex = Math.max(0, portalIndex);
        ClippingPolicy policy = request.clippingPolicy() == null
                ? defaultClippingPolicy
                : request.clippingPolicy();
        MutableRect bounds = switch (policy) {
            case CLIP_TO_ROOT, ALLOW_OUTSIDE_PARENT -> AbsoluteLayoutEngine.placeBelow(
                    host.bounds(),
                    request.anchorBoundsInRoot(),
                    request.desiredWidth(),
                    request.desiredHeight(),
                    request.offsetX(),
                    request.offsetY(),
                    request.flipHorizontal(),
                    request.flipVertical());
            case ALLOW_OUTSIDE_SCREEN -> placeBelowUnconstrained(host.bounds(), request);
        };
        LayoutNodeId portalId = portalNodeId(host.id(), request.overlayId());
        LayoutResult result = new LayoutResult(
                portalId,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                bounds.width(),
                bounds.height());
        return new ResolvedOverlay(
                portalId,
                request.overlayId(),
                request.anchorId(),
                result,
                policy,
                host.drawOrderBase() + request.drawOrderOffset() + normalizedIndex,
                host.hitTestPriorityBase() + request.hitTestPriorityOffset() + normalizedIndex);
    }

    public static LayoutNodeId portalNodeId(LayoutNodeId hostId, LayoutNodeId overlayId) {
        Objects.requireNonNull(hostId, "hostId");
        Objects.requireNonNull(overlayId, "overlayId");
        return LayoutNodeId.of(hostId.value() + "/portal/" + overlayId.value());
    }

    public static MutableRect translateToRoot(RectView bounds, float rootOffsetX, float rootOffsetY) {
        RectView safe = copy(bounds);
        return new MutableRect(
                safe.x() + sanitizePosition(rootOffsetX),
                safe.y() + sanitizePosition(rootOffsetY),
                safe.width(),
                safe.height());
    }

    private static MutableRect placeBelowUnconstrained(RectView hostBounds, Request request) {
        RectView host = copy(hostBounds);
        RectView anchor = request.anchorBoundsInRoot();
        float width = sanitizeSize(request.desiredWidth());
        float height = sanitizeSize(request.desiredHeight());
        float x = anchor.x() + request.offsetX();
        float y = anchor.y() + anchor.height() + request.offsetY();

        if (request.flipHorizontal() && x + width > host.x() + host.width()) {
            float flippedX = anchor.x() + anchor.width() - width - request.offsetX();
            float rightSpace = host.x() + host.width() - x;
            float leftSpace = anchor.x() + anchor.width() - request.offsetX() - host.x();
            if (flippedX >= host.x() || leftSpace > rightSpace) {
                x = flippedX;
            }
        }
        if (request.flipVertical() && y + height > host.y() + host.height()) {
            float flippedY = anchor.y() - height - request.offsetY();
            float belowSpace = host.y() + host.height() - y;
            float aboveSpace = anchor.y() - request.offsetY() - host.y();
            if (flippedY >= host.y() || aboveSpace > belowSpace) {
                y = flippedY;
            }
        }
        return new MutableRect(x, y, width, height);
    }

    private static MutableRect copy(RectView bounds) {
        if (bounds == null) {
            return new MutableRect();
        }
        return new MutableRect(
                sanitizePosition(bounds.x()),
                sanitizePosition(bounds.y()),
                sanitizeSize(bounds.width()),
                sanitizeSize(bounds.height()));
    }

    private static float sanitizePosition(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    public enum ClippingPolicy {
        /** Overlay is constrained to the root overlay host bounds. */
        CLIP_TO_ROOT,
        /** Overlay ignores intermediate parent clipping but is still constrained to the root host. */
        ALLOW_OUTSIDE_PARENT,
        /** Overlay may overflow root/screen bounds; no final clamp is applied. */
        ALLOW_OUTSIDE_SCREEN
    }

    public record Host(
            LayoutNodeId id,
            RectView bounds,
            boolean acceptsPortals,
            int drawOrderBase,
            int hitTestPriorityBase) {
        public Host(LayoutNodeId id, RectView bounds) {
            this(id, bounds, true, DEFAULT_DRAW_ORDER_BASE, DEFAULT_HIT_TEST_PRIORITY_BASE);
        }

        public Host {
            id = Objects.requireNonNull(id, "id");
            bounds = copy(bounds);
        }
    }

    public record Request(
            LayoutNodeId overlayId,
            LayoutNodeId anchorId,
            RectView anchorBoundsInRoot,
            float desiredWidth,
            float desiredHeight,
            float offsetX,
            float offsetY,
            boolean flipHorizontal,
            boolean flipVertical,
            ClippingPolicy clippingPolicy,
            int drawOrderOffset,
            int hitTestPriorityOffset) {
        public Request {
            overlayId = Objects.requireNonNull(overlayId, "overlayId");
            anchorId = Objects.requireNonNull(anchorId, "anchorId");
            anchorBoundsInRoot = copy(anchorBoundsInRoot);
            desiredWidth = sanitizeSize(desiredWidth);
            desiredHeight = sanitizeSize(desiredHeight);
            offsetX = sanitizePosition(offsetX);
            offsetY = sanitizePosition(offsetY);
        }

        public static Request below(LayoutNodeId overlayId,
                                    LayoutNodeId anchorId,
                                    RectView anchorBoundsInRoot,
                                    float desiredWidth,
                                    float desiredHeight) {
            return new Request(
                    overlayId,
                    anchorId,
                    anchorBoundsInRoot,
                    desiredWidth,
                    desiredHeight,
                    0.0f,
                    0.0f,
                    true,
                    true,
                    null,
                    0,
                    0);
        }

        public Request offset(float x, float y) {
            return new Request(
                    overlayId,
                    anchorId,
                    anchorBoundsInRoot,
                    desiredWidth,
                    desiredHeight,
                    x,
                    y,
                    flipHorizontal,
                    flipVertical,
                    clippingPolicy,
                    drawOrderOffset,
                    hitTestPriorityOffset);
        }

        public Request clippingPolicy(ClippingPolicy policy) {
            return new Request(
                    overlayId,
                    anchorId,
                    anchorBoundsInRoot,
                    desiredWidth,
                    desiredHeight,
                    offsetX,
                    offsetY,
                    flipHorizontal,
                    flipVertical,
                    policy,
                    drawOrderOffset,
                    hitTestPriorityOffset);
        }

        public Request flip(boolean horizontal, boolean vertical) {
            return new Request(
                    overlayId,
                    anchorId,
                    anchorBoundsInRoot,
                    desiredWidth,
                    desiredHeight,
                    offsetX,
                    offsetY,
                    horizontal,
                    vertical,
                    clippingPolicy,
                    drawOrderOffset,
                    hitTestPriorityOffset);
        }

        public Request orderOffset(int drawOrderOffset, int hitTestPriorityOffset) {
            return new Request(
                    overlayId,
                    anchorId,
                    anchorBoundsInRoot,
                    desiredWidth,
                    desiredHeight,
                    offsetX,
                    offsetY,
                    flipHorizontal,
                    flipVertical,
                    clippingPolicy,
                    drawOrderOffset,
                    hitTestPriorityOffset);
        }
    }

    public record ResolvedOverlay(
            LayoutNodeId portalId,
            LayoutNodeId overlayId,
            LayoutNodeId anchorId,
            LayoutResult result,
            ClippingPolicy clippingPolicy,
            int drawOrder,
            int hitTestPriority) implements RectView {
        public ResolvedOverlay {
            portalId = Objects.requireNonNull(portalId, "portalId");
            overlayId = Objects.requireNonNull(overlayId, "overlayId");
            anchorId = Objects.requireNonNull(anchorId, "anchorId");
            result = Objects.requireNonNull(result, "result");
            clippingPolicy = clippingPolicy == null ? DEFAULT_CLIPPING_POLICY : clippingPolicy;
        }

        @Override
        public float x() {
            return result.x();
        }

        @Override
        public float y() {
            return result.y();
        }

        @Override
        public float width() {
            return result.width();
        }

        @Override
        public float height() {
            return result.height();
        }
    }
}
