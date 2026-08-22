package dev.sixik.unigui.widgets.interaction;

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
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.TextChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.widgets.render.TextInputRenderType;
import dev.sixik.unigui.widgets.render.TextInputState;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;

/**
 * Generic public single-line text editor widget.
 *
 * <p>TextInput owns the editable text model, cursor, selection, clipboard handling,
 * focus/input behavior and renderer state. Use it directly for raw/unframed editor
 * fields or as a base class for specialized inputs.</p>
 *
 * <p>The high-level text APIs, clipboard paste and text-input events sanitize
 * control characters, so this widget remains single-line. {@link #editorModel()}
 * exposes the low-level editor model for advanced integrations; callers that
 * mutate it directly must preserve that single-line invariant themselves.</p>
 */
@XmlWidgetName("TextInput")
public class TextInput extends Box {
    public static final String STYLE_TYPE = StyleIds.Widget.TEXT_INPUT;

    public static final class StyleProperties {
        public static final String BACKGROUND_COLOR = StyleIds.Key.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;
        public static final String TEXT_COLOR = StyleIds.Key.TEXT_COLOR;
        public static final String PLACEHOLDER_COLOR = StyleIds.Key.PLACEHOLDER_COLOR;
        public static final String ACCENT_COLOR = StyleIds.Key.ACCENT_COLOR;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String TEXT_COLOR = StyleAnimationIds.Property.TEXT_COLOR;
        public static final String PLACEHOLDER_COLOR = StyleAnimationIds.Property.PLACEHOLDER_COLOR;
        public static final String CARET_COLOR = StyleAnimationIds.Property.CARET_COLOR;
        public static final String BACKGROUND_COLOR = StyleAnimationIds.Property.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.TEXT_INPUT;

        private AnimationProperties() {
        }
    }

    public static final class AnimationEvents {
        public static final String ON_TEXT_CHANGED = StyleAnimationIds.Event.ON_TEXT_CHANGED;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_HOVER_ENTER = StyleAnimationIds.Event.ON_HOVER_ENTER;
        public static final String ON_HOVER_EXIT = StyleAnimationIds.Event.ON_HOVER_EXIT;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.TEXT_INPUT;

        private AnimationEvents() {
        }
    }

    protected static final float TEXT_PADDING = 4.0f;
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;

    private final TextEditorModel editor = new TextEditorModel();
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor placeholderColor = new MutableColor(0.65f, 0.65f, 0.65f, 0.9f);
    private final MutableColor caretColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private TextInputRenderer renderer;
    private String placeholder = "";
    private FontFace font;
    private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
    private boolean focused;
    private boolean selectingWithPointer;
    private int pointerSelectionAnchor;
    private float horizontalScrollPixels;
    private String measuredDisplayText = "";
    private Object measuredMetricsSource;
    private float[] measuredPrefixWidths = new float[]{0.0f};
    private boolean visualOnlyTextChanges;

    public TextInput() {
        mouseCursor(MouseCursor.TEXT);
        boxVisualEnabled(false);
        focusable(true);
        editor.onChanged((oldText, newText) -> {
            invalidate(visualOnlyTextChanges ? InvalidationFlags.VISUAL : InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
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

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Editable text value.")
    public TextInput text(String text) {
        editor.text(normalize(text));
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "", description = "Placeholder text shown while the value is empty.")
    public TextInput placeholder(String placeholder) {
        String normalized = normalize(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextInput font(FontFace font, float pixelSize) {
        float normalizedSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : TextRun.DEFAULT_PIXEL_SIZE;
        if (this.font == font && this.pixelSize == normalizedSize) return this;
        this.font = font;
        this.pixelSize = normalizedSize;
        measuredDisplayText = "";
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int cursorIndex() {
        return editor.cursorIndex();
    }

    @XmlAttribute(value = "cursorIndex", category = "Behavior", defaultValue = "0", description = "Initial cursor position in the editable text.")
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

    @XmlAttribute(value = "maxLength", category = "Behavior", defaultValue = "0", description = "Maximum editable character count; 0 means unlimited.")
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

    public TextInputRenderer renderer() {
        return renderer;
    }

    public TextInput renderer(TextInputRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextInput useDefaultRenderer() {
        return renderer(null);
    }

    public boolean visualOnlyTextChanges() {
        return visualOnlyTextChanges;
    }

    @XmlAttribute(value = "visualOnlyTextChanges", category = "Behavior", defaultValue = "false", description = "Whether text changes invalidate visuals only instead of layout.")
    public TextInput visualOnlyTextChanges(boolean visualOnlyTextChanges) {
        if (this.visualOnlyTextChanges == visualOnlyTextChanges) return this;
        this.visualOnlyTextChanges = visualOnlyTextChanges;
        return this;
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
        float width = TextEngine.measureLineWidth(richText(measuredText))
                + leftTextPadding()
                + rightTextPadding();
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
            selectingWithPointer = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(0, this);
            }
            clearSelection();
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
            int cursor = indexAtLocalX(pointer.localX());
            moveCursor(cursor, false);
            pointerSelectionAnchor = cursor;
            selectingWithPointer = true;
            if (context != null) {
                context.capturePointer(pointer.pointerId(), this);
            }
            event.cancel();
            return;
        }

        if (event instanceof PointerMovedEvent pointer && selectingWithPointer) {
            moveCursor(indexAtLocalX(pointer.localX()), true, pointerSelectionAnchor);
            event.cancel();
            return;
        }

        if (event instanceof PointerReleasedEvent pointer && selectingWithPointer) {
            selectingWithPointer = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
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
        applyTheme();
        renderTextInput(context);
        super.renderContent(context);
    }

    protected void renderTextInput(RenderContext context) {
        String visibleText = displayText();
        updateMeasuredPrefixWidths(context, visibleText);
        ensureCursorVisible(context, visibleText);

        float viewportX = textViewportX();
        float viewportY = textViewportY();
        float viewportWidth = textViewportWidth();
        float viewportHeight = textViewportHeight();
        float textHeight = textLineHeight(visibleText);
        float textY = textContentY(textHeight);
        TextInputState state = textInputState(
                visibleText,
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight,
                textY,
                textHeight);
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderer != null) {
            renderer.render(draw, state);
            return;
        }
        TextInputRenderer styled = styleRendererOverride(TextInputRenderer.class);
        if (styled != null) {
            styled.render(draw, state);
            return;
        }
        if (renderStylePlan(context, TextInputState.class, state)) return;
        defaultRenderer().render(draw, state);
    }

    protected TextInputState textInputState(String visibleText,
                                            float viewportX,
                                            float viewportY,
                                            float viewportWidth,
                                            float viewportHeight,
                                            float textY,
                                            float textHeight) {
        return new TextInputState(
                renderType(),
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                backgroundVisible(),
                background().copy(),
                radius(),
                borderVisible(),
                borderColor().copy(),
                borderWidth(),
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight,
                textY,
                textHeight,
                horizontalScrollPixels,
                measuredTextWidth(),
                visibleText,
                richText(visibleText),
                focused,
                isShowingPlaceholder(),
                hasSelection(),
                selectionStart(),
                selectionEnd(),
                cursorIndex(),
                textColor.copy(),
                placeholderColor.copy(),
                caretColor.copy(),
                measuredPrefixWidths,
                clearButtonVisible(),
                clearButtonX(),
                clearButtonY(),
                clearButtonWidth(),
                clearButtonHeight());
    }

    protected TextInputRenderer defaultRenderer() {
        return WidgetsRender.textInput();
    }

    protected TextInputRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TextInputRenderer.class, defaultRenderer()) : renderer;
    }

    protected TextInputRenderType renderType() {
        return TextInputRenderType.TEXT_INPUT;
    }

    protected boolean clearButtonVisible() {
        return false;
    }

    protected float clearButtonX() {
        return 0.0f;
    }

    protected float clearButtonY() {
        return 0.0f;
    }

    protected float clearButtonWidth() {
        return 0.0f;
    }

    protected float clearButtonHeight() {
        return 0.0f;
    }

    protected String displayText() {
        return text().isEmpty() ? placeholder : text();
    }

    protected boolean isShowingPlaceholder() {
        return text().isEmpty() && !placeholder.isEmpty();
    }

    protected float leftTextPadding() {
        return TEXT_PADDING;
    }

    protected float rightTextPadding() {
        return TEXT_PADDING;
    }

    protected float textViewportX() {
        return layoutBounds().x() + leftTextPadding();
    }

    protected float textViewportWidth() {
        return Math.max(0.0f, layoutBounds().width() - leftTextPadding() - rightTextPadding());
    }

    protected float textViewportY() {
        return layoutBounds().y() + textVerticalInset();
    }

    protected float textViewportHeight() {
        float inset = textVerticalInset();
        return Math.max(1.0f, layoutBounds().height() - inset * 2.0f);
    }

    protected float textLineHeight(String visibleText) {
        RichText line = richText(visibleText);
        float measured = TextEngine.measureTextHeight(line);
        return measured > 0.0f ? Math.max(TextEngine.LINE_HEIGHT, measured) : TextEngine.LINE_HEIGHT;
    }

    protected float textContentY(float contentHeight) {
        return textViewportY() + (textViewportHeight() - Math.max(0.0f, contentHeight)) * 0.5f;
    }

    protected float textVerticalInset() {
        return Math.min(3.0f, Math.max(0.0f, layoutBounds().height() * 0.5f));
    }

    private void updateMeasuredPrefixWidths(RenderContext context, String displayText) {
        String measured = displayText == null ? "" : displayText;
        Object metricsSource = context == null ? null : context.backend();
        if (measured.equals(measuredDisplayText)
                && measuredMetricsSource == metricsSource
                && measuredPrefixWidths.length == measured.length() + 1) {
            return;
        }
        measuredDisplayText = measured;
        measuredMetricsSource = metricsSource;
        measuredPrefixWidths = new float[measured.length() + 1];
        for (int i = 1; i <= measured.length(); i++) {
            measuredPrefixWidths[i] = TextEngine.measureLineWidth(context, richText(measured.substring(0, i)));
        }
    }

    private void ensureCursorVisible(RenderContext context, String displayText) {
        if (isShowingPlaceholder() || text().isEmpty()) {
            horizontalScrollPixels = 0.0f;
            return;
        }

        float viewportWidth = textViewportWidth();
        float textWidth = measuredTextWidth();
        if (viewportWidth <= 0.0f || textWidth <= viewportWidth) {
            horizontalScrollPixels = 0.0f;
            return;
        }

        float caret = prefixWidth(cursorIndex());
        if (caret - horizontalScrollPixels > viewportWidth) {
            horizontalScrollPixels = caret - viewportWidth + 1.0f;
        } else if (caret - horizontalScrollPixels < 0.0f) {
            horizontalScrollPixels = caret;
        }
        horizontalScrollPixels = clamp(horizontalScrollPixels, 0.0f, Math.max(0.0f, textWidth - viewportWidth));
    }

    private float measuredTextWidth() {
        return measuredPrefixWidths.length == 0 ? 0.0f : measuredPrefixWidths[measuredPrefixWidths.length - 1];
    }

    private float prefixWidth(int index) {
        int clamped = Math.max(0, Math.min(index, measuredPrefixWidths.length - 1));
        return measuredPrefixWidths[clamped];
    }

    private int indexAtLocalX(float localX) {
        if (text().isEmpty()) return 0;
        String displayText = displayText();
        float x = Math.max(0.0f, localX - leftTextPadding() + horizontalScrollPixels);
        if (displayText.equals(measuredDisplayText) && measuredPrefixWidths.length == displayText.length() + 1) {
            for (int i = 0; i < displayText.length(); i++) {
                float midpoint = (measuredPrefixWidths[i] + measuredPrefixWidths[i + 1]) * 0.5f;
                if (x < midpoint) return i;
            }
            return displayText.length();
        }
        return Math.max(0, Math.min(displayText.length(), Math.round(x / APPROX_CHAR_WIDTH)));
    }

    private void moveCursor(int cursorIndex, boolean extendSelection) {
        if (editor.moveCursor(cursorIndex, extendSelection)) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void moveCursor(int cursorIndex, boolean extendSelection, int selectionAnchor) {
        boolean changed = extendSelection
                ? editor.select(selectionAnchor, cursorIndex)
                : editor.moveCursor(cursorIndex, false);
        if (changed) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private int previousCursorIndex() {
        if (cursorIndex() <= 0 || text().isEmpty()) return 0;
        return text().offsetByCodePoints(cursorIndex(), -1);
    }

    private int nextCursorIndex() {
        if (cursorIndex() >= text().length() || text().isEmpty()) return text().length();
        return text().offsetByCodePoints(cursorIndex(), 1);
    }

    private boolean handleKey(int keyCode, int modifiers) {
        if (KeyModifiers.has(modifiers, KeyModifiers.CONTROL)) {
            return handleControlKey(keyCode);
        }

        boolean extendSelection = KeyModifiers.has(modifiers, KeyModifiers.SHIFT);
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
                moveCursor(previousCursorIndex(), extendSelection);
                yield true;
            }
            case KeyCodes.RIGHT -> {
                moveCursor(nextCursorIndex(), extendSelection);
                yield true;
            }
            case KeyCodes.HOME -> {
                moveCursor(0, extendSelection);
                yield true;
            }
            case KeyCodes.END -> {
                moveCursor(text().length(), extendSelection);
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
            case KeyCodes.BACKSPACE -> {
                backspaceWord();
                yield true;
            }
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

    private void backspaceWord() {
        if (editor.backspaceWord()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void delete() {
        if (editor.delete()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    protected void copySelection() {
        UIContext context = uiContext();
        if (context != null && hasSelection()) {
            context.clipboard().setText(selectedText());
        }
    }

    protected void cutSelection() {
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

    private RichText richText(String text) {
        return RichText.of(text, font, pixelSize);
    }

    private void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        applyTheme();
        invalidate(InvalidationFlags.VISUAL);
    }

    private static String normalize(String text) {
        return TextEditorModel.sanitizePrintable(text);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
