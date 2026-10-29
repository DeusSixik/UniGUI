package dev.sixik.unigui.testmod.client.ui.unity;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.NumberField;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Элементы управления Scene View в стиле Unity, которые живут внутри панели Scene и не могут из неё выйти:
 * верхняя строка настроек (Center/Local, сетка, привязка, режим отображения, Gizmos). Плавающая панель
 * инструментов с докингом - {@link UnityToolsOverlay}.
 */
final class UnitySceneToolbar {
    /** Инструменты трансформации Scene View. */
    enum Tool {
        HAND, MOVE, ROTATE, SCALE, RECT, TRANSFORM
    }

    private static final float BAR_HEIGHT = 26.0f;
    private static final float BUTTON_HEIGHT = 20.0f;

    private UnitySceneToolbar() {
    }

    /**
     * Верхняя строка Scene View с горизонтальной прокруткой.
     *
     * <p>Если элементы не помещаются, по краям появляются кнопки со стрелками; кнопка видна, только пока в её
     * сторону есть что прокручивать, и при удержании двигает строку. Колесо мыши тоже прокручивает строку.</p>
     *
     * @return готовая панель высотой {@value #BAR_HEIGHT} px
     */
    static StackPanel createTopBar() {
        StackPanel host = new StackPanel();
        host.layout(style -> style.widthPercent(100.0f).height(BAR_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        Box background = new Box();
        background.themeEnabled(false);
        background.borderVisible(false);
        background.backgroundVisible(true);
        background.background().set(UnityStyles.BG_TOOLBAR);
        background.layout(style -> style.sizePercent(100.0f, 100.0f));
        host.addChild(background);

        ScrollView scroll = new ScrollView(buildTopBarContent());
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f)
                .overflowX(Overflow.AUTO).overflowY(Overflow.HIDDEN));
        // Полосы прокрутки не нужны: их заменяют стрелки по краям.
        scroll.scrollStep(48.0f);
        scroll.scrollbarTrackColor().set(UnityStyles.BG_TOOLBAR.r(), UnityStyles.BG_TOOLBAR.g(),
                UnityStyles.BG_TOOLBAR.b(), 0.0f);
        scroll.scrollbarThumbColor().set(0.0f, 0.0f, 0.0f, 0.0f);
        scroll.scrollbarThumbHoverColor().set(0.0f, 0.0f, 0.0f, 0.0f);
        scroll.horizontalScrollBar().visibility(Visibility.HIDDEN);
        host.addChild(scroll);

        EdgeArrow left = new EdgeArrow(scroll, -1);
        EdgeArrow right = new EdgeArrow(scroll, 1);
        // Скрытые виджеты не получают tick, поэтому показом стрелок управляет всегда видимый наблюдатель.
        host.addChild(new ArrowWatcher(scroll, left, right, host));
        host.addChild(left);
        host.addChild(right);
        return host;
    }

    /** Кнопка-стрелка у края панели: при удержании прокручивает панель в свою сторону. */
    private static final class EdgeArrow extends Button {
        private static final float SPEED = 420.0f;
        private final ScrollView scroll;
        private final int direction;

        EdgeArrow(ScrollView scroll, int direction) {
            this.scroll = scroll;
            this.direction = direction;
            themeEnabled(false);
            boxVisualEnabled(false);
            focusRingVisible(false);
            focusable(false);
            visibility(Visibility.HIDDEN);
            layout(style -> style.size(16.0f, BAR_HEIGHT)
                    .align(direction < 0 ? Alignment.START : Alignment.END, Alignment.START)
                    .flexGrow(0.0f).flexShrink(0.0f));
            renderer(WidgetRender.of(Button.class, (draw, btn) -> {
                RectView b = btn.layoutBounds();
                ColorView bg = btn.pressed() ? UnityStyles.BG_WORKSPACE
                        : btn.hovered() ? UnityStyles.BG_BUTTON_HOVER : UnityStyles.BG_BUTTON;
                draw.rectFilled(b.x(), b.y(), b.width(), b.height(), bg);
                float edge = direction < 0 ? b.x() + b.width() - 1.0f : b.x();
                draw.rectFilled(edge, b.y(), 1.0f, b.height(), UnityStyles.BORDER_DEFAULT);
                float cx = b.x() + b.width() * 0.5f;
                float cy = b.y() + b.height() * 0.5f;
                float w = 3.0f;
                float h = 5.0f;
                draw.pathClear();
                if (direction < 0) {
                    draw.pathLineTo(cx + w, cy - h);
                    draw.pathLineTo(cx - w, cy);
                    draw.pathLineTo(cx + w, cy + h);
                } else {
                    draw.pathLineTo(cx - w, cy - h);
                    draw.pathLineTo(cx + w, cy);
                    draw.pathLineTo(cx - w, cy + h);
                }
                draw.pathFillConvex(btn.hovered() ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_LABEL);
            }));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            if (!pressed()) return;
            float dt = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
            scroll.scrollBy(direction * SPEED * dt, 0.0f);
        }
    }

    /** Невидимый наблюдатель: показывает стрелку, только если в её сторону панель можно прокрутить. */
    private static final class ArrowWatcher extends Box {
        private final ScrollView scroll;
        private final EdgeArrow left;
        private final EdgeArrow right;
        private final Widget host;

        ArrowWatcher(ScrollView scroll, EdgeArrow left, EdgeArrow right, Widget host) {
            this.scroll = scroll;
            this.left = left;
            this.right = right;
            this.host = host;
            themeEnabled(false);
            borderVisible(false);
            layout(style -> style.size(1.0f, 1.0f).align(Alignment.CENTER, Alignment.CENTER)
                    .flexGrow(0.0f).flexShrink(0.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            boolean canLeft = scroll.scrollX() > 0.5f;
            boolean canRight = scroll.scrollX() < scroll.maxScrollX() - 0.5f;
            boolean changed = update(left, canLeft);
            changed |= update(right, canRight);
            if (changed) {
                host.invalidate(InvalidationFlags.VISUAL);
            }
        }

        private static boolean update(EdgeArrow arrow, boolean visible) {
            Visibility target = visible ? Visibility.VISIBLE : Visibility.HIDDEN;
            if (arrow.visibility() == target) return false;
            arrow.visibility(target);
            return true;
        }
    }

    private static HBox buildTopBarContent() {
        HBox bar = new HBox();
        bar.themeEnabled(false);
        bar.borderVisible(false);
        bar.spacing(4.0f);
        // Ширина по содержимому и без сжатия: именно так строка выходит за видимую область и прокручивается.
        bar.layout(style -> style.height(BAR_HEIGHT)
                .padding(6.0f, 0.0f, 6.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        bar.addChild(dropdown("Center", UnityIcons.CUBE_OUTLINE_ICON, List.of("Center", "Pivot")));
        bar.addChild(dropdown("Local", UnityIcons.CUBE_OUTLINE_ICON, List.of("Local", "Global")));
        bar.addChild(verticalSeparator());

        boolean[] grid = {true};
        bar.addChild(iconToggle(UnityIcons.GRID_ICON, 24.0f, () -> grid[0], () -> grid[0] = !grid[0]));
        bar.addChild(numberField("1", 36.0f));
        bar.addChild(verticalSeparator());

        // Привязка перемещения, вращения и масштаба.
        for (String[] snap : new String[][]{{"0.25"}, {"15"}, {"1"}}) {
            bar.addChild(numberField(snap[0], 40.0f));
        }

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.width(16.0f).height(1.0f).flexGrow(1.0f).flexShrink(0.0f));
        bar.addChild(spacer);

        bar.addChild(dropdown("Shaded", null, List.of("Shaded", "Wireframe", "Shaded Wireframe")));
        boolean[] twoD = {false};
        bar.addChild(textToggle("2D", 26.0f, () -> twoD[0], () -> twoD[0] = !twoD[0]));
        boolean[] light = {true};
        bar.addChild(iconToggle(UnityIcons.SUN_ICON, 24.0f, () -> light[0], () -> light[0] = !light[0]));
        boolean[] audio = {false};
        bar.addChild(iconToggle(UnityIcons.SPEAKER_ICON, 24.0f, () -> audio[0], () -> audio[0] = !audio[0]));
        bar.addChild(dropdown("Effects", null, List.of("Skybox", "Fog", "Flares", "Always Refresh")));
        bar.addChild(dropdown("Gizmos", null, List.of("3D Icons", "Selection Outline", "Selection Wire", "Grid")));
        return bar;
    }

    /** Фон кнопки при наведении и нажатии. */
    static void paintHover(dev.sixik.unigui.api.render.DrawScope draw, Button btn, RectView b) {
        if (btn.pressed()) {
            draw.rectFilled(b.x(), b.y(), b.width(), b.height(), UnityStyles.BG_WORKSPACE);
        } else if (btn.hovered()) {
            draw.rectFilled(b.x(), b.y(), b.width(), b.height(), UnityStyles.BG_HEADER_HOVER);
        }
    }

    private static Button flatButton(float width, float height) {
        Button button = new Button();
        button.themeEnabled(false);
        button.boxVisualEnabled(false);
        button.focusRingVisible(false);
        button.focusable(false);
        button.layout(style -> style.size(width, height).flexGrow(0.0f).flexShrink(0.0f));
        return button;
    }

    /** Кнопка-переключатель с иконкой: выбранное состояние подсвечивается синим. */
    private static Button iconToggle(UiIcon icon, float width, BooleanSupplier selected, Runnable toggle) {
        Button button = flatButton(width, BUTTON_HEIGHT);
        button.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
            RectView b = btn.layoutBounds();
            boolean on = selected.getAsBoolean();
            if (on) {
                draw.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 2.0f, UnityStyles.SELECTION_BLUE);
            } else if (btn.hovered()) {
                draw.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 2.0f, UnityStyles.BG_HEADER_HOVER);
            }
            ColorView tint = on || btn.hovered() ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_LABEL;
            icon.draw(draw, b.x() + (b.width() - 14.0f) * 0.5f, b.y() + (b.height() - 14.0f) * 0.5f, 14.0f, tint);
        }));
        button.onClick(e -> toggle.run());
        return button;
    }

    /** Текстовая кнопка-переключатель. */
    private static Button textToggle(String text, float width, BooleanSupplier selected, Runnable toggle) {
        Button button = flatButton(width, BUTTON_HEIGHT);
        button.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
            RectView b = btn.layoutBounds();
            boolean on = selected.getAsBoolean();
            if (on) {
                draw.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 2.0f, UnityStyles.SELECTION_BLUE);
            } else if (btn.hovered()) {
                draw.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 2.0f, UnityStyles.BG_HEADER_HOVER);
            }
            drawCentered(draw, text, b, on || btn.hovered() ? UnityStyles.TEXT_BOLD : UnityStyles.TEXT_LABEL);
        }));
        button.onClick(e -> toggle.run());
        return button;
    }

    private static void drawCentered(dev.sixik.unigui.api.render.DrawScope draw, String text, RectView b,
                                     ColorView color) {
        RichText rich = RichText.plain(text);
        float textWidth = TextEngine.measureLineWidth(draw.context(), rich);
        float textHeight = TextEngine.measureTextHeight(draw.context(), rich);
        TextEngine.drawInline(draw, rich, b.x() + (b.width() - textWidth) * 0.5f,
                b.y() + Math.max(0.0f, b.height() - textHeight) * 0.5f, textWidth, textHeight, Paint.fill(color));
    }

    /**
     * Выпадающая кнопка с необязательной иконкой, подписью и стрелкой. Выбранный пункт становится подписью.
     */
    private static Button dropdown(String initial, UiIcon icon, List<String> options) {
        String[] current = {initial};
        float iconSpace = icon == null ? 0.0f : 18.0f;
        float width = 10.0f + iconSpace + TextEngine.measureLineWidth(longest(initial, options)) + 22.0f;
        Button button = flatButton(width, BUTTON_HEIGHT);
        button.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
            RectView b = btn.layoutBounds();
            ColorView bg = btn.pressed() ? UnityStyles.BG_BUTTON_ACTIVE
                    : btn.hovered() ? UnityStyles.BG_BUTTON_HOVER : UnityStyles.BG_BUTTON;
            draw.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 3.0f, bg);
            draw.roundedRectStroke(b.x(), b.y(), b.width(), b.height(), 3.0f, UnityStyles.BORDER_BUTTON, 1.0f);
            float x = b.x() + 6.0f;
            if (icon != null) {
                icon.draw(draw, x, b.y() + (b.height() - 13.0f) * 0.5f, 13.0f, UnityStyles.TEXT_PRIMARY);
                x += iconSpace;
            }
            RichText rich = RichText.plain(current[0]);
            float textHeight = TextEngine.measureTextHeight(draw.context(), rich);
            float available = Math.max(0.0f, b.x() + b.width() - 16.0f - x);
            draw.pushTextClip(x, b.y(), available, b.height());
            try {
                TextEngine.drawInline(draw, rich, x, b.y() + Math.max(0.0f, b.height() - textHeight) * 0.5f,
                        available, textHeight, Paint.fill(UnityStyles.TEXT_BOLD));
            } finally {
                draw.popClip();
            }
            UnityIcons.drawDropdownArrow(draw, b.x() + b.width() - 8.0f, b.y() + b.height() * 0.5f, 6.0f,
                    UnityStyles.TEXT_PRIMARY);
        }));
        button.onClick(e -> {
            OverlayLayer overlay = findOverlayLayer(button);
            if (overlay == null) return;
            ContextMenu menu = new ContextMenu();
            UnityStyles.styleContextMenu(menu);
            float menuWidth = Math.max(110.0f, width);
            for (String option : options) {
                UnityStyles.addContextMenuItem(menu, option, true, menuWidth, () -> {
                    current[0] = option;
                    button.invalidate(dev.sixik.unigui.api.core.InvalidationFlags.VISUAL);
                });
            }
            overlay.addOverlay(menu);
            RectView b = button.layoutBounds();
            menu.openAt(b.x(), b.y() + b.height() + 2.0f);
        });
        return button;
    }

    private static String longest(String initial, List<String> options) {
        String best = initial;
        for (String option : options) {
            if (option.length() > best.length()) best = option;
        }
        return best;
    }

    private static NumberField numberField(String value, float width) {
        NumberField field = new NumberField();
        UnityStyles.styleNumberField(field);
        field.value(Double.parseDouble(value));
        field.layout(style -> style.width(width).height(BUTTON_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        return field;
    }

    private static Box verticalSeparator() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.background().set(UnityStyles.SEPARATOR);
        line.borderVisible(false);
        line.layout(style -> style.size(1.0f, 16.0f).margin(3.0f, 0.0f).flexGrow(0.0f).flexShrink(0.0f));
        return line;
    }

    private static OverlayLayer findOverlayLayer(Widget start) {
        for (Widget current = start; current != null; current = current.parent()) {
            if (current instanceof OverlayLayer layer) return layer;
        }
        return null;
    }
}
