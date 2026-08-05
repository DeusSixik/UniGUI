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
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.Checkbox;
import dev.sixik.unigui.widgets.NumberField;
import dev.sixik.unigui.widgets.PasswordField;
import dev.sixik.unigui.widgets.ProgressBar;
import dev.sixik.unigui.widgets.SearchField;
import dev.sixik.unigui.widgets.ScrollView;
import dev.sixik.unigui.widgets.Slider;
import dev.sixik.unigui.widgets.TextField;
import dev.sixik.unigui.widgets.ToggleButton;

public final class BasicControlsSelfTest {
    public static void main(String[] args) {
        new BasicControlsSelfTest().run();
    }

    private void run() {
        testTextFieldFocusAndEditing();
        testTextFieldSelectionAndClipboard();
        testPasswordAndSearchFields();
        testDefaultThemeContracts();
        testSliderPointerAndKeyboardInput();
        testScrollViewBubbledWheelInput();
        testToggleCheckboxProgressAndNumberField();
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

    private void testTextFieldSelectionAndClipboard() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextField field = new TextField("abcd");
        field.setUiContextInternal(uiContext);
        field.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));

        uiContext.routedEvents().dispatch(new PointerPressedEvent(field, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        field.select(1, 3);
        expect(field.hasSelection() && field.selectedText().equals("bc"), "TextField should expose selected text");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.C, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("bc"), "Ctrl+C should copy selected TextField text");
        expect(field.text().equals("abcd"), "Ctrl+C should not mutate TextField text");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.X, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("bc"), "Ctrl+X should copy selected TextField text");
        expect(field.text().equals("ad"), "Ctrl+X should delete selected TextField text");
        expect(!field.hasSelection() && field.cursorIndex() == 1, "Ctrl+X should collapse selection to deleted range start");

        uiContext.clipboard().setText("XYZ");
        field.select(1, 2);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.V, 0, KeyModifiers.CONTROL));
        expect(field.text().equals("aXYZ"), "Ctrl+V should replace selected TextField text");
        expect(field.cursorIndex() == 4, "Ctrl+V should move cursor after inserted text");

        field.text("hello").select(1, 4);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.BACKSPACE, 0, 0));
        expect(field.text().equals("ho"), "Backspace should delete selection");

        field.text("hello").select(1, 4);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.DELETE, 0, 0));
        expect(field.text().equals("ho"), "Delete should delete selection");

        DrawList selectedDrawList = new DrawList();
        field.select(0, 1);
        field.render(new DefaultRenderContext(selectedDrawList));
        int selectionRectIndex = firstCommandIndex(selectedDrawList, DrawCommandType.RECT, 1);
        int textIndex = firstCommandIndex(selectedDrawList, DrawCommandType.TEXT, 0);
        expect(selectionRectIndex >= 0 && textIndex > selectionRectIndex, "TextField should draw selection highlight under text");
    }

    private void testPasswordAndSearchFields() {
        PasswordField passwordField = new PasswordField("secret");
        passwordField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));
        DrawList passwordDrawList = new DrawList();
        passwordField.render(new DefaultRenderContext(passwordDrawList));
        expect(hasText(passwordDrawList, "\u2022\u2022\u2022\u2022\u2022\u2022"), "PasswordField should render masked text");
        expect(!hasText(passwordDrawList, "secret"), "PasswordField should not render plain password text");

        DefaultUIContext uiContext = new DefaultUIContext();
        SearchField searchField = new SearchField("recipe");
        searchField.setUiContextInternal(uiContext);
        searchField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));

        Counter submissions = new Counter();
        searchField.onSearchSubmitted(event -> {
            submissions.count++;
            submissions.lastText = event.query();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(searchField, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new KeyPressedEvent(searchField, KeyCodes.ENTER, 0, 0));
        expect(submissions.count == 1 && submissions.lastText.equals("recipe"), "SearchField should submit query on Enter");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(searchField, 114.0f, 8.0f, 114.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(searchField.text().isEmpty(), "SearchField clear zone should clear query");
    }

    private void testDefaultThemeContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        ColorView buttonText = uiContext.theme().styleFor("Button").get(StyleKeys.TEXT_COLOR, WidgetState.NORMAL, null);
        ColorView buttonPressed = uiContext.theme().styleFor("Button").get(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, null);
        ColorView unknownAccent = uiContext.theme().styleFor("UnknownWidget").get(StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, null);
        expect(buttonText != null && buttonText.a() == 1.0f, "DefaultTheme should expose Button text color");
        expect(buttonPressed != null && buttonPressed.a() == 1.0f, "DefaultTheme should expose Button pressed background color");
        expect(unknownAccent != null && unknownAccent.a() == 1.0f, "DefaultTheme fallback should expose accent token");

        StyleKey<Float> radiusKeyCopy = StyleKey.of("radius", Float.class);
        expect(radiusKeyCopy.equals(StyleKeys.RADIUS), "StyleKey equality should use id and type");
        MutableStyle style = new MutableStyle().put(StyleKeys.RADIUS, 7.0f);
        expect(style.get(radiusKeyCopy, WidgetState.PRESSED, 0.0f) == 7.0f, "MutableStyle should fallback to NORMAL state");

        Button themedButton = new Button("Theme");
        themedButton.setUiContextInternal(uiContext);
        themedButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList normalButtonDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(normalButtonDrawList));
        expect(hasFillColor(normalButtonDrawList, 0.12f, 0.12f, 0.12f, 1.0f), "Button should apply NORMAL theme background at render time");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(themedButton, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        DrawList pressedButtonDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(pressedButtonDrawList));
        expect(hasFillColor(pressedButtonDrawList, 0.18f, 0.45f, 0.75f, 1.0f), "Button should apply PRESSED theme background at render time");

        Button manualButton = new Button("Manual");
        manualButton.themeEnabled(false);
        manualButton.background().set(0.9f, 0.1f, 0.2f, 1.0f);
        manualButton.setUiContextInternal(uiContext);
        manualButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList manualButtonDrawList = new DrawList();
        manualButton.render(new DefaultRenderContext(manualButtonDrawList));
        expect(hasFillColor(manualButtonDrawList, 0.9f, 0.1f, 0.2f, 1.0f), "themeEnabled(false) should preserve manual colors");
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

        uiContext.routedEvents().dispatch(new PointerPressedEvent(scrollView.verticalScrollBar(),
                97.0f, 50.0f, 3.0f, 50.0f, 0, PointerButton.PRIMARY));
        expect(scrollView.verticalScrollBar().dragging(), "ScrollBar should start dragging on primary pointer press");
        expect(near(scrollView.scrollY(), 100.0f), "Dragging vertical ScrollBar should update ScrollView scrollY");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(scrollView.verticalScrollBar(),
                97.0f, 50.0f, 3.0f, 50.0f, 0, PointerButton.PRIMARY));
        expect(!scrollView.verticalScrollBar().dragging(), "ScrollBar should stop dragging on release");

        DrawList drawList = new DrawList();
        scrollView.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "ScrollView should push a clip before rendering content");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "ScrollView should pop the clip after rendering content");
    }

    private void testToggleCheckboxProgressAndNumberField() {
        DefaultUIContext uiContext = new DefaultUIContext();

        ToggleButton toggle = new ToggleButton("Power");
        toggle.setUiContextInternal(uiContext);
        toggle.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 20.0f));
        Counter checkedChanges = new Counter();
        toggle.onCheckedChanged(event -> {
            checkedChanges.count++;
            checkedChanges.lastChecked = event.newValue();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(toggle, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(toggle, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(toggle.checked(), "ToggleButton should toggle on click");
        expect(checkedChanges.count == 1 && checkedChanges.lastChecked, "ToggleButton should emit checked changed event");

        Checkbox checkbox = new Checkbox("Enabled");
        checkbox.setUiContextInternal(uiContext);
        checkbox.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        uiContext.routedEvents().dispatch(new PointerPressedEvent(checkbox, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(checkbox, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(checkbox.checked(), "Checkbox should reuse ToggleButton checked behavior");

        ProgressBar progressBar = new ProgressBar().range(0.0f, 200.0f).value(250.0f);
        progressBar.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 12.0f));
        expect(progressBar.value() == 200.0f && progressBar.progress() == 1.0f, "ProgressBar should clamp value to max");
        progressBar.value(50.0f);
        expect(progressBar.progress() == 0.25f, "ProgressBar should expose normalized progress");
        DrawList progressDrawList = new DrawList();
        progressBar.render(new DefaultRenderContext(progressDrawList));
        expect(progressDrawList.size() >= 2, "ProgressBar should render track and fill commands");

        NumberField numberField = new NumberField().range(0.0d, 10.0d).step(2.0d);
        numberField.setUiContextInternal(uiContext);
        numberField.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        Counter numberChanges = new Counter();
        numberField.onValueChanged(event -> {
            numberChanges.count++;
            numberChanges.lastNumber = event.newValue();
        });
        numberField.text("");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(numberField, 4.0f, 8.0f, 4.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new TextInputEvent(numberField, '4', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(numberField, 'x', 0));
        expect(numberField.text().equals("4"), "NumberField should reject non-numeric text input");
        expect(numberField.value() == 4.0d, "NumberField should sync value from text");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(numberField, KeyCodes.UP, 0, 0));
        expect(numberField.value() == 6.0d, "NumberField should nudge up by step");
        numberField.text("");
        uiContext.clipboard().setText("12x.3");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(numberField, KeyCodes.V, 0, KeyModifiers.CONTROL));
        expect(numberField.text().equals("10"), "NumberField should sanitize pasted text and clamp synced value");
        expect(numberField.value() == 10.0d, "NumberField should sync sanitized pasted numeric value");
        numberField.value(99.0d);
        expect(numberField.value() == 10.0d, "NumberField should clamp programmatic value to max");
        expect(numberChanges.count >= 2 && numberChanges.lastNumber == 10.0d, "NumberField should emit value changed events");
    }

    private static int firstCommandIndex(DrawList drawList, DrawCommandType type, int startIndex) {
        for (int i = Math.max(0, startIndex); i < drawList.commands().size(); i++) {
            if (drawList.commands().get(i).type() == type) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasCommand(DrawList drawList, DrawCommandType type) {
        return drawList.commands().stream().anyMatch(command -> command.type() == type);
    }

    private static boolean hasText(DrawList drawList, String text) {
        return drawList.commands().stream().anyMatch(command -> text.equals(command.text()));
    }

    private static boolean hasFillColor(DrawList drawList, float r, float g, float b, float a) {
        return drawList.commands().stream()
                .filter(command -> command.paint() != null && !command.paint().isStroke())
                .map(command -> command.paint().color())
                .anyMatch(color -> near(color.r(), r) && near(color.g(), g) && near(color.b(), b) && near(color.a(), a));
    }

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
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
        private boolean lastChecked;
        private double lastNumber;
    }
}
