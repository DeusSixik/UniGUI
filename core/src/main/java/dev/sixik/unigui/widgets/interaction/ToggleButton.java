package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.api.utils.reactive.BindingData;
import dev.sixik.unigui.api.utils.reactive.BindingKeys;
import dev.sixik.unigui.api.utils.reactive.BooleanSignal;
import dev.sixik.unigui.api.utils.reactive.SignalSubscription;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

@AiDoc(
        summary = "Two-state push button maintaining an active checked boolean state.",
        purpose = "Allows toggling modes, features, or options with persistent visual selection feedback.",
        useWhen = {"Enabling/disabling an option or setting mode in toolbars and dialogs."},
        related = {"Button", "ToggleToolButton", "ToggleSwitch", "Checkbox"}
)
@WikiPage(
        title = "<key:toggle.button.title>",
        slug = "toggle-button",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:toggle.button.summary>",
        order = 50
)
@WikiSection(
        title = "<key:toggle.button.section.overview.title>",
        markdown = "<key:toggle.button.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:toggle.button.example.basic.title>",
        code = "<key:toggle.button.example.basic.code>",
        order = 10
)
@WikiLink(label = "Button", target = "page:button")
@WikiLink(label = "ToggleSwitch", target = "page:toggle-switch")
@XmlWidgetName("ToggleButton")
public class ToggleButton extends Button {
    /** Style type id для StylePack selector/binding. */
    public static final String STYLE_TYPE = StyleIds.Widget.TOGGLE_BUTTON;

    /** Event id, которые имеет смысл показывать для checked-контролов. */
    public static final class AnimationEvents {
        public static final String ON_CLICK = StyleAnimationIds.Event.ON_CLICK;
        public static final String ON_CHECKED_CHANGED = StyleAnimationIds.Event.ON_CHECKED_CHANGED;
        public static final String ON_STATE_CHANGED = StyleAnimationIds.Event.ON_STATE_CHANGED;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.CHECKED_CONTROL;

        private AnimationEvents() {
        }
    }

    /**
     * Цвета индикатора checked-контролов (Checkbox, ToggleSwitch, опции ComboBox).
     * Сама кнопка их в фон не пишет: checked-фон кнопки отдаёт тема через
     * {@link WidgetState#CHECKED}, иначе hover/pressed затирались бы цианом.
     */
    private final MutableColor checkedBackground = new MutableColor(0.18f, 0.45f, 0.75f, 1.0f);
    private final MutableColor uncheckedBackground = new MutableColor(0.12f, 0.12f, 0.12f, 1.0f);
    private boolean checked;

    public ToggleButton() {
        this("");
    }

    public ToggleButton(String text) {
        super(text);
        checkedBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        uncheckedBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        onClick(this::toggleOnClick);
    }

    public ToggleButton(RichText text) {
        this("");
        richText(text);
    }

    public boolean checked() {
        return checked;
    }

    public ToggleButton checked(boolean checked) {
        setChecked(checked, true);
        return this;
    }

    @XmlAttribute(value = "checked", category = "Behavior", defaultValue = "false", description = "Initial checked state without emitting change events during XML load.")
    public ToggleButton silentChecked(boolean checked) {
        setChecked(checked, false);
        return this;
    }

    /**
     * Возвращает цвет индикатора в состоянии checked.
     *
     * <p>Не является фоном самой кнопки. Его читают индикаторы наследников.</p>
     *
     * @return live-цвет отмеченного индикатора
     */
    public MutableColor checkedBackground() {
        return checkedBackground;
    }

    /**
     * Возвращает цвет индикатора в состоянии unchecked.
     *
     * @return live-цвет неотмеченного индикатора
     */
    public MutableColor uncheckedBackground() {
        return uncheckedBackground;
    }

    @WikiMethod(order = 20, title = "<key:toggle.button.method.on_checked_changed.title>", summary = "<key:toggle.button.method.on_checked_changed.summary>")
    public EventSubscription onCheckedChanged(EventListener<? super CheckedChangedEvent> listener) {
        return on(CheckedChangedEvent.TYPE, listener);
    }

    @Override
    protected <T> SignalSubscription createBinding(BindingData<T> key, T source) {
        if (key == BindingKeys.TOGGLE_BUTTON_CHECKED) {
            BooleanSignal checked = BindingKeys.TOGGLE_BUTTON_CHECKED.cast(source);
            SignalSubscription signalSubscription = checked.subscribe(this::checked);
            EventSubscription widgetSubscription = onCheckedChanged(event -> checked.set(event.newValue()));
            return () -> {
                widgetSubscription.close();
                signalSubscription.close();
            };
        }
        return super.createBinding(key, source);
    }

    @Override
    protected void applyTheme() {
        checkedBackground.set(styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, checkedBackground));
        uncheckedBackground.set(styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, uncheckedBackground));
        super.applyTheme();
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        if (pressed()) return WidgetState.PRESSED;
        return checked ? WidgetState.CHECKED : super.styleState();
    }

    @Override
    protected WidgetRole renderRole() {
        return WidgetRole.TOGGLE_BUTTON;
    }

    @Override
    public void handle(dev.sixik.unigui.api.event.Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key && key.phase() == dev.sixik.unigui.api.event.EventPhase.TARGET
                && uiContext() != null && uiContext().focusManager().isFocused(this)) {
            if (key.keyCode() == KeyCodes.SPACE || key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                checked(!checked);
                event.cancel();
            }
        }
    }

    private void toggleOnClick(ButtonClickEvent ignored) {
        checked(!checked);
    }

    private void setChecked(boolean checked, boolean emitChange) {
        if (this.checked == checked) return;
        boolean oldValue = this.checked;
        this.checked = checked;
        applyTheme();
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new CheckedChangedEvent(this, oldValue, checked));
        }
    }
}
