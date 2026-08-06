package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.event.SliderValueChangedEvent;
import dev.sixik.unigui.api.event.TableSortChangedEvent;
import dev.sixik.unigui.api.event.TextChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.input.FocusDirection;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.MutableTheme;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.impl.input.TransformHitTester;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.Box;
import dev.sixik.unigui.widgets.Checkbox;
import dev.sixik.unigui.widgets.DockPanel;
import dev.sixik.unigui.widgets.DockSide;
import dev.sixik.unigui.widgets.GridBox;
import dev.sixik.unigui.widgets.HBox;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.NumberField;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.PasswordField;
import dev.sixik.unigui.widgets.ProgressBar;
import dev.sixik.unigui.widgets.SearchField;
import dev.sixik.unigui.widgets.ScrollView;
import dev.sixik.unigui.widgets.Slider;
import dev.sixik.unigui.widgets.StackPanel;
import dev.sixik.unigui.widgets.TextBlock;
import dev.sixik.unigui.widgets.TextField;
import dev.sixik.unigui.widgets.TextInput;
import dev.sixik.unigui.widgets.ToggleButton;
import dev.sixik.unigui.widgets.VBox;
import dev.sixik.unigui.widgets.VirtualListView;
import dev.sixik.unigui.widgets.VirtualTableView;
import dev.sixik.unigui.widgets.Widgets;
import dev.sixik.unigui.widgets.WrapPanel;

public final class BasicControlsSelfTest {
    public static void main(String[] args) {
        new BasicControlsSelfTest().run();
    }

    private void run() {
        testTextEditorModelCore();
        testTextInputShellAndTextFieldChrome();
        testTextFieldFocusAndEditing();
        testTextFieldSelectionAndClipboard();
        testPasswordAndSearchFields();
        testDefaultThemeContracts();
        testStyleInheritanceAndScopes();
        testKeyboardFocusTraversal();
        testDirectionalFocusNavigation();
        testHoverTrackingAndStyleState();
        testEnabledVisibleStateFlags();
        testDesiredSizeMeasurement();
        testLayoutConstraintsAndSlotSizing();
        testRicherLayoutContainers();
        testSliderPointerAndKeyboardInput();
        testScrollViewBubbledWheelInput();
        testNestedScissorStack();
        testFixedRowVirtualizationCore();
        testVirtualizedSelectionContracts();
        testVirtualListViewRealizationAndScrolling();
        testVirtualTableSortingContracts();
        testVirtualTableViewVirtualRowsAndRendering();
        testToggleCheckboxProgressAndNumberField();
        System.out.println("BasicControlsSelfTest passed");
    }

    private void testTextEditorModelCore() {
        TextEditorModel editor = new TextEditorModel();
        Counter changes = new Counter();
        editor.onChanged((oldText, newText) -> {
            changes.count++;
            changes.lastText = newText;
        });

        editor.silentText("abcd");
        editor.cursorIndex(editor.text().length());
        editor.select(1, 3);
        expect(editor.hasSelection() && editor.selectedText().equals("bc"), "TextEditorModel should expose selected text");

        editor.insertText("XYZ");
        expect(editor.text().equals("aXYZd"), "TextEditorModel should replace selection with inserted text");
        expect(editor.cursorIndex() == 4 && !editor.hasSelection(), "TextEditorModel should collapse selection after insertion");

        editor.backspace();
        expect(editor.text().equals("aXYd") && editor.cursorIndex() == 3, "TextEditorModel should backspace by code point");

        editor.delete();
        expect(editor.text().equals("aXY"), "TextEditorModel should delete next code point");

        editor.maxLength(2);
        expect(editor.text().equals("aX"), "TextEditorModel should trim text when maxLength shrinks");
        expect(changes.count == 4 && changes.lastText.equals("aX"), "TextEditorModel should emit text change callbacks");
        expect(TextEditorModel.sanitizePrintable("a\nb\tc").equals("abc"), "TextEditorModel should sanitize non-printable input");
    }

    private void testTextInputShellAndTextFieldChrome() {
        TextInput input = new TextInput("core");
        input.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList inputDrawList = new DrawList();
        input.render(new DefaultRenderContext(inputDrawList));
        expect(hasText(inputDrawList, "core"), "TextInput shell should render editor text");
        expect(!hasCommand(inputDrawList, DrawCommandType.ROUNDED_RECT), "TextInput shell should not force field chrome");

        TextField field = new TextField("field");
        field.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList fieldDrawList = new DrawList();
        field.render(new DefaultRenderContext(fieldDrawList));
        expect(hasText(fieldDrawList, "field"), "TextField chrome should preserve TextInput text rendering");
        expect(hasCommand(fieldDrawList, DrawCommandType.ROUNDED_RECT), "TextField should add default field chrome");

        expect(new PasswordField() instanceof TextInput, "PasswordField should reuse TextInput shell directly");
        expect(new NumberField() instanceof TextInput, "NumberField should reuse TextInput shell directly");
        expect(new SearchField() instanceof TextInput, "SearchField should reuse TextInput shell directly");
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

        MutableStyle customButtonStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.4f, 0.2f, 0.8f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.9f, 0.9f, 0.2f, 1.0f));
        MutableTheme customTheme = new MutableTheme().fallback(style).put("Button", customButtonStyle);
        long previousStyleVersion = uiContext.styleVersion();
        uiContext.theme(customTheme, themedButton);
        expect(uiContext.styleVersion() != previousStyleVersion, "DefaultUIContext should bump styleVersion when theme changes");
        DrawList customThemeDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(customThemeDrawList));
        expect(hasFillColor(customThemeDrawList, 0.4f, 0.2f, 0.8f, 1.0f), "Button should apply newly assigned theme on next render");

        Box root = new Box();
        Button child = new Button("Child");
        root.setUiContextInternal(uiContext);
        root.addChild(child);
        root.applyQueuedMutations();
        root.clearInvalidation(InvalidationFlags.ALL);
        child.clearInvalidation(InvalidationFlags.ALL);
        uiContext.theme(new MutableTheme().put("Button", customButtonStyle), root);
        expect(hasFlag(root.invalidationFlags(), InvalidationFlags.VISUAL), "theme(root) should invalidate root visuals");
        expect(hasFlag(child.invalidationFlags(), InvalidationFlags.VISUAL), "theme(root) should invalidate child visuals");
    }

    private void testStyleInheritanceAndScopes() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Box scoped = new Box();
        Button inherited = new Button("Inherited");
        Button local = new Button("Local");
        Button isolated = new Button("Isolated");
        Box boundary = new Box();
        boundary.styleScope(true);

        root.setUiContextInternal(uiContext);
        root.localStyle("*", new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.7f, 0.1f, 0.1f, 1.0f)));
        scoped.localStyle("Button", new MutableStyle()
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.2f, 0.8f, 0.2f, 1.0f)));
        local.localStyle("Button", new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.1f, 0.2f, 0.9f, 1.0f)));
        boundary.localStyle("Button", new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.2f, 0.2f, 0.2f, 1.0f)));

        root.addChild(scoped);
        scoped.addChild(inherited);
        scoped.addChild(local);
        scoped.addChild(boundary);
        boundary.addChild(isolated);
        root.applyQueuedMutations();
        scoped.applyQueuedMutations();
        boundary.applyQueuedMutations();
        inherited.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        local.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        isolated.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));

        DrawList inheritedDrawList = new DrawList();
        inherited.render(new DefaultRenderContext(inheritedDrawList));
        expect(hasFillColor(inheritedDrawList, 0.7f, 0.1f, 0.1f, 1.0f), "Widget should inherit wildcard background from ancestor local style");
        expect(hasFillColor(inheritedDrawList, 0.2f, 0.8f, 0.2f, 1.0f), "Widget should inherit typed text color from ancestor local style");

        DrawList localDrawList = new DrawList();
        local.render(new DefaultRenderContext(localDrawList));
        expect(hasFillColor(localDrawList, 0.1f, 0.2f, 0.9f, 1.0f), "Widget local style should override ancestor local style");

        DrawList isolatedDrawList = new DrawList();
        isolated.render(new DefaultRenderContext(isolatedDrawList));
        expect(hasFillColor(isolatedDrawList, 0.2f, 0.2f, 0.2f, 1.0f), "Nearest style scope should provide local style inside boundary");
        expect(!hasFillColor(isolatedDrawList, 0.7f, 0.1f, 0.1f, 1.0f), "Nearest style scope should stop outer local style inheritance");
    }

    private void testKeyboardFocusTraversal() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        root.setUiContextInternal(uiContext);

        Button first = new Button("First");
        TextField second = new TextField("Second");
        Button skipped = new Button("Skipped");
        skipped.focusable(false);
        first.focusOrder(20);
        second.focusOrder(10);

        root.addChild(first);
        root.addChild(second);
        root.addChild(skipped);
        root.applyQueuedMutations();

        expect(uiContext.focusManager().focusNext(root), "focusNext should focus the first available widget");
        expect(uiContext.focusManager().focusedWidget() == second, "focusNext should respect focusOrder before tree order");
        expect(uiContext.focusManager().focusNext(root), "focusNext should advance to next focusable widget");
        expect(uiContext.focusManager().focusedWidget() == first, "focusNext should advance through ordered focusables");
        expect(uiContext.focusManager().focusNext(root), "focusNext should wrap around");
        expect(uiContext.focusManager().focusedWidget() == second, "focusNext should skip non-focusable widgets");
        uiContext.focusManager().requestFocus(skipped);
        expect(uiContext.focusManager().focusedWidget() == second, "requestFocus should ignore non-focusable widgets");
        expect(uiContext.focusManager().focusPrevious(root), "focusPrevious should move backwards");
        expect(uiContext.focusManager().focusedWidget() == first, "focusPrevious should wrap backwards");

        Box scope = new Box();
        scope.focusScope(true);
        Button scopeA = new Button("Scope A");
        Button scopeB = new Button("Scope B");
        scope.addChild(scopeA);
        scope.addChild(scopeB);
        scope.applyQueuedMutations();
        root.addChild(scope);
        root.applyQueuedMutations();

        uiContext.focusManager().requestFocus(scopeA);
        expect(uiContext.focusManager().focusNext(root), "focusNext should work inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeB, "focusNext should stay inside nearest focus scope");
        expect(uiContext.focusManager().focusNext(root), "focusNext should wrap inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeA, "focusNext should not escape nearest focus scope");
        expect(uiContext.focusManager().focusPrevious(root), "focusPrevious should stay inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeB, "focusPrevious should wrap inside nearest focus scope");
    }

    private void testDirectionalFocusNavigation() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        root.setUiContextInternal(uiContext);

        Button center = new Button("Center");
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button up = new Button("Up");
        Button down = new Button("Down");
        Button hiddenRight = new Button("Hidden right");
        hiddenRight.visibility(Visibility.HIDDEN);

        root.addChild(center);
        root.addChild(left);
        root.addChild(right);
        root.addChild(up);
        root.addChild(down);
        root.addChild(hiddenRight);
        root.applyQueuedMutations();
        center.arrange(new MutableRect(90.0f, 90.0f, 20.0f, 20.0f));
        left.arrange(new MutableRect(40.0f, 90.0f, 20.0f, 20.0f));
        right.arrange(new MutableRect(140.0f, 90.0f, 20.0f, 20.0f));
        up.arrange(new MutableRect(90.0f, 40.0f, 20.0f, 20.0f));
        down.arrange(new MutableRect(90.0f, 140.0f, 20.0f, 20.0f));
        hiddenRight.arrange(new MutableRect(112.0f, 90.0f, 20.0f, 20.0f));

        uiContext.focusManager().requestFocus(center);
        expect(uiContext.focusManager().focusDirectional(root, FocusDirection.RIGHT), "Directional focus should move right");
        expect(uiContext.focusManager().focusedWidget() == right, "Directional focus should skip hidden candidates");
        expect(uiContext.focusManager().focusDirectional(root, FocusDirection.LEFT), "Directional focus should move left from right");
        expect(uiContext.focusManager().focusedWidget() == center, "Directional focus should pick nearest left candidate");
        expect(uiContext.focusManager().focusDown(root), "Directional focus convenience should move down");
        expect(uiContext.focusManager().focusedWidget() == down, "focusDown should pick the lower candidate");
        expect(!uiContext.focusManager().focusDown(root), "Directional focus should not wrap when no candidate exists");
        expect(uiContext.focusManager().focusedWidget() == down, "Directional focus should keep current focus when blocked");

        Box scope = new Box();
        scope.focusScope(true);
        Button scopedCenter = new Button("Scoped center");
        Button scopedRight = new Button("Scoped right");
        Button outsideRight = new Button("Outside right");
        root.addChild(scope);
        root.addChild(outsideRight);
        root.applyQueuedMutations();
        scope.addChild(scopedCenter);
        scope.addChild(scopedRight);
        scope.applyQueuedMutations();
        scopedCenter.arrange(new MutableRect(10.0f, 210.0f, 20.0f, 20.0f));
        scopedRight.arrange(new MutableRect(60.0f, 210.0f, 20.0f, 20.0f));
        outsideRight.arrange(new MutableRect(110.0f, 210.0f, 20.0f, 20.0f));

        uiContext.focusManager().requestFocus(scopedCenter);
        expect(uiContext.focusManager().focusRight(root), "Directional focus should work inside focus scopes");
        expect(uiContext.focusManager().focusedWidget() == scopedRight, "Directional focus should stay inside nearest focus scope");
    }

    private void testHoverTrackingAndStyleState() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Button first = new Button("First");
        Button second = new Button("Second");
        root.setUiContextInternal(uiContext);
        root.addChild(first);
        root.addChild(second);
        root.applyQueuedMutations();
        first.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        second.arrange(new MutableRect(90.0f, 0.0f, 80.0f, 18.0f));

        Counter firstEnter = new Counter();
        Counter firstExit = new Counter();
        first.on(PointerEnteredEvent.TYPE, event -> firstEnter.count++);
        first.on(PointerExitedEvent.TYPE, event -> firstExit.count++);

        uiContext.hoverManager().updateHover(first, 4.0f, 4.0f, 4.0f, 4.0f, 0);
        expect(uiContext.hoverManager().hoveredWidget() == first && first.hovered(), "HoverManager should mark entered widget hovered");
        expect(firstEnter.count == 1 && firstExit.count == 0, "HoverManager should emit pointer entered once");
        uiContext.hoverManager().updateHover(first, 5.0f, 4.0f, 5.0f, 4.0f, 0);
        expect(firstEnter.count == 1, "HoverManager should not re-enter the same widget");

        DrawList hoveredDrawList = new DrawList();
        first.render(new DefaultRenderContext(hoveredDrawList));
        expect(hasFillColor(hoveredDrawList, 0.16f, 0.16f, 0.16f, 1.0f), "Hovered Button should use HOVERED background style");

        uiContext.hoverManager().updateHover(second, 94.0f, 4.0f, 4.0f, 4.0f, 0);
        expect(!first.hovered() && second.hovered(), "HoverManager should transfer hover between widgets");
        expect(firstExit.count == 1, "HoverManager should emit pointer exited when hover leaves");
        uiContext.hoverManager().clearHover();
        expect(uiContext.hoverManager().hoveredWidget() == null && !second.hovered(), "clearHover should clear current hover state");
    }

    private void testEnabledVisibleStateFlags() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Button disabled = new Button("Disabled");
        Button hidden = new Button("Hidden");
        Button collapsed = new Button("Collapsed");
        Button active = new Button("Active");
        root.setUiContextInternal(uiContext);
        disabled.enabled(false);
        hidden.visible(false);
        collapsed.visibility(Visibility.COLLAPSED);
        collapsed.arrange(new MutableRect(11.0f, 12.0f, 13.0f, 14.0f));
        root.addChild(disabled);
        root.addChild(hidden);
        root.addChild(collapsed);
        root.addChild(active);
        root.applyQueuedMutations();
        root.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));

        expect(!disabled.enabled() && !hidden.visible() && hidden.visibility() == Visibility.HIDDEN, "Widget state flags should be mutable");
        expect(collapsed.visibility() == Visibility.COLLAPSED, "Collapsed visibility should be mutable");
        expect(hidden.layoutBounds().x() == 0.0f && hidden.layoutBounds().width() == 100.0f,
                "Hidden widgets should keep participating in layout");
        expect(collapsed.layoutBounds().x() == 11.0f && collapsed.layoutBounds().width() == 13.0f,
                "Collapsed widgets should be skipped by parent layout");
        uiContext.focusManager().requestFocus(disabled);
        expect(uiContext.focusManager().focusedWidget() == null, "requestFocus should ignore disabled widgets");
        expect(uiContext.focusManager().focusNext(root), "focusNext should find an enabled visible widget");
        expect(uiContext.focusManager().focusedWidget() == active, "focusNext should skip disabled, hidden and collapsed widgets");

        Counter clicks = new Counter();
        disabled.onClick(event -> clicks.count++);
        disabled.handle(new PointerPressedEvent(disabled, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        disabled.handle(new PointerReleasedEvent(disabled, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(clicks.count == 0, "Disabled Button should ignore pointer input");

        DrawList disabledDrawList = new DrawList();
        disabled.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        disabled.render(new DefaultRenderContext(disabledDrawList));
        expect(hasFillColor(disabledDrawList, 0.08f, 0.08f, 0.08f, 0.75f), "Disabled Button should use DISABLED style state");

        DrawList hiddenDrawList = new DrawList();
        hidden.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        hidden.render(new DefaultRenderContext(hiddenDrawList));
        expect(hiddenDrawList.size() == 0, "Hidden widget should not render commands");

        TransformHitTester hitTester = new TransformHitTester();
        root.visible(true);
        root.enabled(true);
        disabled.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        hidden.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        active.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        expect(hitTester.hitTest(disabled, 5.0f, 5.0f).isEmpty(), "Hit-test should skip disabled root widgets");
        expect(hitTester.hitTest(hidden, 5.0f, 5.0f).isEmpty(), "Hit-test should skip hidden root widgets");
        expect(hitTester.hitTest(collapsed, 5.0f, 5.0f).isEmpty(), "Hit-test should skip collapsed root widgets");
    }

    private void testDesiredSizeMeasurement() {
        LayoutContext generous = new LayoutContext(500.0f, 100.0f);

        Label label = new Label("Hello");
        label.measure(generous);
        expect(near(label.desiredSize().width(), 30.0f) && near(label.desiredSize().height(), 10.0f),
                "Label should measure intrinsic single-line text size");

        TextBlock block = new TextBlock("abcdefghijklmnopqrstuvwx");
        block.measure(new LayoutContext(60.0f, 100.0f));
        expect(near(block.desiredSize().width(), 60.0f) && near(block.desiredSize().height(), 30.0f),
                "TextBlock should aggregate wrapped desired height from available width");

        TextField field = new TextField("name");
        field.measure(generous);
        expect(near(field.desiredSize().width(), 32.0f) && near(field.desiredSize().height(), 18.0f),
                "TextInput/TextField should include text padding in desired size");
        field.preferredSize(100.0f, LayoutConstraints.AUTO);
        field.measure(generous);
        expect(near(field.desiredSize().width(), 100.0f) && near(field.desiredSize().height(), 18.0f),
                "Explicit preferred width should override measured text width");

        HBox hbox = new HBox();
        hbox.spacing(5.0f);
        Label first = new Label("AA");
        Label second = new Label("BBBB");
        hbox.addChild(first);
        hbox.addChild(second);
        hbox.measure(new LayoutContext(200.0f, 100.0f));
        expect(near(hbox.desiredSize().width(), 41.0f) && near(hbox.desiredSize().height(), 10.0f),
                "HBox should aggregate measured child widths plus spacing");
        hbox.arrange(new MutableRect(0.0f, 0.0f, hbox.desiredSize().width(), hbox.desiredSize().height()));
        expect(near(first.layoutBounds().width(), 12.0f) && near(second.layoutBounds().x(), 17.0f)
                        && near(second.layoutBounds().width(), 24.0f),
                "HBox should use measured desired width as AUTO main-axis fallback after measure");

        GridBox grid = new GridBox().columns(2).spacing(5.0f);
        grid.addChild(new Label("AA"));
        grid.addChild(new Label("BBBB"));
        grid.measure(new LayoutContext(200.0f, 100.0f));
        expect(near(grid.desiredSize().width(), 53.0f) && near(grid.desiredSize().height(), 10.0f),
                "GridBox should aggregate measured max cell size plus spacing");

        WrapPanel wrap = new WrapPanel().spacing(5.0f).lineSpacing(2.0f);
        wrap.addChild(new Label("AAAAAA"));
        wrap.addChild(new Label("BBBBBB"));
        wrap.addChild(new Label("CCCCCC"));
        wrap.measure(new LayoutContext(80.0f, 100.0f));
        expect(near(wrap.desiredSize().width(), 77.0f) && near(wrap.desiredSize().height(), 22.0f),
                "WrapPanel should aggregate measured wrapped lines under available width");
    }

    private void testLayoutConstraintsAndSlotSizing() {
        HBox hbox = new HBox();
        hbox.spacing(10.0f);
        Button fixed = new Button("Fixed");
        Button growA = new Button("Grow A");
        Button growB = new Button("Grow B");
        fixed.preferredSize(40.0f, LayoutConstraints.AUTO).grow(0.0f).margin(5.0f, 0.0f);
        growA.grow(1.0f);
        growB.grow(2.0f);
        hbox.addChild(fixed);
        hbox.addChild(growA);
        hbox.addChild(growB);
        hbox.applyQueuedMutations();
        hbox.arrange(new MutableRect(0.0f, 0.0f, 210.0f, 30.0f));

        expect(near(fixed.layoutBounds().x(), 5.0f) && near(fixed.layoutBounds().width(), 40.0f),
                "HBox should honor preferred width and horizontal margin");
        expect(near(growA.layoutBounds().x(), 60.0f) && near(growA.layoutBounds().width(), 46.67f),
                "HBox should allocate grow weight after fixed slots");
        expect(near(growB.layoutBounds().x(), 116.67f) && near(growB.layoutBounds().width(), 93.33f),
                "HBox should allocate larger grow weight proportionally");

        VBox vbox = new VBox();
        vbox.spacing(5.0f);
        Button top = new Button("Top");
        Button centered = new Button("Centered");
        Button collapsed = new Button("Collapsed");
        top.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        centered.preferredSize(60.0f, 10.0f).grow(0.0f).margin(4.0f).align(Alignment.CENTER, Alignment.CENTER);
        collapsed.visibility(Visibility.COLLAPSED);
        collapsed.arrange(new MutableRect(7.0f, 8.0f, 9.0f, 10.0f));
        vbox.addChild(top);
        vbox.addChild(centered);
        vbox.addChild(collapsed);
        vbox.applyQueuedMutations();
        vbox.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));

        expect(near(top.layoutBounds().height(), 20.0f), "VBox should honor preferred height");
        expect(near(centered.layoutBounds().x(), 20.0f) && near(centered.layoutBounds().y(), 29.0f)
                       && near(centered.layoutBounds().width(), 60.0f) && near(centered.layoutBounds().height(), 10.0f),
                "VBox should apply margin and center alignment inside the child slot");
        expect(near(collapsed.layoutBounds().x(), 7.0f) && near(collapsed.layoutBounds().width(), 9.0f),
                "VBox should skip collapsed children during layout");

        GridBox grid = new GridBox().columns(2).spacing(10.0f);
        Button stretched = new Button("Stretch");
        Button endAligned = new Button("End");
        endAligned.layoutConstraints(new LayoutConstraints(
                30.0f, 20.0f,
                0.0f, 0.0f,
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                EdgeInsets.all(5.0f),
                Alignment.END, Alignment.CENTER,
                0.0f));
        grid.addChild(stretched);
        grid.addChild(endAligned);
        grid.applyQueuedMutations();
        grid.arrange(new MutableRect(0.0f, 0.0f, 110.0f, 30.0f));

        expect(near(stretched.layoutBounds().width(), 50.0f) && near(stretched.layoutBounds().height(), 30.0f),
                "GridBox should stretch default children to the cell");
        expect(near(endAligned.layoutBounds().x(), 75.0f) && near(endAligned.layoutBounds().y(), 5.0f)
                        && near(endAligned.layoutBounds().width(), 30.0f) && near(endAligned.layoutBounds().height(), 20.0f),
                "GridBox should honor margin, preferred size and alignment inside cells");
    }

    private void testRicherLayoutContainers() {
        expect(Widgets.stack() instanceof StackPanel, "Widgets.stack should create StackPanel");
        expect(Widgets.dock() instanceof DockPanel, "Widgets.dock should create DockPanel");
        expect(Widgets.wrap() instanceof WrapPanel, "Widgets.wrap should create WrapPanel");

        StackPanel stack = new StackPanel();
        Button full = new Button("Full");
        Button overlay = new Button("Overlay");
        overlay.preferredSize(40.0f, 20.0f).margin(5.0f).align(Alignment.END, Alignment.CENTER);
        stack.addChild(full);
        stack.addChild(overlay);
        stack.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));

        expect(near(full.layoutBounds().x(), 0.0f) && near(full.layoutBounds().y(), 0.0f)
                        && near(full.layoutBounds().width(), 100.0f) && near(full.layoutBounds().height(), 60.0f),
                "StackPanel should stretch default children over the full slot");
        expect(near(overlay.layoutBounds().x(), 55.0f) && near(overlay.layoutBounds().y(), 20.0f)
                        && near(overlay.layoutBounds().width(), 40.0f) && near(overlay.layoutBounds().height(), 20.0f),
                "StackPanel should honor margin, preferred size and alignment for overlays");

        DockPanel dock = new DockPanel();
        Button top = new Button("Top");
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button fill = new Button("Fill");
        top.preferredSize(LayoutConstraints.AUTO, 20.0f);
        left.preferredSize(30.0f, LayoutConstraints.AUTO);
        right.preferredSize(40.0f, LayoutConstraints.AUTO);
        dock.addChild(top, DockSide.TOP);
        dock.addChild(left, DockSide.LEFT);
        dock.addChild(right, DockSide.RIGHT);
        dock.addChild(fill, DockSide.LEFT);
        dock.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));

        expect(near(top.layoutBounds().x(), 0.0f) && near(top.layoutBounds().y(), 0.0f)
                        && near(top.layoutBounds().width(), 200.0f) && near(top.layoutBounds().height(), 20.0f),
                "DockPanel should dock top children across the remaining width");
        expect(near(left.layoutBounds().x(), 0.0f) && near(left.layoutBounds().y(), 20.0f)
                        && near(left.layoutBounds().width(), 30.0f) && near(left.layoutBounds().height(), 80.0f),
                "DockPanel should dock left children and shrink the remaining rect");
        expect(near(right.layoutBounds().x(), 160.0f) && near(right.layoutBounds().y(), 20.0f)
                        && near(right.layoutBounds().width(), 40.0f) && near(right.layoutBounds().height(), 80.0f),
                "DockPanel should dock right children against the remaining edge");
        expect(near(fill.layoutBounds().x(), 30.0f) && near(fill.layoutBounds().y(), 20.0f)
                        && near(fill.layoutBounds().width(), 130.0f) && near(fill.layoutBounds().height(), 80.0f),
                "DockPanel should fill the remaining rect with the last child by default");

        WrapPanel wrap = new WrapPanel().spacing(5.0f).lineSpacing(10.0f);
        Button first = new Button("A");
        Button second = new Button("B");
        Button skipped = new Button("Skipped");
        Button third = new Button("C");
        first.preferredSize(40.0f, 10.0f);
        second.preferredSize(40.0f, 20.0f);
        skipped.preferredSize(100.0f, 100.0f).visibility(Visibility.COLLAPSED);
        skipped.arrange(new MutableRect(7.0f, 8.0f, 9.0f, 10.0f));
        third.preferredSize(40.0f, 10.0f);
        wrap.addChild(first);
        wrap.addChild(second);
        wrap.addChild(skipped);
        wrap.addChild(third);
        wrap.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        expect(near(first.layoutBounds().x(), 0.0f) && near(first.layoutBounds().y(), 0.0f)
                        && near(first.layoutBounds().width(), 40.0f) && near(first.layoutBounds().height(), 10.0f),
                "WrapPanel should place the first horizontal item at the line origin");
        expect(near(second.layoutBounds().x(), 45.0f) && near(second.layoutBounds().y(), 0.0f)
                        && near(second.layoutBounds().width(), 40.0f) && near(second.layoutBounds().height(), 20.0f),
                "WrapPanel should apply horizontal spacing inside a line");
        expect(near(third.layoutBounds().x(), 0.0f) && near(third.layoutBounds().y(), 30.0f)
                        && near(third.layoutBounds().width(), 40.0f) && near(third.layoutBounds().height(), 10.0f),
                "WrapPanel should wrap to the next line when the next item exceeds available width");
        expect(near(skipped.layoutBounds().x(), 7.0f) && near(skipped.layoutBounds().width(), 9.0f),
                "WrapPanel should skip collapsed children during layout");

        VBox wrappedToolbarLayout = new VBox();
        wrappedToolbarLayout.spacing(8.0f);
        WrapPanel toolbar = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        Button search = new Button("Search");
        Button slider = new Button("Slider");
        Button progress = new Button("Progress");
        Button number = new Button("42");
        search.preferredSize(120.0f, 20.0f).grow(0.0f);
        slider.preferredSize(130.0f, 20.0f).grow(0.0f);
        progress.preferredSize(100.0f, 12.0f).grow(0.0f);
        number.preferredSize(70.0f, 20.0f).grow(0.0f);
        toolbar.grow(0.0f);
        toolbar.addChild(search);
        toolbar.addChild(slider);
        toolbar.addChild(progress);
        toolbar.addChild(number);
        Box main = new Box();
        main.grow(1.0f);
        wrappedToolbarLayout.addChild(toolbar);
        wrappedToolbarLayout.addChild(main);
        wrappedToolbarLayout.measure(new LayoutContext(260.0f, 140.0f));
        wrappedToolbarLayout.arrange(new MutableRect(0.0f, 0.0f, 260.0f, 140.0f));

        expect(toolbar.layoutBounds().height() >= 44.0f,
                "VBox should reserve measured multi-line WrapPanel height");
        expect(main.layoutBounds().y() >= toolbar.layoutBounds().y() + toolbar.layoutBounds().height() + 8.0f,
                "VBox should arrange grow content below a wrapped toolbar without overlap");

        WrapPanel verticalWrap = new WrapPanel().orientation(Orientation.VERTICAL).spacing(5.0f).lineSpacing(10.0f);
        Button verticalFirst = new Button("VA");
        Button verticalSecond = new Button("VB");
        Button verticalThird = new Button("VC");
        verticalFirst.preferredSize(10.0f, 40.0f);
        verticalSecond.preferredSize(20.0f, 40.0f);
        verticalThird.preferredSize(10.0f, 40.0f);
        verticalWrap.addChild(verticalFirst);
        verticalWrap.addChild(verticalSecond);
        verticalWrap.addChild(verticalThird);
        verticalWrap.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        expect(near(verticalFirst.layoutBounds().x(), 0.0f) && near(verticalFirst.layoutBounds().y(), 0.0f)
                        && near(verticalFirst.layoutBounds().width(), 10.0f) && near(verticalFirst.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should place the first item at the column origin");
        expect(near(verticalSecond.layoutBounds().x(), 0.0f) && near(verticalSecond.layoutBounds().y(), 45.0f)
                        && near(verticalSecond.layoutBounds().width(), 20.0f) && near(verticalSecond.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should apply spacing inside a column");
        expect(near(verticalThird.layoutBounds().x(), 30.0f) && near(verticalThird.layoutBounds().y(), 0.0f)
                        && near(verticalThird.layoutBounds().width(), 10.0f) && near(verticalThird.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should wrap to the next column when the next item exceeds available height");
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
        expect(uiContext.capturedPointer(0) == slider, "Slider should capture pointer while dragging");

        uiContext.routedEvents().dispatch(new PointerMovedEvent(slider, 260.0f, 10.0f, 260.0f, 10.0f, 0));
        expect(slider.value() == 100.0f, "Captured Slider drag should continue and clamp outside its visual bounds");

        uiContext.routedEvents().dispatch(new PointerReleasedEvent(slider, 260.0f, 10.0f, 260.0f, 10.0f, 0, PointerButton.PRIMARY));
        expect(!slider.dragging(), "Slider should stop dragging on release");
        expect(uiContext.capturedPointer(0) == null, "Slider should release pointer capture after drag release");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(slider, KeyCodes.LEFT, 0, 0));
        expect(slider.value() == 95.0f, "Focused Slider should nudge left by step");
        expect(changes.count >= 3 && changes.lastValue == 95.0f, "Slider should emit value changed events");
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
        expect(uiContext.capturedPointer(0) == scrollView.verticalScrollBar(), "ScrollBar should capture pointer while dragging");
        expect(near(scrollView.scrollY(), 100.0f), "Dragging vertical ScrollBar should update ScrollView scrollY");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(scrollView.verticalScrollBar(),
                97.0f, 180.0f, 3.0f, 180.0f, 0));
        expect(near(scrollView.scrollY(), 200.0f), "Captured ScrollBar drag should continue outside its visual bounds");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(scrollView.verticalScrollBar(),
                97.0f, 180.0f, 3.0f, 180.0f, 0, PointerButton.PRIMARY));
        expect(!scrollView.verticalScrollBar().dragging(), "ScrollBar should stop dragging on release");
        expect(uiContext.capturedPointer(0) == null, "ScrollBar should release pointer capture after drag release");

        DrawList drawList = new DrawList();
        scrollView.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "ScrollView should push a clip before rendering content");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "ScrollView should pop the clip after rendering content");
    }

    private void testNestedScissorStack() {
        ScissorStack scissorStack = new ScissorStack();

        ScissorStack.Rect root = scissorStack.push(new MutableRect(10.0f, 10.0f, 100.0f, 80.0f));
        expect(root.equals(new ScissorStack.Rect(10, 10, 110, 90)), "ScissorStack should push root clip bounds");

        ScissorStack.Rect nested = scissorStack.push(new MutableRect(50.0f, 0.0f, 80.0f, 50.0f));
        expect(nested.equals(new ScissorStack.Rect(50, 10, 110, 50)), "Nested scissor should intersect with parent bounds");

        ScissorStack.Rect restored = scissorStack.pop();
        expect(restored.equals(root), "Popping nested scissor should restore parent clip");

        ScissorStack.Rect empty = scissorStack.push(new MutableRect(500.0f, 500.0f, 20.0f, 20.0f));
        expect(empty.equals(new ScissorStack.Rect(500, 500, 500, 500)), "Non-overlapping nested scissor should collapse to empty rect");

        scissorStack.clear();
        expect(scissorStack.isEmpty(), "ScissorStack clear should remove all clip state");
    }

    private void testFixedRowVirtualizationCore() {
        FixedRowVirtualizer virtualizer = new FixedRowVirtualizer()
                .itemCount(1_000)
                .itemExtent(10.0f)
                .overscan(1)
                .viewportExtent(50.0f);

        VirtualRange initial = virtualizer.visibleRange();
        expect(initial.firstIndex() == 0 && initial.lastIndexExclusive() == 8 && initial.count() == 8,
                "FixedRowVirtualizer should calculate the first realized range with overscan");
        expect(virtualizer.contentExtent() == 10_000.0f && virtualizer.maxScrollOffset() == 9_950.0f,
                "FixedRowVirtualizer should expose content and max scroll extents");

        virtualizer.scrollOffset(95.0f);
        VirtualRange scrolled = virtualizer.visibleRange();
        expect(scrolled.firstIndex() == 8 && scrolled.lastIndexExclusive() == 16,
                "FixedRowVirtualizer should shift range with scroll offset");
        expect(near(virtualizer.itemOffset(8), -15.0f),
                "FixedRowVirtualizer should map item index to viewport-relative offset");

        virtualizer.itemCount(10).scrollOffset(20_000.0f);
        expect(virtualizer.scrollOffset() == 50.0f && virtualizer.visibleRange().lastIndexExclusive() == 10,
                "FixedRowVirtualizer should clamp scroll and ranges when item count shrinks");
    }

    private void testVirtualizedSelectionContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualListView list = new VirtualListView()
                .itemCount(20)
                .itemHeight(10.0f)
                .selectionMode(SelectionMode.MULTIPLE);
        list.setUiContextInternal(uiContext);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        Counter listSelectionChanges = new Counter();
        list.onSelectionChanged((SelectionChangedEvent event) -> {
            listSelectionChanges.count++;
            listSelectionChanges.lastSelection = event.newSelection();
        });

        list.selectIndex(2);
        expect(list.selectedIndex() == 2 && list.selectedIndices().equals(java.util.List.of(2)),
                "VirtualListView should expose single selected index");
        list.toggleIndex(4);
        expect(list.selectedIndices().equals(java.util.List.of(2, 4)) && listSelectionChanges.count == 2,
                "VirtualListView should support multi-selection toggle and emit changes");
        expect(listSelectionChanges.lastSelection.equals(java.util.List.of(2, 4)),
                "VirtualListView selection event should expose new selection snapshot");

        DrawList selectedListDrawList = new DrawList();
        list.render(new DefaultRenderContext(selectedListDrawList));
        expect(hasFillColor(selectedListDrawList, 0.18f, 0.45f, 0.75f, 0.35f),
                "VirtualListView should render selected row highlight");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(list, 5.0f, 35.0f, 5.0f, 35.0f, 0, PointerButton.PRIMARY));
        expect(list.selectedIndices().equals(java.util.List.of(3)),
                "VirtualListView primary click should single-select clicked row");
        list.selectIndex(0);
        var row3 = list.children().get(3);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(row3, 5.0f, 35.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(list.selectedIndices().equals(java.util.List.of(3)),
                "VirtualListView bubbled row-child click should use list-local coordinates");
        list.itemCount(3);
        expect(list.selectedIndices().isEmpty(), "VirtualListView should prune selected indices when item count shrinks");

        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Value", 50.0f)
                .rowCount(20)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .selectionMode(SelectionMode.MULTIPLE);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        Counter tableSelectionChanges = new Counter();
        table.onSelectionChanged((SelectionChangedEvent event) -> {
            tableSelectionChanges.count++;
            tableSelectionChanges.lastSelection = event.newSelection();
        });

        table.selectRow(1);
        table.toggleRow(3);
        expect(table.selectedRow() == 1 && table.selectedRows().equals(java.util.List.of(1, 3)),
                "VirtualTableView should support row multi-selection");
        expect(tableSelectionChanges.count == 2 && tableSelectionChanges.lastSelection.equals(java.util.List.of(1, 3)),
                "VirtualTableView should emit row selection changes");

        DrawList selectedTableDrawList = new DrawList();
        table.render(new DefaultRenderContext(selectedTableDrawList));
        expect(hasFillColor(selectedTableDrawList, 0.18f, 0.45f, 0.75f, 0.42f),
                "VirtualTableView should render selected row highlight");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 35.0f, 5.0f, 35.0f, 0, PointerButton.PRIMARY));
        expect(table.selectedRows().equals(java.util.List.of(2)),
                "VirtualTableView primary click should single-select clicked row below header");
        table.rowCount(2);
        expect(table.selectedRows().isEmpty(), "VirtualTableView should prune selected rows when row count shrinks");
    }

    private void testVirtualListViewRealizationAndScrolling() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Counter created = new Counter();
        VirtualListView list = new VirtualListView()
                .itemCount(1_000)
                .itemHeight(10.0f)
                .overscan(1)
                .scrollStep(20.0f)
                .itemFactory(index -> {
                    created.count++;
                    return new Label("Row " + index);
                });
        list.setUiContextInternal(uiContext);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));

        expect(list.contentHeight() == 10_000.0f, "VirtualListView should expose virtual content height");
        expect(list.realizedCount() == 8, "VirtualListView should realize only visible rows plus overscan");
        expect(list.realizedRange().equals(new VirtualRange(0, 8)), "VirtualListView should expose shared realized range");
        expect(created.count == 8, "VirtualListView should not materialize all rows");
        expect(list.firstVisibleIndex() == 0 && list.lastVisibleIndexExclusive() == 8, "VirtualListView should track realized range");
        expect(list.children().size() == 9, "VirtualListView children should include realized rows plus scrollbar");
        expect(list.children().get(0).layoutBounds().y() == 0.0f, "First realized row should be arranged at viewport origin");

        list.scrollTo(95.0f);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        expect(list.scrollY() == 95.0f, "VirtualListView should keep explicit scroll offset");
        expect(list.firstVisibleIndex() == 8 && list.lastVisibleIndexExclusive() == 16, "VirtualListView should shift realized range with scroll");
        expect(list.realizedRange().equals(new VirtualRange(8, 16)), "VirtualListView range should be sourced from shared virtualization core");
        expect(list.realizedCount() == 8, "VirtualListView should keep realized window bounded after scroll");
        expect(created.count < 20, "VirtualListView should create only newly visible rows after scroll");

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(list, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(consumed && list.scrollY() == 115.0f, "VirtualListView should consume wheel scroll when offset changes");
        list.scrollTo(20_000.0f);
        expect(list.scrollY() == 9_950.0f, "VirtualListView should clamp to max scroll");

        DrawList drawList = new DrawList();
        list.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "VirtualListView should clip realized rows");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "VirtualListView should pop row clip");
    }

    private void testVirtualTableViewVirtualRowsAndRendering() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Counter cellRequests = new Counter();
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Value", 50.0f)
                .rowCount(1_000)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .overscan(1)
                .scrollStep(20.0f)
                .cellTextProvider((row, column) -> {
                    cellRequests.count++;
                    return "R" + row + "C" + column;
                });
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));

        expect(table.contentWidth() == 110.0f && table.contentHeight() == 10_000.0f,
                "VirtualTableView should expose virtual content size");
        expect(table.firstVisibleRow() == 0 && table.lastVisibleRowExclusive() == 8,
                "VirtualTableView should track visible row range with overscan");
        expect(table.realizedRange().equals(new VirtualRange(0, 8)),
                "VirtualTableView should expose shared realized range");
        expect(table.realizedRowCount() == 8, "VirtualTableView should bound realized row count");
        expect(table.children().size() == 1 && table.children().get(0) == table.verticalScrollBar(),
                "VirtualTableView should expose only scrollbar as child, not every cell");

        DrawList initialDrawList = new DrawList();
        table.render(new DefaultRenderContext(initialDrawList));
        expect(initialDrawList.commands().get(0).type() == DrawCommandType.RECT, "VirtualTableView should draw a fixed header first");
        expect(hasText(initialDrawList, "Name") && hasText(initialDrawList, "Value"), "VirtualTableView should render column headers");
        expect(hasText(initialDrawList, "R0C0") && hasText(initialDrawList, "R7C1"), "VirtualTableView should render visible cells");
        expect(!hasText(initialDrawList, "R100C0"), "VirtualTableView should not render offscreen cells");
        expect(cellRequests.count <= table.realizedRowCount() * table.columns().size(),
                "VirtualTableView should request cell text only for rendered virtual rows");

        table.scrollTo(95.0f);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        expect(table.scrollY() == 95.0f, "VirtualTableView should keep explicit scroll offset");
        expect(table.firstVisibleRow() == 8 && table.lastVisibleRowExclusive() == 16,
                "VirtualTableView should shift visible rows with scroll");
        expect(table.realizedRange().equals(new VirtualRange(8, 16)),
                "VirtualTableView range should be sourced from shared virtualization core");

        DrawList scrolledDrawList = new DrawList();
        table.render(new DefaultRenderContext(scrolledDrawList));
        expect(hasText(scrolledDrawList, "R8C0") && hasText(scrolledDrawList, "R15C1"),
                "VirtualTableView should render scrolled virtual rows");
        expect(!hasText(scrolledDrawList, "R0C0"), "VirtualTableView should stop rendering rows outside the realized window");

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(table, 4.0f, 14.0f, 4.0f, 4.0f, 0.0f, -1.0f));
        expect(consumed && table.scrollY() == 115.0f, "VirtualTableView should consume wheel scroll when offset changes");
        table.scrollTo(20_000.0f);
        expect(table.scrollY() == 9_950.0f, "VirtualTableView should clamp to max row scroll");
    }

    private void testVirtualTableSortingContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        String[] names = {"Bob", "Alice", "Carol"};
        int[] scores = {20, 10, 30};
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Score", 50.0f)
                .rowCount(names.length)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .cellTextProvider((row, column) -> column == 0 ? names[row] : Integer.toString(scores[row]))
                .sortKeyProvider((row, column) -> column == 0 ? names[row] : scores[row]);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 50.0f));

        Counter sortChanges = new Counter();
        table.onSortChanged((TableSortChangedEvent event) -> {
            sortChanges.count++;
            sortChanges.lastSortColumn = event.newColumnIndex();
            sortChanges.lastSortDirection = event.newDirection();
        });

        table.sortBy(0, SortDirection.ASCENDING);
        DrawList nameAscendingDrawList = new DrawList();
        table.render(new DefaultRenderContext(nameAscendingDrawList));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.ASCENDING,
                "VirtualTableView should expose ascending sort state");
        expect(hasText(nameAscendingDrawList, "Name ↑"), "VirtualTableView should render ascending sort marker in header");
        expect(textCommandIndex(nameAscendingDrawList, "Alice", 0) < textCommandIndex(nameAscendingDrawList, "Bob", 0),
                "VirtualTableView should render rows in ascending sort-key order");

        table.sortBy(1, SortDirection.DESCENDING);
        DrawList scoreDescendingDrawList = new DrawList();
        table.render(new DefaultRenderContext(scoreDescendingDrawList));
        expect(hasText(scoreDescendingDrawList, "Score ↓"), "VirtualTableView should render descending sort marker in header");
        expect(textCommandIndex(scoreDescendingDrawList, "30", 0) < textCommandIndex(scoreDescendingDrawList, "20", 0),
                "VirtualTableView should render rows in descending numeric sort-key order");
        expect(sortChanges.count == 2 && sortChanges.lastSortColumn == 1 && sortChanges.lastSortDirection == SortDirection.DESCENDING,
                "VirtualTableView should emit sort changed events");

        table.columnComparator(0, (left, right) -> Integer.compare(right, left));
        table.sortBy(0, SortDirection.ASCENDING);
        DrawList customComparatorDrawList = new DrawList();
        table.render(new DefaultRenderContext(customComparatorDrawList));
        expect(textCommandIndex(customComparatorDrawList, "Carol", 0) < textCommandIndex(customComparatorDrawList, "Alice", 0),
                "VirtualTableView should allow per-column row comparator hooks");

        table.clearSort();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.ASCENDING,
                "VirtualTableView header click should start ascending sort for clicked column");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.DESCENDING,
                "VirtualTableView repeated header click should cycle to descending sort");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == -1 && table.sortDirection() == SortDirection.NONE,
                "VirtualTableView third header click should clear sort");
    }

    private void testToggleCheckboxProgressAndNumberField() {
        DefaultUIContext uiContext = new DefaultUIContext();

        ToggleButton toggle = new ToggleButton("Power");
        toggle.setUiContextInternal(uiContext);
        toggle.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 20.0f));
        DrawList toggleDrawList = new DrawList();
        toggle.render(new DefaultRenderContext(toggleDrawList));
        int toggleTextIndex = textCommandIndex(toggleDrawList, "Power", 0);
        expect(toggleTextIndex < Integer.MAX_VALUE && near(toggleDrawList.commands().get(toggleTextIndex).bounds().y(), 5.0f),
                "Button text should be vertically centered in the control bounds");

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
        DrawList checkboxDrawList = new DrawList();
        checkbox.render(new DefaultRenderContext(checkboxDrawList));
        int checkboxTextIndex = textCommandIndex(checkboxDrawList, "Enabled", 0);
        expect(checkboxTextIndex < Integer.MAX_VALUE && near(checkboxDrawList.commands().get(checkboxTextIndex).bounds().y(), 5.0f),
                "Checkbox label should be vertically centered against the check mark");

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

        ProgressBar lowProgressBar = new ProgressBar().range(0.0f, 100.0f);
        lowProgressBar.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 12.0f));
        for (int value = 1; value <= 13; value++) {
            lowProgressBar.value(value);
            DrawList lowProgressDrawList = new DrawList();
            lowProgressBar.render(new DefaultRenderContext(lowProgressDrawList));
            expect(lowProgressDrawList.size() >= 2, "ProgressBar should render small non-zero fill values");
            var fill = lowProgressDrawList.commands().get(1);
            expect(fill.type() == DrawCommandType.RECT, "ProgressBar fill should use stable rectangular geometry");
            expect(near(fill.bounds().x(), 0.0f), "ProgressBar fill should stay anchored to the track origin");
            expect(near(fill.bounds().width(), value), "ProgressBar fill width should match normalized low percent values");
        }

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

    private static int textCommandIndex(DrawList drawList, String text, int startIndex) {
        for (int i = Math.max(0, startIndex); i < drawList.commands().size(); i++) {
            if (text.equals(drawList.commands().get(i).text())) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
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

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) == flag;
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
        private java.util.List<Integer> lastSelection = java.util.List.of();
        private int lastSortColumn = -1;
        private SortDirection lastSortDirection = SortDirection.NONE;
    }
}
