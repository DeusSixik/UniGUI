package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.math.MutableColor;

import java.util.ArrayList;
import java.util.List;

/** Built-in tokenizer presets for {@link CodeEditor}. */
public final class CodeLanguagePresets {
    private static final CodeTokenizer XAML_TOKENIZER = new XamlTokenizer();

    private static final TokenStyle XAML_TAG_PUNCTUATION = TokenStyle.color(MutableColor.rgba(0.56f, 0.66f, 0.78f, 1.0f));
    private static final TokenStyle XAML_ELEMENT_NAME = TokenStyle.color(MutableColor.rgba(0.42f, 0.78f, 1.0f, 1.0f));
    private static final TokenStyle XAML_ATTRIBUTE_NAME = TokenStyle.color(MutableColor.rgba(1.0f, 0.76f, 0.36f, 1.0f));
    private static final TokenStyle XAML_ATTRIBUTE_VALUE = TokenStyle.color(MutableColor.rgba(0.56f, 0.86f, 0.52f, 1.0f));
    private static final TokenStyle XAML_COMMENT = TokenStyle.color(MutableColor.rgba(0.46f, 0.52f, 0.60f, 1.0f));
    private static final TokenStyle XAML_ENTITY = TokenStyle.color(MutableColor.rgba(0.86f, 0.58f, 1.0f, 1.0f));
    private static final TokenStyle XAML_EQUALS = TokenStyle.color(MutableColor.rgba(0.72f, 0.78f, 0.88f, 1.0f));

    private CodeLanguagePresets() {
    }

    public static void apply(CodeEditor editor, CodeLanguagePreset preset) {
        if (editor == null) return;
        CodeLanguagePreset normalized = preset == null ? CodeLanguagePreset.NONE : preset;
        switch (normalized) {
            case NONE -> editor.languageId("").tokenizer(CodeTokenizer.NONE);
            case XAML -> editor.languageId("xaml").tokenizer(XAML_TOKENIZER);
        }
    }

    public static CodeTokenizer xamlTokenizer() {
        return XAML_TOKENIZER;
    }

    private static final class XamlTokenizer implements CodeTokenizer {
        @Override
        public List<CodeToken> tokenize(CodeTokenizationContext context) {
            String text = context == null ? "" : context.text();
            if (text.isEmpty()) return List.of();
            List<CodeToken> tokens = new ArrayList<>();
            int index = 0;
            while (index < text.length()) {
                if (startsWith(text, index, "<!--")) {
                    index = tokenUntil(text, tokens, index, "-->", XAML_COMMENT);
                } else if (startsWith(text, index, "<![CDATA[")) {
                    index = tokenUntil(text, tokens, index, "]]>", XAML_COMMENT);
                } else if (startsWith(text, index, "<?")) {
                    add(tokens, index, index + 2, XAML_TAG_PUNCTUATION);
                    index = tokenizeTag(text, index + 2, tokens, true, true);
                } else if (startsWith(text, index, "</")) {
                    add(tokens, index, index + 2, XAML_TAG_PUNCTUATION);
                    index = tokenizeTag(text, index + 2, tokens, false, true);
                } else if (startsWith(text, index, "<!")) {
                    add(tokens, index, index + 2, XAML_TAG_PUNCTUATION);
                    index = tokenizeTag(text, index + 2, tokens, false, true);
                } else if (text.charAt(index) == '<') {
                    add(tokens, index, index + 1, XAML_TAG_PUNCTUATION);
                    index = tokenizeTag(text, index + 1, tokens, false, true);
                } else if (text.charAt(index) == '&') {
                    int entityEnd = entityEnd(text, index);
                    if (entityEnd > index) {
                        add(tokens, index, entityEnd, XAML_ENTITY);
                        index = entityEnd;
                    } else {
                        index++;
                    }
                } else {
                    index++;
                }
            }
            return tokens;
        }
    }

    private static int tokenizeTag(String text, int index, List<CodeToken> tokens, boolean processingInstruction, boolean firstName) {
        int i = index;
        boolean expectElementName = firstName;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (processingInstruction && startsWith(text, i, "?>")) {
                add(tokens, i, i + 2, XAML_TAG_PUNCTUATION);
                return i + 2;
            }
            if (!processingInstruction && startsWith(text, i, "/>")) {
                add(tokens, i, i + 2, XAML_TAG_PUNCTUATION);
                return i + 2;
            }
            if (c == '>') {
                add(tokens, i, i + 1, XAML_TAG_PUNCTUATION);
                return i + 1;
            }
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '=') {
                add(tokens, i, i + 1, XAML_EQUALS);
                i++;
                continue;
            }
            if (c == '\'' || c == '"') {
                i = quotedString(text, i, tokens);
                continue;
            }
            if (c == '&') {
                int entityEnd = entityEnd(text, i);
                if (entityEnd > i) {
                    add(tokens, i, entityEnd, XAML_ENTITY);
                    i = entityEnd;
                    continue;
                }
            }
            if (isNameChar(c)) {
                int start = i;
                i = readName(text, i);
                add(tokens, start, i, expectElementName ? XAML_ELEMENT_NAME : XAML_ATTRIBUTE_NAME);
                expectElementName = false;
                continue;
            }
            add(tokens, i, i + 1, XAML_TAG_PUNCTUATION);
            i++;
        }
        return i;
    }

    private static int tokenUntil(String text, List<CodeToken> tokens, int start, String terminator, TokenStyle style) {
        int end = text.indexOf(terminator, start + terminator.length());
        int tokenEnd = end < 0 ? text.length() : end + terminator.length();
        add(tokens, start, tokenEnd, style);
        return tokenEnd;
    }

    private static int quotedString(String text, int start, List<CodeToken> tokens) {
        char quote = text.charAt(start);
        int i = start + 1;
        while (i < text.length()) {
            if (text.charAt(i) == quote) {
                i++;
                break;
            }
            i += Character.charCount(text.codePointAt(i));
        }
        add(tokens, start, i, XAML_ATTRIBUTE_VALUE);
        return i;
    }

    private static int entityEnd(String text, int start) {
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ';') return i + 1;
            if (Character.isWhitespace(c) || c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') return -1;
            i++;
        }
        return -1;
    }

    private static int readName(String text, int start) {
        int i = start;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            if (!isNameCodePoint(codePoint)) break;
            i += Character.charCount(codePoint);
        }
        return i;
    }

    private static boolean isNameChar(char c) {
        return isNameCodePoint(c);
    }

    private static boolean isNameCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint)
                || codePoint == '_'
                || codePoint == '-'
                || codePoint == '.'
                || codePoint == ':';
    }

    private static boolean startsWith(String text, int index, String needle) {
        return index >= 0 && index + needle.length() <= text.length() && text.startsWith(needle, index);
    }

    private static void add(List<CodeToken> tokens, int start, int end, TokenStyle style) {
        if (end > start) tokens.add(new CodeToken(start, end, style));
    }
}
