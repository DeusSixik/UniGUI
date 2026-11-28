package dev.sixik.unigui.demo.ui.unity;

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
import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo.Pivot;
import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo.Space;
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
import java.util.function.Consumer;

/**
 * Unity-style Scene View controls hosted inside the Scene panel without escaping its bounds:
 * top settings bar (Center/Local, grid, snapping, draw mode, Gizmos). The floating dockable
 * toolbar is {@link UnityToolsOverlay}.
 */
final class UnitySceneToolbar {
    /**
     * Scene View transform tools.
     */
    enum Tool {
        HAND, MOVE, ROTATE, SCALE, RECT, TRANSFORM
    }

    private static final float BAR_HEIGHT = 26.0f;
    private static final float BUTTON_HEIGHT = 20.0f;

    private UnitySceneToolbar() {
    }

    /**
     * Horizontally scrollable Scene View top bar.
     *
     * <p>When controls overflow, arrow buttons appear at the edges; each button is visible only while
     * content can scroll in that direction and scrolls the bar while held. Mouse wheel scrolling also works.</p>
     *
     * @param options   settings edited by the bar
     * @param onChanged runs after any setting changes
     * @return top bar with height {@value #BAR_HEIGHT} px
     */
    static StackPanel createTopBar(UnitySceneViewOptions options, Runnable onChanged) {
        StackPanel host = new StackPanel();
        host.layout(style -> style.widthPercent(100.0f).height(BAR_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        Box background = new Box();
        background.themeEnabled(false);
        background.borderVisible(false);
        background.backgroundVisible(true);
        background.background().set(UnityStyles.BG_TOOLBAR);
        background.layout(style -> style.sizePercent(100.0f, 100.0f));
        host.addChild(background);

        ScrollView scroll = new ScrollView(buildTopBarContent(options, onChanged));
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f)
                .overflowX(Overflow.AUTO).overflowY(Overflow.HIDDEN));
        // Scrollbars are omitted and replaced by edge arrow buttons.
        scroll.scrollStep(48.0f);
        scroll.scrollbarTrackColor().set(UnityStyles.BG_TOOLBAR.r(), UnityStyles.BG_TOOLBAR.g(),
                UnityStyles.BG_TOOLBAR.b(), 0.0f);
        scroll.scrollbarThumbColor().set(0.0f, 0.0f, 0.0f, 0.0f);
        scroll.scrollbarThumbHoverColor().set(0.0f, 0.0f, 0.0f, 0.0f);
        scroll.horizontalScrollBar().visibility(Visibility.HIDDEN);
        host.addChild(scroll);

        EdgeArrow left = new EdgeArrow(scroll, -1);
        EdgeArrow right = new EdgeArrow(scroll, 1);
        // Hidden widgets do not receive tick, so an always-visible watcher controls arrow visibility.
        host.addChild(new ArrowWatcher(scroll, left, right, host));
        host.addChild(left);
        host.addChild(right);
        return host;
    }

    /**
     * Edge arrow button: scrolls the toolbar in its direction while held.
     */
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

    /**
     * Invisible watcher: shows an arrow only when the bar can scroll in that direction.
     */
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

    /**
     * Builds the controls hosted in the scrollable toolbar.
     *
     * @param options   settings edited by the controls
     * @param onChanged runs after a setting changes
     * @return row containing the scene view controls
     */
    private static HBox buildTopBarContent(UnitySceneViewOptions options, Runnable onChanged) {
        HBox bar = new HBox();
        bar.themeEnabled(false);
        bar.borderVisible(false);
        bar.spacing(4.0f);
        // Size to content without shrinking so the row can overflow and scroll horizontally.
        bar.layout(style -> style.height(BAR_HEIGHT)
                .padding(6.0f, 0.0f, 6.0f, 0.0f)
                .alignItems(Align.CENTER)
                .flexGrow(0.0f).flexShrink(0.0f));

        bar.addChild(dropdown(options.pivot == Pivot.CENTER ? "Center" : "Pivot", UnityIcons.CUBE_OUTLINE_ICON,
                List.of("Center", "Pivot"), option -> {
                    options.pivot = "Pivot".equals(option) ? Pivot.PIVOT : Pivot.CENTER;
                    onChanged.run();
                }));
        bar.addChild(dropdown(options.space == Space.LOCAL ? "Local" : "Global", UnityIcons.CUBE_OUTLINE_ICON,
                List.of("Local", "Global"), option -> {
                    options.space = "Local".equals(option) ? Space.LOCAL : Space.GLOBAL;
                    onChanged.run();
                }));
        bar.addChild(verticalSeparator());

        bar.addChild(iconToggle(UnityIcons.GRID_ICON, 24.0f, () -> options.grid, () -> {
            options.grid = !options.grid;
            onChanged.run();
        }));
        bar.addChild(verticalSeparator());

        // Snapping: the magnet toggles it persistently, holding Ctrl enables it while dragging.
        bar.addChild(iconToggle(UnityIcons.SNAP_ICON, 24.0f, () -> options.snap, () -> {
            options.snap = !options.snap;
            onChanged.run();
        }));
        bar.addChild(iconLabel(UnityIcons.TOOL_MOVE_ICON));
        bar.addChild(snapField(options.moveStep, value -> options.moveStep = (float) value, onChanged));
        bar.addChild(iconLabel(UnityIcons.TOOL_ROTATE_ICON));
        bar.addChild(snapField(options.rotateStep, value -> options.rotateStep = (float) value, onChanged));
        bar.addChild(iconLabel(UnityIcons.TOOL_SCALE_ICON));
        bar.addChild(snapField(options.scaleStep, value -> options.scaleStep = (float) value, onChanged));

        Box spacer = new Box();
        spacer.themeEnabled(false);
        spacer.layout(style -> style.width(16.0f).height(1.0f).flexGrow(1.0f).flexShrink(0.0f));
        bar.addChild(spacer);

        List<String> modes = new java.util.ArrayList<>();
        for (UnitySceneViewOptions.DrawMode mode : UnitySceneViewOptions.DrawMode.values()) modes.add(mode.label());
        bar.addChild(dropdown(options.drawMode.label(), null, modes, option -> {
            options.drawMode = UnitySceneViewOptions.DrawMode.fromLabel(option);
            onChanged.run();
        }));
        boolean[] twoD = {false};
        bar.addChild(textToggle("2D", 26.0f, () -> twoD[0], () -> twoD[0] = !twoD[0]));
        bar.addChild(iconToggle(UnityIcons.SUN_ICON, 24.0f, () -> options.lighting, () -> {
            options.lighting = !options.lighting;
            onChanged.run();
        }));
        boolean[] audio = {false};
        bar.addChild(iconToggle(UnityIcons.SPEAKER_ICON, 24.0f, () -> audio[0], () -> audio[0] = !audio[0]));
        bar.addChild(dropdown("Effects", null, List.of("Skybox", "Fog", "Flares", "Always Refresh")));
        bar.addChild(dropdown("Gizmos", null, List.of("3D Icons", "Selection Outline", "Selection Wire", "Grid")));
        return bar;
    }

    /**
     * Small non-interactive icon placed before a snap field.
     *
     * @param icon glyph to draw
     * @return 14 px wide label
     */
    private static Button iconLabel(UiIcon icon) {
        Button label = flatButton(14.0f, BUTTON_HEIGHT);
        label.enabled(false);
        label.renderer(WidgetRender.of(Button.class, (draw, btn) -> {
            RectView b = btn.layoutBounds();
            icon.draw(draw, b.x() + 1.0f, b.y() + (b.height() - 12.0f) * 0.5f, 12.0f, UnityStyles.TEXT_MUTED);
        }));
        return label;
    }

    /**
     * Numeric snap step that ignores values that are not positive.
     *
     * @param value     initial step
     * @param apply     stores an accepted step
     * @param onChanged runs after a step is stored
     * @return the field
     */
    private static NumberField snapField(float value, java.util.function.DoubleConsumer apply, Runnable onChanged) {
        NumberField field = numberField(Float.toString(value), 40.0f);
        field.range(0.0, 360.0);
        field.step(value >= 1 ? 1.0 : 0.05);
        field.onValueChanged(event -> {
            if (!(event.newValue() > 0)) return;
            apply.accept(event.newValue());
            onChanged.run();
        });
        return field;
    }

    /**
     * Button background on hover and press.
     */
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

    /**
     * Icon toggle button: highlights the active state in blue.
     */
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

    /**
     * Text toggle button.
     */
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
     * Dropdown button with an optional icon, label, and arrow. The selected item becomes the label.
     */
    private static Button dropdown(String initial, UiIcon icon, List<String> options) {
        return dropdown(initial, icon, options, option -> {
        });
    }

    /**
     * Builds a dropdown that updates its label and notifies the caller on selection.
     *
     * @param initial    starting label
     * @param icon       optional icon
     * @param options    labels shown in the menu
     * @param onSelected receives the chosen label
     * @return dropdown button
     */
    private static Button dropdown(String initial, UiIcon icon, List<String> options, Consumer<String> onSelected) {
        float iconSpace = icon == null ? 0.0f : 18.0f;
        float width = 10.0f + iconSpace + TextEngine.measureLineWidth(longest(initial, options)) + 22.0f;
        Button button = flatButton(width, BUTTON_HEIGHT);
        button.text(initial);
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
            RichText rich = RichText.plain(btn.text());
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
                    button.text(option);
                    onSelected.accept(option);
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
