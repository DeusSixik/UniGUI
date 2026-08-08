package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SearchSubmittedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderType;

public class SearchField extends TextInput {
    private static final float CLEAR_ZONE_WIDTH = 14.0f;

    public SearchField() {
        enableDefaultTextInputChrome();
        placeholder("Search...");
    }

    public SearchField(String query) {
        this();
        text(query);
    }

    public EventSubscription onSearchSubmitted(EventListener<? super SearchSubmittedEvent> listener) {
        return on(SearchSubmittedEvent.TYPE, listener);
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
            emit(new SearchSubmittedEvent(this, text()));
        }

        super.handle(event);
    }

    @Override
    protected TextInputRenderer effectiveRenderer() {
        return renderer() == null ? WidgetsRender.searchField() : renderer();
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
        return layoutBounds().y() + 4.0f;
    }

    @Override
    protected float clearButtonWidth() {
        return CLEAR_ZONE_WIDTH - 4.0f;
    }

    @Override
    protected float clearButtonHeight() {
        return Math.max(1.0f, layoutBounds().height() - 8.0f);
    }
}
