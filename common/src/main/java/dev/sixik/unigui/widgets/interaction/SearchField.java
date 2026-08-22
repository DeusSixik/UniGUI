package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SearchChangedEvent;
import dev.sixik.unigui.api.event.SearchSubmittedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderType;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

@XmlWidgetName("SearchField")
public class SearchField extends TextInput {
    public static final String STYLE_TYPE = StyleIds.Widget.SEARCH_FIELD;

    public static final class AnimationEvents {
        public static final String ON_SEARCH_CHANGED = StyleAnimationIds.Event.ON_SEARCH_CHANGED;
        public static final String ON_SEARCH_SUBMITTED = StyleAnimationIds.Event.ON_SEARCH_SUBMITTED;
        public static final String ON_TEXT_CHANGED = StyleAnimationIds.Event.ON_TEXT_CHANGED;
        public static final String ON_SUBMIT = StyleAnimationIds.Event.ON_SUBMIT;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.SEARCH_FIELD;

        private AnimationEvents() {
        }
    }

    private static final float CLEAR_ZONE_WIDTH = 14.0f;
    private float searchChangeDebounceSeconds = 0.25f;
    private float searchChangeRemainingSeconds;
    private String lastEmittedSearchQuery = "";
    private String pendingSearchQuery = "";
    private boolean searchChangePending;

    public SearchField() {
        enableDefaultTextInputChrome();
        placeholder("Search...");
        lastEmittedSearchQuery = text();
        pendingSearchQuery = text();
        onTextChanged(event -> scheduleSearchChanged(event.newText()));
    }

    public SearchField(String query) {
        this();
        text(query);
    }

    public EventSubscription onSearchSubmitted(EventListener<? super SearchSubmittedEvent> listener) {
        return on(SearchSubmittedEvent.TYPE, listener);
    }

    public EventSubscription onSearchChanged(EventListener<? super SearchChangedEvent> listener) {
        return on(SearchChangedEvent.TYPE, listener);
    }

    public float searchChangeDebounceSeconds() {
        return searchChangeDebounceSeconds;
    }

    @XmlAttribute(value = "searchChangeDebounceSeconds", category = "Behavior", defaultValue = "0.25", description = "Delay before emitting search-change events.")
    public SearchField searchChangeDebounceSeconds(float seconds) {
        float normalized = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 0.25f;
        this.searchChangeDebounceSeconds = normalized;
        if (searchChangePending) {
            searchChangeRemainingSeconds = Math.min(searchChangeRemainingSeconds, normalized);
        }
        return this;
    }

    public SearchField flushSearchChanged() {
        flushPendingSearchChanged();
        return this;
    }

    public SearchField clear() {
        text("");
        return this;
    }

    @Override
    protected float rightTextPadding() {
        return super.rightTextPadding() + (text().isEmpty() ? 0.0f : CLEAR_ZONE_WIDTH);
    }

    @Override
    public void handle(Event event) {
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;
        if (event instanceof PointerPressedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && !text().isEmpty()
                && pointer.localX() >= Math.max(0.0f, layoutBounds().width() - CLEAR_ZONE_WIDTH)) {
            clear();
            event.cancel();
            return;
        }

        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && focused()
                && (key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER)) {
            flushPendingSearchChanged();
            emit(new SearchSubmittedEvent(this, text()));
        }

        super.handle(event);
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!searchChangePending || frame == null) return;
        float delta = Float.isFinite(frame.deltaSeconds()) ? Math.max(0.0f, frame.deltaSeconds()) : 0.0f;
        searchChangeRemainingSeconds -= delta;
        if (searchChangeRemainingSeconds <= 0.0f) {
            flushPendingSearchChanged();
        }
    }

    @Override
    protected TextInputRenderer defaultRenderer() {
        return WidgetsRender.searchField();
    }

    @Override
    protected TextInputRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextInputRenderer.class, defaultRenderer()) : renderer();
    }

    @Override
    protected TextInputRenderType renderType() {
        return TextInputRenderType.SEARCH_FIELD;
    }

    @Override
    protected boolean clearButtonVisible() {
        return !text().isEmpty();
    }

    @Override
    protected float clearButtonX() {
        return layoutBounds().x() + layoutBounds().width() - CLEAR_ZONE_WIDTH + 3.0f;
    }

    @Override
    protected float clearButtonY() {
        return textContentY(clearButtonHeight());
    }

    @Override
    protected float clearButtonWidth() {
        return CLEAR_ZONE_WIDTH - 4.0f;
    }

    @Override
    protected float clearButtonHeight() {
        return TextEngine.LINE_HEIGHT;
    }

    private void scheduleSearchChanged(String query) {
        String normalized = query == null ? "" : query;
        pendingSearchQuery = normalized;
        if (pendingSearchQuery.equals(lastEmittedSearchQuery)) {
            searchChangePending = false;
            searchChangeRemainingSeconds = 0.0f;
            return;
        }
        searchChangePending = true;
        searchChangeRemainingSeconds = searchChangeDebounceSeconds;
        if (searchChangeDebounceSeconds <= 0.0f) {
            flushPendingSearchChanged();
        }
    }

    private void flushPendingSearchChanged() {
        if (!searchChangePending || pendingSearchQuery.equals(lastEmittedSearchQuery)) {
            searchChangePending = false;
            searchChangeRemainingSeconds = 0.0f;
            return;
        }
        String oldQuery = lastEmittedSearchQuery;
        lastEmittedSearchQuery = pendingSearchQuery;
        searchChangePending = false;
        searchChangeRemainingSeconds = 0.0f;
        emit(new SearchChangedEvent(this, oldQuery, lastEmittedSearchQuery));
    }
}
