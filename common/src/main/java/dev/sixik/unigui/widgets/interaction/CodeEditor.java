package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lightweight code editor wrapper over {@link TextArea}.
 */
@XmlWidgetName("CodeEditor")
public class CodeEditor extends TextArea {
    private static final float GUTTER_PADDING = 6.0f;
    private static final float GUTTER_MIN_WIDTH = 28.0f;
    private static final float DIAGNOSTIC_UNDERLINE_WIDTH = 28.0f;

    private final MutableColor lineNumberColor = new MutableColor(0.50f, 0.56f, 0.66f, 0.95f);
    private final MutableColor gutterColor = new MutableColor(0.02f, 0.025f, 0.035f, 0.70f);
    private final MutableColor diagnosticRowColor = new MutableColor(0.95f, 0.18f, 0.18f, 0.16f);
    private final MutableColor errorColor = new MutableColor(1.0f, 0.25f, 0.25f, 0.92f);
    private final MutableColor warningColor = new MutableColor(1.0f, 0.72f, 0.20f, 0.90f);
    private final MutableColor infoColor = new MutableColor(0.32f, 0.68f, 1.0f, 0.85f);
    private List<Diagnostic> diagnostics = List.of();
    private String cleanText = "";
    private boolean dirty;
    private boolean lineNumbersVisible = true;

    public CodeEditor() {
        placeholder("Code...");
        onTextChanged(event -> updateDirty(event.newText()));
        lineNumberColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        gutterColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        diagnosticRowColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        errorColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        warningColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        infoColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public CodeEditor(String text) {
        super(text);
        placeholder("Code...");
        cleanText = text();
        onTextChanged(event -> updateDirty(event.newText()));
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

    public CodeEditor diagnostic(Severity severity, int line, int column, String message) {
        List<Diagnostic> next = new ArrayList<>(diagnostics);
        next.add(new Diagnostic(severity, line, column, message));
        return diagnostics(next);
    }

    public CodeEditor clearDiagnostics() {
        return diagnostics(List.of());
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
    protected void renderContent(RenderContext context) {
        renderCodeDecorations(context);
        super.renderContent(context);
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
            float x = textViewportX() + Math.max(0, diagnostic.column() - 1) * APPROX_CHAR_WIDTH - scrollX();
            draw.rect(x,
                    y + Math.max(1.0f, effectiveLineHeight() - 2.0f),
                    DIAGNOSTIC_UNDERLINE_WIDTH,
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

    private float lineY(int line) {
        return textViewportY() + (Math.max(1, line) - 1) * effectiveLineHeight() - scrollY();
    }

    private boolean lineVisible(float y) {
        return y + effectiveLineHeight() >= textViewportY()
                && y <= textViewportY() + textViewportHeight();
    }

    private float gutterWidth() {
        int digits = Math.max(2, Integer.toString(lineCount()).length());
        return Math.max(GUTTER_MIN_WIDTH, digits * APPROX_CHAR_WIDTH + GUTTER_PADDING * 2.0f);
    }

    private void updateDirty(String currentText) {
        dirty(!currentText.equals(cleanText));
    }

    private static List<Diagnostic> normalizeDiagnostics(List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) return List.of();
        List<Diagnostic> normalized = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic != null) normalized.add(diagnostic);
        }
        return List.copyOf(normalized);
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public record Diagnostic(Severity severity, int line, int column, String message) {
        public Diagnostic {
            severity = severity == null ? Severity.ERROR : severity;
            message = message == null ? "" : message.trim();
        }

        public boolean hasLocation() {
            return line >= 1 && column >= 1;
        }
    }
}
