package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/**
 * Short caption text, optionally associated with a focusable control.
 *
 * <p>{@link Text} is generic display text. Label adds the form-caption role:
 * when {@link #focusTarget(Widget)} is set, primary click requests focus for
 * that control.</p>
 */
@XmlWidgetName("Label")
public final class Label extends TextWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.LABEL;

    private Widget focusTarget;

    public Label() {
        noWrap();
    }

    public Label(String text) {
        super(text);
        noWrap();
    }

    public Label(RichText text) {
        richText(text);
        noWrap();
    }

    public Widget focusTarget() {
        return focusTarget;
    }

    public Label focusTarget(Widget focusTarget) {
        if (this.focusTarget == focusTarget) return this;
        this.focusTarget = focusTarget;
        return this;
    }

    public Label labeledControl(Widget control) {
        return focusTarget(control);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY
                && focusTarget != null
                && focusTarget.focusable()
                && focusTarget.enabled()
                && focusTarget.visible()
                && uiContext() != null) {
            uiContext().focusManager().requestFocus(focusTarget);
            event.cancel();
        }
    }
}
