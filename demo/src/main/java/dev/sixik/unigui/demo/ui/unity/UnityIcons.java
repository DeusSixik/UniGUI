package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.widgets.display.CanvasWidget;

/**
 * Procedural vector icons in the Unity Editor style.
 */
public final class UnityIcons {
    private UnityIcons() {
    }

    /**
     * Blue isometric GameObject cube (3D).
     */
    public static CanvasWidget gameObjectCube(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.44f;

            // Top face (light blue)
            context.pathClear();
            context.pathLineTo(cx, cy - r);
            context.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);

            // Left face (medium blue)
            context.pathClear();
            context.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx, cy + r);
            context.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);

            // Right face (dark blue)
            context.pathClear();
            context.pathLineTo(cx, cy);
            context.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
            context.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
            context.pathLineTo(cx, cy + r);
            context.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
        });
        return canvas;
    }

    /**
     * Transform icon (3 coordinate axes: red X, green Y, blue Z).
     */
    public static CanvasWidget transformAxes(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.55f;
            float len = size * 0.38f;

            // Y axis (up, green)
            context.line(cx, cy, cx, cy - len, Paint.fill(UnityStyles.AXIS_Y));
            context.circleFilled(cx - 1.0f, cy - len - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_Y);

            // X axis (bottom-right, red)
            context.line(cx, cy, cx + len * 0.866f, cy + len * 0.5f, Paint.fill(UnityStyles.AXIS_X));
            context.circleFilled(cx + len * 0.866f - 1.0f, cy + len * 0.5f - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_X);

            // Z axis (bottom-left, blue)
            context.line(cx, cy, cx - len * 0.866f, cy + len * 0.5f, Paint.fill(UnityStyles.AXIS_Z));
            context.circleFilled(cx - len * 0.866f - 1.0f, cy + len * 0.5f - 1.0f, 2.0f, 2.0f, UnityStyles.AXIS_Z);
        });
        return canvas;
    }

    /**
     * Camera icon (blue video camera silhouette).
     */
    public static CanvasWidget cameraIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float bw = size * 0.52f;
            float bh = size * 0.44f;
            float bx = x + size * 0.12f;
            float by = y + (size - bh) * 0.5f;

            // Camera body
            context.rectFilled(bx, by, bw, bh, UnityStyles.AXIS_Z);

            // Lens (trapezoid on the right)
            float lx = bx + bw + 1.0f;
            float ly = by + bh * 0.5f;
            context.pathClear();
            context.pathLineTo(lx, ly - bh * 0.2f);
            context.pathLineTo(lx + size * 0.22f, ly - bh * 0.46f);
            context.pathLineTo(lx + size * 0.22f, ly + bh * 0.46f);
            context.pathLineTo(lx, ly + bh * 0.2f);
            context.pathFillConvex(UnityStyles.AXIS_Z);
        });
        return canvas;
    }

    /**
     * Audio Listener icon (gold headphones).
     */
    public static CanvasWidget audioListenerIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.36f;

            // Headband (top arc)
            context.pathClear();
            int segs = 8;
            for (int i = 0; i <= segs; i++) {
                double angle = Math.PI + (Math.PI * i / segs);
                float px = (float) (cx + Math.cos(angle) * r);
                float py = (float) (cy + Math.sin(angle) * r);
                context.pathLineTo(px, py);
            }
            context.pathStroke(UnityStyles.AUDIO_GOLD, false, 1.6f);

            // Left and right ear cups
            float ew = size * 0.18f;
            float eh = size * 0.32f;
            context.rectFilled(cx - r - ew * 0.5f, cy - eh * 0.3f, ew, eh, UnityStyles.AUDIO_GOLD);
            context.rectFilled(cx + r - ew * 0.5f, cy - eh * 0.3f, ew, eh, UnityStyles.AUDIO_GOLD);
        });
        return canvas;
    }

    /**
     * Foldout arrow (expanded: down triangle, collapsed: right triangle):
     * Styled after Unity — vertically elongated and horizontally narrow when collapsed.
     */
    public static CanvasWidget foldoutArrow(float size, boolean expanded) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;

            context.pathClear();
            if (expanded) {
                // Down arrow
                context.pathLineTo(cx - 3.5f, cy - 2.0f);
                context.pathLineTo(cx + 3.5f, cy - 2.0f);
                context.pathLineTo(cx, cy + 2.5f);
            } else {
                // Right arrow (narrower horizontally, taller vertically)
                context.pathLineTo(cx - 1.5f, cy - 4.0f);
                context.pathLineTo(cx + 2.5f, cy);
                context.pathLineTo(cx - 1.5f, cy + 4.0f);
            }
            context.pathFillConvex(UnityStyles.TEXT_PRIMARY);
        });
        return canvas;
    }

    /**
     * Draws a '?' question mark inside a circle via DrawScope.
     */
    public static void drawHelpQuestionMark(DrawScope draw, float cx, float cy, float r, ColorView col) {
        draw.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, col, 1.0f);
        draw.addLine(cx - 1.6f, cy - 1.3f, cx - 0.8f, cy - 2.5f, col, 1.2f);
        draw.addLine(cx - 0.8f, cy - 2.5f, cx + 0.8f, cy - 2.5f, col, 1.2f);
        draw.addLine(cx + 0.8f, cy - 2.5f, cx + 1.6f, cy - 1.3f, col, 1.2f);
        draw.addLine(cx + 1.6f, cy - 1.3f, cx + 0.5f, cy - 0.1f, col, 1.2f);
        draw.addLine(cx + 0.5f, cy - 0.1f, cx, cy + 0.5f, col, 1.2f);
        draw.addLine(cx, cy + 0.5f, cx, cy + 1.1f, col, 1.2f);
        draw.circleFilled(cx - 0.7f, cy + 1.8f, 1.4f, 1.4f, col);
    }

    /**
     * Draws a '?' question mark inside a circle via RenderContext.
     */
    public static void drawHelpQuestionMark(RenderContext context, float cx, float cy, float r, ColorView col) {
        context.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, col, 1.0f);
        context.addLine(cx - 1.6f, cy - 1.3f, cx - 0.8f, cy - 2.5f, col, 1.2f);
        context.addLine(cx - 0.8f, cy - 2.5f, cx + 0.8f, cy - 2.5f, col, 1.2f);
        context.addLine(cx + 0.8f, cy - 2.5f, cx + 1.6f, cy - 1.3f, col, 1.2f);
        context.addLine(cx + 1.6f, cy - 1.3f, cx + 0.5f, cy - 0.1f, col, 1.2f);
        context.addLine(cx + 0.5f, cy - 0.1f, cx, cy + 0.5f, col, 1.2f);
        context.addLine(cx, cy + 0.5f, cx, cy + 1.1f, col, 1.2f);
        context.circleFilled(cx - 0.7f, cy + 1.8f, 1.4f, 1.4f, col);
    }

    /**
     * Help icon ('?' inside a circle).
     */
    public static CanvasWidget helpIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.42f;
            drawHelpQuestionMark(context, cx, cy, r, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Preset / settings icon (horizontal sliders).
     */
    public static CanvasWidget presetIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float w = size * 0.75f;

            // Top track and thumb
            context.line(cx - w * 0.5f, cy - 2.5f, cx + w * 0.5f, cy - 2.5f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.circleFilled(cx - 1.5f, cy - 4.0f, 3.0f, 3.0f, UnityStyles.TEXT_MUTED);

            // Bottom track and thumb
            context.line(cx - w * 0.5f, cy + 2.5f, cx + w * 0.5f, cy + 2.5f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.circleFilled(cx + 1.0f, cy + 1.0f, 3.0f, 3.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Context menu icon (three vertical dots).
     */
    public static CanvasWidget contextMenuIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = 1.2f;

            context.circleFilled(cx - r, cy - 4.0f - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
            context.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
            context.circleFilled(cx - r, cy + 4.0f - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Target picker icon (Render Texture / Object asset selector).
     */
    public static CanvasWidget targetPickerIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.38f;

            context.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_MUTED, 1.0f);
            context.circleFilled(cx - 1.2f, cy - 1.2f, 2.4f, 2.4f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Eyedropper icon for color picking.
     */
    public static CanvasWidget eyedropperIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;

            // Diagonal dropper
            context.line(cx + 3.0f, cy - 3.0f, cx - 1.0f, cy + 1.0f, Paint.fill(UnityStyles.TEXT_MUTED));
            context.line(cx - 1.0f, cy + 1.0f, cx - 3.5f, cy + 3.5f, Paint.fill(UnityStyles.TEXT_PRIMARY));
            context.circleFilled(cx + 2.5f, cy - 3.5f, 2.0f, 2.0f, UnityStyles.TEXT_MUTED);
        });
        return canvas;
    }

    /**
     * Warning / info icon (circle with exclamation mark '!').
     */
    public static CanvasWidget warningIcon(float size) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float r = size * 0.44f;

            // Light circle background
            context.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.TEXT_HELP_BOX);

            // Exclamation mark wedge: wider at top, narrower at bottom
            context.pathClear();
            context.pathLineTo(cx - 1.25f, cy - r * 0.52f);
            context.pathLineTo(cx + 1.25f, cy - r * 0.52f);
            context.pathLineTo(cx + 0.65f, cy + r * 0.16f);
            context.pathLineTo(cx - 0.65f, cy + r * 0.16f);
            context.pathFillConvex(UnityStyles.BG_WORKSPACE);

            // Core vertical stroke to guarantee crisp rendering across all backends
            context.addLine(cx, cy - r * 0.52f, cx, cy + r * 0.16f, UnityStyles.BG_WORKSPACE, 2.0f);

            // Exclamation dot
            context.circleFilled(cx - 1.15f, cy + r * 0.46f - 1.15f, 2.3f, 2.3f, UnityStyles.BG_WORKSPACE);
        });
        return canvas;
    }

    /**
     * Padlock icon (Lock Inspector).
     */
    public static CanvasWidget lockIcon(float size, boolean locked) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            float x = canvas.layoutBounds().x();
            float y = canvas.layoutBounds().y();
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            float bw = size * 0.5f;
            float bh = size * 0.38f;

            // Lock body
            context.rectFilled(cx - bw * 0.5f, cy - bh * 0.2f, bw, bh, UnityStyles.TEXT_MUTED);

            // Shackle
            float sr = size * 0.22f;
            float sy = cy - bh * 0.2f;
            context.circleStroke(cx - sr, sy - sr * 1.4f, sr * 2.0f, sr * 2.0f, UnityStyles.TEXT_MUTED, 1.2f);
            if (!locked) {
                // Open shackle cutout
                context.rectFilled(cx + sr * 0.4f, sy - sr * 0.8f, sr * 0.8f, sr * 0.8f, UnityStyles.BG_WINDOW);
            }
        });
        return canvas;
    }

    // =========================================================================
    // Vector Icons for Docking Tabs (UiIcon)
    // =========================================================================

    /**
     * Scene tab icon (isometric cube).
     */
    public static final UiIcon SCENE_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.44f;
        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);
        draw.pathClear();
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);
        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
    };

    /**
     * Game tab icon (Play triangle).
     */
    public static final UiIcon GAME_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.40f;
        draw.pathClear();
        draw.pathLineTo(cx - r * 0.7f, cy - r);
        draw.pathLineTo(cx + r * 0.9f, cy);
        draw.pathLineTo(cx - r * 0.7f, cy + r);
        draw.pathFillConvex(tint);
    };

    /**
     * Hierarchy tab icon (hierarchical bars).
     */
    public static final UiIcon HIERARCHY_TAB_ICON = (draw, x, y, size, tint) -> {
        float pad = size * 0.12f;
        float h = Math.max(1.5f, size * 0.16f);
        float w = size - pad * 2.0f;
        draw.rectFilled(x + pad, y + size * 0.18f, w, h, tint);
        draw.rectFilled(x + pad + size * 0.22f, y + size * 0.44f, w - size * 0.22f, h, tint);
        draw.rectFilled(x + pad + size * 0.22f, y + size * 0.70f, w - size * 0.22f, h, tint);
    };

    /**
     * Inspector tab icon (property sliders).
     */
    public static final UiIcon INSPECTOR_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float lw = size * 0.75f;
        draw.addLine(cx - lw * 0.5f, cy - 3.0f, cx + lw * 0.5f, cy - 3.0f, tint, 1.2f);
        draw.circleFilled(cx - 2.0f, cy - 4.5f, 3.0f, 3.0f, tint);
        draw.addLine(cx - lw * 0.5f, cy + 3.0f, cx + lw * 0.5f, cy + 3.0f, tint, 1.2f);
        draw.circleFilled(cx + 2.0f, cy + 1.5f, 3.0f, 3.0f, tint);
    };

    /**
     * Project tab icon (project folder).
     */
    public static final UiIcon PROJECT_TAB_ICON = (draw, x, y, size, tint) -> {
        float fx = x + size * 0.1f;
        float fy = y + size * 0.22f;
        float fw = size * 0.8f;
        float fh = size * 0.58f;
        draw.rectFilled(fx, fy, fw * 0.45f, fh * 0.35f, tint);
        draw.rectFilled(fx, fy + fh * 0.22f, fw, fh * 0.78f, tint);
    };

    /**
     * Console tab icon (log terminal).
     */
    public static final UiIcon CONSOLE_TAB_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.12f;
        float cy = y + size * 0.18f;
        float w = size * 0.76f;
        float h = size * 0.64f;
        draw.roundedRectStroke(cx, cy, w, h, 2.0f, tint, 1.2f);
        draw.addLine(cx + 2.5f, cy + 3.0f, cx + 5.5f, cy + h * 0.5f, tint, 1.2f);
        draw.addLine(cx + 5.5f, cy + h * 0.5f, cx + 2.5f, cy + h - 3.0f, tint, 1.2f);
        draw.addLine(cx + 7.5f, cy + h - 3.5f, cx + w - 3.0f, cy + h - 3.5f, tint, 1.2f);
    };

    // ---- Scene View Tool Icons ------------------------------------------------------------------

    /**
     * Fills an isosceles arrowhead triangle with apex at (tipX, tipY) and base perpendicular to (dirX, dirY).
     */
    private static void arrowHead(DrawScope draw, float tipX, float tipY, float dirX, float dirY, float len,
                                  float halfWidth, ColorView tint) {
        float baseX = tipX - dirX * len;
        float baseY = tipY - dirY * len;
        float nx = -dirY;
        float ny = dirX;
        draw.pathClear();
        draw.pathLineTo(tipX, tipY);
        draw.pathLineTo(baseX + nx * halfWidth, baseY + ny * halfWidth);
        draw.pathLineTo(baseX - nx * halfWidth, baseY - ny * halfWidth);
        draw.pathFillConvex(tint);
    }

    /**
     * View tool (hand): palm and four fingers.
     */
    public static final UiIcon TOOL_HAND_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.0f, size * 0.09f);
        float[] fx = {0.34f, 0.46f, 0.58f, 0.70f};
        float[] top = {0.22f, 0.12f, 0.14f, 0.26f};
        for (int i = 0; i < fx.length; i++) {
            draw.addLine(x + size * fx[i], y + size * top[i], x + size * fx[i], y + size * 0.55f, tint, t);
        }
        draw.addLine(x + size * 0.34f, y + size * 0.55f, x + size * 0.20f, y + size * 0.40f, tint, t);
        draw.pathClear();
        draw.pathLineTo(x + size * 0.34f, y + size * 0.55f);
        draw.pathLineTo(x + size * 0.70f, y + size * 0.55f);
        draw.pathLineTo(x + size * 0.70f, y + size * 0.74f);
        draw.pathLineTo(x + size * 0.58f, y + size * 0.88f);
        draw.pathLineTo(x + size * 0.44f, y + size * 0.88f);
        draw.pathLineTo(x + size * 0.34f, y + size * 0.74f);
        draw.pathStroke(tint, true, t);
    };

    /**
     * Move tool: four-way arrow cross.
     */
    public static final UiIcon TOOL_MOVE_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.42f;
        float t = Math.max(1.0f, size * 0.08f);
        draw.addLine(cx - r, cy, cx + r, cy, tint, t);
        draw.addLine(cx, cy - r, cx, cy + r, tint, t);
        float head = size * 0.20f;
        float half = size * 0.12f;
        arrowHead(draw, cx + r, cy, 1.0f, 0.0f, head, half, tint);
        arrowHead(draw, cx - r, cy, -1.0f, 0.0f, head, half, tint);
        arrowHead(draw, cx, cy - r, 0.0f, -1.0f, head, half, tint);
        arrowHead(draw, cx, cy + r, 0.0f, 1.0f, head, half, tint);
    };

    /**
     * Rotate tool: nearly full circular arc with an arrowhead.
     */
    public static final UiIcon TOOL_ROTATE_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.34f;
        float t = Math.max(1.0f, size * 0.09f);
        float start = (float) Math.toRadians(-40.0);
        float end = (float) Math.toRadians(250.0);
        draw.pathClear();
        draw.pathArcTo(cx, cy, r, start, end, 18);
        draw.pathStroke(tint, false, t);
        // Arrowhead at the arc end: tangent points along increasing angle.
        float ex = cx + (float) Math.cos(end) * r;
        float ey = cy + (float) Math.sin(end) * r;
        float dx = -(float) Math.sin(end);
        float dy = (float) Math.cos(end);
        arrowHead(draw, ex + dx * size * 0.10f, ey + dy * size * 0.10f, dx, dy, size * 0.22f, size * 0.12f, tint);
    };

    /**
     * Scale tool: square with a diagonal arrow toward the corner.
     */
    public static final UiIcon TOOL_SCALE_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.0f, size * 0.09f);
        draw.rectStroke(x + size * 0.14f, y + size * 0.14f, size * 0.46f, size * 0.46f, tint, t);
        draw.addLine(x + size * 0.34f, y + size * 0.66f, x + size * 0.76f, y + size * 0.24f, tint, t);
        arrowHead(draw, x + size * 0.88f, y + size * 0.12f, 0.7071f, -0.7071f, size * 0.26f, size * 0.13f, tint);
    };

    /**
     * Rect tool: frame with corner handles.
     */
    public static final UiIcon TOOL_RECT_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.0f, size * 0.08f);
        float a = size * 0.16f;
        float b = size * 0.84f;
        draw.rectStroke(x + a, y + a, b - a, b - a, tint, t);
        float c = size * 0.16f;
        draw.rectFilled(x + a - c * 0.5f, y + a - c * 0.5f, c, c, tint);
        draw.rectFilled(x + b - c * 0.5f, y + a - c * 0.5f, c, c, tint);
        draw.rectFilled(x + a - c * 0.5f, y + b - c * 0.5f, c, c, tint);
        draw.rectFilled(x + b - c * 0.5f, y + b - c * 0.5f, c, c, tint);
    };

    /**
     * Universal Transform tool: circle with axes and a center box.
     */
    public static final UiIcon TOOL_TRANSFORM_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.30f;
        float t = Math.max(1.0f, size * 0.08f);
        draw.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, tint, t);
        draw.addLine(cx - size * 0.44f, cy, cx + size * 0.44f, cy, tint, t);
        draw.addLine(cx, cy - size * 0.44f, cx, cy + size * 0.44f, tint, t);
        draw.rectFilled(cx - size * 0.07f, cy - size * 0.07f, size * 0.14f, size * 0.14f, tint);
    };

    /**
     * Wireframe cube (tool mode selector button).
     */
    public static final UiIcon CUBE_OUTLINE_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.40f;
        float w = r * 0.866f;
        float t = Math.max(1.0f, size * 0.08f);
        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + w, cy - r * 0.5f);
        draw.pathLineTo(cx + w, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - w, cy + r * 0.5f);
        draw.pathLineTo(cx - w, cy - r * 0.5f);
        draw.pathStroke(tint, true, t);
        draw.pathClear();
        draw.pathLineTo(cx - w, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + w, cy - r * 0.5f);
        draw.pathStroke(tint, false, t);
        draw.addLine(cx, cy, cx, cy + r, tint, t);
    };

    /**
     * Grid (Grid Visibility).
     */
    public static final UiIcon GRID_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.0f, size * 0.08f);
        float a = size * 0.14f;
        float b = size * 0.86f;
        for (int i = 0; i < 4; i++) {
            float p = a + (b - a) * i / 3.0f;
            draw.addLine(x + p, y + a, x + p, y + b, tint, t);
            draw.addLine(x + a, y + p, x + b, y + p, tint, t);
        }
    };

    /**
     * Magnet (Grid Snapping): U-shaped arc with poles at the top.
     */
    public static final UiIcon SNAP_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.4f, size * 0.13f);
        float cx = x + size * 0.5f;
        float cy = y + size * 0.52f;
        float r = size * 0.24f;
        float top = y + size * 0.18f;
        draw.pathClear();
        draw.pathLineTo(cx - r, top);
        for (int i = 0; i <= 8; i++) {
            double angle = Math.PI * i / 8.0;
            draw.pathLineTo(cx - r * (float) Math.cos(angle), cy + r * (float) Math.sin(angle));
        }
        draw.pathLineTo(cx + r, top);
        draw.pathStroke(tint, false, t);
        draw.rectFilled(cx - r - t * 0.5f, top - t * 0.2f, t, t * 1.2f, UnityStyles.AXIS_X);
        draw.rectFilled(cx + r - t * 0.5f, top - t * 0.2f, t, t * 1.2f, UnityStyles.AXIS_Z);
    };

    /**
     * Sun (Lighting).
     */
    public static final UiIcon SUN_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.17f;
        float t = Math.max(1.0f, size * 0.08f);
        draw.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, tint);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (Math.PI * 0.25 * i);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            draw.addLine(cx + cos * size * 0.28f, cy + sin * size * 0.28f,
                    cx + cos * size * 0.42f, cy + sin * size * 0.42f, tint, t);
        }
    };

    /**
     * Speaker (Audio).
     */
    public static final UiIcon SPEAKER_ICON = (draw, x, y, size, tint) -> {
        float t = Math.max(1.0f, size * 0.08f);
        draw.rectFilled(x + size * 0.16f, y + size * 0.38f, size * 0.16f, size * 0.24f, tint);
        draw.pathClear();
        draw.pathLineTo(x + size * 0.32f, y + size * 0.38f);
        draw.pathLineTo(x + size * 0.52f, y + size * 0.22f);
        draw.pathLineTo(x + size * 0.52f, y + size * 0.78f);
        draw.pathLineTo(x + size * 0.32f, y + size * 0.62f);
        draw.pathFillConvex(tint);
        draw.addLine(x + size * 0.64f, y + size * 0.38f, x + size * 0.72f, y + size * 0.50f, tint, t);
        draw.addLine(x + size * 0.72f, y + size * 0.50f, x + size * 0.64f, y + size * 0.62f, tint, t);
    };

    /**
     * Small triangle dropdown arrow.
     */
    public static void drawDropdownArrow(DrawScope draw, float cx, float cy, float size, ColorView tint) {
        draw.pathClear();
        draw.pathLineTo(cx - size * 0.5f, cy - size * 0.25f);
        draw.pathLineTo(cx + size * 0.5f, cy - size * 0.25f);
        draw.pathLineTo(cx, cy + size * 0.3f);
        draw.pathFillConvex(tint);
    }

    public static void drawGameObjectCube(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.44f;

        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);

        draw.pathClear();
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);

        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
    }

    public static void drawCameraIcon(DrawScope draw, float x, float y, float size) {
        float bw = size * 0.52f;
        float bh = size * 0.44f;
        float bx = x + size * 0.12f;
        float by = y + (size - bh) * 0.5f;

        draw.rectFilled(bx, by, bw, bh, UnityStyles.AXIS_Z);
        float tw = size * 0.24f;
        float th = size * 0.36f;
        float tx = bx + bw + 1.0f;
        float ty = y + size * 0.5f;

        draw.pathClear();
        draw.pathLineTo(tx, ty - th * 0.35f);
        draw.pathLineTo(tx + tw, ty - th * 0.5f);
        draw.pathLineTo(tx + tw, ty + th * 0.5f);
        draw.pathLineTo(tx, ty + th * 0.35f);
        draw.pathFillConvex(UnityStyles.AXIS_Z);
    }

    public static void drawLightIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.42f;
        float r = size * 0.26f;
        draw.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, UnityStyles.AXIS_X);
        draw.rectFilled(cx - r * 0.45f, cy + r * 0.8f, r * 0.9f, size * 0.18f, UnityStyles.TEXT_MUTED);
        draw.addLine(cx, cy - r - 2.5f, cx, cy - r - 1.0f, UnityStyles.AXIS_X, 1.2f);
        draw.addLine(cx - r - 2.5f, cy, cx - r - 1.0f, cy, UnityStyles.AXIS_X, 1.2f);
        draw.addLine(cx + r + 1.0f, cy, cx + r + 2.5f, cy, UnityStyles.AXIS_X, 1.2f);
    }

    /**
     * Draws a small cloud over three haze lines.
     *
     * @param draw drawing area
     * @param x    left edge of the icon
     * @param y    top edge of the icon
     * @param size side of the icon square
     */
    public static void drawFogIcon(DrawScope draw, float x, float y, float size) {
        float r = size * 0.16f;
        draw.circleFilled(x + size * 0.22f, y + size * 0.26f, r * 2.0f, r * 2.0f, UnityStyles.TEXT_PRIMARY);
        draw.circleFilled(x + size * 0.40f, y + size * 0.14f, r * 2.4f, r * 2.4f, UnityStyles.TEXT_PRIMARY);
        draw.rectFilled(x + size * 0.30f, y + size * 0.36f, size * 0.42f, r * 1.2f, UnityStyles.TEXT_PRIMARY);
        for (int i = 0; i < 3; i++) {
            float ly = y + size * (0.66f + 0.13f * i);
            float inset = size * (i == 1 ? 0.18f : 0.08f);
            draw.addLine(x + inset, ly, x + size - inset, ly, UnityStyles.TEXT_MUTED, 1.2f);
        }
    }

    public static void drawSceneIcon(DrawScope draw, float x, float y, float size) {
        float w = size * 0.75f;
        float h = size * 0.65f;
        float bx = x + size * 0.12f;
        float by = y + (size - h) * 0.5f;
        draw.roundedRectFilled(bx, by, w, h, 1.5f, UnityStyles.BG_HEADER);
        draw.roundedRectStroke(bx, by, w, h, 1.5f, UnityStyles.TEXT_MUTED, 1.0f);
        draw.rectFilled(bx + 2.0f, by + 2.0f, w * 0.35f, 2.0f, UnityStyles.TEXT_PRIMARY);
        draw.rectFilled(bx + w * 0.5f, by + 2.0f, w * 0.35f, 2.0f, UnityStyles.TEXT_PRIMARY);
    }

    public static void drawAudioIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.22f;
        float cy = y + size * 0.5f;
        draw.rectFilled(cx, cy - size * 0.18f, size * 0.18f, size * 0.36f, UnityStyles.TEXT_PRIMARY);
        draw.pathClear();
        draw.pathLineTo(cx + size * 0.18f, cy - size * 0.18f);
        draw.pathLineTo(cx + size * 0.38f, cy - size * 0.35f);
        draw.pathLineTo(cx + size * 0.38f, cy + size * 0.35f);
        draw.pathLineTo(cx + size * 0.18f, cy + size * 0.18f);
        draw.pathFillConvex(UnityStyles.TEXT_PRIMARY);
        draw.addLine(cx + size * 0.42f, cy - size * 0.15f, cx + size * 0.50f, cy, UnityStyles.TEXT_MUTED, 1.2f);
        draw.addLine(cx + size * 0.50f, cy, cx + size * 0.42f, cy + size * 0.15f, UnityStyles.TEXT_MUTED, 1.2f);
    }

    public static void drawCanvasIcon(DrawScope draw, float x, float y, float size) {
        float w = size * 0.72f;
        float h = size * 0.55f;
        float bx = x + size * 0.14f;
        float by = y + (size - h) * 0.5f;
        draw.rectStroke(bx, by, w, h, UnityStyles.TAB_ACCENT, 1.2f);
        draw.rectFilled(bx - 1.0f, by - 1.0f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx + w - 1.5f, by - 1.0f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx - 1.0f, by + h - 1.5f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
        draw.rectFilled(bx + w - 1.5f, by + h - 1.5f, 2.5f, 2.5f, UnityStyles.TAB_ACCENT);
    }

    /**
     * EventSystem lightning bolt. The silhouette is concave, so it is split into two convex polygons
     * with matching winding order: {@code pathFillConvex} only fills convex shapes properly.
     * The second polygon slightly overlaps the first so no seam shows along the shared edge.
     *
     * @param draw draw scope
     * @param x    left edge of the icon
     * @param y    top edge of the icon
     * @param size side length of the icon square
     */
    public static void drawEventSystemIcon(DrawScope draw, float x, float y, float size) {
        ColorView color = UnityStyles.WARNING_YELLOW;

        // Upper half: top-right tip and left bend.
        draw.pathClear();
        draw.pathLineTo(x + size * 0.64f, y + size * 0.04f);
        draw.pathLineTo(x + size * 0.22f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.45f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.56f, y + size * 0.40f);
        draw.pathFillConvex(color);

        // Lower half: right bend and bottom tip.
        draw.pathClear();
        draw.pathLineTo(x + size * 0.56f, y + size * 0.37f);
        draw.pathLineTo(x + size * 0.45f, y + size * 0.56f);
        draw.pathLineTo(x + size * 0.34f, y + size * 0.96f);
        draw.pathLineTo(x + size * 0.80f, y + size * 0.40f);
        draw.pathFillConvex(color);
    }

    /**
     * Draws an icon matching the given GameObject type.
     *
     * @param draw draw scope
     * @param type object type; {@code null} is treated as an empty GameObject
     * @param x    left edge of the icon
     * @param y    top edge of the icon
     * @param size side length of the icon square
     */
    public static void drawGameObjectIcon(DrawScope draw, UnityEditorHierarchyView.GameObjectType type,
                                          float x, float y, float size) {
        if (type == null) type = UnityEditorHierarchyView.GameObjectType.EMPTY;
        switch (type) {
            case CAMERA -> drawCameraIcon(draw, x, y, size);
            case LIGHT -> drawLightIcon(draw, x, y, size);
            case FOG_VOLUME -> drawFogIcon(draw, x, y, size);
            case PLAYER, MINECRAFT_ENTITY -> drawPlayerIcon(draw, x, y, size);
            case MINECRAFT_BLOCK -> drawProjectPrefabCube(draw, x, y, size);
            case CANVAS, UI_PANEL, UI_BUTTON, UI_TEXT -> drawCanvasIcon(draw, x, y, size);
            case EVENT_SYSTEM -> drawEventSystemIcon(draw, x, y, size);
            case AUDIO -> drawAudioIcon(draw, x, y, size);
            case SCENE -> drawSceneIcon(draw, x, y, size);
            default -> drawGameObjectCube(draw, x, y, size);
        }
    }

    /**
     * Icon widget that renders the icon for the current GameObject type each frame.
     *
     * @param size icon side length
     * @param type supplier for the current type
     * @return canvas widget drawing the icon
     */
    public static CanvasWidget gameObjectIcon(float size,
                                              java.util.function.Supplier<UnityEditorHierarchyView.GameObjectType> type) {
        CanvasWidget canvas = new CanvasWidget();
        canvas.layout(style -> style.size(size, size).flexGrow(0.0f).flexShrink(0.0f));
        canvas.onDraw(context -> {
            DrawScope draw = new DrawScope(context, canvas.transform(), canvas.layoutBounds());
            drawGameObjectIcon(draw, type.get(), canvas.layoutBounds().x(), canvas.layoutBounds().y(), size);
        });
        return canvas;
    }

    public static void drawPlayerIcon(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        draw.circleFilled(cx - 2.5f, cy - size * 0.38f, 5.0f, 5.0f, UnityStyles.TEXT_PRIMARY);
        draw.roundedRectFilled(cx - 3.5f, cy - size * 0.08f, 7.0f, size * 0.45f, 2.0f, UnityStyles.TEXT_PRIMARY);
    }

    public static void drawEyeIcon(DrawScope draw, float x, float y, float size, ColorView color) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float rx = size * 0.45f;
        float ry = size * 0.25f;
        draw.circleStroke(cx - rx, cy - ry, rx * 2.0f, ry * 2.0f, color, 1.0f);
        draw.circleFilled(cx - 1.5f, cy - 1.5f, 3.0f, 3.0f, color);
    }

    /**
     * Draws a folder icon for the Project View:
     * outlined (hollow inside) when empty, or filled when it contains assets.
     */
    public static void drawProjectFolder(DrawScope draw, float x, float y, float w, float h, boolean empty) {
        // Preserves 48x38 folder proportions at any size, from 14 px list rows to large tiles.
        float avail = Math.min(w, h / 0.792f);
        float fw = avail * 0.92f;
        float fh = fw * 0.792f;
        float k = fw / 48.0f;
        float r = 3.0f * k;
        float fx = x + (w - fw) * 0.5f;
        float fy = y + (h - fh) * 0.5f;
        float tabW = fw * 0.38f;
        float tabH = fh * 0.24f;

        if (empty) {
            ColorView outlineColor = MutableColor.rgba255(0x9A, 0xA8, 0xBA, 0xFF);
            // Arc angles are specified in radians.
            final float half = (float) (Math.PI * 0.5);
            final float pi = (float) Math.PI;
            draw.pathClear();
            draw.pathArcTo(fx + r, fy + r, r, pi, pi + half, 6);
            draw.pathLineTo(fx + tabW, fy);
            draw.pathLineTo(fx + tabW + 4.0f * k, fy + tabH);
            draw.pathLineTo(fx + fw - r, fy + tabH);
            draw.pathArcTo(fx + fw - r, fy + tabH + r, r, pi + half, pi * 2.0f, 6);
            draw.pathLineTo(fx + fw, fy + fh - r);
            draw.pathArcTo(fx + fw - r, fy + fh - r, r, 0.0f, half, 6);
            draw.pathLineTo(fx + r, fy + fh);
            draw.pathArcTo(fx + r, fy + fh - r, r, half, pi, 6);
            // Path is closed via the closed flag; repeating the start vertex would create a spike at the seam.
            draw.pathStroke(outlineColor, true, Math.max(1.0f, 2.0f * k));
        } else {
            draw.roundedRectFilled(fx, fy, tabW + 5.0f * k, tabH + 5.0f * k, r, MutableColor.rgba255(0x76, 0x86, 0x98, 0xFF));
            draw.roundedRectFilled(fx, fy + tabH, fw, fh - tabH, r, MutableColor.rgba255(0x9A, 0xA8, 0xBA, 0xFF));
            draw.roundedRectFilled(fx, fy + tabH + 3.0f * k, fw, fh - tabH - 3.0f * k, r, MutableColor.rgba255(0x8A, 0x98, 0xAA, 0xFF));
        }
    }

    /**
     * Unity Scene icon (white 3D isometric cube with cutouts and a hollow center cavity).
     */
    public static void drawProjectSceneCube(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float R = size * 0.44f;
        float W = R * 0.866f;
        float H2 = R * 0.5f;
        float rIn = R * 0.46f;
        float wIn = rIn * 0.866f;
        float h2In = rIn * 0.5f;

        ColorView colTop = MutableColor.rgba255(0xFF, 0xFF, 0xFF, 0xFF);
        ColorView colLeft = MutableColor.rgba255(0xD8, 0xD8, 0xD8, 0xFF);
        ColorView colRight = MutableColor.rgba255(0xAF, 0xAF, 0xAF, 0xFF);
        ColorView colCavity = MutableColor.rgba255(0x28, 0x28, 0x28, 0xFF);

        // Dark inner cavity of the cube
        draw.pathClear();
        draw.pathLineTo(cx, cy - rIn);
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathFillConvex(colCavity);

        // 1. Top face (pure white)
        draw.pathClear();
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathLineTo(cx, cy - R);
        draw.pathLineTo(cx, cy - rIn);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathFillConvex(colTop);

        draw.pathClear();
        draw.pathLineTo(cx, cy - R);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx, cy - rIn);
        draw.pathFillConvex(colTop);

        // 2. Left face (light gray)
        draw.pathClear();
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx - W, cy + H2);
        draw.pathFillConvex(colLeft);

        draw.pathClear();
        draw.pathLineTo(cx - W, cy + H2);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathLineTo(cx, cy + R);
        draw.pathFillConvex(colLeft);

        // 3. Right face (mid gray)
        draw.pathClear();
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx + W, cy + H2);
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathFillConvex(colRight);

        draw.pathClear();
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathLineTo(cx + W, cy + H2);
        draw.pathLineTo(cx, cy + R);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathFillConvex(colRight);
    }

    /**
     * Unity Prefab icon (blue 3D isometric cube).
     */
    public static void drawProjectPrefabCube(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float R = size * 0.44f;
        float W = R * 0.866f;
        float H2 = R * 0.5f;
        float rIn = R * 0.46f;
        float wIn = rIn * 0.866f;
        float h2In = rIn * 0.5f;

        ColorView colTop = MutableColor.rgba255(0x52, 0xA6, 0xFF, 0xFF);
        ColorView colLeft = MutableColor.rgba255(0x30, 0x82, 0xD8, 0xFF);
        ColorView colRight = MutableColor.rgba255(0x1E, 0x5E, 0xAA, 0xFF);
        ColorView colCavity = MutableColor.rgba255(0x10, 0x2A, 0x48, 0xFF);

        draw.pathClear();
        draw.pathLineTo(cx, cy - rIn);
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathFillConvex(colCavity);

        draw.pathClear();
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathLineTo(cx, cy - R);
        draw.pathLineTo(cx, cy - rIn);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathFillConvex(colTop);

        draw.pathClear();
        draw.pathLineTo(cx, cy - R);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx, cy - rIn);
        draw.pathFillConvex(colTop);

        draw.pathClear();
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathLineTo(cx - wIn, cy - h2In);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx - W, cy + H2);
        draw.pathFillConvex(colLeft);

        draw.pathClear();
        draw.pathLineTo(cx - W, cy + H2);
        draw.pathLineTo(cx - wIn, cy + h2In);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathLineTo(cx, cy + R);
        draw.pathFillConvex(colLeft);

        draw.pathClear();
        draw.pathLineTo(cx + wIn, cy - h2In);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx + W, cy + H2);
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathFillConvex(colRight);

        draw.pathClear();
        draw.pathLineTo(cx + wIn, cy + h2In);
        draw.pathLineTo(cx + W, cy + H2);
        draw.pathLineTo(cx, cy + R);
        draw.pathLineTo(cx, cy + rIn);
        draw.pathFillConvex(colRight);
    }

    /**
     * Material icon (shaded sphere with a specular highlight).
     */
    public static void drawProjectMaterial(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.38f;

        draw.circleFilled(cx - r * 0.9f, cy + r * 0.6f, r * 1.8f, r * 0.5f, MutableColor.rgba255(0x14, 0x14, 0x14, 0x88));
        draw.circleFilled(cx - r, cy - r, r * 2.0f, r * 2.0f, MutableColor.rgba255(0x4E, 0x58, 0x66, 0xFF));
        draw.circleFilled(cx - r * 0.85f, cy - r * 0.9f, r * 1.6f, r * 1.6f, MutableColor.rgba255(0x72, 0x82, 0x96, 0xFF));
        draw.circleFilled(cx - r * 0.70f, cy - r * 0.8f, r * 1.2f, r * 1.2f, MutableColor.rgba255(0x9E, 0xB0, 0xC6, 0xFF));
        draw.circleFilled(cx - r * 0.45f, cy - r * 0.55f, r * 0.45f, r * 0.45f, MutableColor.rgba255(0xFF, 0xFF, 0xFF, 0xEE));
    }

    /**
     * C# Script icon (document sheet with a folded corner and '#' glyph).
     */
    public static void drawProjectScript(DrawScope draw, float x, float y, float size) {
        float dw = size * 0.62f;
        float dh = size * 0.78f;
        float dx = x + (size - dw) * 0.5f;
        float dy = y + (size - dh) * 0.5f;
        float fold = dw * 0.28f;

        draw.roundedRectFilled(dx + 1.5f, dy + 1.5f, dw, dh, 2.0f, MutableColor.rgba255(0x18, 0x18, 0x18, 0x60));

        draw.pathClear();
        draw.pathLineTo(dx, dy);
        draw.pathLineTo(dx + dw - fold, dy);
        draw.pathLineTo(dx + dw, dy + fold);
        draw.pathLineTo(dx + dw, dy + dh);
        draw.pathLineTo(dx, dy + dh);
        draw.pathFillConvex(MutableColor.rgba255(0xEA, 0xEE, 0xF2, 0xFF));

        draw.pathClear();
        draw.pathLineTo(dx + dw - fold, dy);
        draw.pathLineTo(dx + dw - fold, dy + fold);
        draw.pathLineTo(dx + dw, dy + fold);
        draw.pathFillConvex(MutableColor.rgba255(0xB4, 0xC2, 0xD2, 0xFF));

        float cx = dx + dw * 0.5f;
        float cy = dy + dh * 0.52f;
        float hs = dw * 0.34f;
        Paint hashPaint = Paint.stroke(MutableColor.rgba255(0x2B, 0x78, 0xC5, 0xFF), 1.8f);

        draw.line(cx - hs * 0.25f + 1.0f, cy - hs * 0.5f, cx - hs * 0.25f - 1.0f, cy + hs * 0.5f, hashPaint);
        draw.line(cx + hs * 0.25f + 1.0f, cy - hs * 0.5f, cx + hs * 0.25f - 1.0f, cy + hs * 0.5f, hashPaint);
        draw.line(cx - hs * 0.5f, cy - hs * 0.2f, cx + hs * 0.5f, cy - hs * 0.2f, hashPaint);
        draw.line(cx - hs * 0.5f, cy + hs * 0.2f, cx + hs * 0.5f, cy + hs * 0.2f, hashPaint);
    }

    /**
     * Texture icon (framed landscape thumbnail).
     */
    public static void drawProjectTexture(DrawScope draw, float x, float y, float size) {
        float tw = size * 0.74f;
        float th = size * 0.62f;
        float tx = x + (size - tw) * 0.5f;
        float ty = y + (size - th) * 0.5f;

        draw.roundedRectFilled(tx, ty, tw, th, 3.0f, MutableColor.rgba255(0x42, 0x4B, 0x56, 0xFF));
        draw.rectFilled(tx + 2.0f, ty + 2.0f, tw - 4.0f, th - 4.0f, MutableColor.rgba255(0x22, 0x2A, 0x38, 0xFF));

        draw.circleFilled(tx + tw - 11.0f, ty + 5.0f, 5.0f, 5.0f, MutableColor.rgba255(0xF6, 0xC4, 0x44, 0xFF));

        draw.pathClear();
        draw.pathLineTo(tx + 2.0f, ty + th - 2.0f);
        draw.pathLineTo(tx + tw * 0.38f, ty + th * 0.35f);
        draw.pathLineTo(tx + tw * 0.75f, ty + th - 2.0f);
        draw.pathFillConvex(MutableColor.rgba255(0x38, 0x6E, 0x4E, 0xFF));

        draw.pathClear();
        draw.pathLineTo(tx + tw * 0.25f, ty + th - 2.0f);
        draw.pathLineTo(tx + tw * 0.65f, ty + th * 0.45f);
        draw.pathLineTo(tx + tw - 2.0f, ty + th - 2.0f);
        draw.pathFillConvex(MutableColor.rgba255(0x4E, 0x9A, 0x66, 0xFF));
    }

    /**
     * Package icon (cardboard box).
     */
    public static void drawProjectPackage(DrawScope draw, float x, float y, float size) {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float R = size * 0.38f;
        float W = R * 0.866f;
        float H2 = R * 0.5f;

        draw.pathClear();
        draw.pathLineTo(cx, cy - R);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathFillConvex(MutableColor.rgba255(0xCE, 0xA2, 0x72, 0xFF));

        draw.pathClear();
        draw.pathLineTo(cx - W, cy - H2);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + R);
        draw.pathLineTo(cx - W, cy + H2);
        draw.pathFillConvex(MutableColor.rgba255(0xAC, 0x82, 0x54, 0xFF));

        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + W, cy - H2);
        draw.pathLineTo(cx + W, cy + H2);
        draw.pathLineTo(cx, cy + R);
        draw.pathFillConvex(MutableColor.rgba255(0x8E, 0x66, 0x3C, 0xFF));

        Paint tapePaint = Paint.stroke(MutableColor.rgba255(0xEC, 0xD4, 0xB0, 0xFF), 2.5f);
        draw.line(cx - W * 0.5f, cy - H2 * 1.5f, cx + W * 0.5f, cy - H2 * 0.5f, tapePaint);
    }

    /**
     * Dispatcher for Project View asset icons.
     */
    public static void drawProjectItemIcon(DrawScope draw, String type, boolean emptyFolder, float x, float y, float w, float h) {
        if (type == null) type = "scene";
        switch (type.toLowerCase()) {
            case "folder" -> drawProjectFolder(draw, x, y, w, h, emptyFolder);
            case "scene" -> drawProjectSceneCube(draw, x, y, Math.min(w, h));
            case "prefab" -> drawProjectPrefabCube(draw, x, y, Math.min(w, h));
            case "material" -> drawProjectMaterial(draw, x, y, Math.min(w, h));
            case "script", "cs", "asset" -> drawProjectScript(draw, x, y, Math.min(w, h));
            case "texture", "png", "jpg" -> drawProjectTexture(draw, x, y, Math.min(w, h));
            case "package" -> drawProjectPackage(draw, x, y, Math.min(w, h));
            default -> drawProjectSceneCube(draw, x, y, Math.min(w, h));
        }
    }

    /**
     * Draws a Unity-style "+ with small dropdown arrow" icon for the Create button.
     *
     * @param draw  draw scope
     * @param x     button left coordinate
     * @param y     button top coordinate
     * @param w     button width
     * @param h     button height
     * @param color icon color
     */
    public static void drawCreateDropdownIcon(DrawScope draw, float x, float y, float w, float h, ColorView color) {
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        float px = cx - 3.5f;
        float py = cy - 1.0f;

        // Plus sign: vertical and horizontal bars (2.0px thick, 12.0px long)
        draw.rectFilled(px - 1.0f, py - 6.0f, 2.0f, 12.0f, color);
        draw.rectFilled(px - 6.0f, py - 1.0f, 12.0f, 2.0f, color);

        // Small dropdown triangle at the bottom-right of the plus (5px wide, 3px tall)
        float triLeft = px + 8.0f;
        float triRight = px + 13.0f;
        float triTop = py + 5.5f;
        float triBottom = py + 8.5f;
        float triMid = (triLeft + triRight) * 0.5f;

        draw.pathClear();
        draw.pathLineTo(triLeft, triTop);
        draw.pathLineTo(triRight, triTop);
        draw.pathLineTo(triMid, triBottom);
        draw.pathFillConvex(color);
    }
}
