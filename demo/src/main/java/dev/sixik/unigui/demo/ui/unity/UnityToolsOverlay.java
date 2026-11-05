package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.demo.ui.unity.UnitySceneToolbar.Tool;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Плавающая панель инструментов Scene View с докингом внутри видового экрана, как оверлеи Unity.
 *
 * <p>Панель можно взять за «ручку» (две черты) и перетащить. Пока она в руке, поверх Scene подсвечиваются
 * зоны докинга: четыре угла, левый и правый края (панель становится вертикальной) и верхний и нижний края
 * (панель становится горизонтальной). Отпущенная над зоной панель встаёт в неё, отпущенная в другом месте
 * остаётся там, где её оставили. Выйти за границы Scene панель не может.</p>
 */
final class UnityToolsOverlay {
    /** Зоны докинга панели внутри Scene View. */
    enum Zone {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, RIGHT, TOP, BOTTOM
    }

    private static final float EDGE_MARGIN = 8.0f;
    private static final float CORNER_ZONE = 64.0f;
    private static final float STRIP_ZONE = 40.0f;
    private static final float PADDING = 3.0f;

    private static final MutableColor ZONE_IDLE = MutableColor.rgba(0.30f, 0.65f, 0.85f, 0.35f);
    private static final MutableColor ZONE_HOT = MutableColor.rgba(0.35f, 0.75f, 0.95f, 0.75f);

    private final StackPanel host;
    private final LinearBox panel = new LinearBox(Orientation.VERTICAL);
    private final CanvasWidget grip = new CanvasWidget();
    private final CanvasWidget zonesCanvas = new CanvasWidget();
    private final Button modeButton = new Button();
    private final List<Button> toolButtons = new ArrayList<>();

    private Tool selected;
    private boolean dragging;
    private float grabX;
    private float grabY;
    private Zone hotZone;

    /**
     * Создаёт панель и добавляет её вместе со слоем зон докинга в {@code host}.
     *
     * @param host      контейнер видового экрана; панель ограничена его областью
     * @param initial   выбранный сначала инструмент
     * @param onChanged вызывается при выборе другого инструмента
     */
    UnityToolsOverlay(StackPanel host, Tool initial, Consumer<Tool> onChanged) {
        this.host = host;
        this.selected = initial;

        buildPanel(onChanged);
        buildZonesCanvas();
        applyOrientation(Orientation.VERTICAL);
        applyZone(Zone.TOP_LEFT);

        host.addChild(zonesCanvas);
        host.addChild(panel);
    }

    /** @return текущий выбранный инструмент */
    Tool selectedTool() {
        return selected;
    }

    private void buildPanel(Consumer<Tool> onChanged) {
        panel.themeEnabled(false);
        panel.spacing(0.0f);
        panel.background(UnityStyles.BG_TOOLBAR);
        panel.border(UnityStyles.BORDER_DEFAULT);
        panel.radius(3.0f);

        grip.onDraw(context -> {
            DrawScope draw = new DrawScope(context, grip.transform(), grip.layoutBounds());
            RectView b = grip.layoutBounds();
            float cx = b.x() + b.width() * 0.5f;
            float cy = b.y() + b.height() * 0.5f;
            if (panel.orientation() == Orientation.VERTICAL) {
                draw.rectFilled(cx - 7.0f, cy - 2.0f, 14.0f, 1.0f, UnityStyles.TEXT_MUTED);
                draw.rectFilled(cx - 7.0f, cy + 1.0f, 14.0f, 1.0f, UnityStyles.TEXT_MUTED);
            } else {
                draw.rectFilled(cx - 2.0f, cy - 7.0f, 1.0f, 14.0f, UnityStyles.TEXT_MUTED);
                draw.rectFilled(cx + 1.0f, cy - 7.0f, 1.0f, 14.0f, UnityStyles.TEXT_MUTED);
            }
        });
        installDrag();
        panel.addChild(grip);

        modeButton.themeEnabled(false);
        modeButton.boxVisualEnabled(false);
        modeButton.focusRingVisible(false);
        modeButton.focusable(false);
        modeButton.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
            RectView b = btn.layoutBounds();
            UnitySceneToolbar.paintHover(draw, btn, b);
            ColorView tint = btn.hovered() ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_LABEL;
            UnityIcons.CUBE_OUTLINE_ICON.draw(draw, b.x() + 3.0f, b.y() + (b.height() - 14.0f) * 0.5f, 14.0f, tint);
            UnityIcons.drawDropdownArrow(draw, b.x() + b.width() - 6.0f, b.y() + b.height() * 0.5f, 6.0f, tint);
        }));
        panel.addChild(modeButton);

        UiIcon[] icons = {
                UnityIcons.TOOL_HAND_ICON, UnityIcons.TOOL_MOVE_ICON, UnityIcons.TOOL_ROTATE_ICON,
                UnityIcons.TOOL_SCALE_ICON, UnityIcons.TOOL_RECT_ICON, UnityIcons.TOOL_TRANSFORM_ICON
        };
        Tool[] tools = Tool.values();
        for (int i = 0; i < tools.length; i++) {
            Tool tool = tools[i];
            UiIcon icon = icons[i];
            Button button = new Button();
            button.themeEnabled(false);
            button.boxVisualEnabled(false);
            button.focusRingVisible(false);
            button.focusable(false);
            button.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
                RectView b = btn.layoutBounds();
                boolean on = selected == tool;
                if (on) {
                    draw.rectFilled(b.x(), b.y(), b.width(), b.height(), UnityStyles.SELECTION_BLUE);
                } else {
                    UnitySceneToolbar.paintHover(draw, btn, b);
                }
                ColorView tint = on || btn.hovered() ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_LABEL;
                icon.draw(draw, b.x() + (b.width() - 16.0f) * 0.5f, b.y() + (b.height() - 16.0f) * 0.5f, 16.0f, tint);
            }));
            button.onClick(e -> {
                if (selected == tool) return;
                selected = tool;
                // Каждая кнопка читает selected при отрисовке, поэтому перерисовать нужно все, а не только нажатую:
                // иначе прежний инструмент остаётся подсвеченным, пока на него не наведут мышь.
                for (Button other : toolButtons) {
                    other.invalidate(InvalidationFlags.VISUAL);
                }
                panel.invalidate(InvalidationFlags.VISUAL);
                if (onChanged != null) onChanged.accept(tool);
            });
            toolButtons.add(button);
            panel.addChild(button);
        }
    }

    private void buildZonesCanvas() {
        zonesCanvas.layout(style -> style.sizePercent(100.0f, 100.0f));
        zonesCanvas.visibility(Visibility.HIDDEN);
        zonesCanvas.onDraw(context -> {
            DrawScope draw = new DrawScope(context, zonesCanvas.transform(), zonesCanvas.layoutBounds());
            RectView host = zonesCanvas.layoutBounds();
            for (Zone zone : Zone.values()) {
                float[] r = zoneRect(zone, host.width(), host.height());
                ColorView color = zone == hotZone ? ZONE_HOT : ZONE_IDLE;
                draw.roundedRectFilled(host.x() + r[0] + 3.0f, host.y() + r[1] + 3.0f,
                        Math.max(0.0f, r[2] - 6.0f), Math.max(0.0f, r[3] - 6.0f), 8.0f, color);
            }
        });
    }

    private void installDrag() {
        grip.on(PointerPressedEvent.TYPE, event -> {
            if (event.button() != PointerButton.PRIMARY) return;
            RectView p = panel.layoutBounds();
            dragging = true;
            grabX = event.rootX() - p.x();
            grabY = event.rootY() - p.y();
            UIContext context = grip.uiContext();
            if (context != null) {
                context.capturePointer(event.pointerId(), grip);
            }
            zonesCanvas.visibility(Visibility.VISIBLE);
            zonesCanvas.invalidate(InvalidationFlags.VISUAL);
            event.cancel();
        });
        grip.on(PointerMovedEvent.TYPE, event -> {
            if (!dragging) return;
            followPointer(event.rootX(), event.rootY());
            event.cancel();
        });
        grip.on(PointerReleasedEvent.TYPE, event -> {
            if (!dragging || event.button() != PointerButton.PRIMARY) return;
            dragging = false;
            UIContext context = grip.uiContext();
            if (context != null) {
                context.releasePointer(event.pointerId(), grip);
            }
            Zone zone = hotZone;
            hotZone = null;
            zonesCanvas.visibility(Visibility.HIDDEN);
            if (zone != null) {
                applyZone(zone);
            }
            event.cancel();
        });
    }

    /** Панель следует за указателем внутри Scene, а зона под указателем подсвечивается. */
    private void followPointer(float rootX, float rootY) {
        RectView h = host.layoutBounds();
        RectView p = panel.layoutBounds();
        float x = clamp(rootX - grabX - h.x(), 0.0f, Math.max(0.0f, h.width() - p.width()));
        float y = clamp(rootY - grabY - h.y(), 0.0f, Math.max(0.0f, h.height() - p.height()));
        panel.layout(style -> style.align(Alignment.START, Alignment.START).margin(x, y, 0.0f, 0.0f));

        hotZone = zoneAt(rootX - h.x(), rootY - h.y(), h.width(), h.height());
        zonesCanvas.invalidate(InvalidationFlags.VISUAL);
    }

    private static Zone zoneAt(float x, float y, float width, float height) {
        for (Zone zone : Zone.values()) {
            float[] r = zoneRect(zone, width, height);
            if (x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3]) return zone;
        }
        return null;
    }

    /** Прямоугольник зоны в координатах хоста: {@code {x, y, width, height}}. */
    private static float[] zoneRect(Zone zone, float width, float height) {
        float corner = Math.min(CORNER_ZONE, Math.min(width, height) / 3.0f);
        float strip = Math.min(STRIP_ZONE, corner);
        return switch (zone) {
            case TOP_LEFT -> new float[]{0.0f, 0.0f, corner, corner};
            case TOP_RIGHT -> new float[]{width - corner, 0.0f, corner, corner};
            case BOTTOM_LEFT -> new float[]{0.0f, height - corner, corner, corner};
            case BOTTOM_RIGHT -> new float[]{width - corner, height - corner, corner, corner};
            case LEFT -> new float[]{0.0f, corner, strip, Math.max(0.0f, height - corner * 2.0f)};
            case RIGHT -> new float[]{width - strip, corner, strip, Math.max(0.0f, height - corner * 2.0f)};
            case TOP -> new float[]{corner, 0.0f, Math.max(0.0f, width - corner * 2.0f), strip};
            case BOTTOM -> new float[]{corner, height - strip, Math.max(0.0f, width - corner * 2.0f), strip};
        };
    }

    /** Ставит панель в зону: боковые края делают её вертикальной, верхний и нижний - горизонтальной. */
    private void applyZone(Zone zone) {
        switch (zone) {
            case LEFT, RIGHT -> applyOrientation(Orientation.VERTICAL);
            case TOP, BOTTOM -> applyOrientation(Orientation.HORIZONTAL);
            default -> {
            }
        }
        float m = EDGE_MARGIN;
        panel.layout(style -> {
            switch (zone) {
                case TOP_LEFT -> style.align(Alignment.START, Alignment.START).margin(m, m, 0.0f, 0.0f);
                case TOP_RIGHT -> style.align(Alignment.END, Alignment.START).margin(0.0f, m, m, 0.0f);
                case BOTTOM_LEFT -> style.align(Alignment.START, Alignment.END).margin(m, 0.0f, 0.0f, m);
                case BOTTOM_RIGHT -> style.align(Alignment.END, Alignment.END).margin(0.0f, 0.0f, m, m);
                case LEFT -> style.align(Alignment.START, Alignment.CENTER).margin(m, 0.0f, 0.0f, 0.0f);
                case RIGHT -> style.align(Alignment.END, Alignment.CENTER).margin(0.0f, 0.0f, m, 0.0f);
                case TOP -> style.align(Alignment.CENTER, Alignment.START).margin(0.0f, m, 0.0f, 0.0f);
                case BOTTOM -> style.align(Alignment.CENTER, Alignment.END).margin(0.0f, 0.0f, 0.0f, m);
            }
        });
        panel.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /** Размеры панели и её элементов под вертикальную или горизонтальную раскладку. */
    private void applyOrientation(Orientation orientation) {
        boolean vertical = orientation == Orientation.VERTICAL;
        panel.orientation(orientation);
        float gripLong = 24.0f;
        float gripShort = 10.0f;
        float toolW = vertical ? 28.0f : 26.0f;
        float toolH = 24.0f;
        float modeW = vertical ? 28.0f : 34.0f;
        float count = toolButtons.size();
        float length = PADDING * 2.0f + gripShort + (vertical ? toolH : modeW)
                + (vertical ? toolH : toolW) * count;
        float thickness = vertical ? 34.0f : 30.0f;

        panel.layout(style -> style.size(vertical ? thickness : length, vertical ? length : thickness)
                .padding(PADDING, PADDING, PADDING, PADDING)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));
        grip.layout(style -> style.size(vertical ? gripLong : gripShort, vertical ? gripShort : gripLong)
                .flexGrow(0.0f).flexShrink(0.0f));
        modeButton.layout(style -> style.size(modeW, toolH).flexGrow(0.0f).flexShrink(0.0f));
        for (Button button : toolButtons) {
            button.layout(style -> style.size(toolW, toolH).flexGrow(0.0f).flexShrink(0.0f));
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
