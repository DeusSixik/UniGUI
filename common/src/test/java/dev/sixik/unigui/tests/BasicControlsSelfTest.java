package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.SliderValueChangedEvent;
import dev.sixik.unigui.api.event.TextChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.ScrollView;
import dev.sixik.unigui.widgets.Slider;
import dev.sixik.unigui.widgets.TextField;

public final class BasicControlsSelfTest {
    public static void main(String[] args) {
        new BasicControlsSelfTest().run();
    }

    private void run() {
        testTextFieldFocusAndEditing();
        testSliderPointerAndKeyboardInput();
        testScrollViewBubbledWheelInput();
        System.out.println("BasicControlsSelfTest passed");
    }

    private void testTextFieldFocusAndEditing() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextField field = new TextField().placeholder("Name");
        field.setUiContextInternal(uiContext);
        field.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 18.0f));

        Counter textChanges = new Counter();
        field.onTextChanged(event -> {
            textChanges.count++;
            textChanges.lastText = event.newText();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(field, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(field.focused(), "TextField should focus on primary pointer press");
        expect(uiContext.focusManager().focusedWidget() == field, "FocusManager should point to focused TextField");

        uiContext.routedEvents().dispatch(new TextInputEvent(field, 'A', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(field, 'b', 0));
        expect(field.text().equals("Ab"), "TextField should accept routed text input");
        expect(textChanges.count == 2 && textChanges.lastText.equals("Ab"), "TextField should emit text changed events");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.LEFT, 0, 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(field, '!', 0));
        expect(field.text().equals("A!b"), "TextField should insert at cursor");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.BACKSPACE, 0, 0));
        expect(field.text().equals("Ab"), "TextField should backspace before cursor");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.ESCAPE, 0, 0));
        expect(!field.focused(), "TextField should blur on Escape");
        expect(uiContext.focusManager().focusedWidget() == null, "FocusManager should clear after Escape");
    }

    private void testSliderPointerAndKeyboardInput() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f);
        slider.setUiContextInternal(uiContext);
        slider.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 20.0f));

        Counter changes = new Counter();
        slider.onValueChanged((SliderValueChangedEvent event) -> {
            changes.count++;
            changes.lastValue = event.newValue();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(slider, 50.0f, 10.0f, 50.0f, 10.0f, 0, PointerButton.PRIMARY));
        expect(slider.dragging(), "Slider should start dragging on primary pointer press");
        expect(slider.value() == 25.0f, "Slider should map pointer position to stepped value");
        expect(uiContext.focusManager().focusedWidget() == slider, "Slider should request focus on pointer press");

        uiContext.routedEvents().dispatch(new PointerMovedEvent(slider, 180.0f, 10.0f, 180.0f, 10.0f, 0));
        expect(slider.value() == 90.0f, "Slider should update value while dragging");

        uiContext.routedEvents().dispatch(new PointerReleasedEvent(slider, 180.0f, 10.0f, 180.0f, 10.0f, 0, PointerButton.PRIMARY));
        expect(!slider.dragging(), "Slider should stop dragging on release");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(slider, KeyCodes.LEFT, 0, 0));
        expect(slider.value() == 85.0f, "Focused Slider should nudge left by step");
        expect(changes.count >= 3 && changes.lastValue == 85.0f, "Slider should emit value changed events");
    }

    private void testScrollViewBubbledWheelInput() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box content = new Box();
        ScrollView scrollView = new ScrollView(content).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        scrollView.setUiContextInternal(uiContext);
        scrollView.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(content, 10.0f, 10.0f, 10.0f, 10.0f, 0.0f, -1.0f));
        expect(consumed, "ScrollView should consume bubbled scroll when offset changes");
        expect(scrollView.scrollY() == 20.0f, "ScrollView should scroll down on negative wheel delta");

        scrollView.scrollTo(0.0f, 500.0f);
        expect(scrollView.scrollY() == 200.0f, "ScrollView should clamp to max scroll");

        scrollView.scrollBy(0.0f, -500.0f);
        expect(scrollView.scrollY() == 0.0f, "ScrollView should clamp to zero");

        DrawList drawList = new DrawList();
        scrollView.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "ScrollView should push a clip before rendering content");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "ScrollView should pop the clip after rendering content");
    }

    private static boolean hasCommand(DrawList drawList, DrawCommandType type) {
        return drawList.commands().stream().anyMatch(command -> command.type() == type);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Counter {
        private int count;
        private String lastText = "";
        private float lastValue;
    }
}
