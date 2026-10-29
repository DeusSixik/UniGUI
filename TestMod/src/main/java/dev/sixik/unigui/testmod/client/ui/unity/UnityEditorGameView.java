package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Интерактивный видовой экран Game View в стиле Unity:
 * панель инструментов (Display 1, Free Aspect, Scale, Maximize on Play, Mute Audio, Stats, Gizmos),
 * рендеринг игрового кадра (включая анимацию при запуске Play mode)
 * и оверлей статистики (Statistics: FPS, Batches, Tris, Verts).
 */
public class UnityEditorGameView extends LinearBox {
    private final CanvasWidget gameCanvas = new CanvasWidget();

    private boolean isPlaying = false;
    private boolean statsVisible = false;
    private boolean maximizeOnPlay = false;
    private boolean muteAudio = false;

    private static final MutableColor BG_GAME = new MutableColor(0.10f, 0.10f, 0.10f, 1.0f);
    private static final MutableColor COLOR_GRID = new MutableColor(0.20f, 0.20f, 0.22f, 0.5f);
    private static final MutableColor STATS_BG = new MutableColor(0.12f, 0.12f, 0.14f, 0.88f);
    private static final MutableColor STATS_BORDER = new MutableColor(0.25f, 0.25f, 0.28f, 0.8f);

    public UnityEditorGameView() {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Панель инструментов Game View
        HBox toolbar = new HBox();
        toolbar.spacing(6.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .padding(6.0f, 0.0f, 6.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        toolbar.addChild(createToolBtn("Display 1 v"));
        toolbar.addChild(createToolBtn("Free Aspect v"));
        toolbar.addChild(createToolBtn("Scale 1x"));

        Button maxBtn = createToggleBtn("Maximize on Play", maximizeOnPlay);
        maxBtn.onClick(e -> {
            maximizeOnPlay = !maximizeOnPlay;
            updateToggleVisual(maxBtn, maximizeOnPlay);
        });
        toolbar.addChild(maxBtn);

        Button muteBtn = createToggleBtn("Mute Audio", muteAudio);
        muteBtn.onClick(e -> {
            muteAudio = !muteAudio;
            updateToggleVisual(muteBtn, muteAudio);
        });
        toolbar.addChild(muteBtn);

        Button statsBtn = createToggleBtn("Stats", statsVisible);
        statsBtn.onClick(e -> {
            statsVisible = !statsVisible;
            updateToggleVisual(statsBtn, statsVisible);
            gameCanvas.invalidate(InvalidationFlags.VISUAL);
        });
        toolbar.addChild(statsBtn);

        toolbar.addChild(createToolBtn("Gizmos v"));

        addChild(toolbar);
        addChild(UnityStyles.separatorLine());

        // 2. Игровой Canvas
        gameCanvas.layout(style -> style.sizePercent(100.0f, 100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        gameCanvas.onDraw(context -> {
            float x = gameCanvas.layoutBounds().x();
            float y = gameCanvas.layoutBounds().y();
            float w = gameCanvas.layoutBounds().width();
            float h = gameCanvas.layoutBounds().height();

            if (w <= 1.0f || h <= 1.0f) return;

            // Заливка фона
            context.rectFilled(x, y, w, h, BG_GAME);

            float cx = x + w * 0.5f;
            float cy = y + h * 0.55f;

            // Сетка перспективы пола
            int gridLines = 14;
            float gridSpan = Math.min(w * 0.45f, 260.0f);
            float vanishY = cy - 70.0f;

            for (int i = -gridLines; i <= gridLines; i++) {
                float gx = (float) i / gridLines;
                float startX = cx + gx * gridSpan;
                float startY = cy + 120.0f;
                float endX = cx + gx * (gridSpan * 0.12f);
                float endY = vanishY;

                context.addLine(startX, startY, endX, endY, COLOR_GRID, 1.0f);
            }

            for (int i = 0; i <= 8; i++) {
                float t = (float) i / 8.0f;
                float curY = cy + 120.0f - (120.0f + 70.0f) * (t * t);
                float curSpan = gridSpan * (1.0f - t * 0.88f);
                context.addLine(cx - curSpan, curY, cx + curSpan, curY, COLOR_GRID, 1.0f);
            }

            // Отрисовка 3D объекта (Куб)
            float angle = isPlaying ? (System.currentTimeMillis() % 4000L) / 4000.0f * (float) (Math.PI * 2) : 0.45f;
            drawCube(context, cx, cy - 20.0f, 44.0f, angle);

            // Статус внизу, если пауза/игра
            if (isPlaying) {
                // Запрос непрерывной перерисовки для плавной анимации
                gameCanvas.invalidate(InvalidationFlags.VISUAL);
            } else {
                String hint = "Play mode stopped. Press ▶ in toolbar to run.";
                context.addText(hint, x + 8.0f, y + h - 20.0f, w - 16.0f, 18.0f, UnityStyles.TEXT_MUTED);
            }

            // Оверлей статистики (Unity Statistics panel)
            if (statsVisible) {
                drawStatsOverlay(context, x + w - 210.0f, y + 10.0f, 200.0f, 130.0f);
            }
        });

        addChild(gameCanvas);
    }

    private void drawCube(dev.sixik.unigui.api.render.RenderContext context, float cx, float cy, float s, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Вершины изометрического куба с вращением
        float rx = s * 0.866f * cos;
        float ry = s * 0.5f;

        // Верхняя грань
        context.pathClear();
        context.pathLineTo(cx, cy - s);
        context.pathLineTo(cx + rx, cy - s * 0.5f + sin * 6.0f);
        context.pathLineTo(cx, cy + sin * 4.0f);
        context.pathLineTo(cx - rx, cy - s * 0.5f - sin * 6.0f);
        context.pathFillConvex(UnityStyles.CUBE_BLUE_TOP);

        // Левая грань
        context.pathClear();
        context.pathLineTo(cx - rx, cy - s * 0.5f - sin * 6.0f);
        context.pathLineTo(cx, cy + sin * 4.0f);
        context.pathLineTo(cx, cy + s + sin * 4.0f);
        context.pathLineTo(cx - rx, cy + s * 0.5f - sin * 6.0f);
        context.pathFillConvex(UnityStyles.CUBE_BLUE_LEFT);

        // Правая грань
        context.pathClear();
        context.pathLineTo(cx, cy + sin * 4.0f);
        context.pathLineTo(cx + rx, cy - s * 0.5f + sin * 6.0f);
        context.pathLineTo(cx + rx, cy + s * 0.5f + sin * 6.0f);
        context.pathLineTo(cx, cy + s + sin * 4.0f);
        context.pathFillConvex(UnityStyles.CUBE_BLUE_RIGHT);
    }

    private void drawStatsOverlay(dev.sixik.unigui.api.render.RenderContext context, float sx, float sy, float sw, float sh) {
        context.rectFilled(sx, sy, sw, sh, STATS_BG);
        context.addRect(sx, sy, sw, sh, STATS_BORDER, 1.0f);

        float px = sx + 8.0f;
        float py = sy + 6.0f;
        float tw = sw - 16.0f;

        context.addText("Statistics", px, py, tw, 18.0f, UnityStyles.TEXT_BOLD);
        py += 18.0f;
        context.addLine(sx, py, sx + sw, py, STATS_BORDER, 1.0f);
        py += 6.0f;

        String fps = isPlaying ? "60.0 FPS (16.6ms)" : "0.0 FPS (paused)";
        context.addText(fps, px, py, tw, 16.0f, isPlaying ? UnityStyles.PLAY_GREEN : UnityStyles.TEXT_MUTED);
        py += 15.0f;

        context.addText("Batches: 14   Saved: 6", px, py, tw, 15.0f, UnityStyles.TEXT_PRIMARY);
        py += 14.0f;

        context.addText("Tris: 2.4k    Verts: 4.8k", px, py, tw, 15.0f, UnityStyles.TEXT_PRIMARY);
        py += 14.0f;

        context.addText("Screen: 1920x1080 - 1.2 MB", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
        py += 14.0f;

        context.addText("SetPass calls: 12", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
        py += 14.0f;

        context.addText("Shadow casters: 1", px, py, tw, 15.0f, UnityStyles.TEXT_MUTED);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        gameCanvas.invalidate(InvalidationFlags.VISUAL);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private static Button createToolBtn(String label) {
        Button btn = new Button(label);
        UnityStyles.styleButton(btn);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        return btn;
    }

    private static Button createToggleBtn(String label, boolean initial) {
        Button btn = new Button(label);
        btn.themeEnabled(false);
        btn.backgroundVisible(true);
        btn.background().set(initial ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
        btn.borderVisible(true);
        btn.borderColor().set(UnityStyles.BORDER_INPUT);
        btn.radius(2.0f);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.textColor().set(UnityStyles.TEXT_PRIMARY);
        btn.layout(style -> style.height(18.0f).padding(5.0f, 0.0f));
        return btn;
    }

    private static void updateToggleVisual(Button btn, boolean active) {
        btn.background().set(active ? UnityStyles.BG_BUTTON_ACTIVE : UnityStyles.BG_INPUT);
    }
}
