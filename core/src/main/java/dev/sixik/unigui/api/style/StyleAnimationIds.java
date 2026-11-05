package dev.sixik.unigui.api.style;

import java.util.List;

/**
 * Канонические строковые id для декларативных анимаций StylePack.
 *
 * <p>Эти id используются в {@link StylePropertyTween#propertyName()} и
 * {@link StyleDefinition#eventAnimation(String, String)}. Их лучше брать отсюда,
 * а не писать руками строки вроде {@code "scale"} или {@code "onClick"}.</p>
 */
public final class StyleAnimationIds {
    private StyleAnimationIds() {
    }

    /** Id событий, к которым StylePack может привязывать animation preset. */
    public static final class Event {
        public static final String ON_CLICK = "onClick";
        public static final String ON_FOCUS = "onFocus";
        public static final String ON_BLUR = "onBlur";
        public static final String ON_HOVER = "onHover";
        public static final String ON_HOVER_ENTER = "onHoverEnter";
        public static final String ON_HOVER_EXIT = "onHoverExit";
        public static final String ON_PRESS = "onPress";
        public static final String ON_RELEASE = "onRelease";
        public static final String ON_VALUE_CHANGED = "onValueChanged";
        public static final String ON_CHECKED_CHANGED = "onCheckedChanged";
        public static final String ON_STATE_CHANGED = "onStateChanged";
        public static final String ON_TEXT_CHANGED = "onTextChanged";
        public static final String ON_SELECTION_CHANGED = "onSelectionChanged";
        public static final String ON_SUBMIT = "onSubmit";
        public static final String ON_SEARCH_CHANGED = "onSearchChanged";
        public static final String ON_SEARCH_SUBMITTED = "onSearchSubmitted";
        public static final String ON_ERROR = "onError";

        /** Базовый набор событий, который имеет смысл показывать почти всем focusable/hoverable виджетам. */
        public static final List<String> COMMON_WIDGET = List.of(
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT,
                ON_PRESS,
                ON_RELEASE);

        /** События обычной кнопки. */
        public static final List<String> BUTTON = List.of(
                ON_CLICK,
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT,
                ON_PRESS,
                ON_RELEASE);

        /** События редактируемого текстового поля. */
        public static final List<String> TEXT_INPUT = List.of(
                ON_TEXT_CHANGED,
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT);

        /** События checked-контролов: ToggleButton, Checkbox, RadioButton, ToggleSwitch. */
        public static final List<String> CHECKED_CONTROL = List.of(
                ON_CLICK,
                ON_CHECKED_CHANGED,
                ON_STATE_CHANGED,
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT,
                ON_PRESS,
                ON_RELEASE);

        /** События search-поля. */
        public static final List<String> SEARCH_FIELD = List.of(
                ON_SEARCH_CHANGED,
                ON_SEARCH_SUBMITTED,
                ON_TEXT_CHANGED,
                ON_SUBMIT,
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT);

        /** События value-based контролов: Slider, ScrollBar, ProgressBar/NumberField. */
        public static final List<String> VALUE_CONTROL = List.of(
                ON_VALUE_CHANGED,
                ON_FOCUS,
                ON_BLUR,
                ON_HOVER,
                ON_HOVER_ENTER,
                ON_HOVER_EXIT,
                ON_PRESS,
                ON_RELEASE);

        private Event() {
        }
    }

    /** Id свойств, которые могут выступать target'ом в StylePropertyTween. */
    public static final class Property {
        public static final String POSITION_X = "position.x";
        public static final String POSITION_Y = "position.y";
        public static final String SCALE = "scale";
        public static final String SCALE_X = "scale.x";
        public static final String SCALE_Y = "scale.y";
        public static final String ROTATION_DEGREES = "rotationDegrees";
        public static final String OPACITY = "opacity";

        public static final String BACKGROUND_COLOR = "backgroundColor";
        public static final String BACKGROUND_TEXTURE_TINT = "backgroundTextureTint";
        public static final String BORDER_COLOR = "borderColor";
        public static final String BORDER_WIDTH = "borderWidth";
        public static final String RADIUS = "radius";

        public static final String TEXT_COLOR = "textColor";
        public static final String PLACEHOLDER_COLOR = "placeholderColor";
        public static final String CARET_COLOR = "caretColor";

        public static final String ACCENT_COLOR = "accentColor";
        public static final String TRACK_COLOR = "trackColor";
        public static final String THUMB_COLOR = "thumbColor";
        public static final String VALUE = "value";
        public static final String PROGRESS = "progress";

        public static final String TEXTURE = "texture";
        public static final String TEXTURE_TINT = "textureTint";

        /** Transform/visibility свойства доступны всем WidgetBase-наследникам. */
        public static final List<String> COMMON_WIDGET = List.of(
                POSITION_X,
                POSITION_Y,
                SCALE,
                SCALE_X,
                SCALE_Y,
                ROTATION_DEGREES,
                OPACITY);

        /** Визуальная подложка Box и наследников. */
        public static final List<String> BOX = List.of(
                BACKGROUND_COLOR,
                BACKGROUND_TEXTURE_TINT,
                BORDER_COLOR,
                BORDER_WIDTH,
                RADIUS,
                OPACITY,
                SCALE,
                ROTATION_DEGREES);

        /** Кнопочные виджеты: Button, ToggleButton, Checkbox, RadioButton, ToggleSwitch. */
        public static final List<String> BUTTON = List.of(
                TEXT_COLOR,
                BACKGROUND_COLOR,
                BORDER_COLOR,
                BORDER_WIDTH,
                RADIUS,
                OPACITY,
                SCALE,
                ROTATION_DEGREES);

        /** Однострочные поля ввода. */
        public static final List<String> TEXT_INPUT = List.of(
                TEXT_COLOR,
                PLACEHOLDER_COLOR,
                CARET_COLOR,
                BACKGROUND_COLOR,
                BORDER_COLOR,
                BORDER_WIDTH,
                RADIUS,
                OPACITY,
                SCALE);

        /** Value-based контролы. */
        public static final List<String> VALUE_CONTROL = List.of(
                VALUE,
                TRACK_COLOR,
                ACCENT_COLOR,
                THUMB_COLOR,
                OPACITY,
                SCALE);

        /** Текстурные виджеты. */
        public static final List<String> TEXTURE_WIDGET = List.of(
                TEXTURE,
                TEXTURE_TINT,
                RADIUS,
                OPACITY,
                SCALE,
                ROTATION_DEGREES);

        private Property() {
        }
    }
}