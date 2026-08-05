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
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.Objects;

public class TextField extends Box {
    private static final float TEXT_PADDING = 4.0f;
    private static final float APPROX_CHAR_WIDTH = 6.0f;

    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor placeholderColor = new MutableColor(0.65f, 0.65f, 0.65f, 0.9f);
    private final MutableColor caretColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private String text = "";
    private String placeholder = "";
    private int cursorIndex;
    private int selectionAnchor = -1;
    private int selectionFocus = -1;
    private int maxLength = Integer.MAX_VALUE;
    private boolean focused;

    public TextField() {
        backgroundVisible(true);
        borderVisible(true);
        background().set(0.04f, 0.04f, 0.04f, 0.82f);
        borderColor().set(0.35f, 0.35f, 0.35f, 1.0f);
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        placeholderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        caretColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextField(String text) {
        this();
        this.text = normalize(text);
        cursorIndex = this.text.length();
    }

    public String text() {
        return text;
    }

    public TextField text(String text) {
        setText(normalize(text), true);
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    public TextField placeholder(String placeholder) {
        String normalized = normalize(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public TextField cursorIndex(int cursorIndex) {
        int clamped = clamp(cursorIndex, 0, text.length());
        if (this.cursorIndex == clamped) return this;
        this.cursorIndex = clamped;
        clearSelection();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int selectionStart() {
        return hasSelection() ? Math.min(selectionAnchor, selectionFocus) : cursorIndex;
    }

    public int selectionEnd() {
        return hasSelection() ? Math.max(selectionAnchor, selectionFocus) : cursorIndex;
    }

    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionFocus >= 0 && selectionAnchor != selectionFocus;
    }

    public String selectedText() {
        return hasSelection() ? text.substring(selectionStart(), selectionEnd()) : "";
    }

    public TextField select(int start, int end) {
        selectionAnchor = clamp(start, 0, text.length());
        selectionFocus = clamp(end, 0, text.length());
        cursorIndex = selectionEnd();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextField selectAll() {
        return select(0, text.length());
    }

    public TextField clearSelection() {
        if (selectionAnchor < 0 && selectionFocus < 0) return this;
        selectionAnchor = -1;
        selectionFocus = -1;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public int maxLength() {
        return maxLength;
    }

    public TextField maxLength(int maxLength) {
        int normalized = Math.max(0, maxLength);
        if (this.maxLength == normalized) return this;
        this.maxLength = normalized;
        if (text.length() > normalized) {
            setText(text.substring(0, normalized), true);
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
    protected void applyTheme() {
        super.applyTheme();
        textColor.set(styleValue(StyleKeys.TEXT_COLOR, textColor));
        placeholderColor.set(styleValue(StyleKeys.PLACEHOLDER_COLOR, placeholderColor));
        caretColor.set(styleValue(StyleKeys.ACCENT_COLOR, caretColor));
    }

    @Override
    protected WidgetState styleState() {
        return focused ? WidgetState.FOCUSED : WidgetState.NORMAL;
    }

    @Override
    public void handle(Event event) {
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
                    Paint.fill(text.isEmpty() ? placeholderColor : textColor),
                    transform());
        }

        if (focused) {
            float caretX = layoutBounds().x() + TEXT_PADDING + cursorIndex * APPROX_CHAR_WIDTH;
            context.rect(caretX,
                    layoutBounds().y() + 3.0f,
                    1.0f,
                    Math.max(1.0f, layoutBounds().height() - 6.0f),
                    Paint.fill(caretColor),
                    transform());
        }

        super.renderContent(context);
    }

    protected String displayText() {
        return text.isEmpty() ? placeholder : text;
    }

    protected boolean isShowingPlaceholder() {
        return text.isEmpty() && !placeholder.isEmpty();
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
                cursorIndex(cursorIndex - 1);
                yield true;
            }
            case KeyCodes.RIGHT -> {
                cursorIndex(cursorIndex + 1);
                yield true;
            }
            case KeyCodes.HOME -> {
                cursorIndex(0);
                yield true;
            }
            case KeyCodes.END -> {
                cursorIndex(text.length());
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
        if (!isPrintable(codePoint)) return;
        String inserted = new String(Character.toChars(codePoint));
        replaceSelectionIfNeeded();
        if (text.length() >= maxLength) return;
        if (text.length() + inserted.length() > maxLength) return;
        setText(text.substring(0, cursorIndex) + inserted + text.substring(cursorIndex), true);
        cursorIndex += inserted.length();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void backspace() {
        if (deleteSelectionIfNeeded()) return;
        if (cursorIndex <= 0 || text.isEmpty()) return;
        int previous = text.offsetByCodePoints(cursorIndex, -1);
        setText(text.substring(0, previous) + text.substring(cursorIndex), true);
        cursorIndex = previous;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void delete() {
        if (deleteSelectionIfNeeded()) return;
        if (cursorIndex >= text.length() || text.isEmpty()) return;
        int next = text.offsetByCodePoints(cursorIndex, 1);
        setText(text.substring(0, cursorIndex) + text.substring(next), true);
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean deleteSelectionIfNeeded() {
        if (!hasSelection()) return false;
        int start = selectionStart();
        int end = selectionEnd();
        setText(text.substring(0, start) + text.substring(end), true);
        cursorIndex = start;
        clearSelection();
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    private void replaceSelectionIfNeeded() {
        deleteSelectionIfNeeded();
    }

    private void copySelection() {
        UIContext context = uiContext();
        if (context != null && hasSelection()) {
            context.clipboard().setText(selectedText());
        }
    }

    private void cutSelection() {
        copySelection();
        deleteSelectionIfNeeded();
    }

    private void pasteClipboard() {
        UIContext context = uiContext();
        if (context == null) return;
        String clipboard = context.clipboard().getText();
        if (clipboard == null || clipboard.isEmpty()) return;
        replaceSelectionIfNeeded();
        String sanitized = sanitizeTextInput(clipboard);
        if (sanitized.isEmpty()) return;
        int remaining = Math.max(0, maxLength - text.length());
        if (sanitized.length() > remaining) {
            sanitized = sanitized.substring(0, remaining);
        }
        setText(text.substring(0, cursorIndex) + sanitized + text.substring(cursorIndex), true);
        cursorIndex += sanitized.length();
        invalidate(InvalidationFlags.VISUAL);
    }

    protected String sanitizeTextInput(String text) {
        return sanitizePrintable(text);
    }

    private void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        applyTheme();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setText(String text, boolean emitChange) {
        String normalized = trimToMax(normalize(text));
        if (Objects.equals(this.text, normalized)) return;
        String oldText = this.text;
        this.text = normalized;
        cursorIndex = clamp(cursorIndex, 0, this.text.length());
        if (selectionAnchor > this.text.length() || selectionFocus > this.text.length()) {
            clearSelection();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new TextChangedEvent(this, oldText, this.text));
        }
    }

    private String trimToMax(String value) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static boolean isPrintable(int codePoint) {
        return codePoint >= 32 && !Character.isISOControl(codePoint);
    }

    private static String sanitizePrintable(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints()
                .filter(TextField::isPrintable)
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
