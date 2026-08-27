package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TestMod-пример мини-игры, где гайку нужно затянуть гаечным ключом.
 */
public final class WrenchNutMinigameScreen {
    private static final MutableColor COLOR_TITLE = MutableColor.rgba(0.78f, 0.90f, 1.0f, 1.0f);
    private static final MutableColor COLOR_TEXT = MutableColor.rgba(0.72f, 0.80f, 0.90f, 1.0f);
    private static final MutableColor COLOR_MUTED = MutableColor.rgba(0.45f, 0.54f, 0.66f, 1.0f);
    private static final MutableColor COLOR_BORDER = MutableColor.rgba(0.22f, 0.34f, 0.52f, 0.76f);
    private static final MutableColor COLOR_ACCENT = MutableColor.rgba(0.45f, 0.84f, 1.0f, 1.0f);

    private WrenchNutMinigameScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.4f);
        context.scaleProvider(scale);

        Label status = label("Зажми ключ и крути гайку по часовой стрелке.", COLOR_MUTED);
        WrenchNutMinigameWidget minigame = new WrenchNutMinigameWidget();
        minigame.onCompleted(() -> status.text("Гайка затянута. Узел зафиксирован."));

        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.literal("Wrench Nut Minigame"), root(minigame, status), context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.continuous());
        minecraft.setScreen(screen);
    }

    private static Widget root(WrenchNutMinigameWidget minigame, Label status) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f));
        viewport.addChild(backgroundFrame());

        VBox shell = new VBox();
        shell.layout(style -> style.sizePercent(86.0f, 82.0f)
                .padding(14.0f)
                .gap(12.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        HBox header = new HBox();
        header.layout(style -> style.size(LayoutConstraints.AUTO, 42.0f)
                .gap(12.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        Label title = label("MECHANICAL LOCK TEST", COLOR_TITLE);
        title.layout(style -> style.size(LayoutConstraints.AUTO, 30.0f).flexGrow(1.0f).flexShrink(1.0f));
        status.layout(style -> style.size(390.0f, 30.0f).flexGrow(0.0f).flexShrink(0.0f));
        header.addChild(title);
        header.addChild(status);

        HBox body = new HBox();
        body.layout(style -> style.sizePercent(100.0f, 100.0f)
                .gap(12.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        Box gamePanel = panelBox(0.014f, 0.019f, 0.028f, 0.98f);
        gamePanel.layout(style -> style.sizePercent(100.0f, 100.0f)
                .padding(10.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
        minigame.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));
        gamePanel.addChild(minigame);

        Box sidePanel = panelBox(0.020f, 0.027f, 0.039f, 0.96f);
        sidePanel.layout(style -> style.size(280.0f, LayoutConstraints.AUTO)
                .padding(12.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        VBox side = new VBox();
        side.layout(style -> style.sizePercent(100.0f, 100.0f)
                .gap(8.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
        side.addChild(label("Цель", COLOR_ACCENT));
        side.addChild(label("1. Зажми рукоять ключа.", COLOR_TEXT));
        side.addChild(label("2. Веди мышь вокруг гайки.", COLOR_TEXT));
        side.addChild(label("3. Прогресс идёт по часовой стрелке.", COLOR_TEXT));
        side.addChild(label("Обратный ход немного ослабляет гайку.", COLOR_MUTED));

        Button reset = new Button("Reset");
        reset.layout(style -> style.size(LayoutConstraints.AUTO, 28.0f).flexGrow(0.0f).flexShrink(0.0f));
        reset.onClick(event -> {
            minigame.resetGame();
            status.text("Зажми ключ и крути гайку по часовой стрелке.");
        });
        Button close = new Button("Close");
        close.layout(style -> style.size(LayoutConstraints.AUTO, 28.0f).flexGrow(0.0f).flexShrink(0.0f));
        close.onClick(event -> Minecraft.getInstance().screen.onClose());
        side.addChild(reset);
        side.addChild(close);

        body.addChild(gamePanel);
        sidePanel.addChild(side);
        body.addChild(sidePanel);
        shell.addChild(header);
        shell.addChild(body);
        viewport.addChild(shell);
        return new OverlayLayer(viewport);
    }

    private static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        label.color(color);
        label.noWrap();
        label.overflowMode(TextOverflowMode.CLIP);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0.0f).flexShrink(0.0f));
        return label;
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.006f, 0.009f, 0.014f, 1.0f);
        frame.borderVisible(false);
        frame.radius(0.0f);
        frame.layout(style -> style.sizePercent(100.0f, 100.0f).align(Alignment.STRETCH, Alignment.STRETCH));
        return frame;
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(8.0f);
        box.borderWidth(1.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(COLOR_BORDER);
        return box;
    }

    private static final class WrenchNutMinigameWidget extends WidgetBase {
        private static final float TARGET_TURNS = 2.35f;
        private static final float TWO_PI = (float) (Math.PI * 2.0);
        private static final MutableColor TRACK = MutableColor.rgba(0.030f, 0.041f, 0.060f, 1.0f);
        private static final MutableColor GRID = MutableColor.rgba(0.16f, 0.25f, 0.36f, 0.22f);
        private static final MutableColor NUT = MutableColor.rgba(0.78f, 0.68f, 0.46f, 1.0f);
        private static final MutableColor NUT_DARK = MutableColor.rgba(0.36f, 0.30f, 0.20f, 1.0f);
        private static final MutableColor BOLT = MutableColor.rgba(0.23f, 0.27f, 0.33f, 1.0f);
        private static final MutableColor WRENCH = MutableColor.rgba(0.72f, 0.78f, 0.84f, 1.0f);
        private static final MutableColor WRENCH_DARK = MutableColor.rgba(0.32f, 0.38f, 0.45f, 1.0f);
        private static final MutableColor WARNING = MutableColor.rgba(1.0f, 0.63f, 0.25f, 1.0f);
        private static final MutableColor SUCCESS = MutableColor.rgba(0.44f, 1.0f, 0.70f, 1.0f);

        private float wrenchAngle = -0.95f;
        private float lastPointerAngle;
        private int dragPointerId = -1;
        private float progress;
        private float ratchetFlash;
        private float shakeTimer;
        private float pulseTime;
        private boolean completed;
        private Runnable completedCallback = () -> {};

        private WrenchNutMinigameWidget() {
            focusable(true);
            mouseCursor(MouseCursor.POINTER);
        }

        private void onCompleted(Runnable callback) {
            completedCallback = callback == null ? () -> {} : callback;
        }

        private void resetGame() {
            int pointerId = dragPointerId;
            wrenchAngle = -0.95f;
            lastPointerAngle = 0.0f;
            dragPointerId = -1;
            progress = 0.0f;
            ratchetFlash = 0.0f;
            shakeTimer = 0.0f;
            pulseTime = 0.0f;
            completed = false;
            UIContext context = uiContext();
            if (context != null && pointerId >= 0) {
                context.releasePointer(pointerId, this);
            }
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 640.0f, 420.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            if (visibility() != Visibility.VISIBLE) return;
            super.tick(frame);
            float dt = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
            if (ratchetFlash > 0.0f) {
                ratchetFlash = Math.max(0.0f, ratchetFlash - dt * 3.5f);
                invalidate(InvalidationFlags.VISUAL);
            }
            if (shakeTimer > 0.0f) {
                shakeTimer = Math.max(0.0f, shakeTimer - dt);
                invalidate(InvalidationFlags.VISUAL);
            }
            if (completed) {
                pulseTime += dt;
                invalidate(InvalidationFlags.VISUAL);
            }
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
            pushOpacity(context);
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            boolean clip = false;
            try {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                clip = true;
                renderBoard(draw);
                renderGuideRing(draw);
                renderBoltAndNut(draw);
                renderWrench(draw);
                renderHud(draw);
            } finally {
                if (clip) draw.popClip();
                popOpacity(context);
            }
        }

        @Override
        public void handle(Event event) {
            if (visibility() != Visibility.VISIBLE || !enabled()) return;
            super.handle(event);
            if (event.isCancelled()) return;
            if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

            if (event instanceof PointerPressedEvent pointer
                    && pointer.button() == PointerButton.PRIMARY
                    && !completed
                    && hitWrench(localX(pointer), localY(pointer))) {
                beginDrag(pointer);
                event.cancel();
            } else if (event instanceof PointerMovedEvent pointer && dragPointerId == pointer.pointerId()) {
                updateDrag(pointer);
                event.cancel();
            } else if (event instanceof PointerReleasedEvent pointer
                    && pointer.button() == PointerButton.PRIMARY
                    && dragPointerId == pointer.pointerId()) {
                finishDrag(pointer);
                event.cancel();
            }
        }

        private void beginDrag(PointerPressedEvent pointer) {
            dragPointerId = pointer.pointerId();
            lastPointerAngle = pointerAngle(localX(pointer), localY(pointer));
            UIContext context = uiContext();
            if (context != null) {
                context.capturePointer(pointer.pointerId(), this);
            }
            invalidate(InvalidationFlags.VISUAL);
        }

        private void updateDrag(PointerMovedEvent pointer) {
            float nextPointerAngle = pointerAngle(localX(pointer), localY(pointer));
            float delta = normalizeAngle(nextPointerAngle - lastPointerAngle);
            if (Math.abs(delta) > 1.2f) {
                lastPointerAngle = nextPointerAngle;
                return;
            }

            wrenchAngle = normalizeAngle(wrenchAngle + delta);
            if (delta > 0.0f) {
                progress = clamp(progress + delta / (TWO_PI * TARGET_TURNS), 0.0f, 1.0f);
                ratchetFlash = 1.0f;
                if (progress >= 1.0f) {
                    completed = true;
                    dragPointerId = -1;
                    UIContext context = uiContext();
                    if (context != null) {
                        context.releasePointer(pointer.pointerId(), this);
                    }
                    completedCallback.run();
                }
            } else if (delta < -0.015f) {
                progress = clamp(progress + delta / (TWO_PI * TARGET_TURNS) * 0.35f, 0.0f, 1.0f);
                shakeTimer = 0.16f;
            }
            lastPointerAngle = nextPointerAngle;
            invalidate(InvalidationFlags.VISUAL);
        }

        private void finishDrag(PointerReleasedEvent pointer) {
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            dragPointerId = -1;
            invalidate(InvalidationFlags.VISUAL);
        }

        private boolean hitWrench(float x, float y) {
            float cx = nutX();
            float cy = nutY();
            float dx = x - cx;
            float dy = y - cy;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < 52.0f || distance > handleLength() + 48.0f) return false;
            float angle = (float) Math.atan2(dy, dx);
            float diff = Math.abs(normalizeAngle(angle - wrenchAngle));
            return diff < 0.46f || distance > handleLength() - 42.0f;
        }

        private void renderBoard(DrawScope draw) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float w = layoutBounds().width();
            float h = layoutBounds().height();
            draw.addRectFilled(x, y, w, h, 6.0f, TRACK);
            for (float gx = x + 28.0f; gx < x + w; gx += 48.0f) {
                draw.line(gx, y + 18.0f, gx, y + h - 18.0f, dev.sixik.unigui.api.render.Paint.stroke(GRID, 1.0f));
            }
            for (float gy = y + 28.0f; gy < y + h; gy += 48.0f) {
                draw.line(x + 18.0f, gy, x + w - 18.0f, gy, dev.sixik.unigui.api.render.Paint.stroke(GRID, 1.0f));
            }
        }

        private void renderGuideRing(DrawScope draw) {
            float cx = absX(nutX());
            float cy = absY(nutY());
            float radius = Math.min(layoutBounds().width(), layoutBounds().height()) * 0.31f;
            draw.addCircle(cx, cy, radius, MutableColor.rgba(0.45f, 0.84f, 1.0f, 0.18f), 64, 1.5f);
            draw.addCircle(cx, cy, radius + 8.0f, MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.24f), 64, 2.0f);
            if (ratchetFlash > 0.0f) {
                draw.addCircle(cx, cy, radius - 12.0f, MutableColor.rgba(0.44f, 1.0f, 0.70f, 0.22f * ratchetFlash), 64, 5.0f);
            }
        }

        private void renderBoltAndNut(DrawScope draw) {
            float cx = absX(nutX());
            float cy = absY(nutY());
            if (shakeTimer > 0.0f) {
                cx += (float) Math.sin(shakeTimer * 75.0f) * 2.0f;
            }
            float glow = completed ? 0.26f + (float) Math.sin(pulseTime * 5.0f) * 0.08f : 0.0f;
            if (glow > 0.0f) {
                draw.addCircleFilled(cx, cy, 92.0f, MutableColor.rgba(0.32f, 1.0f, 0.72f, glow), 48);
            }
            draw.addCircleFilled(cx, cy, 72.0f, BOLT, 48);
            draw.addCircle(cx, cy, 72.0f, MutableColor.rgba(0.64f, 0.76f, 0.90f, 0.45f), 48, 2.0f);
            draw.addConvexPolyFilled(hex(cx, cy, 48.0f, wrenchAngle * 0.15f), NUT);
            draw.addPolyline(hex(cx, cy, 48.0f, wrenchAngle * 0.15f), NUT_DARK, true, 3.0f);
            draw.addCircleFilled(cx, cy, 22.0f, TRACK, 32);
            draw.addCircle(cx, cy, 22.0f, NUT_DARK, 32, 2.0f);
        }

        private void renderWrench(DrawScope draw) {
            float cx = nutX();
            float cy = nutY();
            float angle = wrenchAngle + (shakeTimer > 0.0f ? (float) Math.sin(shakeTimer * 80.0f) * 0.018f : 0.0f);
            float length = handleLength();
            float width = 24.0f;

            DrawPoint h1 = rotated(cx, cy, angle, 42.0f, -width * 0.5f);
            DrawPoint h2 = rotated(cx, cy, angle, length, -width * 0.5f);
            DrawPoint h3 = rotated(cx, cy, angle, length, width * 0.5f);
            DrawPoint h4 = rotated(cx, cy, angle, 42.0f, width * 0.5f);
            draw.addQuadFilled(h1, h2, h3, h4, WRENCH);
            draw.addQuad(h1, h2, h3, h4, WRENCH_DARK, 2.0f);

            DrawPoint g1 = rotated(cx, cy, angle, length - 64.0f, -4.0f);
            DrawPoint g2 = rotated(cx, cy, angle, length - 18.0f, -4.0f);
            DrawPoint g3 = rotated(cx, cy, angle, length - 18.0f, 4.0f);
            DrawPoint g4 = rotated(cx, cy, angle, length - 64.0f, 4.0f);
            draw.addQuadFilled(g1, g2, g3, g4, MutableColor.rgba(0.42f, 0.50f, 0.58f, 0.72f));

            draw.addCircleFilled(absX(cx), absY(cy), 43.0f, WRENCH, 38);
            draw.addCircleFilled(absX(cx), absY(cy), 27.0f, TRACK, 38);
            draw.addCircle(absX(cx), absY(cy), 43.0f, WRENCH_DARK, 38, 2.0f);

            DrawPoint jawA = rotated(cx, cy, angle, 16.0f, -44.0f);
            DrawPoint jawB = rotated(cx, cy, angle, 72.0f, -34.0f);
            DrawPoint jawC = rotated(cx, cy, angle, 64.0f, -12.0f);
            DrawPoint jawD = rotated(cx, cy, angle, 20.0f, -17.0f);
            draw.addQuadFilled(jawA, jawB, jawC, jawD, WRENCH);
            draw.addQuad(jawA, jawB, jawC, jawD, WRENCH_DARK, 2.0f);

            DrawPoint jawE = rotated(cx, cy, angle, 20.0f, 17.0f);
            DrawPoint jawF = rotated(cx, cy, angle, 64.0f, 12.0f);
            DrawPoint jawG = rotated(cx, cy, angle, 72.0f, 34.0f);
            DrawPoint jawH = rotated(cx, cy, angle, 16.0f, 44.0f);
            draw.addQuadFilled(jawE, jawF, jawG, jawH, WRENCH);
            draw.addQuad(jawE, jawF, jawG, jawH, WRENCH_DARK, 2.0f);

            if (dragPointerId >= 0) {
                DrawPoint end = rotated(cx, cy, angle, length, 0.0f);
                draw.addCircle(end.x(), end.y(), 34.0f, MutableColor.rgba(0.45f, 0.84f, 1.0f, 0.38f), 32, 2.0f);
            }
        }

        private void renderHud(DrawScope draw) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float w = layoutBounds().width();
            draw.addText(completed ? "TORQUE LOCK / SECURED" : dragPointerId >= 0 ? "TURN CLOCKWISE..." : "GRAB THE WRENCH", x + 18.0f, y + 14.0f, w - 36.0f, 24.0f, completed ? SUCCESS : COLOR_MUTED);
            draw.addRectFilled(x + 18.0f, y + 44.0f, 220.0f, 10.0f, 5.0f, MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.35f));
            draw.addRectFilled(x + 18.0f, y + 44.0f, 220.0f * progress, 10.0f, 5.0f, progress >= 1.0f ? SUCCESS : COLOR_ACCENT);
            draw.addText(Math.round(progress * 100.0f) + "%", x + 248.0f, y + 38.0f, 68.0f, 22.0f, progress >= 1.0f ? SUCCESS : COLOR_TEXT);
            if (shakeTimer > 0.0f) {
                draw.addText("REVERSE SLIP", x + 18.0f, y + 66.0f, 180.0f, 22.0f, WARNING);
            }
        }

        private DrawPoint rotated(float originX, float originY, float angle, float localX, float localY) {
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            return new DrawPoint(absX(originX + localX * cos - localY * sin), absY(originY + localX * sin + localY * cos));
        }

        private List<DrawPoint> hex(float centerX, float centerY, float radius, float angleOffset) {
            List<DrawPoint> points = new ArrayList<>(6);
            for (int i = 0; i < 6; i++) {
                float angle = angleOffset + (float) Math.PI / 6.0f + i * TWO_PI / 6.0f;
                points.add(new DrawPoint(centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius));
            }
            return points;
        }

        private float pointerAngle(float localX, float localY) {
            return (float) Math.atan2(localY - nutY(), localX - nutX());
        }

        private float handleLength() {
            return Math.max(172.0f, Math.min(255.0f, layoutBounds().width() * 0.32f));
        }

        private float nutX() {
            return layoutBounds().width() * 0.48f;
        }

        private float nutY() {
            return layoutBounds().height() * 0.52f;
        }

        private float absX(float localX) {
            return layoutBounds().x() + localX;
        }

        private float absY(float localY) {
            return layoutBounds().y() + localY;
        }

        private float localX(PointerEvent pointer) {
            return pointer.rootX() - layoutBounds().x();
        }

        private float localY(PointerEvent pointer) {
            return pointer.rootY() - layoutBounds().y();
        }

        private static float normalizeAngle(float value) {
            while (value > Math.PI) value -= TWO_PI;
            while (value < -Math.PI) value += TWO_PI;
            return value;
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
