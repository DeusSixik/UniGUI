package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Code-oriented multiline editor built on top of {@link TextArea}.
 */
@XmlWidgetName("CodeEditor")
public class CodeEditor extends TextArea {
    private static final float GUTTER_PADDING = 6.0f;
    private static final float GUTTER_MIN_WIDTH = 28.0f;
    private static final float DIAGNOSTIC_UNDERLINE_MIN_WIDTH = 4.0f;
    private static final float COMPLETION_POPUP_MIN_WIDTH = 160.0f;
    private static final float COMPLETION_POPUP_MAX_WIDTH = 280.0f;
    private static final float COMPLETION_ROW_HEIGHT = 18.0f;
    private static final int DEFAULT_MAX_VISIBLE_COMPLETIONS = 8;

    private final MutableColor lineNumberColor = new MutableColor(0.50f, 0.56f, 0.66f, 0.95f);
    private final MutableColor gutterColor = new MutableColor(0.02f, 0.025f, 0.035f, 0.70f);
    private final MutableColor diagnosticRowColor = new MutableColor(0.95f, 0.18f, 0.18f, 0.16f);
    private final MutableColor errorColor = new MutableColor(1.0f, 0.25f, 0.25f, 0.92f);
    private final MutableColor warningColor = new MutableColor(1.0f, 0.72f, 0.20f, 0.90f);
    private final MutableColor infoColor = new MutableColor(0.32f, 0.68f, 1.0f, 0.85f);
    private final CompletionAnchor completionAnchor = new CompletionAnchor();
    private final VBox completionList = new VBox();
    private final Popup completionPopup = new Popup();

    private List<Diagnostic> diagnostics = List.of();
    private String cleanText = "";
    private boolean dirty;
    private boolean lineNumbersVisible = true;
    private String languageId = "";
    private CodeLanguagePreset languagePreset = CodeLanguagePreset.NONE;
    private int tabSize = 4;
    private TabInputMode tabInputMode = TabInputMode.INSERT_TAB;
    private TabStorageMode tabStorageMode = TabStorageMode.PRESERVE;
    private CodeTokenizer tokenizer = CodeTokenizer.NONE;
    private CodeValidator validator = CodeValidator.NONE;
    private CompletionProvider completionProvider = CompletionProvider.NONE;
    private OverlayLayer explicitCompletionOverlayLayer;
    private OverlayLayer attachedCompletionOverlayLayer;
    private List<CompletionItem> activeCompletionItems = List.of();
    private int selectedCompletionIndex;
    private int maxVisibleCompletionItems = DEFAULT_MAX_VISIBLE_COMPLETIONS;
    private CodeTokenizer cachedTokenizer;
    private String cachedTokenText;
    private String cachedTokenLanguageId;
    private List<CodeToken> cachedTokens = List.of();

    public CodeEditor() {
        initializeCodeEditor();
    }

    public CodeEditor(String text) {
        super(text);
        cleanText = text();
        initializeCodeEditor();
    }

    private void initializeCodeEditor() {
        placeholder("Code...");
        completionList.spacing(1.0f);
        completionPopup.anchor(completionAnchor)
                .content(completionList)
                .padding(EdgeInsets.all(2.0f))
                .offset(0.0f, 2.0f)
                .closeOnOutsideClick(true);
        onTextChanged(event -> {
            clearSyntaxCache();
            updateDirty(event.newText());
        });
        lineNumberColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        gutterColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        diagnosticRowColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        errorColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        warningColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        infoColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Editable source text.")
    public CodeEditor text(String text) {
        super.text(text);
        return this;
    }

    @Override
    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "Code...", description = "Placeholder text shown while the editor is empty.")
    public CodeEditor placeholder(String placeholder) {
        super.placeholder(placeholder);
        return this;
    }

    @Override
    @XmlAttribute(value = "fontSize", category = "Appearance", defaultValue = "10", description = "Default editor text size in UI pixels.")
    public CodeEditor fontSize(float pixelSize) {
        super.fontSize(pixelSize);
        clearSyntaxCache();
        return this;
    }

    @Override
    public CodeEditor font(FontFace font, float pixelSize) {
        super.font(font, pixelSize);
        clearSyntaxCache();
        return this;
    }

    @Override
    @XmlAttribute(value = "lineHeight", category = "Appearance", defaultValue = "0", description = "Line height in UI pixels; 0 uses font metrics.")
    public CodeEditor lineHeight(float lineHeight) {
        super.lineHeight(lineHeight);
        return this;
    }

    @Override
    @XmlAttribute(value = "readOnly", category = "Behavior", defaultValue = "false", description = "When true, user input cannot mutate editor text.")
    public CodeEditor readOnly(boolean readOnly) {
        super.readOnly(readOnly);
        return this;
    }

    public CodeEditor loadText(String text) {
        text(text);
        return markClean();
    }

    public boolean dirty() {
        return dirty;
    }

    @XmlAttribute(value = "dirty", category = "State", defaultValue = "false", description = "Whether editor contents differ from the last clean snapshot.")
    public CodeEditor dirty(boolean dirty) {
        if (this.dirty == dirty) return this;
        this.dirty = dirty;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public CodeEditor markClean() {
        cleanText = text();
        return dirty(false);
    }

    public boolean lineNumbersVisible() {
        return lineNumbersVisible;
    }

    @XmlAttribute(value = "lineNumbers", category = "Appearance", defaultValue = "true", description = "Whether the line-number gutter is rendered.")
    public CodeEditor lineNumbersVisible(boolean lineNumbersVisible) {
        if (this.lineNumbersVisible == lineNumbersVisible) return this;
        this.lineNumbersVisible = lineNumbersVisible;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public String languageId() {
        return languageId;
    }

    @XmlAttribute(value = "languageId", category = "Behavior", defaultValue = "", description = "Stable language id used by tokenizer, validator and completion providers.")
    public CodeEditor languageId(String languageId) {
        String normalized = languageId == null ? "" : languageId.trim();
        if (Objects.equals(this.languageId, normalized)) return this;
        this.languageId = normalized;
        clearSyntaxCache();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public CodeLanguagePreset languagePreset() {
        return languagePreset;
    }

    @XmlAttribute(value = "languagePreset", category = "Behavior", defaultValue = "NONE", description = "Built-in language preset used to install a default tokenizer.")
    public CodeEditor languagePreset(CodeLanguagePreset languagePreset) {
        CodeLanguagePreset normalized = languagePreset == null ? CodeLanguagePreset.NONE : languagePreset;
        if (this.languagePreset == normalized) {
            if (!isLanguagePresetApplied(normalized)) {
                CodeLanguagePresets.apply(this, normalized);
            }
            return this;
        }
        this.languagePreset = normalized;
        CodeLanguagePresets.apply(this, normalized);
        return this;
    }

    private boolean isLanguagePresetApplied(CodeLanguagePreset preset) {
        return switch (preset) {
            case NONE -> languageId.isEmpty() && tokenizer == CodeTokenizer.NONE;
            case XAML -> languageId.equals("xaml") && tokenizer == CodeLanguagePresets.xamlTokenizer();
        };
    }

    public int tabSize() {
        return tabSize;
    }

    @XmlAttribute(value = "tabSize", category = "Behavior", defaultValue = "4", description = "Visual and insertion width used for indentation tabs.")
    public CodeEditor tabSize(int tabSize) {
        int normalized = Math.max(1, Math.min(32, tabSize));
        if (this.tabSize == normalized) return this;
        this.tabSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TabInputMode tabInputMode() {
        return tabInputMode;
    }

    @XmlAttribute(value = "tabInputMode", category = "Behavior", defaultValue = "INSERT_TAB", description = "How the Tab key inserts indentation.")
    public CodeEditor tabInputMode(TabInputMode tabInputMode) {
        TabInputMode normalized = tabInputMode == null ? TabInputMode.INSERT_TAB : tabInputMode;
        if (this.tabInputMode == normalized) return this;
        this.tabInputMode = normalized;
        return this;
    }

    public TabStorageMode tabStorageMode() {
        return tabStorageMode;
    }

    @XmlAttribute(value = "tabStorageMode", category = "Behavior", defaultValue = "PRESERVE", description = "Optional storage conversion applied after Tab indentation.")
    public CodeEditor tabStorageMode(TabStorageMode tabStorageMode) {
        TabStorageMode normalized = tabStorageMode == null ? TabStorageMode.PRESERVE : tabStorageMode;
        if (this.tabStorageMode == normalized) return this;
        this.tabStorageMode = normalized;
        return this;
    }

    public CodeTokenizer tokenizer() {
        return tokenizer;
    }

    public CodeEditor tokenizer(CodeTokenizer tokenizer) {
        CodeTokenizer normalized = tokenizer == null ? CodeTokenizer.NONE : tokenizer;
        if (this.tokenizer == normalized) return this;
        this.tokenizer = normalized;
        clearSyntaxCache();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public CodeValidator validator() {
        return validator;
    }

    public CodeEditor validator(CodeValidator validator) {
        this.validator = validator == null ? CodeValidator.NONE : validator;
        return this;
    }

    public CompletionProvider completionProvider() {
        return completionProvider;
    }

    public CodeEditor completionProvider(CompletionProvider completionProvider) {
        this.completionProvider = completionProvider == null ? CompletionProvider.NONE : completionProvider;
        hideCompletionPopup();
        return this;
    }

    public OverlayLayer completionOverlayLayer() {
        return explicitCompletionOverlayLayer == null ? findTopmostOverlayLayer() : explicitCompletionOverlayLayer;
    }

    public CodeEditor completionOverlayLayer(OverlayLayer overlayLayer) {
        if (explicitCompletionOverlayLayer == overlayLayer) return this;
        explicitCompletionOverlayLayer = overlayLayer;
        syncCompletionAttachment();
        if (!activeCompletionItems.isEmpty() && attachedCompletionOverlayLayer != null) {
            completionPopup.open();
        }
        return this;
    }

    public Popup completionPopup() {
        return completionPopup;
    }

    public boolean completionPopupOpened() {
        return completionPopup.opened() && attachedCompletionOverlayLayer != null;
    }

    public List<CompletionItem> activeCompletionItems() {
        return activeCompletionItems;
    }

    public int selectedCompletionIndex() {
        return selectedCompletionIndex;
    }

    public CodeEditor selectedCompletionIndex(int index) {
        if (activeCompletionItems.isEmpty()) {
            selectedCompletionIndex = 0;
            return this;
        }
        int clamped = Math.max(0, Math.min(index, activeCompletionItems.size() - 1));
        if (selectedCompletionIndex == clamped) return this;
        selectedCompletionIndex = clamped;
        rebuildCompletionList();
        return this;
    }

    public int maxVisibleCompletionItems() {
        return maxVisibleCompletionItems;
    }

    @XmlAttribute(value = "maxVisibleCompletionItems", category = "Behavior", defaultValue = "8", description = "Maximum completion rows shown in the popup.")
    public CodeEditor maxVisibleCompletionItems(int maxVisibleCompletionItems) {
        int normalized = Math.max(1, maxVisibleCompletionItems);
        if (this.maxVisibleCompletionItems == normalized) return this;
        this.maxVisibleCompletionItems = normalized;
        if (!activeCompletionItems.isEmpty()) {
            showCompletionPopup();
        }
        return this;
    }

    public CodeEditor showCompletionPopup() {
        List<CompletionItem> items = completionItems();
        if (items.isEmpty()) {
            return hideCompletionPopup();
        }
        int limit = Math.min(items.size(), maxVisibleCompletionItems);
        activeCompletionItems = List.copyOf(items.subList(0, limit));
        selectedCompletionIndex = Math.max(0, Math.min(selectedCompletionIndex, activeCompletionItems.size() - 1));
        updateCompletionAnchor();
        rebuildCompletionList();
        syncCompletionAttachment();
        if (attachedCompletionOverlayLayer != null) {
            completionPopup.open();
        } else {
            completionPopup.close();
        }
        return this;
    }

    public CodeEditor hideCompletionPopup() {
        completionPopup.close();
        activeCompletionItems = List.of();
        selectedCompletionIndex = 0;
        completionList.clearChildren();
        detachCompletionPopup();
        return this;
    }

    public CodeEditor acceptSelectedCompletion() {
        if (activeCompletionItems.isEmpty()) return this;
        return acceptCompletion(activeCompletionItems.get(selectedCompletionIndex));
    }

    public CodeEditor scrollToLine(int line) {
        int target = Math.max(1, Math.min(line, lineCount()));
        scrollTo(scrollX(), (target - 1) * effectiveLineHeight());
        return this;
    }

    public int lineCount() {
        String value = text();
        if (value.isEmpty()) return 1;
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\n') count++;
        }
        return count;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public Optional<Diagnostic> firstDiagnostic() {
        return diagnostics.stream().findFirst();
    }

    public CodeEditor scrollToFirstDiagnostic() {
        firstDiagnostic()
                .filter(Diagnostic::hasLocation)
                .ifPresent(diagnostic -> scrollToLine(diagnostic.line()));
        return this;
    }

    public CodeEditor diagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = normalizeDiagnostics(diagnostics);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public CodeEditor codeDiagnostics(List<CodeDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) return clearDiagnostics();
        List<Diagnostic> converted = new ArrayList<>(diagnostics.size());
        for (CodeDiagnostic diagnostic : diagnostics) {
            if (diagnostic != null) converted.add(Diagnostic.from(diagnostic));
        }
        return diagnostics(converted);
    }

    public CodeEditor diagnostic(Severity severity, int line, int column, String message) {
        List<Diagnostic> next = new ArrayList<>(diagnostics);
        next.add(new Diagnostic(severity, line, column, message));
        return diagnostics(next);
    }

    public CodeEditor clearDiagnostics() {
        return diagnostics(List.of());
    }

    public List<CodeDiagnostic> validateCode() {
        List<CodeDiagnostic> result = safeValidate();
        codeDiagnostics(result);
        return result;
    }

    public List<CompletionItem> completionItems() {
        if (completionProvider == CompletionProvider.NONE) return List.of();
        try {
            List<CompletionItem> items = completionProvider.complete(new CompletionContext(
                    text(), languageId, cursorIndex(), selectionStart(), selectionEnd()));
            if (items == null || items.isEmpty()) return List.of();
            List<CompletionItem> normalized = new ArrayList<>(items.size());
            for (CompletionItem item : items) {
                if (item != null && !item.label().isEmpty()) normalized.add(item);
            }
            return List.copyOf(normalized);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public CodeEditor acceptCompletion(CompletionItem item) {
        if (item == null || item.insertText().isEmpty()) return this;
        if (item.hasReplacementRange()) {
            int start = TextEditorModel.clampToCodePointBoundary(text(), item.replaceStart());
            int end = TextEditorModel.clampToCodePointBoundary(text(), item.replaceEnd());
            editorModel().select(Math.min(start, end), Math.max(start, end));
        }
        insertText(item.insertText());
        hideCompletionPopup();
        return this;
    }

    public CodeEditor convertIndentation(TabStorageMode mode) {
        TabStorageMode normalized = mode == null ? tabStorageMode : mode;
        if (normalized == TabStorageMode.PRESERVE) return this;
        String converted = switch (normalized) {
            case PRESERVE -> text();
            case CONVERT_TABS_TO_SPACES -> text().replace("\t", spaces(tabSize));
            case CONVERT_SPACES_TO_TABS -> convertLeadingSpacesToTabs(text());
        };
        if (!Objects.equals(converted, text())) {
            text(converted);
            requestCursorFollow();
        }
        return this;
    }

    public MutableColor lineNumberColor() {
        return lineNumberColor;
    }

    public MutableColor gutterColor() {
        return gutterColor;
    }

    public MutableColor diagnosticRowColor() {
        return diagnosticRowColor;
    }

    public MutableColor errorColor() {
        return errorColor;
    }

    public MutableColor warningColor() {
        return warningColor;
    }

    public MutableColor infoColor() {
        return infoColor;
    }

    @Override
    protected float leftTextPadding() {
        return super.leftTextPadding() + (lineNumbersVisible ? gutterWidth() : 0.0f);
    }

    @Override
    public CodeEditor scrollTo(float x, float y) {
        super.scrollTo(x, y);
        updateCompletionAnchorIfOpen();
        return this;
    }

    @Override
    protected void moveCursor(int cursorIndex, boolean extendSelection) {
        super.moveCursor(cursorIndex, extendSelection);
        updateCompletionAnchorIfOpen();
    }

    @Override
    protected void moveCursor(int cursorIndex, boolean extendSelection, int selectionAnchor) {
        super.moveCursor(cursorIndex, extendSelection, selectionAnchor);
        updateCompletionAnchorIfOpen();
    }

    @Override
    protected void moveCursor(int cursorIndex, boolean extendSelection, boolean keepDesiredCursorX) {
        super.moveCursor(cursorIndex, extendSelection, keepDesiredCursorX);
        updateCompletionAnchorIfOpen();
    }

    @Override
    protected boolean handleKey(int keyCode, int modifiers) {
        if (KeyModifiers.has(modifiers, KeyModifiers.CONTROL) && keyCode == KeyCodes.SPACE) {
            showCompletionPopup();
            return true;
        }
        if (completionPopupOpened()) {
            if (keyCode == KeyCodes.DOWN) {
                selectedCompletionIndex(selectedCompletionIndex + 1);
                return true;
            }
            if (keyCode == KeyCodes.UP) {
                selectedCompletionIndex(selectedCompletionIndex - 1);
                return true;
            }
            if (keyCode == KeyCodes.ENTER || keyCode == KeyCodes.KEYPAD_ENTER || keyCode == KeyCodes.TAB) {
                acceptSelectedCompletion();
                return true;
            }
            if (keyCode == KeyCodes.ESCAPE) {
                hideCompletionPopup();
                return true;
            }
        }
        return super.handleKey(keyCode, modifiers);
    }

    @Override
    protected void insertText(String text) {
        boolean wasOpen = completionPopupOpened();
        super.insertText(text);
        if (wasOpen) {
            hideCompletionPopup();
        }
    }

    @Override
    protected void handleTabKey(int modifiers) {
        if (!canMutateText()) return;
        if (KeyModifiers.has(modifiers, KeyModifiers.SHIFT)) {
            outdentCurrentLine();
            return;
        }
        insertText(tabInputMode == TabInputMode.INSERT_SPACES ? spaces(tabSize) : "\t");
        convertIndentation(tabStorageMode);
    }

    @Override
    protected void renderContent(RenderContext context) {
        updateCompletionAnchorIfOpen();
        renderCodeDecorations(context);
        super.renderContent(context);
        renderSyntaxTokens(context);
    }

    protected void renderCodeDecorations(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        float viewportY = textViewportY();
        float viewportHeight = textViewportHeight();
        draw.pushClip(layoutBounds().x(), viewportY, layoutBounds().width(), viewportHeight);
        try {
            renderDiagnosticRows(draw);
            if (lineNumbersVisible) renderLineNumbers(draw);
            renderDiagnosticUnderlines(draw);
        } finally {
            draw.popClip();
        }
    }

    protected ColorView diagnosticColor(Diagnostic diagnostic) {
        return switch (diagnostic.severity()) {
            case ERROR -> errorColor;
            case WARNING -> warningColor;
            case INFO -> infoColor;
        };
    }

    private void renderDiagnosticRows(DrawScope draw) {
        for (Diagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasLocation()) continue;
            float y = lineY(diagnostic.line());
            if (!lineVisible(y)) continue;
            draw.rect(layoutBounds().x(), y, layoutBounds().width(), effectiveLineHeight(), Paint.fill(diagnosticRowColor));
        }
    }

    private void renderDiagnosticUnderlines(DrawScope draw) {
        for (Diagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasLocation()) continue;
            float y = lineY(diagnostic.line());
            if (!lineVisible(y)) continue;
            int startIndex = lineColumnToIndex(diagnostic.line(), diagnostic.column());
            int endIndex = diagnostic.hasRange()
                    ? lineColumnToIndex(diagnostic.endLine(), diagnostic.endColumn())
                    : lineColumnToIndex(diagnostic.line(), diagnostic.column() + 1);
            float x = textIndexX(startIndex);
            float width = Math.max(DIAGNOSTIC_UNDERLINE_MIN_WIDTH, textRangeWidth(startIndex, endIndex));
            draw.rect(x,
                    y + Math.max(1.0f, effectiveLineHeight() - 2.0f),
                    width,
                    1.0f,
                    Paint.fill(diagnosticColor(diagnostic)));
        }
    }

    private void renderLineNumbers(DrawScope draw) {
        float lineHeight = effectiveLineHeight();
        int firstLine = Math.max(1, (int) Math.floor(scrollY() / Math.max(1.0f, lineHeight)) + 1);
        int lastLine = Math.min(lineCount(), (int) Math.ceil((scrollY() + textViewportHeight()) / Math.max(1.0f, lineHeight)) + 1);
        float gutterRight = layoutBounds().x() + leftTextPadding() - GUTTER_PADDING;
        draw.rect(layoutBounds().x(), textViewportY(), gutterWidth(), textViewportHeight(), Paint.fill(gutterColor));
        for (int line = firstLine; line <= lastLine; line++) {
            String number = Integer.toString(line);
            float textWidth = TextEngine.measureLineWidth(number);
            float x = gutterRight - textWidth;
            draw.text(RichText.plain(number), x, lineY(line), textWidth, lineHeight, Paint.fill(lineNumberColor));
        }
    }

    private void renderSyntaxTokens(RenderContext context) {
        List<CodeToken> tokens = syntaxTokens();
        if (tokens.isEmpty() || isShowingPlaceholder()) return;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        draw.pushTextClip(textViewportX(), textViewportY(), textViewportWidth(), textViewportHeight());
        try {
            for (CodeToken token : tokens) {
                renderSyntaxToken(draw, token);
            }
        } finally {
            draw.popClip();
        }
    }

    private void renderSyntaxToken(DrawScope draw, CodeToken token) {
        if (token == null || token.isEmpty() || !token.style().hasColor()) return;
        String value = text();
        int index = TextEditorModel.clampToCodePointBoundary(value, token.startIndex());
        int end = TextEditorModel.clampToCodePointBoundary(value, token.endIndex());
        while (index < end && index < value.length()) {
            if (value.charAt(index) == '\n') {
                index++;
                continue;
            }
            int lineEnd = value.indexOf('\n', index);
            if (lineEnd < 0) lineEnd = value.length();
            int segmentEnd = Math.min(end, lineEnd);
            if (segmentEnd > index) {
                String fragment = value.substring(index, segmentEnd);
                float x = textIndexX(index);
                float y = textIndexY(index);
                float width = Math.max(1.0f, textRangeWidth(index, segmentEnd));
                draw.text(RichText.of(fragment, fontFace(), fontSize()),
                        x,
                        y,
                        width,
                        Math.max(1.0f, effectiveLineHeight() - 2.0f),
                        Paint.fill(token.style().color()),
                        false);
            }
            index = segmentEnd;
            if (index < end && index < value.length() && value.charAt(index) == '\n') index++;
        }
    }

    private List<CodeToken> syntaxTokens() {
        if (tokenizer == CodeTokenizer.NONE) return List.of();
        String value = text();
        if (cachedTokenizer == tokenizer
                && Objects.equals(cachedTokenText, value)
                && Objects.equals(cachedTokenLanguageId, languageId)) {
            return cachedTokens;
        }
        cachedTokenizer = tokenizer;
        cachedTokenText = value;
        cachedTokenLanguageId = languageId;
        cachedTokens = safeTokenize(value);
        return cachedTokens;
    }

    private List<CodeToken> safeTokenize(String value) {
        try {
            List<CodeToken> tokens = tokenizer.tokenize(new CodeTokenizationContext(value, languageId));
            return normalizeTokens(value, tokens);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<CodeDiagnostic> safeValidate() {
        if (validator == CodeValidator.NONE) return List.of();
        try {
            List<CodeDiagnostic> diagnostics = validator.validate(new CodeValidationContext(text(), languageId));
            if (diagnostics == null || diagnostics.isEmpty()) return List.of();
            List<CodeDiagnostic> normalized = new ArrayList<>(diagnostics.size());
            for (CodeDiagnostic diagnostic : diagnostics) {
                if (diagnostic != null) normalized.add(diagnostic);
            }
            return List.copyOf(normalized);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private void outdentCurrentLine() {
        String value = text();
        int cursor = Math.max(0, Math.min(cursorIndex(), value.length()));
        int lineStart = value.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1;
        int removeEnd = lineStart;
        if (removeEnd < value.length() && value.charAt(removeEnd) == '\t') {
            removeEnd++;
        } else {
            int spaces = 0;
            while (removeEnd < value.length() && spaces < tabSize && value.charAt(removeEnd) == ' ') {
                removeEnd++;
                spaces++;
            }
        }
        if (removeEnd > lineStart) {
            editorModel().select(lineStart, removeEnd);
            insertText("");
        }
    }

    private String convertLeadingSpacesToTabs(String value) {
        if (value == null || value.isEmpty()) return "";
        String[] lines = value.split("\n", -1);
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) builder.append('\n');
            String line = lines[i];
            int spaces = 0;
            while (spaces < line.length() && line.charAt(spaces) == ' ') spaces++;
            int tabs = spaces / tabSize;
            int remainder = spaces % tabSize;
            builder.append("\t".repeat(tabs));
            builder.append(" ".repeat(remainder));
            builder.append(line.substring(spaces));
        }
        return builder.toString();
    }

    private String spaces(int count) {
        return " ".repeat(Math.max(0, count));
    }

    private float lineY(int line) {
        return textViewportY() + (Math.max(1, line) - 1) * effectiveLineHeight() - scrollY();
    }

    private boolean lineVisible(float y) {
        return y + effectiveLineHeight() >= textViewportY()
                && y <= textViewportY() + textViewportHeight();
    }

    private float gutterWidth() {
        int digits = Math.max(2, Integer.toString(lineCount()).length());
        return Math.max(GUTTER_MIN_WIDTH, digits * TextEngine.measureLineWidth("0") + GUTTER_PADDING * 2.0f);
    }

    private void updateDirty(String currentText) {
        dirty(!currentText.equals(cleanText));
    }

    private void rebuildCompletionList() {
        completionList.clearChildren();
        float width = completionPopupWidth();
        completionList.layout(style -> style
                .size(width, activeCompletionItems.size() * COMPLETION_ROW_HEIGHT)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        for (int i = 0; i < activeCompletionItems.size(); i++) {
            CompletionItem item = activeCompletionItems.get(i);
            Button button = new Button(completionLabel(item));
            button.textPadding(5.0f, 2.0f);
            button.backgroundVisible(true);
            button.borderVisible(false);
            if (i == selectedCompletionIndex) {
                button.background().set(0.11f, 0.24f, 0.34f, 0.96f);
            } else {
                button.background().set(0.035f, 0.040f, 0.052f, 0.94f);
            }
            button.layout(style -> style
                    .size(width, COMPLETION_ROW_HEIGHT)
                    .flexGrow(0.0f)
                    .flexShrink(0.0f));
            button.onClick(event -> acceptCompletion(item));
            completionList.addChild(button);
        }
        completionList.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private String completionLabel(CompletionItem item) {
        if (item.detail().isEmpty()) return item.label();
        return item.label() + "  " + item.detail();
    }

    private float completionPopupWidth() {
        float baseWidth = Math.max(COMPLETION_POPUP_MIN_WIDTH, textViewportWidth());
        return Math.min(COMPLETION_POPUP_MAX_WIDTH, baseWidth);
    }

    private void updateCompletionAnchorIfOpen() {
        if (completionPopupOpened()) {
            updateCompletionAnchor();
        }
    }

    private void updateCompletionAnchor() {
        updateLineMetrics(null);
        int cursor = cursorIndex();
        completionAnchor.update(textIndexX(cursor), textIndexY(cursor), 1.0f, effectiveLineHeight());
        completionPopup.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void syncCompletionAttachment() {
        if (activeCompletionItems.isEmpty()) {
            if (!completionPopup.opened()) {
                detachCompletionPopup();
            }
            return;
        }
        OverlayLayer targetOverlay = completionOverlayLayer();
        if (targetOverlay == null) {
            detachCompletionPopup();
            return;
        }
        if (attachedCompletionOverlayLayer != targetOverlay) {
            detachCompletionPopup();
            attachedCompletionOverlayLayer = targetOverlay;
            attachedCompletionOverlayLayer.addOverlay(completionPopup);
        }
    }

    private void detachCompletionPopup() {
        if (attachedCompletionOverlayLayer != null) {
            attachedCompletionOverlayLayer.removeOverlay(completionPopup);
            attachedCompletionOverlayLayer = null;
        }
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private void clearSyntaxCache() {
        cachedTokenizer = null;
        cachedTokenText = null;
        cachedTokenLanguageId = null;
        cachedTokens = List.of();
    }

    private static List<Diagnostic> normalizeDiagnostics(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) return List.of();
        List<Diagnostic> normalized = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic != null) normalized.add(diagnostic);
        }
        return List.copyOf(normalized);
    }

    private static List<CodeToken> normalizeTokens(String text, List<CodeToken> tokens) {
        if (tokens == null || tokens.isEmpty()) return List.of();
        List<CodeToken> normalized = new ArrayList<>(tokens.size());
        for (CodeToken token : tokens) {
            if (token == null || token.isEmpty()) continue;
            int start = TextEditorModel.clampToCodePointBoundary(text, token.startIndex());
            int end = TextEditorModel.clampToCodePointBoundary(text, token.endIndex());
            if (end > start) normalized.add(new CodeToken(start, end, token.style()));
        }
        return List.copyOf(normalized);
    }

    private static final class CompletionAnchor extends WidgetBase {
        private float desiredWidth = 1.0f;
        private float desiredHeight = TextRun.DEFAULT_PIXEL_SIZE;

        private void update(float x, float y, float width, float height) {
            desiredWidth = Math.max(1.0f, width);
            desiredHeight = Math.max(1.0f, height);
            mutableLayoutBounds().set(x, y, desiredWidth, desiredHeight);
            setDesiredSize(desiredWidth, desiredHeight);
        }

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(desiredWidth, desiredHeight);
        }

        @Override
        public void arrange(RectView bounds) {
            mutableLayoutBounds().set(bounds);
        }
    }

    public enum TabInputMode {
        INSERT_TAB,
        INSERT_SPACES
    }

    public enum TabStorageMode {
        PRESERVE,
        CONVERT_TABS_TO_SPACES,
        CONVERT_SPACES_TO_TABS
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public record Diagnostic(Severity severity,
                             int line,
                             int column,
                             int endLine,
                             int endColumn,
                             String message) {
        public Diagnostic(Severity severity, int line, int column, String message) {
            this(severity, line, column, line, column + 1, message);
        }

        public Diagnostic {
            severity = severity == null ? Severity.ERROR : severity;
            endLine = Math.max(line, endLine);
            endColumn = endLine == line ? Math.max(column, endColumn) : Math.max(1, endColumn);
            message = message == null ? "" : message.trim();
        }

        public boolean hasLocation() {
            return line >= 1 && column >= 1;
        }

        public boolean hasRange() {
            return hasLocation() && endLine >= line && (endLine > line || endColumn > column);
        }

        public static Diagnostic from(CodeDiagnostic diagnostic) {
            if (diagnostic == null) return new Diagnostic(Severity.ERROR, 1, 1, "");
            return new Diagnostic(
                    diagnostic.severity(),
                    diagnostic.line(),
                    diagnostic.column(),
                    diagnostic.endLine(),
                    diagnostic.endColumn(),
                    diagnostic.message());
        }
    }
}
