package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.List;

/**
 * Рисование хрома докинга: полосы вкладок, вкладки, кнопки закрытия и заголовка плавающего окна.
 *
 * <p>Класс собирает всю «косметику» в одном месте и работает только с {@link DockTheme}. Им пользуются и
 * {@link DockingRoot} (вкладки докнутых панелей), и {@code WindowWidget} (заголовок плавающего окна), так
 * что оба выглядят одинаково. Геометрия вкладки (иконка, текст, крестик) считается теми же функциями,
 * которые использует хит-тест, поэтому нарисованное и кликабельное не расходятся.</p>
 */
@AiDoc(summary = "Utility drawing engine responsible for rendering docking chrome: tab strips, tab items, close/collapse buttons, and window frames based on DockTheme."
)
public final class DockChrome {
    private static final float HALF_PI = (float) (Math.PI * 0.5);

    private DockChrome() {
    }

    /**
     * Состояние полосы вкладок листа, нужное painter'у.
     *
     * @param leafActive лист содержит активную панель (подсвечивается акцентом)
     * @param hoveredId id вкладки под курсором или пустая строка
     * @param pressedId id нажатой вкладки или пустая строка
     * @param hoveredCloseId id вкладки, чей крестик под курсором, или пустая строка
     * @param pressedCloseId id вкладки, чей крестик нажат, или пустая строка
     * @param draggingId id перетаскиваемой вкладки или пустая строка
     * @param overflow полоса переполнена и показывает кнопку меню
     * @param overflowButton прямоугольник кнопки переполнения (имеет смысл при {@code overflow})
     * @param overflowOpen меню переполнения открыто
     */
    record StripState(boolean leafActive, String hoveredId, String pressedId, String hoveredCloseId,
                      String pressedCloseId, String draggingId, boolean overflow, MutableRect overflowButton,
                      boolean overflowOpen) {
    }

    /**
     * Предпочтительная ширина вкладки: отступы, иконка, текст и зарезервированное место под крестик.
     *
     * @param pane панель
     * @param theme тема
     * @return ширина в пределах {@code [tabMinWidth, tabMaxWidth]}
     */
    static float preferredTabWidth(DockPane pane, DockTheme theme) {
        float width = theme.tabPaddingX() * 2.0f + pane.titleWidth();
        if (!pane.icon().isNone()) width += theme.iconSize() + theme.iconGap();
        if (pane.closable()) width += theme.closeGap() + theme.closeSize();
        return Math.max(theme.tabMinWidth(), Math.min(theme.tabMaxWidth(), width));
    }

    /**
     * Прямоугольник иконки вкладки.
     *
     * @param tab вкладка
     * @param theme тема
     * @param out куда записать результат
     * @return {@code out}
     */
    static MutableRect iconRect(DockTabLayout tab, DockTheme theme, MutableRect out) {
        float size = theme.iconSize();
        out.set(tab.x() + theme.tabPaddingX(), tab.y() + (tab.height() - size) * 0.5f, size, size);
        return out;
    }

    /**
     * Прямоугольник кнопки закрытия вкладки.
     *
     * @param tab вкладка
     * @param theme тема
     * @param out куда записать результат
     * @return {@code out}
     */
    static MutableRect closeRect(DockTabLayout tab, DockTheme theme, MutableRect out) {
        float size = theme.closeSize();
        out.set(tab.x() + tab.width() - theme.tabPaddingX() - size + 2.0f,
                tab.y() + (tab.height() - size) * 0.5f, size, size);
        return out;
    }

    /**
     * Попадает ли точка в кнопку закрытия вкладки.
     *
     * @param tab вкладка
     * @param theme тема
     * @param rootX X точки
     * @param rootY Y точки
     * @return {@code true}, если вкладка закрываемая и точка внутри её крестика (с запасом 2 px)
     */
    static boolean hitsClose(DockTabLayout tab, DockTheme theme, float rootX, float rootY) {
        if (!tab.pane().closable() || !tab.contains(rootX, rootY)) return false;
        MutableRect rect = closeRect(tab, theme, new MutableRect());
        return rootX >= rect.x() - 2.0f && rootX <= rect.x() + rect.width() + 2.0f
                && rootY >= rect.y() - 2.0f && rootY <= rect.y() + rect.height() + 2.0f;
    }

    /**
     * Рисует полосу вкладок листа.
     *
     * @param draw scope рисования
     * @param theme тема
     * @param bounds границы листа (полоса — его верхняя часть)
     * @param stripHeight высота полосы
     * @param tabs раскладка вкладок
     * @param selectedIndex индекс выбранной вкладки
     * @param state состояние наведения/нажатия
     */
    static void paintStrip(DrawScope draw, DockTheme theme, dev.sixik.unigui.api.math.RectView bounds,
                           float stripHeight, List<DockTabLayout> tabs, int selectedIndex, StripState state) {
        draw.rectFilled(bounds.x(), bounds.y(), bounds.width(), stripHeight,
                theme.color(DockColor.STRIP_BACKGROUND));
        draw.rectFilled(bounds.x(), bounds.y() + stripHeight - 1.0f, bounds.width(), 1.0f,
                theme.color(DockColor.STRIP_BORDER));
        // Невыбранные вкладки рисуем первыми, выбранную — последней: её фон перекрывает линию под полосой.
        DockTabLayout selected = null;
        for (DockTabLayout tab : tabs) {
            if (tab.width() <= 0.0f || tab.height() <= 0.0f) continue;
            if (tab.index() == selectedIndex) {
                selected = tab;
                continue;
            }
            paintTab(draw, theme, tab, false, state);
        }
        if (selected != null) paintTab(draw, theme, selected, true, state);
        if (state.overflow()) paintOverflowButton(draw, theme, state);
    }

    private static void paintTab(DrawScope draw, DockTheme theme, DockTabLayout tab, boolean selected,
                                 StripState state) {
        DockPane pane = tab.pane();
        boolean hovered = pane.id().equals(state.hoveredId());
        boolean pressed = pane.id().equals(state.pressedId());
        boolean dragging = pane.id().equals(state.draggingId());
        float x = tab.x();
        float y = tab.y();
        float w = tab.width();
        float h = tab.height();

        if (selected) {
            ColorView fill = state.leafActive()
                    ? theme.color(DockColor.TAB_SELECTED)
                    : theme.color(DockColor.TAB_SELECTED_INACTIVE);
            if (pressed) fill = theme.color(DockColor.TAB_PRESSED);
            fillTopRounded(draw, x, y, w, h, theme.tabRadius(), fill);
            float thickness = theme.accentThickness();
            if (thickness > 0.0f) {
                float inset = Math.min(theme.tabRadius() * 0.75f, w * 0.25f);
                draw.rectFilled(x + inset, y, Math.max(0.0f, w - inset * 2.0f), thickness,
                        state.leafActive()
                                ? theme.color(DockColor.TAB_ACCENT)
                                : theme.color(DockColor.TAB_ACCENT_INACTIVE));
            }
        } else {
            ColorView fill = pressed
                    ? theme.color(DockColor.TAB_PRESSED)
                    : hovered ? theme.color(DockColor.TAB_HOVER) : theme.color(DockColor.TAB_BACKGROUND);
            draw.rectFilled(x, y + 2.0f, w, Math.max(0.0f, h - 3.0f), fill);
            draw.rectFilled(x + w - 1.0f, y + 2.0f, 1.0f, Math.max(0.0f, h - 3.0f),
                    theme.color(DockColor.TAB_SEPARATOR));
        }
        if (dragging) {
            draw.rectStroke(x, y + 1.0f, w, Math.max(0.0f, h - 1.0f), theme.color(DockColor.TAB_DRAG), 1.5f);
        }

        ColorView textColor = selected || hovered
                ? theme.color(DockColor.TAB_TEXT)
                : theme.color(DockColor.TAB_TEXT_MUTED);
        float left = x + theme.tabPaddingX();
        if (!pane.icon().isNone()) {
            MutableRect icon = iconRect(tab, theme, new MutableRect());
            pane.icon().draw(draw, icon.x(), icon.y(), icon.width(), selected || hovered
                    ? theme.color(DockColor.TAB_TEXT) : theme.color(DockColor.TAB_TEXT_MUTED));
            left += theme.iconSize() + theme.iconGap();
        }
        float right = x + w - theme.tabPaddingX();
        if (pane.closable()) {
            MutableRect close = closeRect(tab, theme, new MutableRect());
            right = close.x() - theme.closeGap();
            boolean overClose = pane.id().equals(state.hoveredCloseId());
            if (pane.dirty() && !hovered) {
                paintDirtyDot(draw, theme, close);
            } else if (selected || hovered) {
                paintCloseButton(draw, theme, close.x(), close.y(), close.width(), overClose,
                        pane.id().equals(state.pressedCloseId()));
            }
        }
        float textWidth = Math.max(0.0f, right - left);
        if (textWidth > 0.0f) {
            drawTitle(draw, pane.richTitle(), left, y, textWidth, h, textColor);
        }
    }

    /**
     * Рисует заголовок, центрируя его по вертикали в {@code [y, y + height]} и обрезая по ширине.
     *
     * @param draw scope рисования
     * @param title текст
     * @param x левая граница
     * @param y верхняя граница области заголовка
     * @param width доступная ширина
     * @param height высота области заголовка
     * @param color цвет текста
     */
    public static void drawTitle(DrawScope draw, RichText title, float x, float y, float width, float height,
                          ColorView color) {
        if (title == null || title.isEmpty() || width <= 0.0f || height <= 0.0f) return;
        float textHeight = Math.min(height, Math.max(0.0f, TextEngine.measureTextHeight(draw.context(), title)));
        float textY = y + Math.max(0.0f, height - textHeight) * 0.5f;
        draw.pushTextClip(x, y, width, height);
        try {
            TextEngine.drawInline(draw, title, x, textY, width, textHeight, Paint.fill(color));
        } finally {
            draw.popClip();
        }
    }

    /**
     * Рисует кнопку закрытия: красный круг-подложку под курсором и крестик.
     *
     * @param draw scope рисования
     * @param theme тема
     * @param x левая граница квадрата кнопки
     * @param y верхняя граница квадрата кнопки
     * @param size сторона квадрата
     * @param hovered курсор над кнопкой
     * @param pressed кнопка нажата
     */
    public static void paintCloseButton(DrawScope draw, DockTheme theme, float x, float y, float size,
                                 boolean hovered, boolean pressed) {
        if (hovered || pressed) {
            MutableColor back = theme.color(DockColor.CLOSE_HOVER_BACKGROUND);
            draw.roundedRectFilled(x, y, size, size, 3.0f,
                    pressed ? new MutableColor(back.r() * 0.8f, back.g() * 0.8f, back.b() * 0.8f, back.a()) : back);
        }
        float arm = size * 0.22f;
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        ColorView glyph = hovered || pressed
                ? theme.color(DockColor.CLOSE_GLYPH_HOVER)
                : theme.color(DockColor.CLOSE_GLYPH);
        draw.line(cx - arm, cy - arm, cx + arm, cy + arm, Paint.stroke(glyph, 1.4f));
        draw.line(cx - arm, cy + arm, cx + arm, cy - arm, Paint.stroke(glyph, 1.4f));
    }

    /**
     * Рисует кнопку сворачивания окна: чёрточка в развёрнутом состоянии, плюс — в свёрнутом.
     *
     * @param draw scope рисования
     * @param theme тема
     * @param x левая граница квадрата кнопки
     * @param y верхняя граница квадрата кнопки
     * @param size сторона квадрата
     * @param hovered курсор над кнопкой
     * @param collapsed окно сейчас свёрнуто
     */
    public static void paintCollapseButton(DrawScope draw, DockTheme theme, float x, float y, float size,
                                           boolean hovered, boolean collapsed) {
        if (hovered) {
            draw.roundedRectFilled(x, y, size, size, 3.0f, theme.color(DockColor.TAB_HOVER));
        }
        float arm = size * 0.24f;
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        ColorView glyph = hovered ? theme.color(DockColor.CLOSE_GLYPH_HOVER) : theme.color(DockColor.CLOSE_GLYPH);
        draw.line(cx - arm, cy, cx + arm, cy, Paint.stroke(glyph, 1.4f));
        if (collapsed) {
            draw.line(cx, cy - arm, cx, cy + arm, Paint.stroke(glyph, 1.4f));
        }
    }
    /**
     * Рисует точку «есть несохранённые изменения» вместо крестика.
     *
     * @param draw scope рисования
     * @param theme тема
     * @param close прямоугольник кнопки закрытия вкладки
     */
    static void paintDirtyDot(DrawScope draw, DockTheme theme, MutableRect close) {
        float radius = Math.max(2.0f, close.width() * 0.22f);
        draw.circleFilled(close.x() + close.width() * 0.5f - radius, close.y() + close.height() * 0.5f - radius,
                radius * 2.0f, radius * 2.0f, theme.color(DockColor.TAB_DIRTY));
    }

    private static void paintOverflowButton(DrawScope draw, DockTheme theme, StripState state) {
        MutableRect button = state.overflowButton();
        draw.rectFilled(button.x(), button.y(), button.width(), button.height() - 1.0f,
                state.overflowOpen()
                        ? theme.color(DockColor.TAB_HOVER)
                        : theme.color(DockColor.STRIP_BACKGROUND));
        draw.rectFilled(button.x(), button.y() + 3.0f, 1.0f, Math.max(0.0f, button.height() - 7.0f),
                theme.color(DockColor.TAB_SEPARATOR));
        float cx = button.x() + button.width() * 0.5f;
        float cy = button.y() + (button.height() - 1.0f) * 0.5f;
        draw.pathClear();
        draw.pathLineTo(cx - 4.0f, cy - 2.0f);
        draw.pathLineTo(cx + 4.0f, cy - 2.0f);
        draw.pathLineTo(cx, cy + 3.0f);
        draw.pathFillConvex(theme.color(DockColor.OVERFLOW_GLYPH));
    }

    /**
     * Рисует заголовок плавающего окна: фон полосы, иконка, название и линия-акцент.
     *
     * <p>Кнопки заголовка (закрыть/свернуть) окно рисует само поверх, этот метод резервирует под них
     * {@code rightReserved} пикселей справа. Три последних параметра необязательны: если не {@code null},
     * они заменяют цвета темы (так работают {@code headerColor()}, {@code headerSeparatorColor()} и
     * {@code titleColor()} окна).</p>
     *
     * @param draw scope рисования
     * @param theme тема
     * @param x левая граница заголовка
     * @param y верхняя граница заголовка
     * @param width ширина заголовка
     * @param height высота заголовка
     * @param icon иконка окна
     * @param title название окна
     * @param active окно активно (акцентная линия сверху)
     * @param dragging окно сейчас перетаскивают (акцент толще)
     * @param leftPadding левый отступ
     * @param rightReserved ширина, занятая кнопками справа
     * @param backgroundOverride цвет фона вместо темы или {@code null}
     * @param separatorOverride цвет нижней линии вместо темы или {@code null}
     * @param textOverride цвет текста вместо темы или {@code null}
     */
    public static void paintCaption(DrawScope draw, DockTheme theme, float x, float y, float width, float height,
                                    UiIcon icon, RichText title, boolean active, boolean dragging,
                                    float leftPadding, float rightReserved, ColorView backgroundOverride,
                                    ColorView separatorOverride, ColorView textOverride) {
        fillTopRounded(draw, x, y, width, height, theme.windowRadius(),
                backgroundOverride != null ? backgroundOverride : theme.color(DockColor.STRIP_BACKGROUND));
        draw.rectFilled(x, y + height - 1.0f, width, 1.0f,
                separatorOverride != null ? separatorOverride : theme.color(DockColor.STRIP_BORDER));
        if (active || dragging) {
            float thickness = dragging ? Math.max(2.0f, theme.accentThickness()) : theme.accentThickness();
            float inset = Math.min(theme.windowRadius() * 0.75f, width * 0.25f);
            draw.rectFilled(x + inset, y, Math.max(0.0f, width - inset * 2.0f), thickness,
                    theme.color(DockColor.TAB_ACCENT));
        }
        float left = x + leftPadding;
        ColorView textColor = textOverride != null ? textOverride
                : active || dragging ? theme.color(DockColor.TAB_TEXT) : theme.color(DockColor.TAB_TEXT_MUTED);
        if (icon != null && !icon.isNone()) {
            float size = Math.min(theme.iconSize(), height - 4.0f);
            icon.draw(draw, left, y + (height - size) * 0.5f, size, textColor);
            left += size + theme.iconGap();
        }
        float textWidth = Math.max(0.0f, x + width - rightReserved - left);
        drawTitle(draw, title, left, y, textWidth, height - 1.0f, textColor);
    }
    /**
     * Заливает прямоугольник с закруглёнными только верхними углами.
     *
     * @param draw scope рисования
     * @param x левая граница
     * @param y верхняя граница
     * @param w ширина
     * @param h высота
     * @param radius радиус верхних углов
     * @param color цвет заливки
     */
    public static void fillTopRounded(DrawScope draw, float x, float y, float w, float h, float radius,
                               ColorView color) {
        float r = Math.min(radius, Math.min(w, h) * 0.5f);
        if (r <= 0.5f) {
            draw.rectFilled(x, y, w, h, color);
            return;
        }
        draw.pathClear();
        draw.pathLineTo(x, y + h);
        draw.pathArcTo(x + r, y + r, r, (float) Math.PI, (float) Math.PI + HALF_PI, 5);
        draw.pathArcTo(x + w - r, y + r, r, (float) Math.PI + HALF_PI, (float) (Math.PI * 2.0), 5);
        draw.pathLineTo(x + w, y + h);
        draw.pathFillConvex(color);
    }

    /**
     * Рисует мягкую тень под прямоугольником несколькими слоями убывающей прозрачности.
     *
     * @param draw scope рисования
     * @param x левая граница окна
     * @param y верхняя граница окна
     * @param w ширина окна
     * @param h высота окна
     * @param radius радиус скругления окна
     * @param size ширина тени
     * @param color цвет тени (альфа — сила у края окна)
     */
    public static void paintShadow(DrawScope draw, float x, float y, float w, float h, float radius, float size,
                            ColorView color) {
        if (size <= 0.5f || color.a() <= 0.0f) return;
        int layers = Math.max(2, Math.min(5, (int) (size / 2.5f)));
        float offsetY = size * 0.25f;
        for (int i = layers; i >= 1; i--) {
            float t = i / (float) layers;
            float grow = size * t;
            float alpha = color.a() * (1.0f - t) * 0.55f + color.a() * 0.05f;
            draw.roundedRectFilled(x - grow, y - grow + offsetY, w + grow * 2.0f, h + grow * 2.0f,
                    radius + grow, new MutableColor(color.r(), color.g(), color.b(), alpha));
        }
    }
}
