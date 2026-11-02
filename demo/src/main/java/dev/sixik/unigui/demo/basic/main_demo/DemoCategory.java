package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.navigation.NavigationCategory;
import dev.sixik.unigui.widgets.navigation.NavigationView;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;

import java.util.List;

abstract class DemoCategory {
    private static final ColorView ICON_IDLE = UniGuiDemo.GalleryTemplate.color(176, 190, 204);
    private static final ColorView ICON_SELECTED = UniGuiDemo.GalleryTemplate.color(255, 255, 255);

    record Control(String id, String title, String description) {
    }

    /** Сколько строк кода видно без прокрутки: длиннее фрагмент прокручивается по вертикали. */
    private static final int CODE_MAX_VISIBLE_LINES = 10;

    protected UniGuiDemo.GalleryTemplate gallery;

    public final void init(UniGuiDemo.GalleryTemplate gallery) {
        this.gallery = gallery;
    }

    abstract String title();

    abstract boolean expandedByDefault();

    abstract List<Control> controls();

    abstract void addDemo(String controlId, VBox root);

    final void addNavigation(NavigationView navigation) {
        NavigationCategory category = navigation.addCategory(title(), expandedByDefault());
        for (Control control : controls()) {
            category.addItem(control.id(), control.title()).icon(controlIcon(control.id()));
        }
    }

    private static TreeViewNode.IconDraw controlIcon(String id) {
        return (draw, node, x, y, width, height) -> {
            boolean selected = node.owner() != null && node.owner().selectedNode() == node;
            Paint stroke = Paint.stroke(selected ? ICON_SELECTED : ICON_IDLE, 1.25f);
            ColorView fill = selected ? ICON_SELECTED : ICON_IDLE;
            float cx = x + width * 0.5f;
            float cy = y + height * 0.5f;
            switch (id) {
                case "button" -> {
                    draw.rect(x + 1.0f, y + 3.0f, width - 2.0f, height - 6.0f, stroke);
                    draw.line(x + 5.0f, cy, x + width - 5.0f, cy, stroke);
                }
                case "toggle" -> {
                    draw.rect(x + 1.0f, y + 4.0f, width - 2.0f, height - 8.0f, stroke);
                    draw.circleFilled(x + width - 7.0f, y + 5.5f, 5.0f, 5.0f, fill);
                }
                case "checkbox" -> {
                    draw.rect(x + 2.0f, y + 2.0f, width - 4.0f, height - 4.0f, stroke);
                    draw.line(x + 5.0f, cy, cx - 0.5f, cy + 3.0f, stroke);
                    draw.line(cx - 0.5f, cy + 3.0f, x + width - 4.5f, cy - 3.0f, stroke);
                }
                case "radio" -> {
                    draw.circle(x + 1.5f, y + 1.5f, width - 3.0f, height - 3.0f, stroke);
                    draw.circleFilled(cx - 2.5f, cy - 2.5f, 5.0f, 5.0f, fill);
                }
                case "slider" -> {
                    draw.line(x + 1.0f, cy, x + width - 1.0f, cy, stroke);
                    draw.circleFilled(x + 3.0f, cy - 3.0f, 6.0f, 6.0f, fill);
                }
                case "text", "password", "search" -> {
                    draw.rect(x + 1.0f, y + 3.0f, width - 2.0f, height - 6.0f, stroke);
                    draw.line(x + 5.0f, y + 5.5f, x + 5.0f, y + height - 5.5f, stroke);
                }
                case "grid" -> {
                    float cell = (width - 4.0f) * 0.5f - 1.0f;
                    draw.rect(x + 1.5f, y + 1.5f, cell, cell, stroke);
                    draw.rect(x + 2.5f + cell, y + 1.5f, cell, cell, stroke);
                    draw.rect(x + 1.5f, y + 2.5f + cell, cell, cell, stroke);
                    draw.rect(x + 2.5f + cell, y + 2.5f + cell, cell, cell, stroke);
                }
                case "tree" -> {
                    draw.line(x + 3.0f, y + 2.0f, x + 3.0f, y + height - 4.0f, stroke);
                    draw.line(x + 3.0f, y + 6.0f, x + width - 2.0f, y + 6.0f, stroke);
                    draw.line(x + 3.0f, y + height - 4.0f, x + width - 2.0f, y + height - 4.0f, stroke);
                }
                case "progress" -> {
                    draw.rect(x + 1.0f, y + 5.0f, width - 2.0f, height - 10.0f, stroke);
                    draw.rectFilled(x + 2.0f, y + 6.0f, (width - 4.0f) * 0.6f, height - 12.0f, fill);
                }
                case "spinner" -> {
                    draw.circle(x + 2.0f, y + 2.0f, width - 4.0f, height - 4.0f, stroke);
                    draw.line(cx, cy, cx, y + 3.5f, stroke);
                }
                case "combo-box" -> {
                    draw.rect(x + 1.0f, y + 3.0f, width - 2.0f, height - 6.0f, stroke);
                    draw.line(x + width - 8.0f, cy - 1.0f, x + width - 5.5f, cy + 1.5f, stroke);
                    draw.line(x + width - 5.5f, cy + 1.5f, x + width - 3.0f, cy - 1.0f, stroke);
                }
                case "typography" -> {
                    draw.line(x + 2.0f, y + height - 2.0f, cx, y + 2.0f, stroke);
                    draw.line(cx, y + 2.0f, x + width - 2.0f, y + height - 2.0f, stroke);
                    draw.line(x + 5.0f, cy + 2.5f, x + width - 5.0f, cy + 2.5f, stroke);
                }
                case "split-panel" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.line(cx, y + 2.0f, cx, y + height - 2.0f, stroke);
                }
                case "docking" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.line(x + 1.0f, y + 5.5f, x + width - 1.0f, y + 5.5f, stroke);
                    draw.line(x + 6.0f, y + 5.5f, x + 6.0f, y + height - 2.0f, stroke);
                }
                case "expandable-panel" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.line(x + 1.0f, y + 6.0f, x + width - 1.0f, y + 6.0f, stroke);
                    draw.line(x + 4.0f, y + height - 6.0f, cx, y + height - 4.0f, stroke);
                    draw.line(cx, y + height - 4.0f, x + width - 4.0f, y + height - 6.0f, stroke);
                }
                case "navigation-view" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.line(x + 6.0f, y + 2.0f, x + 6.0f, y + height - 2.0f, stroke);
                    draw.line(x + 3.0f, y + 5.5f, x + 4.5f, y + 5.5f, stroke);
                    draw.line(x + 3.0f, cy, x + 4.5f, cy, stroke);
                    draw.line(x + 3.0f, y + height - 5.5f, x + 4.5f, y + height - 5.5f, stroke);
                }
                case "virtual-table" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.line(x + 1.0f, y + 6.0f, x + width - 1.0f, y + 6.0f, stroke);
                    draw.line(x + 1.0f, y + height - 6.0f, x + width - 1.0f, y + height - 6.0f, stroke);
                    draw.line(cx, y + 2.0f, cx, y + height - 2.0f, stroke);
                }
                case "chart" -> {
                    draw.line(x + 2.0f, y + 2.0f, x + 2.0f, y + height - 2.0f, stroke);
                    draw.line(x + 2.0f, y + height - 2.0f, x + width - 1.0f, y + height - 2.0f, stroke);
                    draw.line(x + 4.0f, y + height - 5.0f, x + 7.0f, cy - 1.0f, stroke);
                    draw.line(x + 7.0f, cy - 1.0f, x + 10.0f, cy + 1.0f, stroke);
                    draw.line(x + 10.0f, cy + 1.0f, x + width - 2.0f, y + 3.5f, stroke);
                }
                case "sparkline" -> {
                    draw.line(x + 1.0f, cy + 3.0f, x + 5.0f, cy - 1.0f, stroke);
                    draw.line(x + 5.0f, cy - 1.0f, x + 8.0f, cy + 2.0f, stroke);
                    draw.line(x + 8.0f, cy + 2.0f, x + 11.0f, cy - 3.0f, stroke);
                    draw.line(x + 11.0f, cy - 3.0f, x + width - 1.0f, cy, stroke);
                }
                case "graph-view" -> {
                    draw.line(x + 4.0f, y + 4.0f, x + width - 4.0f, y + 5.0f, stroke);
                    draw.line(x + 4.0f, y + 4.0f, cx, y + height - 4.0f, stroke);
                    draw.line(x + width - 4.0f, y + 5.0f, cx, y + height - 4.0f, stroke);
                    draw.circleFilled(x + 1.5f, y + 1.5f, 5.0f, 5.0f, fill);
                    draw.circleFilled(x + width - 6.5f, y + 2.5f, 5.0f, 5.0f, fill);
                    draw.circleFilled(cx - 2.5f, y + height - 6.5f, 5.0f, 5.0f, fill);
                }
                case "node-graph" -> {
                    draw.rect(x + 1.0f, y + 2.0f, 5.0f, 4.0f, stroke);
                    draw.rect(x + width - 6.0f, y + height - 6.0f, 5.0f, 4.0f, stroke);
                    draw.line(x + 6.0f, y + 4.0f, cx, y + 4.0f, stroke);
                    draw.line(cx, y + 4.0f, cx, y + height - 4.0f, stroke);
                    draw.line(cx, y + height - 4.0f, x + width - 6.0f, y + height - 4.0f, stroke);
                }
                case "blueprint-graph" -> {
                    draw.rect(x + 1.0f, y + 2.0f, 6.0f, 5.0f, stroke);
                    draw.rect(x + width - 7.0f, y + height - 7.0f, 6.0f, 5.0f, stroke);
                    draw.line(x + 7.0f, y + 4.5f, x + width - 4.0f, y + 4.5f, stroke);
                    draw.line(x + width - 4.0f, y + 4.5f, x + width - 4.0f, y + height - 7.0f, stroke);
                    draw.circleFilled(x + 5.0f, y + 3.5f, 2.0f, 2.0f, fill);
                }
                case "world-canvas" -> {
                    draw.circle(x + 1.0f, y + 1.0f, width - 2.0f, height - 2.0f, stroke);
                    draw.line(x + 1.0f, cy, x + width - 1.0f, cy, stroke);
                    draw.circle(cx - 3.0f, y + 1.0f, 6.0f, height - 2.0f, stroke);
                }
                case "map-canvas" -> {
                    draw.circle(cx - 3.5f, y + 1.5f, 7.0f, 7.0f, stroke);
                    draw.line(cx - 3.0f, y + 7.0f, cx, y + height - 1.5f, stroke);
                    draw.line(cx + 3.0f, y + 7.0f, cx, y + height - 1.5f, stroke);
                    draw.circleFilled(cx - 1.0f, y + 4.0f, 2.0f, 2.0f, fill);
                }
                case "context-menu" -> {
                    draw.rect(x + 3.0f, y + 1.0f, width - 6.0f, height - 2.0f, stroke);
                    draw.line(x + 5.5f, y + 4.5f, x + width - 5.5f, y + 4.5f, stroke);
                    draw.line(x + 5.5f, cy, x + width - 5.5f, cy, stroke);
                    draw.line(x + 5.5f, y + height - 4.5f, x + width - 5.5f, y + height - 4.5f, stroke);
                }
                case "tooltip" -> {
                    draw.rect(x + 1.0f, y + 4.0f, width - 2.0f, height - 7.0f, stroke);
                    draw.line(x + 4.0f, y + 4.0f, x + 6.0f, y + 1.5f, stroke);
                    draw.line(x + 6.0f, y + 1.5f, x + 8.0f, y + 4.0f, stroke);
                    draw.line(x + 4.0f, cy + 1.0f, x + width - 4.0f, cy + 1.0f, stroke);
                }
                case "popup" -> {
                    draw.rect(x + 1.0f, y + 1.0f, width - 6.0f, height - 6.0f, stroke);
                    draw.rectFilled(x + 5.0f, y + 5.0f, width - 6.0f, height - 6.0f, fill);
                }
                case "toast" -> {
                    draw.rect(x + 1.0f, y + height - 8.0f, width - 2.0f, 6.0f, stroke);
                    draw.line(x + 4.0f, y + height - 5.0f, x + width - 6.0f, y + height - 5.0f, stroke);
                }
                case "animations" -> {
                    draw.circleFilled(x + width - 8.0f, cy - 3.5f, 7.0f, 7.0f, fill);
                    draw.line(x + 1.0f, y + 4.5f, x + 6.0f, y + 4.5f, stroke);
                    draw.line(x + 1.0f, cy, x + 4.5f, cy, stroke);
                    draw.line(x + 1.0f, y + height - 4.5f, x + 6.0f, y + height - 4.5f, stroke);
                }
                case "textures" -> {
                    draw.rect(x + 1.0f, y + 1.0f, width - 2.0f, height - 2.0f, stroke);
                    draw.rectFilled(x + 1.0f, y + 1.0f, cx - x - 1.0f, cy - y - 1.0f, fill);
                    draw.rectFilled(cx, cy, x + width - 1.0f - cx, y + height - 1.0f - cy, fill);
                }
                case "mc-items" -> {
                    draw.rect(x + 1.5f, y + 1.5f, width - 3.0f, height - 3.0f, stroke);
                    draw.rectFilled(x + 4.5f, y + 4.5f, width - 9.0f, height - 9.0f, fill);
                }
                case "mc-blocks" -> {
                    draw.line(cx, y + 1.0f, x + width - 1.5f, y + 4.5f, stroke);
                    draw.line(cx, y + 1.0f, x + 1.5f, y + 4.5f, stroke);
                    draw.line(cx, cy - 1.0f, x + width - 1.5f, y + 4.5f, stroke);
                    draw.line(cx, cy - 1.0f, x + 1.5f, y + 4.5f, stroke);
                    draw.line(x + 1.5f, y + 4.5f, x + 1.5f, y + height - 4.5f, stroke);
                    draw.line(x + width - 1.5f, y + 4.5f, x + width - 1.5f, y + height - 4.5f, stroke);
                    draw.line(cx, cy - 1.0f, cx, y + height - 1.0f, stroke);
                    draw.line(x + 1.5f, y + height - 4.5f, cx, y + height - 1.0f, stroke);
                    draw.line(x + width - 1.5f, y + height - 4.5f, cx, y + height - 1.0f, stroke);
                }
                case "mc-entities" -> {
                    draw.circle(cx - 3.0f, y + 1.0f, 6.0f, 6.0f, stroke);
                    draw.line(cx, y + 7.0f, cx, y + height - 4.5f, stroke);
                    draw.line(x + 3.0f, cy, x + width - 3.0f, cy, stroke);
                    draw.line(cx, y + height - 4.5f, x + 3.5f, y + height - 1.0f, stroke);
                    draw.line(cx, y + height - 4.5f, x + width - 3.5f, y + height - 1.0f, stroke);
                }
                case "mc-pickers" -> {
                    draw.rect(x + 1.0f, y + 2.0f, width - 2.0f, height - 4.0f, stroke);
                    draw.rectFilled(x + 4.0f, y + 5.0f, 3.0f, 3.0f, fill);
                    draw.rectFilled(x + width - 7.0f, y + 5.0f, 3.0f, 3.0f, fill);
                    draw.rectFilled(x + 4.0f, y + height - 8.0f, 3.0f, 3.0f, fill);
                    draw.rectFilled(x + width - 7.0f, y + height - 8.0f, 3.0f, 3.0f, fill);
                }
                case "post-effects" -> {
                    draw.line(cx, y + 1.0f, cx, y + height - 1.0f, stroke);
                    draw.line(x + 1.0f, cy, x + width - 1.0f, cy, stroke);
                    draw.line(x + 4.0f, y + 4.0f, x + width - 4.0f, y + height - 4.0f, stroke);
                    draw.line(x + width - 4.0f, y + 4.0f, x + 4.0f, y + height - 4.0f, stroke);
                }
                default -> draw.circleFilled(cx - 2.0f, cy - 2.0f, 4.0f, 4.0f, fill);
            }
        };
    }

    protected Slider slider(float min, float max, float step, float value) {
        Slider slider = new Slider();
        slider.range(min, max).step(step).value(value);
        slider.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).height(28.0f));
        slider.fillColor().set(gallery.accent);
        gallery.accentTargets.add(slider);
        return slider;
    }

    protected Slider verticalSlider(float min, float max, float step, float value) {
        Slider slider = new Slider();
        slider.orientation(Orientation.VERTICAL).range(min, max).step(step).value(value);
        slider.layout(style -> style.size(24.0f, 130.0f).flexGrow(0.0f).flexShrink(0.0f));
        slider.fillColor().set(gallery.accent);
        gallery.accentTargets.add(slider);
        return slider;
    }

    /**
     * Вертикальный слайдер с подписью и текущим значением под ним.
     */
    protected Widget verticalSliderColumn(String name, float min, float max, float step, float value) {
        Slider slider = verticalSlider(min, max, step, value);
        Label valueLabel = UniGuiDemo.GalleryTemplate.valueLabel(slider.value());
        slider.onValueChanged(event -> valueLabel.text(UniGuiDemo.GalleryTemplate.format(event.newValue())));
        VBox column = new VBox();
        column.spacing(8.0f);
        column.layout(style -> style.alignItems(Align.CENTER));
        column.addChild(slider);
        column.addChild(valueLabel);
        column.addChild(UniGuiDemo.GalleryTemplate.label(name, UniGuiDemo.GalleryTemplate.TEXT_DARK));
        return column;
    }

    protected Widget sliderRow(String name, Slider slider, String initialValue) {
        HBox row = new HBox();
        row.spacing(12.0f);
        row.layout(style -> style.widthPercent(100.0f).height(34.0f).alignItems(Align.CENTER));
        Label nameLabel = label(name, UniGuiDemo.GalleryTemplate.TEXT_DARK);
        nameLabel.layout(style -> style.width(112.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label value = UniGuiDemo.GalleryTemplate.valueLabel(Float.parseFloat(initialValue));
        value.layout(style -> style.width(48.0f).flexGrow(0.0f).flexShrink(0.0f));
        slider.onValueChanged(event -> value.text(UniGuiDemo.GalleryTemplate.format(event.newValue())));
        row.addChild(nameLabel);
        row.addChild(slider);
        row.addChild(value);
        return row;
    }

    protected Label label(String text, ColorView color) {
        Label label = new Label(text);
        label.color(color);
        return label;
    }

    /**
     * Прокручиваемый блок кода высотой не больше {@link #CODE_MAX_VISIBLE_LINES} строк.
     *
     * <p>Обычный {@code ScrollView} берёт высоту контента и не знает, что горизонтальная полоса прокрутки
     * заберёт часть viewport: короткий фрагмент из двух строк получал высоту ровно в две строки, полоса съедала
     * нижнюю часть, и вторая строка оказывалась почти скрыта. Здесь высота считается так:</p>
     *
     * <pre>
     * height = lineHeight * min(lines, CODE_MAX_VISIBLE_LINES) + reserve
     * reserve = scrollbarSize + scrollbarGap, если строки могут не поместиться по ширине
     * lineHeight = высота контента / число строк
     * </pre>
     *
     * <p>Место под полосу резервируется с запасом: лишние несколько пикселей снизу безвредны, а нехватка
     * обрезает текст.</p>
     */
    private static final class CodeScrollView extends ScrollView {
        /** Запас под отступы контейнера вокруг блока: полоса нужна, если текст шире {@code available - запас}. */
        private static final float WIDTH_SLACK = 40.0f;

        private final int lines;

        private CodeScrollView(Widget content, int lines) {
            super(content);
            this.lines = Math.max(1, lines);
        }

        @Override
        public void measure(LayoutContext context) {
            super.measure(context);
            Widget content = content();
            if (content == null) return;
            float contentHeight = content.desiredSize().height();
            float lineHeight = contentHeight / lines;
            float textHeight = Math.min(contentHeight, lineHeight * CODE_MAX_VISIBLE_LINES);
            float available = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
            boolean mayOverflowX = content.desiredSize().width() > available - WIDTH_SLACK;
            float reserve = mayOverflowX ? scrollbarSize() + scrollbarGap() : 0.0f;
            setDesiredSize(desiredSize().width(), textHeight + reserve);
        }
    }

    private static boolean isInsideScrollBar(Widget widget) {
        for (Widget current = widget; current != null; current = current.parent()) {
            if (current instanceof ScrollBar) return true;
        }
        return false;
    }

    protected VBox samplePanel(String title, String description, Widget sample, String code) {
        VBox section = new VBox();
        section.spacing(7.0f);
        section.layout(style -> style.widthPercent(100.0f));
        Label sectionTitle = label(title, UniGuiDemo.GalleryTemplate.color(255, 255, 255));
        sectionTitle.font(null, 14.0f);
        section.addChild(sectionTitle);
        Label hint = label(description, UniGuiDemo.GalleryTemplate.TEXT_MUTED);
        hint.layout(style -> style.widthPercent(100.0f));
        section.addChild(hint);

        Box board = new Box();
        board.themeEnabled(false);
        board.backgroundVisible(true);
        board.background().set(UniGuiDemo.GalleryTemplate.CARD);
        board.borderVisible(true);
        board.borderColor().set(UniGuiDemo.GalleryTemplate.CARD_BORDER);
        board.borderWidth(1.0f);
        board.radius(4.0f);
        board.layout(style -> style.widthPercent(100.0f).padding(18.0f, 16.0f));

        VBox boardContent = new VBox();
        boardContent.themeEnabled(false);
        boardContent.backgroundVisible(false);
        boardContent.borderVisible(false);
        boardContent.spacing(12.0f);
        boardContent.layout(style -> style.widthPercent(100.0f));
        Button reveal = null;
        if (code != null && !code.isBlank()) {
            HBox boardTools = new HBox();
            boardTools.layout(style -> style.widthPercent(100.0f).alignItems(Align.CENTER));
            reveal = new Button("Show Java");
            UniGuiDemo.GalleryTemplate.styleButton(reveal, UniGuiDemo.GalleryTemplate.color(232, 238, 243), UniGuiDemo.GalleryTemplate.color(56, 99, 130));
            boardTools.addChild(reveal);
            boardContent.addChild(boardTools);
        }
        boardContent.addChild(sample);
        Box codeBox = null;
        if (code != null && !code.isBlank()) {
            codeBox = new Box();
        codeBox.themeEnabled(false);
        codeBox.backgroundVisible(true);
        codeBox.background().set(UniGuiDemo.GalleryTemplate.CODE_BACKGROUND);
        codeBox.borderVisible(true);
        codeBox.borderColor().set(UniGuiDemo.GalleryTemplate.CODE_BORDER);
        codeBox.borderWidth(1.0f);
        codeBox.radius(4.0f);
        codeBox.visibility(Visibility.COLLAPSED);
        codeBox.layout(style -> style.widthPercent(100.0f).padding(12.0f, 10.0f));
        VBox codeContent = new VBox();
        codeContent.themeEnabled(false);
        codeContent.backgroundVisible(false);
        codeContent.borderVisible(false);
        codeContent.spacing(6.0f);
        codeContent.layout(style -> style.widthPercent(100.0f));
        Label copyHint = label("Click the code to copy it", UniGuiDemo.GalleryTemplate.CODE_HINT);
        copyHint.font(null, 10.0f);
        codeContent.addChild(copyHint);

        Label codeLabel = label(code, UniGuiDemo.GalleryTemplate.CODE_TEXT);
        // Явный шрифт обязателен: без него Label меряет высоту запасной метрикой, а рисует шрифтом бэкенда
        // с более высокой строкой, и ScrollView считает контент ниже, чем он выглядит (низ кода не прокрутить).
        codeLabel.font(Fonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
        CodeScrollView codeScroll = new CodeScrollView(codeLabel, code.split("\n", -1).length);
        codeScroll.overlayScrollBars(false);
        codeScroll.consumeWheelAtScrollBounds(false);
        codeScroll.scrollStep(24.0f);
        codeScroll.scrollbarTrackColor().set(UniGuiDemo.GalleryTemplate.CODE_BORDER);
        codeScroll.scrollbarThumbColor().set(UniGuiDemo.GalleryTemplate.CODE_SCROLL_THUMB);
        codeScroll.scrollbarThumbHoverColor().set(UniGuiDemo.GalleryTemplate.CODE_SCROLL_THUMB_HOVER);
        codeScroll.layout(style -> style.widthPercent(100.0f).overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        codeContent.addChild(codeScroll);
        codeBox.addChild(codeContent);
        // Клик по полосе прокрутки не должен копировать код: фильтруем события, пришедшие из ScrollBar.
        // Слушатель предка получает событие дважды: в фазе CAPTURE и в фазе BUBBLE. Копируем один раз, на всплытии.
        codeBox.on(PointerReleasedEvent.TYPE, event -> {
            if (event.phase() == EventPhase.CAPTURE) return;
            if (event.button() != PointerButton.PRIMARY || isInsideScrollBar(event.target())) return;
            gallery.copyToClipboard(code, "Java code copied");
        });
            Box codeSection = codeBox;
            Button codeButton = reveal;
            codeButton.onClick(event -> {
                boolean show = codeSection.visibility() != Visibility.VISIBLE;
                codeSection.visibility(show ? Visibility.VISIBLE : Visibility.COLLAPSED);
                codeButton.text(show ? "Hide Java" : "Show Java");
            });
            boardContent.addChild(codeSection);
        }
        board.addChild(boardContent);
        section.addChild(board);
        return section;
    }
}
