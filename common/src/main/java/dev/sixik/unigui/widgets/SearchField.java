package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
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
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

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
    protected void renderContent(RenderContext context) {
        super.renderContent(context);
        if (!text().isEmpty()) {
            float x = layoutBounds().x() + layoutBounds().width() - CLEAR_ZONE_WIDTH + 3.0f;
            float y = layoutBounds().y() + 4.0f;
            context.text("x", x, y, CLEAR_ZONE_WIDTH - 4.0f, Math.max(1.0f, layoutBounds().height() - 8.0f), Paint.fill(placeholderColor()), transform());
        }
    }
}
