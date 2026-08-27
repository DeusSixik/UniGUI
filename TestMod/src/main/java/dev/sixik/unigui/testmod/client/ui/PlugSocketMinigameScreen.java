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

import java.util.List;

/**
 * TestMod-пример маленькой drag-and-snap мини-игры: вставить вилку в розетку.
 */
public final class PlugSocketMinigameScreen {
    private static final MutableColor COLOR_TITLE = MutableColor.rgba(0.78f, 0.90f, 1.0f, 1.0f);
    private static final MutableColor COLOR_TEXT = MutableColor.rgba(0.72f, 0.80f, 0.90f, 1.0f);
    private static final MutableColor COLOR_MUTED = MutableColor.rgba(0.45f, 0.54f, 0.66f, 1.0f);
    private static final MutableColor COLOR_BORDER = MutableColor.rgba(0.22f, 0.34f, 0.52f, 0.76f);
    private static final MutableColor COLOR_ACCENT = MutableColor.rgba(0.45f, 0.84f, 1.0f, 1.0f);

    private PlugSocketMinigameScreen() {
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

        Label status = label("Зажми вилку и вставь её в розетку.", COLOR_MUTED);
        PlugSocketMinigameWidget minigame = new PlugSocketMinigameWidget();
        minigame.onCompleted(() -> status.text("Контакт есть. Цепь замкнута."));

        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.literal("Plug Socket Minigame"), root(minigame, status), context) {
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

    private static Widget root(PlugSocketMinigameWidget minigame, Label status) {
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
        Label title = label("POWER COUPLING TEST", COLOR_TITLE);
        title.layout(style -> style.size(LayoutConstraints.AUTO, 30.0f).flexGrow(1.0f).flexShrink(1.0f));
        status.layout(style -> style.size(330.0f, 30.0f).flexGrow(0.0f).flexShrink(0.0f));
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
        sidePanel.layout(style -> style.size(260.0f, LayoutConstraints.AUTO)
                .padding(12.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        VBox side = new VBox();
        side.layout(style -> style.sizePercent(100.0f, 100.0f)
                .gap(8.0f)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
        side.addChild(label("Цель", COLOR_ACCENT));
        side.addChild(label("1. Зажми вилку левой кнопкой.", COLOR_TEXT));
        side.addChild(label("2. Подведи штыри к контактам.", COLOR_TEXT));
        side.addChild(label("3. Отпусти мышь рядом с розеткой.", COLOR_TEXT));
        side.addChild(label("Угол и позиция должны совпасть.", COLOR_MUTED));

        Button reset = new Button("Reset");
        reset.layout(style -> style.size(LayoutConstraints.AUTO, 28.0f).flexGrow(0.0f).flexShrink(0.0f));
        reset.onClick(event -> {
            minigame.resetGame();
            status.text("Зажми вилку и вставь её в розетку.");
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

    private static final class PlugSocketMinigameWidget extends WidgetBase {
        private static final MutableColor TRACK = MutableColor.rgba(0.030f, 0.041f, 0.060f, 1.0f);
        private static final MutableColor GRID = MutableColor.rgba(0.16f, 0.25f, 0.36f, 0.22f);
        private static final MutableColor SOCKET = MutableColor.rgba(0.30f, 0.34f, 0.39f, 1.0f);
        private static final MutableColor SOCKET_DARK = MutableColor.rgba(0.055f, 0.064f, 0.075f, 1.0f);
        private static final MutableColor SOCKET_HOT = MutableColor.rgba(0.42f, 1.0f, 0.78f, 0.70f);
        private static final MutableColor PLUG = MutableColor.rgba(0.92f, 0.84f, 0.64f, 1.0f);
        private static final MutableColor PLUG_DARK = MutableColor.rgba(0.50f, 0.43f, 0.31f, 1.0f);
        private static final MutableColor METAL = MutableColor.rgba(0.78f, 0.84f, 0.90f, 1.0f);
        private static final MutableColor WIRE = MutableColor.rgba(0.08f, 0.11f, 0.15f, 1.0f);
        private static final MutableColor WIRE_HIGHLIGHT = MutableColor.rgba(0.22f, 0.32f, 0.46f, 0.92f);
        private static final MutableColor WARNING = MutableColor.rgba(1.0f, 0.58f, 0.22f, 1.0f);
        private static final MutableColor SUCCESS = MutableColor.rgba(0.44f, 1.0f, 0.70f, 1.0f);

        private float plugX = Float.NaN;
        private float plugY = Float.NaN;
        private float plugAngle;
        private int dragPointerId = -1;
        private float dragOffsetX;
        private float dragOffsetY;
        private float sparkTimer;
        private float pulseTime;
        private boolean completed;
        private Runnable completedCallback = () -> {};

        private PlugSocketMinigameWidget() {
            focusable(true);
            mouseCursor(MouseCursor.POINTER);
        }

        private void onCompleted(Runnable callback) {
            completedCallback = callback == null ? () -> {} : callback;
        }

        private void resetGame() {
            int pointerId = dragPointerId;
            plugX = Float.NaN;
            plugY = Float.NaN;
            plugAngle = 0.0f;
            dragPointerId = -1;
            sparkTimer = 0.0f;
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
            if (sparkTimer > 0.0f) {
                sparkTimer = Math.max(0.0f, sparkTimer - dt);
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
            ensurePlugPosition();

            pushOpacity(context);
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            boolean clip = false;
            try {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                clip = true;
                renderBoard(draw);
                renderSocket(draw);
                renderWire(draw);
                renderPlug(draw);
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
                    && hitPlug(localX(pointer), localY(pointer))) {
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
            ensurePlugPosition();
            dragPointerId = pointer.pointerId();
            dragOffsetX = localX(pointer) - plugX;
            dragOffsetY = localY(pointer) - plugY;
            UIContext context = uiContext();
            if (context != null) {
                context.capturePointer(pointer.pointerId(), this);
            }
            invalidate(InvalidationFlags.VISUAL);
        }

        private void updateDrag(PointerMovedEvent pointer) {
            float w = layoutBounds().width();
            float h = layoutBounds().height();
            plugX = clamp(localX(pointer) - dragOffsetX, 74.0f, w - 120.0f);
            plugY = clamp(localY(pointer) - dragOffsetY, 72.0f, h - 72.0f);
            updateAngle();
            invalidate(InvalidationFlags.VISUAL);
        }

        private void finishDrag(PointerReleasedEvent pointer) {
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            dragPointerId = -1;
            if (canSnap()) {
                snapPlug();
                completed = true;
                completedCallback.run();
            } else if (isNearSocket()) {
                sparkTimer = 0.55f;
            }
            invalidate(InvalidationFlags.VISUAL);
        }

        private void ensurePlugPosition() {
            if (!Float.isNaN(plugX) && !Float.isNaN(plugY)) return;
            plugX = layoutBounds().width() * 0.22f;
            plugY = layoutBounds().height() * 0.62f;
            updateAngle();
        }

        private void updateAngle() {
            if (completed) {
                plugAngle = 0.0f;
                return;
            }
            float dy = socketY() - plugY;
            float dx = Math.max(1.0f, socketX() - plugX);
            float target = (float) Math.atan2(dy, dx) * 0.48f;
            plugAngle = clamp(target, -0.42f, 0.42f);
        }

        private void snapPlug() {
            float reach = plugReach();
            plugAngle = 0.0f;
            plugX = socketX() - reach;
            plugY = socketY();
        }

        private boolean canSnap() {
            float distance = distance(plugTipX(), plugTipY(), socketX(), socketY());
            return distance <= 34.0f && Math.abs(plugAngle) <= 0.20f;
        }

        private boolean isNearSocket() {
            return distance(plugTipX(), plugTipY(), socketX(), socketY()) <= 64.0f;
        }

        private boolean hitPlug(float x, float y) {
            float dx = x - plugX;
            float dy = y - plugY;
            return Math.abs(dx) <= 98.0f && Math.abs(dy) <= 58.0f;
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

        private void renderSocket(DrawScope draw) {
            float cx = absX(socketX());
            float cy = absY(socketY());
            float glow = completed ? 0.28f + (float) Math.sin(pulseTime * 5.0f) * 0.08f : isNearSocket() ? 0.20f : 0.0f;
            if (glow > 0.0f) {
                draw.addCircleFilled(cx, cy, 78.0f, MutableColor.rgba(0.32f, 1.0f, 0.72f, glow), 48);
            }

            draw.addRectFilled(cx - 76.0f, cy - 98.0f, 152.0f, 196.0f, 18.0f, SOCKET);
            draw.addRect(cx - 76.0f, cy - 98.0f, 152.0f, 196.0f, 18.0f, MutableColor.rgba(0.72f, 0.78f, 0.86f, 0.42f), 2.0f);
            draw.addCircleFilled(cx, cy, 52.0f, MutableColor.rgba(0.20f, 0.23f, 0.28f, 1.0f), 48);
            draw.addCircle(cx, cy, 52.0f, isNearSocket() ? SOCKET_HOT : MutableColor.rgba(0.74f, 0.82f, 0.90f, 0.55f), 48, 2.0f);
            draw.addRectFilled(cx - 22.0f, cy - 24.0f, 10.0f, 48.0f, 3.0f, SOCKET_DARK);
            draw.addRectFilled(cx + 12.0f, cy - 24.0f, 10.0f, 48.0f, 3.0f, SOCKET_DARK);
            draw.addCircleFilled(cx, cy + 58.0f, 7.0f, SOCKET_DARK, 24);

            if (sparkTimer > 0.0f) {
                float a = sparkTimer / 0.55f;
                draw.line(cx - 42.0f, cy - 55.0f, cx - 62.0f, cy - 75.0f, dev.sixik.unigui.api.render.Paint.stroke(WARNING, 2.5f * a));
                draw.line(cx + 44.0f, cy + 42.0f, cx + 70.0f, cy + 58.0f, dev.sixik.unigui.api.render.Paint.stroke(WARNING, 2.5f * a));
                draw.line(cx + 8.0f, cy - 66.0f, cx + 20.0f, cy - 96.0f, dev.sixik.unigui.api.render.Paint.stroke(WARNING, 2.0f * a));
            }
        }

        private void renderWire(DrawScope draw) {
            DrawPoint rear = plugPoint(-plugBodyWidth() * 0.5f - 12.0f, 0.0f);
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float h = layoutBounds().height();
            DrawPoint anchor = new DrawPoint(x + 54.0f, y + h - 58.0f);
            DrawPoint c1 = new DrawPoint(x + plugX - 90.0f, y + plugY + 82.0f);
            DrawPoint c2 = new DrawPoint(x + 120.0f, y + h - 40.0f);
            List<DrawPoint> wire = List.of(anchor, c2, c1, rear);
            draw.addPolyline(wire, WIRE, false, 9.0f);
            draw.addPolyline(wire, WIRE_HIGHLIGHT, false, 2.0f);
        }

        private void renderPlug(DrawScope draw) {
            float bw = plugBodyWidth();
            float bh = plugBodyHeight();
            DrawPoint a = plugPoint(-bw * 0.5f, -bh * 0.5f);
            DrawPoint b = plugPoint(bw * 0.5f, -bh * 0.5f);
            DrawPoint c = plugPoint(bw * 0.5f, bh * 0.5f);
            DrawPoint d = plugPoint(-bw * 0.5f, bh * 0.5f);
            draw.addQuadFilled(a, b, c, d, PLUG);
            draw.addQuad(a, b, c, d, PLUG_DARK, 2.0f);

            drawProng(draw, -bh * 0.20f);
            drawProng(draw, bh * 0.20f);

            DrawPoint gripA = plugPoint(-bw * 0.22f, -bh * 0.50f);
            DrawPoint gripB = plugPoint(-bw * 0.08f, -bh * 0.50f);
            DrawPoint gripC = plugPoint(-bw * 0.08f, bh * 0.50f);
            DrawPoint gripD = plugPoint(-bw * 0.22f, bh * 0.50f);
            draw.addQuadFilled(gripA, gripB, gripC, gripD, MutableColor.rgba(0.68f, 0.58f, 0.40f, 0.55f));

            if (dragPointerId >= 0) {
                draw.addCircle(absX(plugX), absY(plugY), 64.0f, MutableColor.rgba(0.45f, 0.84f, 1.0f, 0.35f), 42, 2.0f);
            }
        }

        private void drawProng(DrawScope draw, float localY) {
            float bw = plugBodyWidth();
            float len = plugProngLength();
            float h = 9.0f;
            DrawPoint a = plugPoint(bw * 0.5f - 2.0f, localY - h * 0.5f);
            DrawPoint b = plugPoint(bw * 0.5f + len, localY - h * 0.5f);
            DrawPoint c = plugPoint(bw * 0.5f + len, localY + h * 0.5f);
            DrawPoint d = plugPoint(bw * 0.5f - 2.0f, localY + h * 0.5f);
            draw.addQuadFilled(a, b, c, d, METAL);
            draw.addQuad(a, b, c, d, MutableColor.rgba(0.45f, 0.52f, 0.60f, 1.0f), 1.0f);
        }

        private void renderHud(DrawScope draw) {
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float w = layoutBounds().width();
            draw.addText(completed ? "LOCKED / POWER ONLINE" : dragPointerId >= 0 ? "ALIGNING PLUG..." : "DRAG THE PLUG", x + 18.0f, y + 14.0f, w - 36.0f, 24.0f, completed ? SUCCESS : COLOR_MUTED);
            if (!completed) {
                float distance = distance(plugTipX(), plugTipY(), socketX(), socketY());
                float readiness = clamp(1.0f - distance / 220.0f, 0.0f, 1.0f);
                draw.addRectFilled(x + 18.0f, y + 44.0f, 184.0f, 8.0f, 4.0f, MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.35f));
                draw.addRectFilled(x + 18.0f, y + 44.0f, 184.0f * readiness, 8.0f, 4.0f, readiness > 0.84f ? SUCCESS : COLOR_ACCENT);
            }
        }

        private DrawPoint plugPoint(float dx, float dy) {
            float cos = (float) Math.cos(plugAngle);
            float sin = (float) Math.sin(plugAngle);
            return new DrawPoint(absX(plugX + dx * cos - dy * sin), absY(plugY + dx * sin + dy * cos));
        }

        private float plugTipX() {
            return plugX + (float) Math.cos(plugAngle) * plugReach();
        }

        private float plugTipY() {
            return plugY + (float) Math.sin(plugAngle) * plugReach();
        }

        private float plugReach() {
            return plugBodyWidth() * 0.5f + plugProngLength();
        }

        private float plugBodyWidth() {
            return Math.max(96.0f, Math.min(132.0f, layoutBounds().width() * 0.16f));
        }

        private float plugBodyHeight() {
            return Math.max(54.0f, Math.min(74.0f, layoutBounds().height() * 0.16f));
        }

        private float plugProngLength() {
            return Math.max(40.0f, Math.min(58.0f, layoutBounds().width() * 0.075f));
        }

        private float socketX() {
            return layoutBounds().width() * 0.72f;
        }

        private float socketY() {
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

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private static float distance(float ax, float ay, float bx, float by) {
            float dx = ax - bx;
            float dy = ay - by;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }
}
