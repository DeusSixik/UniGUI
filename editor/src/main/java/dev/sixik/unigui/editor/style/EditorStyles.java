package dev.sixik.unigui.editor.style;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.TreeDisclosureStyle;
import dev.sixik.unigui.api.style.TreeRowStyle;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.navigation.TreeView;

/**
 * Вид редактора: палитра, размеры и стили виджетов. Основа — почти чёрный фон, на котором лежат панели-«острова»,
 * один синий акцент и приглушённый текст; цвет появляется только там, где несёт смысл (выбор, тип пина, ошибка). Все
 * панели берут цвета, размеры строк и внешний вид элементов отсюда, поэтому тему можно поменять в одном месте.
 */
public final class EditorStyles {
    private EditorStyles() {
    }

    // --- Фоны и панели ---
    public static final MutableColor BG_WORKSPACE = MutableColor.rgba255(17, 18, 21, 255); // #111215
    public static final MutableColor BG_WINDOW = MutableColor.rgba255(29, 30, 34, 255); // #1D1E22
    public static final MutableColor BG_PANEL = MutableColor.rgba255(23, 24, 28, 255); // #17181C
    public static final MutableColor BG_HEADER = MutableColor.rgba255(38, 40, 45, 255); // #26282D
    public static final MutableColor BG_HEADER_HOVER = MutableColor.rgba255(47, 49, 55, 255); // #2F3137
    public static final MutableColor BG_TOOLBAR = MutableColor.rgba255(23, 24, 28, 255); // #17181C
    public static final MutableColor BG_TAB_ACTIVE = MutableColor.rgba255(29, 30, 34, 255); // #1D1E22
    public static final MutableColor BG_TAB_INACTIVE = MutableColor.rgba255(20, 21, 24, 255); // #141518
    public static final MutableColor BG_INPUT = MutableColor.rgba255(18, 19, 22, 255); // #121316
    public static final MutableColor BG_HELP_BOX = MutableColor.rgba255(23, 24, 28, 255); // #17181C
    public static final MutableColor BG_BUTTON = MutableColor.rgba255(43, 45, 51, 255); // #2B2D33
    public static final MutableColor BG_BUTTON_HOVER = MutableColor.rgba255(54, 56, 64, 255); // #363840
    public static final MutableColor BG_BUTTON_ACTIVE = MutableColor.rgba255(34, 36, 41, 255); // #222429
    public static final MutableColor TAB_ACCENT = MutableColor.rgba255(76, 141, 255, 255); // #4C8DFF
    public static final MutableColor WARNING_YELLOW = MutableColor.rgba255(232, 180, 76, 255); // #E8B44C

    // --- Границы и разделители ---
    public static final MutableColor BORDER_DEFAULT = MutableColor.rgba255(11, 12, 14, 255); // #0B0C0E
    public static final MutableColor BORDER_SUBTLE = MutableColor.rgba255(42, 44, 50, 255); // #2A2C32
    public static final MutableColor BORDER_INPUT = MutableColor.rgba255(44, 46, 53, 255); // #2C2E35
    public static final MutableColor BORDER_INPUT_FOCUS = MutableColor.rgba255(76, 141, 255, 255); // #4C8DFF
    public static final MutableColor BORDER_BUTTON = MutableColor.rgba255(58, 61, 69, 255); // #3A3D45
    public static final MutableColor SEPARATOR = MutableColor.rgba255(38, 40, 45, 255); // #26282D

    // --- Цвета текста ---
    public static final MutableColor TEXT_PRIMARY = MutableColor.rgba255(217, 219, 224, 255); // #D9DBE0
    public static final MutableColor TEXT_LABEL = MutableColor.rgba255(163, 167, 176, 255); // #A3A7B0
    public static final MutableColor TEXT_MUTED = MutableColor.rgba255(108, 112, 122, 255); // #6C707A
    public static final MutableColor TEXT_BOLD = MutableColor.rgba255(243, 244, 246, 255); // #F3F4F6
    public static final MutableColor TEXT_HELP_BOX = MutableColor.rgba255(163, 167, 176, 255); // #A3A7B0

    // --- Оси координат Unity ---
    public static final MutableColor AXIS_X = MutableColor.rgba255(224, 93, 93, 255);             // Red X
    public static final MutableColor AXIS_Y = MutableColor.rgba255(123, 209, 94, 255);            // Green Y
    public static final MutableColor AXIS_Z = MutableColor.rgba255(90, 148, 232, 255);            // Blue Z

    // --- Акценты и иконки ---
    public static final MutableColor CUBE_BLUE_TOP = MutableColor.rgba255(75, 163, 227, 255);
    public static final MutableColor CUBE_BLUE_LEFT = MutableColor.rgba255(32, 114, 196, 255);
    public static final MutableColor CUBE_BLUE_RIGHT = MutableColor.rgba255(16, 76, 140, 255);
    public static final MutableColor AUDIO_GOLD = MutableColor.rgba255(244, 184, 64, 255);
    public static final MutableColor SELECTION_BLUE = MutableColor.rgba255(43, 79, 138, 255); // #2B4F8A
    public static final MutableColor PLAY_GREEN = MutableColor.rgba255(47, 158, 92, 255); // #2F9E5C

    // --- Выпадающие списки (Dropdown / ComboBox) в палитре Unity Dark ---
    public static final MutableColor BG_DROPDOWN_HOST = MutableColor.rgba255(26, 27, 31, 255); // #1A1B1F
    public static final MutableColor BORDER_DROPDOWN_HOST = MutableColor.rgba255(52, 54, 61, 255); // #34363D
    public static final MutableColor BG_DROPDOWN_ITEM_NORMAL = MutableColor.rgba255(26, 27, 31, 255); // #1A1B1F
    public static final MutableColor BG_DROPDOWN_ITEM_CHECKED = MutableColor.rgba255(36, 55, 90, 255); // #24375A
    public static final MutableColor BG_DROPDOWN_ITEM_HOVER = MutableColor.rgba255(43, 79, 138, 255); // #2B4F8A
    public static final MutableColor BG_DROPDOWN_ITEM_PRESS = MutableColor.rgba255(35, 63, 110, 255); // #233F6E
    public static final MutableColor BORDER_DROPDOWN_ITEM_HOVER = MutableColor.rgba255(43, 79, 138, 255); // #2B4F8A
    public static final MutableColor SCROLLBAR_TRACK = MutableColor.rgba255(0, 0, 0, 0); // трек не виден, остаётся только ползунок
    public static final MutableColor SCROLLBAR_THUMB = MutableColor.rgba255(70, 73, 82, 255);

    // --- Размеры ---
    public static final float ROW_HEIGHT = 18.0f;
    public static final float COMPONENT_HEADER_HEIGHT = 20.0f;
    public static final float LABEL_WIDTH = 92.0f;
    /** Внутренний отступ панелей. */
    public static final float PANEL_PADDING = 6.0f;
    /** Радиус полей и кнопок. */
    public static final float RADIUS = 3.0f;
    /** Толщина полосы прокрутки. */
    public static final float SCROLLBAR_SIZE = 6.0f;

    // --- Смысловые цвета ---
    public static final MutableColor ACCENT_SOFT = MutableColor.rgba255(76, 141, 255, 40);
    public static final MutableColor TEXT_SECONDARY = MutableColor.rgba255(139, 144, 155, 255);
    public static final MutableColor ERROR_RED = MutableColor.rgba255(235, 96, 96, 255);
    public static final MutableColor INFO_BLUE = MutableColor.rgba255(120, 170, 255, 255);
    public static final MutableColor BG_STAGE = MutableColor.rgba255(13, 14, 16, 255);
    public static final MutableColor GRID_DOT = MutableColor.rgba255(255, 255, 255, 16);
    public static final MutableColor SHADOW = MutableColor.rgba255(0, 0, 0, 90);

    /**
     * Кастомный рендерер Checkbox в стиле Unity:
     * Тёмный квадрат с тонкой рамкой (белая при hover, синяя при press)
     * и изящной белой галочкой (✓) правильных пропорций.
     */
    public static final WidgetRender CHECKBOX_RENDERER = WidgetRender.of(Checkbox.class, (draw, cb) -> {
        RectView bounds = cb.layoutBounds();
        float x = bounds.x();
        float y = bounds.y();
        float width = bounds.width();
        float height = bounds.height();

        float boxSize = cb.boxSize();
        float indicatorX = x;
        float indicatorY = y + Math.max(0.0f, height - boxSize) * 0.5f;

        // Фон чекбокса: Unity тёмно-серый
        draw.roundedRectFilled(indicatorX, indicatorY, boxSize, boxSize, 2.0f, BG_INPUT);

        // Тонкая рамка (1px): синяя при нажатии, белая при наведении, одинаково тёмная в покое (Checked и Unchecked)
        ColorView borderCol;
        if (!cb.enabled()) {
            borderCol = BORDER_INPUT;
        } else if (cb.pressed()) {
            borderCol = BORDER_INPUT_FOCUS;
        } else if (cb.hovered()) {
            borderCol = TEXT_PRIMARY;
        } else {
            borderCol = BORDER_INPUT;
        }
        draw.roundedRectStroke(indicatorX, indicatorY, boxSize, boxSize, 2.0f, borderCol, 1.0f);

        // Галочка (✓) в стиле Unity (изящная тонкая линия 1.15px, острый угол)
        if (cb.checked()) {
            float ix = indicatorX;
            float iy = indicatorY;
            ColorView markCol = cb.enabled() ? TEXT_PRIMARY : TEXT_MUTED;
            draw.pathClear();
            draw.pathLineTo(ix + 2.8f, iy + 5.8f);
            draw.pathLineTo(ix + 4.8f, iy + 8.2f);
            draw.pathLineTo(ix + 9.2f, iy + 3.2f);
            draw.pathStroke(markCol, false, 1.15f);
        } else if (cb.indeterminate()) {
            float dashW = boxSize * 0.5f;
            float dashH = 2.0f;
            ColorView markCol = cb.enabled() ? TEXT_PRIMARY : TEXT_MUTED;
            draw.rectFilled(indicatorX + (boxSize - dashW) * 0.5f,
                    indicatorY + (boxSize - dashH) * 0.5f, dashW, dashH, markCol);
        }

        // Текст чекбокса (если есть)
        String text = cb.text();
        if (text != null && !text.isEmpty()) {
            float textX = indicatorX + boxSize + cb.textGap();
            float textY = y + (height - 10.0f) * 0.5f;
            draw.text(text, textX, textY, Math.max(0.0f, width - (textX - x)), height, Paint.fill(TEXT_PRIMARY));
        }
    });

    /**
     * Рендерер кнопки выбора целевого объекта (Target Texture: ⊙)
     * с центрированной векторной иконкой мишени/круга и подсветкой при наведении.
     */
    public static final WidgetRender TARGET_PICKER_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();

        ColorView bg = btn.pressed() ? BG_WORKSPACE : btn.hovered() ? BG_BUTTON_HOVER : BG_INPUT;
        draw.roundedRectFilled(x, y, w, h, 2.0f, bg);

        ColorView borderCol = btn.pressed() ? BORDER_INPUT_FOCUS : btn.hovered() ? TEXT_PRIMARY : BORDER_INPUT;
        draw.roundedRectStroke(x, y, w, h, 2.0f, borderCol, 1.0f);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        float r = 4.2f;
        ColorView iconCol = btn.hovered() ? TEXT_PRIMARY : TEXT_LABEL;
        draw.circleStroke(cx - r, cy - r, r * 2.0f, r * 2.0f, iconCol, 1.0f);
        draw.circleFilled(cx - 1.2f, cy - 1.2f, 2.4f, 2.4f, iconCol);
    });

    /**
     * Рендерер кнопки пипетки выбора цвета с центрированной векторной пипеткой.
     */
    public static final WidgetRender EYEDROPPER_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();

        ColorView bg = btn.pressed() ? BG_WORKSPACE : btn.hovered() ? BG_BUTTON_HOVER : BG_INPUT;
        draw.roundedRectFilled(x, y, w, h, 2.0f, bg);

        ColorView borderCol = btn.pressed() ? BORDER_INPUT_FOCUS : btn.hovered() ? TEXT_PRIMARY : BORDER_INPUT;
        draw.roundedRectStroke(x, y, w, h, 2.0f, borderCol, 1.0f);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        ColorView col = btn.hovered() ? TEXT_PRIMARY : TEXT_LABEL;
        draw.pathClear();
        draw.pathLineTo(cx + 3.2f, cy - 3.2f);
        draw.pathLineTo(cx - 0.8f, cy + 0.8f);
        draw.pathLineTo(cx - 3.2f, cy + 3.2f);
        draw.pathStroke(col, false, 1.4f);
    });

    /**
     * Рендерер опций выпадающего списка (ComboBox popup items) в палитре Unity:
     * - Покой: нейтральный тёмно-серый фон (#262626), текст TEXT_PRIMARY (#DCDCDC)
     * - Выбранный пункт: глубокий синий фон (#244462), текст TEXT_BOLD (#FFFFFF)
     * - Наведение (Hover): фирменный Unity Selection Blue (#2C5D88), рамка фокуса (#488EEE), текст TEXT_BOLD (#FFFFFF)
     * - Нажатие (Press): глубокий синий (#204870)
     */
    public static final WidgetRender DROPDOWN_OPTION_RENDERER = WidgetRender.of(ToggleButton.class, (draw, btn) -> {
        RectView bounds = btn.layoutBounds();
        float x = bounds.x();
        float y = bounds.y();
        float width = Math.max(0.0f, bounds.width());
        float height = Math.max(0.0f, bounds.height());
        if (width <= 0.0f || height <= 0.0f) return;

        // Фон опции в палитре Unity Dark
        ColorView bg;
        if (btn.pressed()) {
            bg = BG_DROPDOWN_ITEM_PRESS;
        } else if (btn.hovered()) {
            bg = BG_DROPDOWN_ITEM_HOVER;
        } else if (btn.checked()) {
            bg = BG_DROPDOWN_ITEM_CHECKED;
        } else {
            bg = BG_DROPDOWN_ITEM_NORMAL;
        }
        draw.rectFilled(x, y, width, height, bg);

        // Тонкая рамка при наведении в стиле Unity Focus (1px)
        if (btn.hovered() && btn.enabled()) {
            draw.rectStroke(x, y, width, height, BORDER_DROPDOWN_ITEM_HOVER, 1.0f);
        }

        // Белый текст опции (без uppercase, сохраняя оригинальный регистр строк)
        String plain = btn.text();
        RichText rich = btn.richText();
        float textX = x + 7.0f;
        float textWidth = Math.max(0.0f, width - 14.0f);
        if (textWidth > 0.0f) {
            ColorView textCol = (btn.hovered() || btn.checked() || btn.pressed()) ? TEXT_BOLD : TEXT_PRIMARY;
            float textHeight = rich != null && !rich.isEmpty() ? TextEngine.measureTextHeight(rich) : 9.0f;
            float drawY = y + Math.max(0.0f, height - textHeight) * 0.5f;
            draw.pushClip(textX, y, textWidth, height);
            try {
                if (rich != null && !rich.isEmpty()) {
                    draw.text(rich, textX, drawY, textWidth, textHeight, Paint.fill(textCol));
                } else if (plain != null && !plain.isEmpty()) {
                    draw.text(plain, textX, drawY, textWidth, textHeight, Paint.fill(textCol));
                }
            } finally {
                draw.popClip();
            }
        }
    });

    public static void styleCheckbox(Checkbox checkbox) {
        checkbox.themeEnabled(false);
        checkbox.boxVisualEnabled(false);
        checkbox.focusRingVisible(false);
        checkbox.focusable(false);
        checkbox.renderer(CHECKBOX_RENDERER);
        checkbox.boxSize(12.0f);
        checkbox.checkSize(8.0f);
        checkbox.textGap(4.0f);
    }

    public static void styleNumberField(NumberField field) {
        field.themeEnabled(false);
        field.backgroundVisible(true);
        field.background().set(BG_INPUT);
        field.borderVisible(true);
        field.borderColor().set(BORDER_INPUT);
        field.borderWidth(1.0f);
        field.radius(RADIUS);
        field.textColor().set(TEXT_PRIMARY);
        field.textPaddingX(5.0f);
        field.layout(style -> style.height(ROW_HEIGHT));
    }

    public static void styleTextInput(TextInput input) {
        input.themeEnabled(false);
        input.backgroundVisible(true);
        input.background().set(BG_INPUT);
        input.borderVisible(true);
        input.borderColor().set(BORDER_INPUT);
        input.borderWidth(1.0f);
        input.radius(RADIUS);
        input.textColor().set(TEXT_PRIMARY);
        input.textPaddingX(5.0f);
        input.layout(style -> style.height(ROW_HEIGHT));
    }

    public static void styleComboBox(ComboBox combo) {
        combo.optionRowHeight(18.0f);
        combo.optionsHost().themeEnabled(false);
        combo.optionsHost().backgroundVisible(true);
        combo.optionsHost().background().set(BG_DROPDOWN_HOST);
        combo.optionsHost().borderVisible(true);
        combo.optionsHost().borderColor().set(BORDER_DROPDOWN_HOST);
        combo.optionsHost().borderWidth(1.0f);
        combo.optionsHost().radius(2.0f);

        styleScroll(combo.optionsScroll());

        Button header = combo.headerButton();
        header.themeEnabled(false);
        header.backgroundVisible(true);
        header.background().set(BG_INPUT);
        header.borderVisible(true);
        header.borderColor().set(BORDER_INPUT);
        header.radius(RADIUS);
        header.textPaddingX(5.0f);
        header.textColor().set(TEXT_PRIMARY);
        header.layout(style -> style.height(ROW_HEIGHT));

        combo.layout(style -> style.height(ROW_HEIGHT));

        for (int i = 0; i < combo.itemCount(); i++) {
            ToggleButton option = combo.optionButton(i);
            styleComboBoxOption(option);
        }
    }

    public static void styleComboBoxOption(ToggleButton option) {
        option.themeEnabled(false);
        option.backgroundVisible(false);
        option.borderVisible(false);
        option.focusRingVisible(false);
        option.renderer(DROPDOWN_OPTION_RENDERER);
        option.layout(style -> style
                .size(LayoutConstraints.AUTO, 18.0f)
                .margin(0.0f, 0.0f)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
    }

    public static void styleButton(Button button) {
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.background().set(BG_BUTTON);
        button.hoverBackground().set(BG_BUTTON_HOVER);
        button.pressedBackground().set(BG_BUTTON_ACTIVE);
        button.borderVisible(true);
        button.borderColor().set(BORDER_BUTTON);
        button.radius(RADIUS);
        button.textPaddingX(8.0f);
        button.textColor().set(TEXT_PRIMARY);
    }

    public static Box separatorLine() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.background().set(SEPARATOR);
        line.borderVisible(false);
        line.layout(style -> style.widthPercent(100.0f).height(1.0f).flexShrink(0.0f));
        return line;
    }

    public static Label label(String text) {
        Label lbl = new Label(text);
        lbl.color(TEXT_LABEL);
        lbl.layout(style -> style.width(LABEL_WIDTH).height(ROW_HEIGHT));
        return lbl;
    }

    public static Label labelMuted(String text, float width) {
        Label lbl = new Label(text);
        lbl.color(TEXT_MUTED);
        lbl.layout(style -> style.width(width).height(ROW_HEIGHT));
        return lbl;
    }

    /**
     * Кастомный рендерер элементов контекстного меню в стиле Unity:
     * - при наведении синяя подсветка SELECTION_BLUE и белый текст
     * - при нажатии тёмно-синяя заливка BG_DROPDOWN_ITEM_PRESS
     * - для отключённых элементов серый цвет TEXT_MUTED и отсутствие hover-фона
     * - ровное выравнивание по левому краю с аккуратным отступом
     */
    public static final WidgetRender CONTEXT_MENU_ITEM_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;

        boolean enabled = btn.enabled();
        boolean hovered = btn.hovered() && enabled;
        boolean pressed = btn.pressed() && enabled;

        if (pressed) {
            draw.roundedRectFilled(x, y, w, h, 2.0f, BG_DROPDOWN_ITEM_PRESS);
        } else if (hovered) {
            draw.roundedRectFilled(x, y, w, h, 2.0f, SELECTION_BLUE);
        }

        ColorView textColor;
        if (!enabled) {
            textColor = TEXT_MUTED;
        } else if (hovered || pressed) {
            textColor = TEXT_BOLD;
        } else {
            textColor = TEXT_PRIMARY;
        }

        RichText rt = RichText.plain(btn.text());
        float padX = 14.0f;

        String shortcut = (btn instanceof UnityContextMenuItem item) ? item.shortcut() : null;
        float shortcutWidth = 0.0f;
        if (shortcut != null && !shortcut.isEmpty()) {
            RichText srt = RichText.plain(shortcut);
            shortcutWidth = TextEngine.measureLineWidth(draw.context(), srt) + 8.0f;
            float sHeight = TextEngine.measureTextHeight(draw.context(), srt);
            float sX = x + w - padX - (shortcutWidth - 8.0f);
            float sY = y + Math.max(0.0f, h - sHeight) * 0.5f;
            ColorView sColor = !enabled ? TEXT_MUTED : (hovered || pressed ? TEXT_BOLD : TEXT_MUTED);
            TextEngine.drawInline(draw, srt, sX, sY, shortcutWidth - 8.0f, sHeight, Paint.fill(sColor));
        }

        float contentWidth = Math.max(0.0f, w - padX * 2.0f - shortcutWidth);
        float textHeight = TextEngine.measureTextHeight(draw.context(), rt);
        float drawY = y + Math.max(0.0f, h - textHeight) * 0.5f;

        draw.pushTextClip(x + padX, y, contentWidth, h);
        try {
            TextEngine.drawInline(draw, rt, x + padX, drawY, contentWidth, textHeight, Paint.fill(textColor));
        } finally {
            draw.popClip();
        }
    });

    /**
     * Стилизация всплывающего контекстного меню под тёмную тему Unity.
     */
    /**
     * Стилизация TreeView под компактный и строгий вид иерархии / проекта Unity.
     */
    public static void styleTreeView(TreeView tree) {
        if (tree == null) return;
        tree.style(style -> style
                .put(StyleKeys.TREE_ROW_HEIGHT, ROW_HEIGHT + 1.0f)
                .put(StyleKeys.TREE_ROW_STYLE, TreeRowStyle.FULL_WIDTH)
                .put(StyleKeys.TREE_DISCLOSURE_STYLE, TreeDisclosureStyle.TRIANGLE)
                .put(StyleKeys.TREE_ROW_RADIUS, RADIUS)
                .put(StyleKeys.TREE_ROW_INSET_X, 3.0f)
                .put(StyleKeys.TREE_ROW_INSET_Y, 0.0f)
                .put(StyleKeys.TREE_INDENT_WIDTH, 12.0f)
                .put(StyleKeys.TREE_SELECTED_COLOR, SELECTION_BLUE)
                .put(StyleKeys.TREE_HOVER_COLOR, BG_HEADER)
                .put(StyleKeys.TREE_TEXT_COLOR, TEXT_PRIMARY)
                .put(StyleKeys.TREE_SELECTED_TEXT_COLOR, TEXT_BOLD)
                .put(StyleKeys.TREE_DISABLED_TEXT_COLOR, TEXT_MUTED)
                .put(StyleKeys.TREE_DISCLOSURE_COLOR, TEXT_MUTED)
                .put(StyleKeys.TREE_ICON_COLOR, TEXT_PRIMARY));
    }

    public static void styleContextMenu(ContextMenu menu) {
        if (menu == null) return;
        menu.themeEnabled(false);
        menu.backgroundVisible(true);
        menu.background().set(BG_DROPDOWN_HOST);
        menu.borderVisible(true);
        menu.borderColor().set(BORDER_DROPDOWN_HOST);
        menu.borderWidth(1.0f);
        menu.radius(3.0f);
        menu.padding(EdgeInsets.all(3.0f));
    }

    /**
     * Добавление элемента контекстного меню с кастомным рендерером и размером.
     */
    public static class UnityContextMenuItem extends Button {
        private String shortcut;

        public UnityContextMenuItem(String text) {
            this(text, null);
        }

        public UnityContextMenuItem(String text, String shortcut) {
            super(text);
            this.shortcut = shortcut;
        }

        public String shortcut() {
            return shortcut;
        }

        public void shortcut(String shortcut) {
            this.shortcut = shortcut;
        }
    }

    public static Button addContextMenuItem(ContextMenu menu, String text, String shortcut, boolean enabled, float menuWidth, Runnable action) {
        UnityContextMenuItem btn = new UnityContextMenuItem(text, shortcut);
        btn.themeEnabled(false);
        btn.enabled(enabled);
        btn.renderer(CONTEXT_MENU_ITEM_RENDERER);
        if (action != null) {
            btn.onClick(e -> action.run());
        }
        menu.item(btn);
        btn.layout(style -> style.size(menuWidth, 19.0f).flexGrow(0.0f).flexShrink(0.0f));
        return btn;
    }

    public static Button addContextMenuItem(ContextMenu menu, String text, boolean enabled, float menuWidth, Runnable action) {
        return addContextMenuItem(menu, text, null, enabled, menuWidth, action);
    }

    public static Button addContextMenuSubmenu(ContextMenu menu, String text, ContextMenu subMenu, float menuWidth) {
        UnityContextMenuItem btn = new UnityContextMenuItem(text, ">");
        btn.themeEnabled(false);
        btn.renderer(CONTEXT_MENU_ITEM_RENDERER);
        btn.layout(style -> style.size(menuWidth, 19.0f).flexGrow(0.0f).flexShrink(0.0f));
        menu.submenu(btn, subMenu);
        return btn;
    }

    /**
     * Добавление тонкого разделителя в контекстное меню Unity.
     */
    public static Separator addContextMenuSeparator(ContextMenu menu, float menuWidth) {
        Separator sep = new Separator();
        sep.color().set(BORDER_SUBTLE);
        sep.thickness(1.0f);
        menu.separator(sep);
        sep.layout(style -> style.size(menuWidth, 1.0f).margin(1.0f, 2.0f).flexGrow(0.0f).flexShrink(0.0f));
        return sep;
    }

    /**
     * Стилизация подсказки Tooltip в стиле Unity.
     */
    public static void styleTooltip(Tooltip tooltip) {
        if (tooltip == null) return;
        tooltip.themeEnabled(false);
        tooltip.backgroundVisible(true);
        tooltip.background().set(BG_WORKSPACE);
        tooltip.borderVisible(true);
        tooltip.borderColor().set(BORDER_SUBTLE);
        tooltip.borderWidth(1.0f);
        tooltip.radius(3.0f);
        tooltip.textColor().set(TEXT_PRIMARY);
        tooltip.offset(4.0f, 6.0f);
    }

    /**
     * Сегментная кнопка пути в строке хлебных крошек Unity.
     */
    public static class BreadcrumbSegmentButton extends Button {
        private boolean current;

        public BreadcrumbSegmentButton(String text, boolean current) {
            super(text);
            this.current = current;
            themeEnabled(false);
            textPadding(4.0f, 2.0f);
        }

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }
    }

    /**
     * Рендерер сегментов пути (хлебных крошек) в стиле Unity:
     * - текущая папка выделяется более ярким шрифтом (TEXT_BOLD)
     * - промежуточные папки отображаются цветом TEXT_PRIMARY
     * - при наведении или нажатии отображается аккуратный закруглённый hover-фон
     */
    public static final WidgetRender BREADCRUMB_SEGMENT_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;

        boolean isCurrent = (btn instanceof BreadcrumbSegmentButton sb) && sb.isCurrent();

        if (btn.pressed()) {
            draw.roundedRectFilled(x, y + 1.0f, w, h - 2.0f, 2.0f, BG_DROPDOWN_ITEM_PRESS);
        } else if (btn.hovered()) {
            draw.roundedRectFilled(x, y + 1.0f, w, h - 2.0f, 2.0f, MutableColor.rgba(1.0f, 1.0f, 1.0f, 0.08f));
        }

        ColorView textColor;
        if (isCurrent) {
            textColor = TEXT_BOLD;
        } else if (btn.hovered() || btn.pressed()) {
            textColor = TEXT_BOLD;
        } else {
            textColor = TEXT_PRIMARY;
        }

        RichText rt = RichText.plain(btn.text());
        float textWidth = TextEngine.measureLineWidth(draw.context(), rt);
        float textHeight = TextEngine.measureTextHeight(draw.context(), rt);
        float textX = x + Math.max(0.0f, w - textWidth) * 0.5f;
        float textY = y + Math.max(0.0f, h - textHeight) * 0.5f;

        draw.pushTextClip(x, y, w, h);
        try {
            TextEngine.drawInline(draw, rt, textX, textY, w, textHeight, Paint.fill(textColor));
        } finally {
            draw.popClip();
        }
    });

    /**
     * Рендерер кнопки-стрелочки шеврона (>) в хлебных крошках Unity:
     * - процедурная стрелочка вправо правильных пропорций Unity
     * - при наведении подсвечивается ярким цветом и полупрозрачным фоном
     */
    public static final WidgetRender BREADCRUMB_CHEVRON_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;

        if (btn.pressed()) {
            draw.roundedRectFilled(x, y + 1.0f, w, h - 2.0f, 2.0f, BG_DROPDOWN_ITEM_PRESS);
        } else if (btn.hovered()) {
            draw.roundedRectFilled(x, y + 1.0f, w, h - 2.0f, 2.0f, MutableColor.rgba(1.0f, 1.0f, 1.0f, 0.08f));
        }

        ColorView chevronColor = (btn.hovered() || btn.pressed()) ? TEXT_BOLD : TEXT_MUTED;
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;

        draw.pathClear();
        draw.pathLineTo(cx - 1.5f, cy - 3.5f);
        draw.pathLineTo(cx + 2.0f, cy);
        draw.pathLineTo(cx - 1.5f, cy + 3.5f);
        draw.pathStroke(chevronColor, false, 1.4f);
    });

    /**
     * Элемент выпадающего меню хлебных крошек с отметкой активной папки (галочка в голубом квадрате).
     */
    public static class BreadcrumbMenuItem extends Button {
        private boolean checked;

        public BreadcrumbMenuItem(String text, boolean checked) {
            super(text);
            this.checked = checked;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    /**
     * Рендерер элементов выпадающего меню хлебных крошек (Скриншот 2):
     * - при наведении синяя подсветка SELECTION_BLUE
     * - слева слот для чекбокса
     * - если элемент выбран (находится на текущем пути), рисуется голубой квадратик (#90C8F6)
     *   с тёмной галочкой внутри (✓)
     */
    public static final WidgetRender BREADCRUMB_DROPDOWN_ITEM_RENDERER = WidgetRender.of(Button.class, (draw, btn) -> {
        RectView b = btn.layoutBounds();
        float x = b.x();
        float y = b.y();
        float w = b.width();
        float h = b.height();
        if (w <= 0.0f || h <= 0.0f) return;

        boolean enabled = btn.enabled();
        boolean hovered = btn.hovered() && enabled;
        boolean pressed = btn.pressed() && enabled;

        if (pressed) {
            draw.roundedRectFilled(x, y, w, h, 2.0f, BG_DROPDOWN_ITEM_PRESS);
        } else if (hovered) {
            draw.roundedRectFilled(x, y, w, h, 2.0f, SELECTION_BLUE);
        }

        boolean isChecked = (btn instanceof BreadcrumbMenuItem item) && item.isChecked();
        float slotWidth = 24.0f;

        if (isChecked) {
            float boxSize = 12.0f;
            float boxX = x + 6.0f;
            float boxY = y + Math.max(0.0f, h - boxSize) * 0.5f;

            // Голубой квадрат (#90C8F6)
            ColorView boxBg = MutableColor.rgba255(144, 200, 246, 255);
            draw.roundedRectFilled(boxX, boxY, boxSize, boxSize, 2.0f, boxBg);

            // Тёмная галочка внутри квадрата (✓)
            ColorView checkColor = MutableColor.rgba255(32, 32, 32, 255);
            draw.pathClear();
            draw.pathLineTo(boxX + 2.8f, boxY + 5.8f);
            draw.pathLineTo(boxX + 4.8f, boxY + 8.4f);
            draw.pathLineTo(boxX + 9.2f, boxY + 3.2f);
            draw.pathStroke(checkColor, false, 1.3f);
        }

        ColorView textColor;
        if (!enabled) {
            textColor = TEXT_MUTED;
        } else if (hovered || pressed) {
            textColor = TEXT_BOLD;
        } else {
            textColor = TEXT_PRIMARY;
        }

        RichText rt = RichText.plain(btn.text());
        float textX = x + slotWidth;
        float contentWidth = Math.max(0.0f, w - (slotWidth + 8.0f));
        float textHeight = TextEngine.measureTextHeight(draw.context(), rt);
        float drawY = y + Math.max(0.0f, h - textHeight) * 0.5f;

        draw.pushTextClip(textX, y, contentWidth, h);
        try {
            TextEngine.drawInline(draw, rt, textX, drawY, contentWidth, textHeight, Paint.fill(textColor));
        } finally {
            draw.popClip();
        }
    });

    /**
     * Добавление элемента в выпадающее меню хлебных крошек.
     */
    public static Button addBreadcrumbDropdownItem(ContextMenu menu, String folderName, boolean isChecked, float menuWidth, Runnable action) {
        BreadcrumbMenuItem btn = new BreadcrumbMenuItem(folderName, isChecked);
        btn.themeEnabled(false);
        btn.renderer(BREADCRUMB_DROPDOWN_ITEM_RENDERER);
        if (action != null) {
            btn.onClick(e -> action.run());
        }
        menu.item(btn);
        btn.layout(style -> style.size(menuWidth, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        return btn;
    }

    // ------------------------------------------------------------------ готовые элементы редактора

    /**
     * Поле поиска: то же поле ввода, но с лупой внутри, поэтому назначение понятно и без подписи.
     *
     * @return поле поиска панели
     */
    public static dev.sixik.unigui.widgets.interaction.TextField searchField() {
        dev.sixik.unigui.widgets.interaction.TextField field = new SearchBox();
        styleTextInput(field);
        field.textPaddingLeft(19.0f);
        field.placeholder("Search");
        return field;
    }

    /**
     * @param field поле поиска
     * @return строка с полем поиска и отступами панели
     */
    public static dev.sixik.unigui.widgets.containers.HBox searchBar(dev.sixik.unigui.widgets.interaction.TextField field) {
        dev.sixik.unigui.widgets.containers.HBox bar = new dev.sixik.unigui.widgets.containers.HBox();
        bar.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).padding(4.0f, 4.0f, 4.0f, 3.0f));
        field.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).width(10.0f));
        bar.addChild(field);
        bar.applyQueuedMutations();
        return bar;
    }

    /** Поле с лупой: значок рисуется поверх обычного поля и не мешает вводу. */
    private static final class SearchBox extends dev.sixik.unigui.widgets.interaction.TextField {
        @Override
        public void render(dev.sixik.unigui.api.render.RenderContext context) {
            super.render(context);
            RectView b = layoutBounds();
            float size = 11.0f;
            EditorIcons.SEARCH.draw(new dev.sixik.unigui.api.render.DrawScope(context, transform(), b), b.x() + 5.0f,
                    b.y() + (b.height() - size) * 0.5f, size, TEXT_MUTED);
        }
    }

    /** @return текстовое поле в стиле Unity: низкое, тёмное, с тонкой рамкой */
    public static dev.sixik.unigui.widgets.interaction.TextField textField() {
        dev.sixik.unigui.widgets.interaction.TextField field = new dev.sixik.unigui.widgets.interaction.TextField();
        styleTextInput(field);
        return field;
    }

    /**
     * @param text начальный текст
     * @return текстовое поле в стиле Unity
     */
    public static dev.sixik.unigui.widgets.interaction.TextField textField(String text) {
        dev.sixik.unigui.widgets.interaction.TextField field = textField();
        field.text(text == null ? "" : text);
        return field;
    }

    /**
     * @param text подпись
     * @return кнопка в стиле Unity высотой в строку
     */
    public static Button button(String text) {
        Button button = new Button(text);
        styleButton(button);
        button.layout(style -> style.height(ROW_HEIGHT));
        return button;
    }

    /** @return выпадающий список в стиле Unity; пункты, добавленные позже, нужно стилизовать через {@link #restyle} */
    public static ComboBox combo() {
        ComboBox combo = new ComboBox();
        styleComboBox(combo);
        return combo;
    }

    /**
     * Дополнительно стилизует пункты списка после {@code items(...)}.
     *
     * @param combo список с уже заданными пунктами
     * @return этот список
     */
    public static ComboBox restyle(ComboBox combo) {
        styleComboBox(combo);
        return combo;
    }

    /**
     * @param text подпись
     * @return заголовок раздела: светлый текст, строка заголовка Unity
     */
    public static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.color(TEXT_BOLD);
        label.layout(style -> style.height(COMPONENT_HEADER_HEIGHT));
        return label;
    }

    /**
     * Строка свойства: подпись фиксированной ширины и поле, как в инспекторе Unity.
     *
     * @param name  подпись свойства
     * @param field виджет значения
     * @return строка
     */
    public static dev.sixik.unigui.widgets.containers.HBox propertyRow(String name, dev.sixik.unigui.api.widget.Widget field) {
        dev.sixik.unigui.widgets.containers.HBox row = new dev.sixik.unigui.widgets.containers.HBox();
        row.spacing(4.0f);
        row.layout(style -> style.widthPercent(100.0f).height(ROW_HEIGHT + 2.0f).flexShrink(0.0f));
        Label label = label(name);
        row.addChild(label);
        if (field instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
            base.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        }
        row.addChild(field);
        row.applyQueuedMutations();
        return row;
    }

    /**
     * Полоса прокрутки редактора: тонкий ползунок поверх невидимого трека, который не отнимает место у содержимого.
     *
     * @param scroll область прокрутки
     */
    public static void styleScroll(dev.sixik.unigui.widgets.containers.ScrollView scroll) {
        scroll.overlayScrollBars(true);
        scroll.scrollbarSize(SCROLLBAR_SIZE);
        scroll.scrollbarGap(1.0f);
        scroll.scrollStep(ROW_HEIGHT);
        scroll.scrollbarTrackColor().set(SCROLLBAR_TRACK);
        scroll.scrollbarThumbColor().set(SCROLLBAR_THUMB);
        scroll.scrollbarThumbHoverColor().set(TEXT_MUTED);
    }

    /**
     * @param context контекст, который получает колбэк холста
     * @param widget  холст
     * @return область рисования холста: она нужна векторным иконкам
     */
    public static dev.sixik.unigui.api.render.DrawScope scope(dev.sixik.unigui.api.render.RenderContext context,
                                                              dev.sixik.unigui.impl.widget.WidgetBase widget) {
        return new dev.sixik.unigui.api.render.DrawScope(context, widget.transform(), widget.layoutBounds());
    }

    /**
     * Цвет типа данных: один и тот же у пина ноды, провода и записи контракта, поэтому тип узнаётся с первого взгляда.
     *
     * @param type имя типа
     * @return цвет {@code 0xAARRGGBB}
     */
    public static int pinColor(String type) {
        return switch (type == null ? "" : type) {
            case "bool" -> 0xFFE05A5A;
            case "int" -> 0xFF3CCBB2;
            case "float", "double" -> 0xFF8FD65A;
            case "string" -> 0xFFE872D2;
            case "color" -> 0xFFF0B72F;
            case "widget" -> 0xFF5AA2FF;
            default -> 0xFF9BA1AC;
        };
    }

    /**
     * @param text надпись
     * @return мелкая подпись-комментарий: приглушённый текст без фиксированной ширины
     */
    public static Label caption(String text) {
        Label label = new Label(text);
        label.color(TEXT_MUTED);
        return label;
    }

    /**
     * Приводит готовое поддерево панели к виду редактора: поля ввода, кнопки, списки, флажки и полосы прокрутки
     * получают стили Unity. Панели строят содержимое обычными виджетами и вызывают это после каждой пересборки, поэтому
     * правила вида лежат в одном месте. Деревья и полотна графа стилизуются отдельно и пропускаются.
     *
     * @param root корень поддерева
     */
    public static void apply(dev.sixik.unigui.api.widget.Widget root) {
        if (root == null) return;
        if (root instanceof dev.sixik.unigui.widgets.graph.NodeGraph || root instanceof TreeView) return;
        if (root instanceof ComboBox combo) {
            styleComboBox(combo);
            return;
        }
        if (root instanceof FlatField) return; // поле-подпись оформлено само
        if (root instanceof Checkbox checkbox) {
            styleCheckbox(checkbox);
        } else if (root instanceof NumberField number) {
            styleNumberField(number);
        } else if (root instanceof TextInput input) {
            styleTextInput(input);
        } else if (root instanceof ToolButton || root instanceof SectionHeader || root instanceof ToggleButton) {
            // у этих кнопок свой вид
        } else if (root instanceof Button button) {
            styleButton(button);
            button.layout(style -> style.height(ROW_HEIGHT));
        } else if (root instanceof dev.sixik.unigui.widgets.containers.ScrollView scroll) {
            styleScroll(scroll);
        }
        for (dev.sixik.unigui.api.widget.Widget child : root.children()) apply(child);
    }
}
