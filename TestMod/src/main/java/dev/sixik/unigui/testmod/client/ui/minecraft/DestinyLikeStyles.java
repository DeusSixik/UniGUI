package dev.sixik.unigui.testmod.client.ui.minecraft;

import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleDefinition;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.text.TextTransform;
import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeButtonRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeCheckboxRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeDropDownRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeScrollBarRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeSliderRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeTooltipRenders;
import dev.sixik.unigui.testmod.client.ui.renders.DestinyLikeToggleSwitchRenders;

/**
 * Локальный StylePack с renderer-переопределениями оформления DestinyLike.
 *
 * <p>Пакет оставляет цвета и динамику в самих контролах, а выбор процедурного
 * renderer'а выполняется каскадом Style API по типу и style-class.</p>
 */
final class DestinyLikeStyles {
    /**
     * Selector class кнопок действий меню.
     */
    static final String ACTION = "destiny-action";
    /**
     * Selector class заголовка списка выбора.
     */
    static final String DROPDOWN_HEADER = "destiny-dropdown-header";
    /**
     * Selector class строки списка выбора.
     */
    static final String DROPDOWN_OPTION = "destiny-dropdown-option";
    /**
     * Selector class поверхности выпадающего списка.
     */
    static final String DROPDOWN_HOST = "destiny-dropdown-host";
    /**
     * Selector class ползунков меню.
     */
    static final String SLIDER = "destiny-slider";
    /**
     * Selector class флажков меню.
     */
    static final String CHECKBOX = "destiny-checkbox";
    /**
     * Selector class переключателей меню.
     */
    static final String TOGGLE_SWITCH = "destiny-toggle-switch";
    /**
     * Selector class полос прокрутки меню.
     */
    static final String SCROLLBAR = "destiny-scrollbar";
    /**
     * Selector class всплывающих подсказок меню.
     */
    static final String TOOLTIP = "destiny-tooltip";

    /**
     * Registry id renderer'а кнопок действий.
     */
    private static final String ID_BUTTON = "testmod:destiny/button";
    /**
     * Registry id renderer'а заголовка списка.
     */
    private static final String ID_DROPDOWN_HEADER = "testmod:destiny/dropdown-header";
    /**
     * Registry id renderer'а вариантов списка.
     */
    private static final String ID_DROPDOWN_OPTION = "testmod:destiny/dropdown-option";
    /**
     * Registry id renderer'а поверхности списка.
     */
    private static final String ID_DROPDOWN_HOST = "testmod:destiny/dropdown-host";
    /**
     * Registry id renderer'а ползунка.
     */
    private static final String ID_SLIDER = "testmod:destiny/slider";
    /**
     * Registry id renderer'а флажка.
     */
    private static final String ID_CHECKBOX = "testmod:destiny/checkbox";
    /**
     * Registry id renderer'а переключателя.
     */
    private static final String ID_TOGGLE_SWITCH = "testmod:destiny/toggle-switch";
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
    static final StylePack PACK = create();

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
        registry.register(ID_DROPDOWN_OPTION, WidgetRole.TOGGLE_BUTTON, DestinyLikeDropDownRenders.OPTION);
        registry.register(ID_DROPDOWN_HOST, WidgetRole.PANEL, DestinyLikeDropDownRenders.OPTIONS_HOST);
        registry.register(ID_SLIDER, WidgetRole.SLIDER, DestinyLikeSliderRenders.DEFAULT);
        registry.register(ID_CHECKBOX, WidgetRole.CHECKBOX, DestinyLikeCheckboxRenders.DOMINION_CHECKBOX_RENDERER);
        registry.register(ID_TOGGLE_SWITCH, WidgetRole.TOGGLE_SWITCH, DestinyLikeToggleSwitchRenders.DOMINION_TOGGLE_SWITCH_RENDERER);
        registry.register(ID_SCROLLBAR, WidgetRole.PANEL, DestinyLikeScrollBarRenders.DEFAULT);
        registry.register(ID_TOOLTIP, WidgetRole.POPUP, DestinyLikeTooltipRenders.DEFAULT);

        return StylePack.create("testmod:destiny-like")
                .put(renderer("button.action", StyleIds.Widget.BUTTON, ACTION, ID_BUTTON,
                        new MutableStyle()
                                .put(StyleKeys.TEXT_SIZE, 3.2f)
                                .put(StyleKeys.TEXT_TRACKING, 0.34f)
                                .put(StyleKeys.TEXT_TRANSFORM, TextTransform.UPPERCASE)))
                .put(renderer("button.dropdown.header", StyleIds.Widget.BUTTON, DROPDOWN_HEADER, ID_DROPDOWN_HEADER))
                .put(renderer("toggle.dropdown.option", StyleIds.Widget.TOGGLE_BUTTON, DROPDOWN_OPTION, ID_DROPDOWN_OPTION))
                .put(renderer("box.dropdown.host", StyleIds.Widget.BOX, DROPDOWN_HOST, ID_DROPDOWN_HOST))
                .put(renderer("slider.default", StyleIds.Widget.SLIDER, SLIDER, ID_SLIDER))
                .put(renderer("checkbox.default", StyleIds.Widget.CHECKBOX, CHECKBOX, ID_CHECKBOX))
                .put(renderer("toggle-switch.default", StyleIds.Widget.TOGGLE_SWITCH, TOGGLE_SWITCH, ID_TOGGLE_SWITCH))
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
}
