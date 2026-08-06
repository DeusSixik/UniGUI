package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.TextChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;

import java.util.Objects;

public class TextInput extends Box {
    protected static final float TEXT_PADDING = 4.0f;
    protected static final float APPROX_CHAR_WIDTH = 6.0f;

    private final TextEditorModel editor = new TextEditorModel();
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor placeholderColor = new MutableColor(0.65f, 0.65f, 0.65f, 0.9f);
    private final MutableColor caretColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private String placeholder = "";
    private boolean focused;

    public TextInput() {
        focusable(true);
        editor.onChanged((oldText, newText) -> {
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            emit(new TextChangedEvent(this, oldText, newText));
        });
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        placeholderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        caretColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextInput(String text) {
        this();
        editor.silentText(normalize(text));
        editor.cursorIndex(editor.text().length());
    }

    protected final void enableDefaultTextInputChrome() {
        backgroundVisible(true);
        borderVisible(true);
        background().set(0.04f, 0.04f, 0.04f, 0.82f);
        borderColor().set(0.35f, 0.35f, 0.35f, 1.0f);
    }

    public TextEditorModel editorModel() {
        return editor;
    }

    public String text() {
        return editor.text();
    }

    public TextInput text(String text) {
        editor.text(normalize(text));
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    public TextInput placeholder(String placeholder) {
        String normalized = normalize(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int cursorIndex() {
        return editor.cursorIndex();
    }

    public TextInput cursorIndex(int cursorIndex) {
        if (editor.cursorIndex(cursorIndex)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public int selectionStart() {
        return editor.selectionStart();
    }

    public int selectionEnd() {
        return editor.selectionEnd();
    }

    public boolean hasSelection() {
        return editor.hasSelection();
    }

    public String selectedText() {
        return editor.selectedText();
    }

    public TextInput select(int start, int end) {
        if (editor.select(start, end)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public TextInput selectAll() {
        return select(0, text().length());
    }

    public TextInput clearSelection() {
        if (editor.clearSelection()) {
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public int maxLength() {
        return editor.maxLength();
    }

    public TextInput maxLength(int maxLength) {
        if (editor.maxLength(maxLength)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public boolean focused() {
        return focused;
    }

    public MutableColor textColor() {
        return textColor;
    }

    public MutableColor placeholderColor() {
        return placeholderColor;
    }

    public MutableColor caretColor() {
        return caretColor;
    }

    public EventSubscription onTextChanged(EventListener<? super TextChangedEvent> listener) {
        return on(TextChangedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        String measuredText = displayText();
        float width = measuredText.codePointCount(0, measuredText.length()) * APPROX_CHAR_WIDTH + TEXT_PADDING * 2.0f;
        setDesiredSize(resolveDesiredSize(context, width, 18.0f));
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        textColor.set(styleValue(StyleKeys.TEXT_COLOR, textColor));
        placeholderColor.set(styleValue(StyleKeys.PLACEHOLDER_COLOR, placeholderColor));
        caretColor.set(styleValue(StyleKeys.ACCENT_COLOR, caretColor));
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        return focused ? WidgetState.FOCUSED : super.styleState();
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;

        if (event instanceof FocusGainedEvent) {
            setFocused(true);
            return;
        }
        if (event instanceof FocusLostEvent) {
            setFocused(false);
            return;
        }
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            UIContext context = uiContext();
            if (context != null) {
                context.focusManager().requestFocus(this);
            } else {
                setFocused(true);
            }
            cursorIndex(Math.round(Math.max(0.0f, pointer.localX() - TEXT_PADDING) / APPROX_CHAR_WIDTH));
            clearSelection();
            event.cancel();
            return;
        }

        if (!focused) return;

        if (event instanceof TextInputEvent input && input.phase() == EventPhase.TARGET) {
            insert(input.codePoint());
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET) {
            if (handleKey(key.keyCode(), key.modifiers())) {
                event.cancel();
            }
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        renderTextInput(context);
        super.renderContent(context);
    }

    protected void renderTextInput(RenderContext context) {
        String visibleText = displayText();
        if (hasSelection()) {
            int start = selectionStart();
            int end = selectionEnd();
            float selectionX = layoutBounds().x() + TEXT_PADDING + start * APPROX_CHAR_WIDTH;
            float selectionWidth = Math.max(1.0f, (end - start) * APPROX_CHAR_WIDTH);
            context.rect(selectionX,
                    layoutBounds().y() + 3.0f,
                    selectionWidth,
                    Math.max(1.0f, layoutBounds().height() - 6.0f),
                    Paint.fill(caretColor),
                    transform());
        }

        if (!visibleText.isEmpty()) {
            context.text(visibleText,
                    layoutBounds().x() + TEXT_PADDING,
                    layoutBounds().y() + 4.0f,
                    Math.max(0.0f, layoutBounds().width() - TEXT_PADDING * 2.0f),
                    Math.max(0.0f, layoutBounds().height() - 8.0f),
                    Paint.fill(text().isEmpty() ? placeholderColor : textColor),
                    transform());
        }

        if (focused) {
            float caretX = layoutBounds().x() + TEXT_PADDING + cursorIndex() * APPROX_CHAR_WIDTH;
            context.rect(caretX,
                    layoutBounds().y() + 3.0f,
                    1.0f,
                    Math.max(1.0f, layoutBounds().height() - 6.0f),
                    Paint.fill(caretColor),
                    transform());
        }
    }

    protected String displayText() {
        return text().isEmpty() ? placeholder : text();
    }

    protected boolean isShowingPlaceholder() {
        return text().isEmpty() && !placeholder.isEmpty();
    }

    private boolean handleKey(int keyCode, int modifiers) {
        if (KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            return handleControlKey(keyCode);
        }

        return switch (keyCode) {
            case KeyCodes.BACKSPACE -> {
                backspace();
                yield true;
            }
            case KeyCodes.DELETE -> {
                delete();
                yield true;
            }
            case KeyCodes.LEFT -> {
                cursorIndex(cursorIndex() - 1);
                yield true;
            }
            case KeyCodes.RIGHT -> {
                cursorIndex(cursorIndex() + 1);
                yield true;
            }
            case KeyCodes.HOME -> {
                cursorIndex(0);
                yield true;
            }
            case KeyCodes.END -> {
                cursorIndex(text().length());
                yield true;
            }
            case KeyCodes.ESCAPE, KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                UIContext context = uiContext();
                if (context != null) {
                    context.focusManager().clearFocus();
                } else {
                    setFocused(false);
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleControlKey(int keyCode) {
        return switch (keyCode) {
            case KeyCodes.A -> {
                selectAll();
                yield true;
            }
            case KeyCodes.C -> {
                copySelection();
                yield true;
            }
            case KeyCodes.X -> {
                cutSelection();
                yield true;
            }
            case KeyCodes.V -> {
                pasteClipboard();
                yield true;
            }
            default -> false;
        };
    }

    private void insert(int codePoint) {
        if (editor.insertCodePoint(codePoint)) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void backspace() {
        if (editor.backspace()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void delete() {
        if (editor.delete()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void copySelection() {
        UIContext context = uiContext();
        if (context != null && hasSelection()) {
            context.clipboard().setText(selectedText());
        }
    }

    private void cutSelection() {
        copySelection();
        if (editor.deleteSelectionIfNeeded()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void pasteClipboard() {
        UIContext context = uiContext();
        if (context == null) return;
        String clipboard = context.clipboard().getText();
        if (clipboard == null || clipboard.isEmpty()) return;
        String sanitized = sanitizeTextInput(clipboard);
        if (editor.insertText(sanitized)) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    protected String sanitizeTextInput(String text) {
        return TextEditorModel.sanitizePrintable(text);
    }

    private void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        applyTheme();
        invalidate(InvalidationFlags.VISUAL);
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }
}
