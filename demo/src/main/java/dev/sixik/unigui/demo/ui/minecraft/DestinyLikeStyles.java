package dev.sixik.unigui.demo.ui.minecraft;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleDefinition;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.TextTransform;
import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.demo.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.demo.ui.renders.DestinyLikeDropDownRenders;
import dev.sixik.unigui.demo.ui.renders.DestinyLikeScrollBarRenders;
import dev.sixik.unigui.demo.ui.renders.DestinyLikeSliderRenders;
import dev.sixik.unigui.demo.ui.renders.DestinyLikeTooltipRenders;

/**
 * Локальный StylePack с renderer-переопределениями оформления DestinyLike.
 *
 * <p>Пакет оставляет цвета и динамику в самих контролах, а выбор процедурного
 * renderer'а выполняется каскадом Style API по типу и style-class.</p>
 */
public final class DestinyLikeStyles {
    /**
     * Selector class кнопок действий меню.
     */
    public static final String ACTION = "destiny-action";
    /**
     * Selector class заголовка списка выбора.
     */
    public static final String DROPDOWN_HEADER = "destiny-dropdown-header";
    /**
     * Selector class строки списка выбора.
     */
    public static final String DROPDOWN_OPTION = "destiny-dropdown-option";
    /**
     * Selector class поверхности выпадающего списка.
     */
    public static final String DROPDOWN_HOST = "destiny-dropdown-host";
    /**
     * Selector class ползунков меню.
     */
    public static final String SLIDER = "destiny-slider";
    /**
     * Selector class флажков меню.
     */
    public static final String CHECKBOX = "destiny-checkbox";
    /**
     * Selector class переключателей меню.
     */
    public static final String TOGGLE_SWITCH = "destiny-toggle-switch";
    /**
     * Selector class полос прокрутки меню.
     */
    public static final String SCROLLBAR = "destiny-scrollbar";
    /**
     * Selector class всплывающих подсказок меню.
     */
    public static final String TOOLTIP = "destiny-tooltip";

    /**
     * Registry id renderer'а кнопок действий.
     */
    private static final String ID_BUTTON = "testmod:destiny/button";
    /**
     * Registry id renderer'а заголовка списка.
     */
    private static final String ID_DROPDOWN_HEADER = "testmod:destiny/dropdown-header";
    /**
     * Registry id renderer'а ползунка.
     */
    private static final String ID_SLIDER = "testmod:destiny/slider";
    /**
     * Registry id renderer'а полосы прокрутки.
     */
    private static final String ID_SCROLLBAR = "testmod:destiny/scrollbar";
    /**
     * Registry id renderer'а подсказки.
     */
    private static final String ID_TOOLTIP = "testmod:destiny/tooltip";
    /**
     * Общий пакет стилей экрана; renderer'ы регистрируются при его инициализации.
     */
    public static final StylePack PACK = create();

    private DestinyLikeStyles() {
    }

    /**
     * Создаёт пакет и регистрирует его renderer'ы в общем реестре.
     *
     * @return новый локальный пакет оформления DestinyLike
     */
    static StylePack create() {
        WidgetRenderRegistry registry = WidgetRenderRegistry.global();
        registry.register(ID_BUTTON, WidgetRole.BUTTON, DestinyLikeButtonRenders.DEFAULT);
        registry.register(ID_DROPDOWN_HEADER, WidgetRole.BUTTON, DestinyLikeDropDownRenders.HEADER);
        registry.register(ID_SLIDER, WidgetRole.SLIDER, DestinyLikeSliderRenders.DEFAULT);
        registry.register(ID_SCROLLBAR, WidgetRole.PANEL, DestinyLikeScrollBarRenders.DEFAULT);
        registry.register(ID_TOOLTIP, WidgetRole.POPUP, DestinyLikeTooltipRenders.DEFAULT);

        return StylePack.create("testmod:destiny-like")
                .put(renderer("button.action", StyleIds.Widget.BUTTON, ACTION, ID_BUTTON,
                        new MutableStyle()
                                .put(StyleKeys.TEXT_SIZE, 3.2f)
                                .put(StyleKeys.TEXT_TRACKING, 0.34f)
                                .put(StyleKeys.TEXT_TRANSFORM, TextTransform.UPPERCASE)))
                .put(renderer("button.dropdown.header", StyleIds.Widget.BUTTON, DROPDOWN_HEADER, ID_DROPDOWN_HEADER))
                .put(style("toggle.dropdown.option", StyleIds.Widget.TOGGLE_BUTTON, DROPDOWN_OPTION,
                        new MutableStyle()
                                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, MutableColor.rgba255(22, 25, 31, 246))
                                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, MutableColor.rgba255(39, 43, 52, 246))
                                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, MutableColor.rgba255(39, 43, 52, 246))
                                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, MutableColor.rgba255(39, 43, 52, 246))
                                .put(StyleKeys.BORDER_VISIBLE, false)
                                .put(StyleKeys.BORDER_VISIBLE, WidgetState.HOVERED, true)
                                .put(StyleKeys.BORDER_VISIBLE, WidgetState.PRESSED, true)
                                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, MutableColor.rgba255(238, 241, 247, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.PRESSED, MutableColor.rgba255(238, 241, 247, 255))
                                .put(StyleKeys.BORDER_WIDTH, 0.18f)
                                .put(StyleKeys.RADIUS, 0.0f)
                                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba255(255, 255, 255, 255))
                                .put(StyleKeys.TEXT_SIZE, 4.2f)
                                .put(StyleKeys.TEXT_TRACKING, 0.32f)
                                .put(StyleKeys.TEXT_TRANSFORM, TextTransform.UPPERCASE)))
                .put(style("box.dropdown.host", StyleIds.Widget.BOX, DROPDOWN_HOST,
                        new MutableStyle()
                                .put(StyleKeys.BACKGROUND_VISIBLE, true)
                                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.09f, 0.10f, 0.13f, 0.98f))
                                .put(StyleKeys.BORDER_VISIBLE, true)
                                .put(StyleKeys.BORDER_COLOR, MutableColor.rgba(0.41f, 0.43f, 0.48f, 0.88f))
                                .put(StyleKeys.BORDER_WIDTH, 0.18f)
                                .put(StyleKeys.RADIUS, 0.0f)))
                .put(renderer("slider.default", StyleIds.Widget.SLIDER, SLIDER, ID_SLIDER))
                .put(style("checkbox.default", StyleIds.Widget.CHECKBOX, CHECKBOX,
                        new MutableStyle()
                                .put(StyleKeys.CHECKBOX_BACKGROUND_COLOR, WidgetState.NORMAL, MutableColor.rgba255(45, 47, 53, 255))
                                .put(StyleKeys.CHECKBOX_BACKGROUND_COLOR, WidgetState.CHECKED, MutableColor.rgba255(24, 76, 43, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.NORMAL, MutableColor.rgba255(126, 129, 138, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, MutableColor.rgba255(90, 165, 106, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, MutableColor.rgba255(255, 255, 255, 255))
                                .put(StyleKeys.BORDER_WIDTH, 0.16f)
                                .put(StyleKeys.RADIUS, 0.0f)
                                .put(StyleKeys.ACCENT_COLOR, MutableColor.rgba255(90, 165, 106, 255))
                                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba255(245, 247, 255, 255))
                                .put(StyleKeys.TEXT_SIZE, 4.4f)
                                .put(StyleKeys.TEXT_TRACKING, 0.34f)
                                .put(StyleKeys.TEXT_TRANSFORM, TextTransform.UPPERCASE)))
                .put(style("toggle-switch.default", StyleIds.Widget.TOGGLE_SWITCH, TOGGLE_SWITCH,
                        new MutableStyle()
                                .put(StyleKeys.TRACK_COLOR, WidgetState.NORMAL, MutableColor.rgba255(45, 47, 53, 255))
                                .put(StyleKeys.TRACK_COLOR, WidgetState.CHECKED, MutableColor.rgba255(24, 76, 43, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.NORMAL, MutableColor.rgba255(126, 129, 138, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, MutableColor.rgba255(90, 165, 106, 255))
                                .put(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, MutableColor.rgba255(255, 255, 255, 255))
                                .put(StyleKeys.BORDER_WIDTH, 0.16f)
                                .put(StyleKeys.RADIUS, 0.0f)
                                .put(StyleKeys.THUMB_COLOR, WidgetState.NORMAL, MutableColor.rgba255(192, 195, 202, 255))
                                .put(StyleKeys.THUMB_COLOR, WidgetState.CHECKED, MutableColor.rgba255(90, 165, 106, 255))
                                .put(StyleKeys.THUMB_RADIUS, 0.0f)
                                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba255(245, 247, 255, 255))
                                .put(StyleKeys.TEXT_SIZE, 4.4f)
                                .put(StyleKeys.TEXT_TRACKING, 0.34f)
                                .put(StyleKeys.TEXT_TRANSFORM, TextTransform.UPPERCASE)))
                .put(renderer("scrollbar.default", StyleIds.Widget.SCROLL_BAR, SCROLLBAR, ID_SCROLLBAR))
                .put(renderer("tooltip.default", StyleIds.Widget.TOOLTIP, TOOLTIP, ID_TOOLTIP));
    }

    private static StyleDefinition renderer(String id, String target, String styleClass, String rendererId) {
        return renderer(id, target, styleClass, rendererId, new MutableStyle());
    }

    private static StyleDefinition renderer(String id, String target, String styleClass, String rendererId,
                                            MutableStyle style) {
        return StyleDefinition.custom(id, rendererId, style)
                .target(target)
                .styleClass(styleClass);
    }

    private static StyleDefinition style(String id, String target, String styleClass, MutableStyle style) {
        return StyleDefinition.of(id, style)
                .target(target)
                .styleClass(styleClass);
    }
}
