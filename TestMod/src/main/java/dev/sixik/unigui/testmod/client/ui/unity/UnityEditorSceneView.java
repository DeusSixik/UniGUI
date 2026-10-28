package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
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
 * Интерактивный видовой экран Scene View в стиле Unity:
 * сетка перспективы, ориентационный гизмо, конус фрустума камеры,
 * реагирующий на изменение FOV и Transform в Inspector.
 */
public class UnityEditorSceneView extends LinearBox {
    private final CanvasWidget viewportCanvas = new CanvasWidget();

    private float cameraFov = 60.0f;
    private float camX = 0.0f;
    private float camY = 1.0f;
    private float camZ = -10.0f;

    private float panX = 0.0f;
    private float panY = 0.0f;
    private boolean dragging = false;
    private float lastDragX = 0.0f;
    private float lastDragY = 0.0f;

    public UnityEditorSceneView() {
        super(Orientation.VERTICAL);
        spacing(0.0f);
        layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        // 1. Панель инструментов Scene View (Shaded, 2D, Light, Gizmos)
        HBox toolbar = new HBox();
        toolbar.spacing(6.0f);
        toolbar.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .padding(6.0f, 0.0f, 6.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        toolbar.addChild(createToolBtn("Shaded v"));
        toolbar.addChild(createToolBtn("2D"));
        toolbar.addChild(createToolBtn("☀"));
        toolbar.addChild(createToolBtn("🔊"));
        toolbar.addChild(createToolBtn("Effects v"));
        toolbar.addChild(createToolBtn("Gizmos v"));

        addChild(toolbar);
        addChild(UnityStyles.separatorLine());

        // 2. Canvas отрисовки 3D вьюпорта
        viewportCanvas.layout(style -> style.sizePercent(100.0f, 100.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        viewportCanvas.onDraw(context -> {
            float vx = viewportCanvas.layoutBounds().x();
            float vy = viewportCanvas.layoutBounds().y();
            float vw = viewportCanvas.layoutBounds().width();
            float vh = viewportCanvas.layoutBounds().height();

            // Тёмно-серый фон Unity Scene
            context.rectFilled(vx, vy, vw, vh, UnityStyles.BG_WORKSPACE);

            float cx = vx + vw * 0.5f + panX;
            float cy = vy + vh * 0.55f + panY;

            // 1. Перспективная сетка
            MutableColor gridColor = MutableColor.rgba255(48, 48, 48, 255);
            MutableColor axisColor = MutableColor.rgba255(64, 64, 64, 255);

            for (int i = -8; i <= 8; i++) {
                float lineX = cx + i * 28.0f;
                // Линии глубины с лёгкой перспективой
                float topOffset = (lineX - cx) * 0.5f;
                context.line(lineX - topOffset, cy - 90.0f, lineX + topOffset * 1.5f, cy + 90.0f,
                        Paint.fill(i == 0 ? axisColor : gridColor));

                // Горизонтальные линии
                float lineY = cy + i * 14.0f;
                float w = 240.0f + i * 16.0f;
                context.line(cx - w, lineY, cx + w, lineY,
                        Paint.fill(i == 0 ? axisColor : gridColor));
            }

            // Центр осей координат (красная X, зелёная Y, синяя Z)
            context.line(cx, cy, cx + 45.0f, cy + 8.0f, Paint.fill(UnityStyles.AXIS_X));
            context.line(cx, cy, cx, cy - 45.0f, Paint.fill(UnityStyles.AXIS_Y));
            context.line(cx, cy, cx - 35.0f, cy + 18.0f, Paint.fill(UnityStyles.AXIS_Z));

            // 2. Объект Main Camera и её усечённая пирамида обзора (Frustum)
            float camDrawX = cx + camX * 12.0f;
            float camDrawY = cy - 40.0f - camY * 10.0f + camZ * 4.0f;

            // Иконка камеры
            float cSize = 14.0f;
            context.rectFilled(camDrawX - cSize * 0.5f, camDrawY - cSize * 0.4f, cSize * 0.8f, cSize * 0.7f, UnityStyles.AXIS_Z);
            context.circleFilled(camDrawX - 1.0f, camDrawY - 1.0f, 2.5f, 2.5f, UnityStyles.TEXT_PRIMARY);

            // Фрустум (проволочный конус обзора камеры)
            // Угол и ширина зависят от cameraFov
            float fovRad = (float) Math.toRadians(cameraFov * 0.5f);
            float frustumLen = 130.0f;
            float spreadW = (float) (Math.tan(fovRad) * frustumLen * 0.85f);
            float spreadH = spreadW * 0.5625f; // 16:9

            float fx1 = camDrawX - spreadW;
            float fx2 = camDrawX + spreadW;
            float fy1 = camDrawY + frustumLen - spreadH;
            float fy2 = camDrawY + frustumLen + spreadH;

            MutableColor frustumColor = MutableColor.rgba255(240, 240, 240, 160);

            // 4 луча из камеры
            context.line(camDrawX, camDrawY, fx1, fy1, Paint.fill(frustumColor));
            context.line(camDrawX, camDrawY, fx2, fy1, Paint.fill(frustumColor));
            context.line(camDrawX, camDrawY, fx1, fy2, Paint.fill(frustumColor));
            context.line(camDrawX, camDrawY, fx2, fy2, Paint.fill(frustumColor));

            // Дальняя рамка
            context.line(fx1, fy1, fx2, fy1, Paint.fill(frustumColor));
            context.line(fx2, fy1, fx2, fy2, Paint.fill(frustumColor));
            context.line(fx2, fy2, fx1, fy2, Paint.fill(frustumColor));
            context.line(fx1, fy2, fx1, fy1, Paint.fill(frustumColor));

            // Подпись камеры
            context.circleFilled(camDrawX - 2.0f, camDrawY - 2.0f, 4.0f, 4.0f, UnityStyles.AUDIO_GOLD);

            // 3. 3D Ориентационный гизмо в правом верхнем углу (Cones X, Y, Z + Persp)
            float gx = vx + vw - 45.0f;
            float gy = vy + 45.0f;

            // Ось Y (вверх)
            context.line(gx, gy, gx, gy - 20.0f, Paint.fill(UnityStyles.AXIS_Y));
            context.circleFilled(gx - 3.0f, gy - 24.0f, 6.0f, 6.0f, UnityStyles.AXIS_Y);

            // Ось X (вправо)
            context.line(gx, gy, gx + 20.0f, gy + 4.0f, Paint.fill(UnityStyles.AXIS_X));
            context.circleFilled(gx + 18.0f, gy + 1.0f, 6.0f, 6.0f, UnityStyles.AXIS_X);

            // Ось Z (влево-вниз)
            context.line(gx, gy, gx - 16.0f, gy + 12.0f, Paint.fill(UnityStyles.AXIS_Z));
            context.circleFilled(gx - 20.0f, gy + 9.0f, 6.0f, 6.0f, UnityStyles.AXIS_Z);

            // Центр гизмо
            context.circleFilled(gx - 2.0f, gy - 2.0f, 4.0f, 4.0f, UnityStyles.TEXT_PRIMARY);
        });

        // Интерактивное перемещение камеры в Scene View
        viewportCanvas.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() == PointerButton.PRIMARY || event.button() == PointerButton.MIDDLE) {
                dragging = true;
                lastDragX = event.rootX();
                lastDragY = event.rootY();
                event.cancel();
            }
        });

        viewportCanvas.on(PointerMovedEvent.TYPE, event -> {
            if (dragging) {
                float dx = event.rootX() - lastDragX;
                float dy = event.rootY() - lastDragY;
                lastDragX = event.rootX();
                lastDragY = event.rootY();
                panX += dx;
                panY += dy;
                viewportCanvas.invalidate(InvalidationFlags.VISUAL);
                event.cancel();
            }
        });

        viewportCanvas.on(PointerReleasedEvent.TYPE, event -> {
            if (dragging) {
                dragging = false;
                event.cancel();
            }
        });

        addChild(viewportCanvas);
    }

    private Button createToolBtn(String text) {
        Button btn = new Button(text);
        btn.themeEnabled(false);
        btn.boxVisualEnabled(false);
        btn.focusRingVisible(false);
        btn.focusable(false);
        btn.textColor().set(UnityStyles.TEXT_LABEL);
        btn.layout(style -> style.height(18.0f).padding(4.0f, 0.0f));
        return btn;
    }

    public void updateCameraFov(float fov) {
        this.cameraFov = fov;
        viewportCanvas.invalidate(InvalidationFlags.VISUAL);
    }

    public void updateCameraPosition(float x, float y, float z) {
        this.camX = x;
        this.camY = y;
        this.camZ = z;
        viewportCanvas.invalidate(InvalidationFlags.VISUAL);
    }
}
