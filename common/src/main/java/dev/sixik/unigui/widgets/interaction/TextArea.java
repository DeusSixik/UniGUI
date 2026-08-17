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
import dev.sixik.unigui.api.event.ScrollEvent;
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
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.render.TextAreaLineState;
import dev.sixik.unigui.widgets.render.TextAreaRenderer;
import dev.sixik.unigui.widgets.render.TextAreaState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Multiline editable text widget for editor surfaces and code-like inputs.
 */
@XmlWidgetName("TextArea")
public class TextArea extends Box {
    protected static final float TEXT_PADDING = 4.0f;
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;

    private final TextEditorModel editor = new TextEditorModel();
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor placeholderColor = new MutableColor(0.65f, 0.65f, 0.65f, 0.9f);
    private final MutableColor caretColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private TextAreaRenderer renderer;
    private String placeholder = "";
    private FontFace font;
    private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
    private float explicitLineHeight;
    private int visibleLines = 6;
    private float scrollStep = 16.0f;
    private boolean focused;
    private boolean selectingWithPointer;
    private int pointerSelectionAnchor;
    private float horizontalScrollPixels;
    private float verticalScrollPixels;
    private float desiredCursorXPixels = Float.NaN;
    private String measuredDisplayText = null;
    private Object measuredMetricsSource;
    private float measuredLineHeight;
    private float measuredTextWidth;
    private List<LineMetrics> lineMetrics = List.of(new LineMetrics(0, 0, 0, "", RichText.plain(""), 0.0f, new float[]{0.0f}));
    private boolean visualOnlyTextChanges;

    public TextArea() {
        mouseCursor(MouseCursor.TEXT);
        focusable(true);
        enableDefaultTextAreaChrome();
        editor.onChanged((oldText, newText) -> {
            clearLineMetrics();
            invalidate(visualOnlyTextChanges ? InvalidationFlags.VISUAL : InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            emit(new TextChangedEvent(this, oldText, newText));
        });
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        placeholderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        caretColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextArea(String text) {
        this();
        editor.silentText(normalizeEditableText(text));
        editor.cursorIndex(text().length());
        clearLineMetrics();
    }

    protected final void enableDefaultTextAreaChrome() {
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

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Editable multiline text value.")
    public TextArea text(String text) {
        editor.text(normalizeEditableText(text));
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "", description = "Placeholder text shown while the value is empty.")
    public TextArea placeholder(String placeholder) {
        String normalized = normalizeEditableText(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        clearLineMetrics();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextArea font(FontFace font, float pixelSize) {
        float normalizedSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : TextRun.DEFAULT_PIXEL_SIZE;
        if (this.font == font && this.pixelSize == normalizedSize) return this;
        this.font = font;
        this.pixelSize = normalizedSize;
        clearLineMetrics();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float lineHeight() {
        return explicitLineHeight;
    }

    public float effectiveLineHeight() {
        return explicitLineHeight > 0.0f
                ? explicitLineHeight
                : Math.max(12.0f, TextEngine.lineHeight(richText("Ag")) + 4.0f);
    }

    @XmlAttribute(value = "lineHeight", category = "Appearance", defaultValue = "0", description = "Line height in UI pixels; 0 uses font metrics.")
    public TextArea lineHeight(float lineHeight) {
        float normalized = Float.isFinite(lineHeight) ? Math.max(0.0f, lineHeight) : 0.0f;
        if (this.explicitLineHeight == normalized) return this;
        this.explicitLineHeight = normalized;
        clearLineMetrics();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int visibleLines() {
        return visibleLines;
    }

    @XmlAttribute(value = "visibleLines", category = "Layout", defaultValue = "6", description = "Preferred visible line count used during measurement.")
    public TextArea visibleLines(int visibleLines) {
        int normalized = Math.max(1, visibleLines);
        if (this.visibleLines == normalized) return this;
        this.visibleLines = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int cursorIndex() {
        return editor.cursorIndex();
    }

    @XmlAttribute(value = "cursorIndex", category = "Behavior", defaultValue = "0", description = "Initial cursor position in the editable text.")
    public TextArea cursorIndex(int cursorIndex) {
        if (editor.cursorIndex(cursorIndex)) {
            resetDesiredCursorX();
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

    public TextArea select(int start, int end) {
        if (editor.select(start, end)) {
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public TextArea selectAll() {
        return select(0, text().length());
    }

    public TextArea clearSelection() {
        if (editor.clearSelection()) {
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public int maxLength() {
        return editor.maxLength() == Integer.MAX_VALUE ? 0 : editor.maxLength();
    }

    @XmlAttribute(value = "maxLength", category = "Behavior", defaultValue = "0", description = "Maximum editable character count; 0 means unlimited.")
    public TextArea maxLength(int maxLength) {
        int normalized = Math.max(0, maxLength);
        if (editor.maxLength(normalized == 0 ? Integer.MAX_VALUE : normalized)) {
            clearLineMetrics();
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    public boolean focused() {
        return focused;
    }

    public float scrollX() {
        return horizontalScrollPixels;
    }

    public float scrollY() {
        return verticalScrollPixels;
    }

    @XmlAttribute(value = "scrollX", category = "Behavior", defaultValue = "0", description = "Initial horizontal scroll offset in UI pixels.")
    public TextArea scrollX(float scrollX) {
        return scrollTo(scrollX, verticalScrollPixels);
    }

    @XmlAttribute(value = "scrollY", category = "Behavior", defaultValue = "0", description = "Initial vertical scroll offset in UI pixels.")
    public TextArea scrollY(float scrollY) {
        return scrollTo(horizontalScrollPixels, scrollY);
    }

    public float scrollStep() {
        return scrollStep;
    }

    @XmlAttribute(value = "scrollStep", category = "Behavior", defaultValue = "16", description = "UI pixels scrolled per wheel/page delta unit.")
    public TextArea scrollStep(float scrollStep) {
        this.scrollStep = Float.isFinite(scrollStep) ? Math.max(1.0f, scrollStep) : 16.0f;
        return this;
    }

    public TextArea scrollTo(float x, float y) {
        updateLineMetrics(null);
        float nextX = clamp(Float.isFinite(x) ? x : 0.0f, 0.0f, maxScrollX());
        float nextY = clamp(Float.isFinite(y) ? y : 0.0f, 0.0f, maxScrollY());
        if (horizontalScrollPixels == nextX && verticalScrollPixels == nextY) return this;
        horizontalScrollPixels = nextX;
        verticalScrollPixels = nextY;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextArea scrollBy(float dx, float dy) {
        return scrollTo(horizontalScrollPixels + dx, verticalScrollPixels + dy);
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

    public TextAreaRenderer renderer() {
        return renderer;
    }

    public TextArea renderer(TextAreaRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextArea useDefaultRenderer() {
        return renderer(null);
    }

    public boolean visualOnlyTextChanges() {
        return visualOnlyTextChanges;
    }

    @XmlAttribute(value = "visualOnlyTextChanges", category = "Behavior", defaultValue = "false", description = "Whether text changes invalidate visuals only instead of layout.")
    public TextArea visualOnlyTextChanges(boolean visualOnlyTextChanges) {
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
        updateLineMetrics(null);
        float width = Math.max(120.0f, measuredTextWidth + leftTextPadding() + rightTextPadding());
        float height = effectiveLineHeight() * visibleLines + topTextPadding() + bottomTextPadding();
        setDesiredSize(resolveDesiredSize(context, width, height));
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
            int cursor = indexAtLocal(pointer.localX(), pointer.localY());
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
            moveCursor(indexAtLocal(pointer.localX(), pointer.localY()), true, pointerSelectionAnchor);
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

        if (event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float beforeX = horizontalScrollPixels;
            float beforeY = verticalScrollPixels;
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)) {
                scrollBy(-scroll.deltaY() * scrollStep, 0.0f);
            } else {
                scrollBy(-scroll.deltaX() * scrollStep, -scroll.deltaY() * scrollStep);
            }
            if (beforeX != horizontalScrollPixels || beforeY != verticalScrollPixels) {
                event.cancel();
            }
            return;
        }

        if (!focused) return;

        if (event instanceof TextInputEvent input && input.phase() == EventPhase.TARGET) {
            insertCodePoint(input.codePoint());
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET) {
            if (handleKey(key.keyCode(), key.modifiers())) {
                event.cancel();
            }
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        renderTextArea(context);
        super.renderContent(context);
    }

    protected void renderTextArea(RenderContext context) {
        updateLineMetrics(context);
        ensureCursorVisible();
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), textAreaState());
    }

    protected TextAreaState textAreaState() {
        float lineHeight = effectiveLineHeight();
        List<TextAreaLineState> states = new ArrayList<>(lineMetrics.size());
        for (LineMetrics line : lineMetrics) {
            float lineY = textViewportY() + line.lineIndex * lineHeight - verticalScrollPixels;
            states.add(new TextAreaLineState(
                    line.lineIndex,
                    line.start,
                    line.end,
                    lineY,
                    lineHeight,
                    line.measuredWidth,
                    line.text,
                    line.richText,
                    line.prefixWidths));
        }
        return new TextAreaState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                textViewportX(),
                textViewportY(),
                textViewportWidth(),
                textViewportHeight(),
                horizontalScrollPixels,
                verticalScrollPixels,
                lineHeight,
                Math.max(1.0f, lineHeight - 2.0f),
                measuredTextWidth,
                lineMetrics.size(),
                states,
                focused,
                isShowingPlaceholder(),
                hasSelection(),
                selectionStart(),
                selectionEnd(),
                cursorIndex(),
                textColor.copy(),
                placeholderColor.copy(),
                caretColor.copy(),
                APPROX_CHAR_WIDTH);
    }

    protected TextAreaRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TextAreaRenderer.class, WidgetsRender.textArea()) : renderer;
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

    protected float topTextPadding() {
        return TEXT_PADDING;
    }

    protected float bottomTextPadding() {
        return TEXT_PADDING;
    }

    protected float textViewportX() {
        return layoutBounds().x() + leftTextPadding();
    }

    protected float textViewportY() {
        return layoutBounds().y() + topTextPadding();
    }

    protected float textViewportWidth() {
        return Math.max(0.0f, layoutBounds().width() - leftTextPadding() - rightTextPadding());
    }

    protected float textViewportHeight() {
        return Math.max(0.0f, layoutBounds().height() - topTextPadding() - bottomTextPadding());
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
            clearLineMetrics();
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    protected String sanitizeTextInput(String text) {
        return normalizeEditableText(text);
    }

    private void updateLineMetrics(RenderContext context) {
        String displayText = displayText();
        float lineHeight = effectiveLineHeight();
        Object metricsSource = context == null ? null : context.backend();
        if (Objects.equals(measuredDisplayText, displayText)
                && measuredMetricsSource == metricsSource
                && measuredLineHeight == lineHeight) {
            return;
        }

        measuredDisplayText = displayText;
        measuredMetricsSource = metricsSource;
        measuredLineHeight = lineHeight;
        measuredTextWidth = 0.0f;
        List<LineMetrics> lines = new ArrayList<>();
        int start = 0;
        int lineIndex = 0;
        while (start <= displayText.length()) {
            int end = displayText.indexOf('\n', start);
            if (end < 0) end = displayText.length();
            String lineText = displayText.substring(start, end);
            RichText richText = richText(lineText);
            float[] prefixWidths = new float[lineText.length() + 1];
            for (int i = 1; i <= lineText.length(); i++) {
                prefixWidths[i] = TextEngine.measureLineWidth(context, richText(lineText.substring(0, i)));
            }
            float width = prefixWidths.length == 0 ? 0.0f : prefixWidths[prefixWidths.length - 1];
            measuredTextWidth = Math.max(measuredTextWidth, width);
            lines.add(new LineMetrics(lineIndex, start, end, lineText, richText, width, prefixWidths));
            lineIndex++;
            if (end >= displayText.length()) break;
            start = end + 1;
        }
        if (lines.isEmpty()) {
            lines.add(new LineMetrics(0, 0, 0, "", RichText.plain(""), 0.0f, new float[]{0.0f}));
        }
        lineMetrics = List.copyOf(lines);
        horizontalScrollPixels = clamp(horizontalScrollPixels, 0.0f, maxScrollX());
        verticalScrollPixels = clamp(verticalScrollPixels, 0.0f, maxScrollY());
    }

    private void clearLineMetrics() {
        measuredDisplayText = null;
    }

    private void ensureCursorVisible() {
        if (isShowingPlaceholder() || text().isEmpty()) {
            horizontalScrollPixels = 0.0f;
            verticalScrollPixels = 0.0f;
            return;
        }
        LineMetrics line = lineForIndex(cursorIndex());
        float lineHeight = effectiveLineHeight();
        float caretX = line.prefixWidth(cursorIndex() - line.start);
        float viewportWidth = textViewportWidth();
        if (viewportWidth <= 0.0f || measuredTextWidth <= viewportWidth) {
            horizontalScrollPixels = 0.0f;
        } else if (caretX - horizontalScrollPixels > viewportWidth) {
            horizontalScrollPixels = caretX - viewportWidth + 1.0f;
        } else if (caretX - horizontalScrollPixels < 0.0f) {
            horizontalScrollPixels = caretX;
        }

        float lineTop = line.lineIndex * lineHeight;
        float lineBottom = lineTop + lineHeight;
        float viewportHeight = textViewportHeight();
        if (viewportHeight <= 0.0f || contentHeight() <= viewportHeight) {
            verticalScrollPixels = 0.0f;
        } else if (lineBottom - verticalScrollPixels > viewportHeight) {
            verticalScrollPixels = lineBottom - viewportHeight;
        } else if (lineTop - verticalScrollPixels < 0.0f) {
            verticalScrollPixels = lineTop;
        }

        horizontalScrollPixels = clamp(horizontalScrollPixels, 0.0f, maxScrollX());
        verticalScrollPixels = clamp(verticalScrollPixels, 0.0f, maxScrollY());
    }

    private int indexAtLocal(float localX, float localY) {
        updateLineMetrics(null);
        if (lineMetrics.isEmpty()) return 0;
        float lineHeight = effectiveLineHeight();
        int lineIndex = (int) Math.floor((localY - topTextPadding() + verticalScrollPixels) / Math.max(1.0f, lineHeight));
        lineIndex = clamp(lineIndex, 0, lineMetrics.size() - 1);
        float x = Math.max(0.0f, localX - leftTextPadding() + horizontalScrollPixels);
        return indexAtLineX(lineMetrics.get(lineIndex), x);
    }

    private int indexAtLineX(LineMetrics line, float x) {
        if (line.length() <= 0) return line.start;
        for (int i = 0; i < line.length(); i++) {
            float midpoint = (line.prefixWidth(i) + line.prefixWidth(i + 1)) * 0.5f;
            if (x < midpoint) return line.start + i;
        }
        return line.end;
    }

    private void moveCursor(int cursorIndex, boolean extendSelection) {
        moveCursor(cursorIndex, extendSelection, false);
    }

    private void moveCursor(int cursorIndex, boolean extendSelection, int selectionAnchor) {
        boolean changed = extendSelection
                ? editor.select(selectionAnchor, cursorIndex)
                : editor.moveCursor(cursorIndex, false);
        if (changed) {
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void moveCursor(int cursorIndex, boolean extendSelection, boolean keepDesiredCursorX) {
        if (editor.moveCursor(cursorIndex, extendSelection)) {
            if (!keepDesiredCursorX) resetDesiredCursorX();
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
            return handleControlKey(keyCode, modifiers);
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
                moveCursor(lineForIndex(cursorIndex()).start, extendSelection);
                yield true;
            }
            case KeyCodes.END -> {
                moveCursor(lineForIndex(cursorIndex()).end, extendSelection);
                yield true;
            }
            case KeyCodes.UP -> {
                moveVertical(-1, extendSelection);
                yield true;
            }
            case KeyCodes.DOWN -> {
                moveVertical(1, extendSelection);
                yield true;
            }
            case KeyCodes.PAGE_UP -> {
                moveVertical(-pageLineDelta(), extendSelection);
                yield true;
            }
            case KeyCodes.PAGE_DOWN -> {
                moveVertical(pageLineDelta(), extendSelection);
                yield true;
            }
            case KeyCodes.ENTER, KeyCodes.KEYPAD_ENTER -> {
                insertText("\n");
                yield true;
            }
            case KeyCodes.TAB -> {
                insertText("\t");
                yield true;
            }
            case KeyCodes.ESCAPE -> {
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

    private boolean handleControlKey(int keyCode, int modifiers) {
        boolean extendSelection = KeyModifiers.has(modifiers, KeyModifiers.SHIFT);
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
            case KeyCodes.HOME -> {
                moveCursor(0, extendSelection);
                yield true;
            }
            case KeyCodes.END -> {
                moveCursor(text().length(), extendSelection);
                yield true;
            }
            default -> false;
        };
    }

    private void moveVertical(int lineDelta, boolean extendSelection) {
        updateLineMetrics(null);
        if (lineMetrics.isEmpty()) return;
        LineMetrics current = lineForIndex(cursorIndex());
        if (!Float.isFinite(desiredCursorXPixels)) {
            desiredCursorXPixels = current.prefixWidth(cursorIndex() - current.start);
        }
        int targetLineIndex = clamp(current.lineIndex + lineDelta, 0, lineMetrics.size() - 1);
        int targetIndex = indexAtLineX(lineMetrics.get(targetLineIndex), desiredCursorXPixels);
        moveCursor(targetIndex, extendSelection, true);
    }

    private int pageLineDelta() {
        return Math.max(1, (int) Math.floor(textViewportHeight() / Math.max(1.0f, effectiveLineHeight())));
    }

    private void insertCodePoint(int codePoint) {
        if (codePoint == '\r' || codePoint == '\n') {
            insertText("\n");
            return;
        }
        if (codePoint == '\t') {
            insertText("\t");
            return;
        }
        if (!Character.isValidCodePoint(codePoint) || !TextEditorModel.isPrintable(codePoint)) return;
        insertText(new String(Character.toChars(codePoint)));
    }

    private void insertText(String text) {
        if (editor.insertText(sanitizeTextInput(text))) {
            clearLineMetrics();
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void backspace() {
        if (editor.backspace()) {
            clearLineMetrics();
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void delete() {
        if (editor.delete()) {
            clearLineMetrics();
            resetDesiredCursorX();
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    private void pasteClipboard() {
        UIContext context = uiContext();
        if (context == null) return;
        String clipboard = context.clipboard().getText();
        if (clipboard == null || clipboard.isEmpty()) return;
        insertText(clipboard);
    }

    private LineMetrics lineForIndex(int index) {
        if (measuredDisplayText == null) {
            updateLineMetrics(null);
        }
        int clamped = clamp(index, 0, isShowingPlaceholder() ? 0 : text().length());
        for (LineMetrics line : lineMetrics) {
            if (clamped >= line.start && clamped <= line.end) return line;
        }
        return clamped < lineMetrics.get(0).start ? lineMetrics.get(0) : lineMetrics.get(lineMetrics.size() - 1);
    }

    private float contentHeight() {
        return lineMetrics.size() * effectiveLineHeight();
    }

    private float maxScrollX() {
        return Math.max(0.0f, measuredTextWidth - textViewportWidth());
    }

    private float maxScrollY() {
        return Math.max(0.0f, contentHeight() - textViewportHeight());
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

    private void resetDesiredCursorX() {
        desiredCursorXPixels = Float.NaN;
    }

    private static String normalizeEditableText(String text) {
        if (text == null || text.isEmpty()) return "";
        String normalizedBreaks = text.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder builder = new StringBuilder(normalizedBreaks.length());
        normalizedBreaks.codePoints()
                .filter(codePoint -> codePoint == '\n' || codePoint == '\t' || TextEditorModel.isPrintable(codePoint))
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record LineMetrics(int lineIndex,
                               int start,
                               int end,
                               String text,
                               RichText richText,
                               float measuredWidth,
                               float[] prefixWidths) {
        int length() {
            return Math.max(0, end - start);
        }

        float prefixWidth(int localIndex) {
            if (prefixWidths.length == 0) return 0.0f;
            int clamped = Math.max(0, Math.min(localIndex, prefixWidths.length - 1));
            return prefixWidths[clamped];
        }
    }
}
