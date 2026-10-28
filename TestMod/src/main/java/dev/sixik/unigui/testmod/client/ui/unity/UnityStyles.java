package dev.sixik.unigui.testmod.client.ui.unity;

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
 * Цветовая палитра и стили в духе Unity Dark Theme (Unity 2020 / 2022+).
 */
public final class UnityStyles {
    private UnityStyles() {
    }

    // --- Фоны и панели ---
    public static final MutableColor BG_WORKSPACE = MutableColor.rgba255(32, 32, 32, 255);        // #202020
    public static final MutableColor BG_WINDOW = MutableColor.rgba255(56, 56, 56, 255);           // #383838
    public static final MutableColor BG_PANEL = MutableColor.rgba255(48, 48, 48, 255);            // #303030
    public static final MutableColor BG_HEADER = MutableColor.rgba255(62, 62, 62, 255);           // #3E3E3E
    public static final MutableColor BG_HEADER_HOVER = MutableColor.rgba255(72, 72, 72, 255);     // #484848
    public static final MutableColor BG_TOOLBAR = MutableColor.rgba255(44, 44, 44, 255);          // #2C2C2C
    public static final MutableColor BG_TAB_ACTIVE = MutableColor.rgba255(56, 56, 56, 255);       // #383838
    public static final MutableColor BG_TAB_INACTIVE = MutableColor.rgba255(38, 38, 38, 255);     // #262626
    public static final MutableColor BG_INPUT = MutableColor.rgba255(42, 42, 42, 255);            // #2A2A2A
    public static final MutableColor BG_HELP_BOX = MutableColor.rgba255(44, 44, 44, 255);         // #2C2C2C
    public static final MutableColor BG_BUTTON = MutableColor.rgba255(76, 76, 76, 255);           // #4C4C4C
    public static final MutableColor BG_BUTTON_HOVER = MutableColor.rgba255(90, 90, 90, 255);     // #5A5A5A
    public static final MutableColor BG_BUTTON_ACTIVE = MutableColor.rgba255(60, 60, 60, 255);    // #3C3C3C
    public static final MutableColor TAB_ACCENT = MutableColor.rgba255(59, 120, 216, 255);        // #3B78D8
    public static final MutableColor WARNING_YELLOW = MutableColor.rgba255(245, 185, 45, 255);     // #F5B92D

    // --- Границы и разделители ---
    public static final MutableColor BORDER_DEFAULT = MutableColor.rgba255(34, 34, 34, 255);      // #222222
    public static final MutableColor BORDER_SUBTLE = MutableColor.rgba255(44, 44, 44, 255);       // #2C2C2C
    public static final MutableColor BORDER_INPUT = MutableColor.rgba255(32, 32, 32, 255);        // #202020
    public static final MutableColor BORDER_INPUT_FOCUS = MutableColor.rgba255(59, 120, 216, 255);// #3B78D8
    public static final MutableColor BORDER_BUTTON = MutableColor.rgba255(48, 48, 48, 255);       // #303030
    public static final MutableColor SEPARATOR = MutableColor.rgba255(36, 36, 36, 255);           // #242424

    // --- Цвета текста ---
    public static final MutableColor TEXT_PRIMARY = MutableColor.rgba255(220, 220, 220, 255);     // #DCDCDC
    public static final MutableColor TEXT_LABEL = MutableColor.rgba255(190, 190, 190, 255);       // #BEBEBE
    public static final MutableColor TEXT_MUTED = MutableColor.rgba255(136, 136, 136, 255);       // #888888
    public static final MutableColor TEXT_BOLD = MutableColor.rgba255(236, 236, 236, 255);        // #ECECEC
    public static final MutableColor TEXT_HELP_BOX = MutableColor.rgba255(196, 196, 196, 255);    // #C4C4C4

    // --- Оси координат Unity ---
    public static final MutableColor AXIS_X = MutableColor.rgba255(224, 93, 93, 255);             // Red X
    public static final MutableColor AXIS_Y = MutableColor.rgba255(123, 209, 94, 255);            // Green Y
    public static final MutableColor AXIS_Z = MutableColor.rgba255(90, 148, 232, 255);            // Blue Z

    // --- Акценты и иконки ---
    public static final MutableColor CUBE_BLUE_TOP = MutableColor.rgba255(75, 163, 227, 255);
    public static final MutableColor CUBE_BLUE_LEFT = MutableColor.rgba255(32, 114, 196, 255);
    public static final MutableColor CUBE_BLUE_RIGHT = MutableColor.rgba255(16, 76, 140, 255);
    public static final MutableColor AUDIO_GOLD = MutableColor.rgba255(244, 184, 64, 255);
    public static final MutableColor SELECTION_BLUE = MutableColor.rgba255(44, 93, 136, 255);      // #2C5D88
    public static final MutableColor PLAY_GREEN = MutableColor.rgba255(63, 166, 82, 255);

    // --- Выпадающие списки (Dropdown / ComboBox) в палитре Unity Dark ---
    public static final MutableColor BG_DROPDOWN_HOST = MutableColor.rgba255(34, 34, 34, 255);         // #222222
    public static final MutableColor BORDER_DROPDOWN_HOST = MutableColor.rgba255(44, 44, 44, 255);     // #2C2C2C
    public static final MutableColor BG_DROPDOWN_ITEM_NORMAL = MutableColor.rgba255(38, 38, 38, 255);  // #262626 (Unity dark gray)
    public static final MutableColor BG_DROPDOWN_ITEM_CHECKED = MutableColor.rgba255(36, 68, 98, 255); // #244462 (глубокий Unity Blue)
    public static final MutableColor BG_DROPDOWN_ITEM_HOVER = MutableColor.rgba255(44, 93, 136, 255);   // #2C5D88 (SELECTION_BLUE в Unity)
    public static final MutableColor BG_DROPDOWN_ITEM_PRESS = MutableColor.rgba255(32, 72, 112, 255);   // #204870
    public static final MutableColor BORDER_DROPDOWN_ITEM_HOVER = MutableColor.rgba255(72, 142, 238, 255); // #488EEE (акцент Unity Focus Blue)
    public static final MutableColor SCROLLBAR_TRACK = MutableColor.rgba255(26, 26, 26, 200);           // Тёмный трек скроллбара
    public static final MutableColor SCROLLBAR_THUMB = MutableColor.rgba255(88, 88, 88, 220);           // Серый ползунок скроллбара Unity

    // --- Размеры ---
    public static final float ROW_HEIGHT = 18.0f;
    public static final float COMPONENT_HEADER_HEIGHT = 22.0f;
    public static final float LABEL_WIDTH = 112.0f;

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
        float indicatorX = x + Math.max(0.0f, width - boxSize) * 0.5f;
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
        field.radius(2.0f);
        field.textColor().set(TEXT_PRIMARY);
        field.textPaddingX(4.0f);
        field.layout(style -> style.height(ROW_HEIGHT));
    }

    public static void styleTextInput(TextInput input) {
        input.themeEnabled(false);
        input.backgroundVisible(true);
        input.background().set(BG_INPUT);
        input.borderVisible(true);
        input.borderColor().set(BORDER_INPUT);
        input.borderWidth(1.0f);
        input.radius(2.0f);
        input.textColor().set(TEXT_PRIMARY);
        input.textPaddingX(4.0f);
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

        combo.optionsScroll().scrollbarGap(1.0f);
        combo.optionsScroll().scrollStep(18.0f);
        combo.optionsScroll().scrollbarTrackColor().set(SCROLLBAR_TRACK);
        combo.optionsScroll().scrollbarThumbColor().set(SCROLLBAR_THUMB);

        Button header = combo.headerButton();
        header.themeEnabled(false);
        header.backgroundVisible(true);
        header.background().set(BG_INPUT);
        header.borderVisible(true);
        header.borderColor().set(BORDER_INPUT);
        header.radius(2.0f);
        header.textPaddingX(6.0f);
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
        button.radius(3.0f);
        button.textPaddingX(8.0f);
        button.textColor().set(TEXT_BOLD);
    }

    public static Box separatorLine() {
        Box line = new Box();
        line.themeEnabled(false);
        line.backgroundVisible(true);
        line.background().set(SEPARATOR);
        line.borderVisible(false);
        line.layout(style -> style.widthPercent(100.0f).height(1.0f).margin(0.0f, 2.0f));
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
                .put(StyleKeys.TREE_ROW_HEIGHT, ROW_HEIGHT)
                .put(StyleKeys.TREE_ROW_STYLE, TreeRowStyle.FULL_WIDTH)
                .put(StyleKeys.TREE_DISCLOSURE_STYLE, TreeDisclosureStyle.TRIANGLE)
                .put(StyleKeys.TREE_ROW_RADIUS, 0.0f)
                .put(StyleKeys.TREE_ROW_INSET_X, 0.0f)
                .put(StyleKeys.TREE_ROW_INSET_Y, 0.0f)
                .put(StyleKeys.TREE_INDENT_WIDTH, 14.0f)
                .put(StyleKeys.TREE_SELECTED_COLOR, SELECTION_BLUE)
                .put(StyleKeys.TREE_HOVER_COLOR, BG_HEADER_HOVER)
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
}
