# Text, RichText and Code Editor SPEC

## Context

UniGUI has enough editor infrastructure to start building serious authoring tools, but the current text stack is still uneven:

- `TextWidget` can render `RichText` runs with per-run pixel size, but plain widget/XML usage has no explicit `fontSize` property.
- `TextArea` owns most editing mechanics and already uses `TextEditorModel`, but its line metrics and hit testing are still too close to a simple multiline field.
- `CodeEditor` is currently a thin `TextArea` subclass with line numbers and simple diagnostics. It is not yet a code editor surface.
- Rich text can style text runs, but it cannot embed inline icons or custom draw commands in a string-like flow.

This SPEC defines the target contracts before implementation so the upgrade is shared by normal widgets, XML editor panes and future mod-specific editors.

## Goals

- Add first-class font size support to `TextWidget` and keep it aligned with `TextArea` / `CodeEditor` metrics.
- Fix pointer hit testing so mouse selection maps to the intended text position, including variable advances, tabs and Unicode code points.
- Decouple manual scrolling from caret-follow behavior.
- Add visible vertical and horizontal scrollbars to code/text editor surfaces.
- Make tab behavior configurable through explicit policy instead of hardcoded insertion.
- Turn `CodeEditor` into a pluggable editor surface with tokenizer, validator and completion extension points.
- Add read-only as a flag on the same editor widget.
- Keep cursor, selection, backspace and delete code-point aware, matching the discipline already started in `TextEditorModel`.
- Introduce an inline content mechanism for icons and custom draw commands that works across widgets consuming `String` or `RichText`.

## Non-Goals For First Slice

- Do not build a full IDE language server.
- Do not implement semantic analysis in UniGUI core.
- Do not require every existing widget to support editing rich inline content.
- Do not make inline draw spans part of plain string storage.
- Do not introduce Minecraft-only dependencies into `api.text` or `widgets.interaction`.
- Do not replace `TextEditorModel`; extend and harden it.
- Do not make autocomplete mandatory for every `CodeEditor` instance.

## Existing State

### TextWidget

`TextWidget` currently stores plain `text` plus optional `RichText` and exposes:

```java
public TextWidget font(FontFace font, float pixelSize)
```

This is useful for Java code, but missing pieces are:

- no `fontSize()` getter;
- no `fontSize(float)` setter for the common case;
- no XML `fontSize` attribute;
- no single source of truth for plain text font size when `text(String)` recreates `RichText.plain(...)`.

### TextArea

`TextArea` already has:

```java
private FontFace font;
private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
private float explicitLineHeight;
private final TextEditorModel editor = new TextEditorModel();
```

Known gaps:

- `updateLineMetrics(...)` stores `prefixWidths` by UTF-16 local char index.
- hit testing compares mouse X to those char-index prefix widths.
- tabs are inserted as literal `\t` with no policy.
- `ensureCursorVisible()` is called during render, so manual scroll can be snapped back to the caret.
- scrollbars are logical state only, not explicit visual controls.

### CodeEditor

`CodeEditor` currently adds line numbers, dirty state and diagnostics over `TextArea`.

Known gaps:

- diagnostics use approximate column math instead of measured text positions;
- no syntax token model;
- no validator interface;
- no completion interface;
- no read-only flag;
- no horizontal scrollbar UI;
- no independent editor viewport behavior for large files.

### RichText

`RichText` is immutable and made of `TextRun` values. A `TextRun` contains text, font, pixel size, color, tracking and transform.

Known gap:

- the model can style text but cannot represent inline non-text content such as an item icon, status glyph, or custom draw command.

## Text Size Contract

### Public API

`TextWidget`, `TextArea` and `CodeEditor` should expose the same basic size contract:

```java
float fontSize();
WidgetType fontSize(float pixelSize);
WidgetType font(FontFace font, float pixelSize);
```

Rules:

- `fontSize` is measured in UI pixels.
- Invalid values normalize to `TextRun.DEFAULT_PIXEL_SIZE` or the previous valid value.
- Minimum value is `1.0f`.
- Changing `fontSize` invalidates layout and visual state.
- Plain `text(String)` must preserve the current default font and `fontSize`.
- `richText(RichText)` may contain mixed sizes; `fontSize()` still reports the widget default for future plain text runs.

### XML Attributes

Add editor-facing XML attributes where relevant:

```xml
<TextWidget text="Title" fontSize="18" />
<TextArea text="Notes" fontSize="12" lineHeight="0" />
<CodeEditor text="..." fontSize="11" tabSize="4" />
```

Rules:

- `fontSize` belongs to `Appearance`.
- `lineHeight=0` continues to mean use font metrics.
- `CodeEditor` inherits the text size behavior from `TextArea`.

## Text Layout And Hit Testing

The text stack needs a shared line layout result instead of every widget guessing character positions.

### Line Metrics Model

Introduce an internal layout representation for editable text:

```java
record TextLineLayout(
        int lineIndex,
        int startOffset,
        int endOffset,
        RichText content,
        float width,
        List<TextPosition> positions) {}

record TextPosition(
        int utf16Offset,
        int codePoint,
        float leadingX,
        float trailingX) {}
```

Rules:

- Offsets are still UTF-16 string offsets because Java strings and existing model APIs use them.
- Every stored offset must be a valid code point boundary.
- Hit testing returns the nearest valid boundary, never the middle of a surrogate pair.
- Widths come from `TextEngine` / backend font metrics when available, not `APPROX_CHAR_WIDTH` except as fallback.
- Tabs are expanded in layout according to tab policy; they do not have to mutate the source text.
- Placeholder layout is visual only and must not affect source offsets.

### Mouse Hit Testing

Pointer selection should use this flow:

```text
screen point
  -> widget local point
  -> text viewport point
  -> add scroll offsets
  -> resolve line by Y
  -> resolve nearest TextPosition by measured X
  -> move/select using valid source offset
```

Acceptance rule:

- Clicking around visual character 33 must not produce source position 27 unless measured glyph advances genuinely place that point nearer to offset 27.

## Unicode Editing Discipline

UniGUI should remain code-point aware, not Java-char aware, for editor mutation and navigation.

Rules:

- Cursor and selection offsets are stored as UTF-16 offsets but always clamped/snapped to code point boundaries.
- Backspace deletes the previous code point.
- Delete deletes the next code point.
- Selection created by mouse, keyboard or API cannot split a surrogate pair.
- `maxLength` should be clarified as code points, not UTF-16 chars, for new editor behavior.
- Tests must include BMP text, surrogate pairs, mixed ASCII plus emoji, and multiline selections.

First implementation can stop at code point awareness. Grapheme cluster support is a later enhancement if combining marks become important.

## Scroll Model

Manual scroll and cursor-follow scroll must be separate.

### Scroll State

`TextArea` / `CodeEditor` should keep:

```java
float scrollX();
float scrollY();
float maxScrollX();
float maxScrollY();
boolean followCaretOnEdit();
```

Rules:

- Wheel, scrollbar drag and API `scrollTo` are manual viewport changes.
- Manual scroll must not be undone during render.
- `ensureCursorVisible()` runs only after explicit caret movement, typing, deletion, paste, selection commands, or an explicit API call.
- Render must be side-effect-light and must not change scroll offsets.
- Programmatic `scrollToLine` and `scrollToOffset` are allowed to move the viewport without moving the caret.

### Scrollbars

`CodeEditor` must render clear scrollbars on the right and bottom when content overflows. `TextArea` can share the same implementation behind a flag.

Required behavior:

- vertical scrollbar reflects `scrollY / maxScrollY`;
- horizontal scrollbar reflects `scrollX / maxScrollX`;
- thumb size reflects viewport/content ratio with a minimum visible size;
- mouse wheel scrolls vertically;
- shift-wheel or explicit horizontal wheel path scrolls horizontally;
- dragging a thumb changes only scroll state;
- scrollbars do not steal text selection unless the pointer press starts inside a scrollbar hit area.

## Tab Policy

Tabs must be configurable and explicit.

### API

```java
enum TabInputMode {
    INSERT_TAB,
    INSERT_SPACES
}

enum TabStorageMode {
    PRESERVE,
    CONVERT_TABS_TO_SPACES,
    CONVERT_SPACES_TO_TABS
}

record TabPolicy(int tabSize, TabInputMode inputMode, TabStorageMode storageMode) {}
```

Widget-facing API:

```java
int tabSize();
CodeEditor tabSize(int spaces);
TabInputMode tabInputMode();
CodeEditor tabInputMode(TabInputMode mode);
TabStorageMode tabStorageMode();
CodeEditor tabStorageMode(TabStorageMode mode);
CodeEditor convertIndentation(TabStorageMode mode);
```

Rules:

- `tabSize` controls layout expansion and indentation commands.
- Pressing Tab follows `TabInputMode`.
- Existing text is not rewritten just because `tabSize` changes.
- `TabStorageMode` conversion is an explicit command or load/save policy, not an implicit render side effect.
- Indent/outdent multi-line selections should be added with the same policy.

## Code Editor Extension Points

`CodeEditor` should remain a UI widget, not a language-specific parser. Language behavior is supplied through pluggable interfaces.

### Tokenizer

```java
public interface CodeTokenizer {
    CodeTokenizer NONE = text -> List.of();
    List<CodeToken> tokenize(String text);
}

public record CodeToken(
        int startOffset,
        int endOffset,
        String type,
        TokenStyle style) {}

public record TokenStyle(
        ColorView color,
        FontFace font,
        float fontSize,
        boolean bold,
        boolean italic) {}
```

Rules:

- Token offsets are UTF-16 offsets snapped to code point boundaries.
- Tokenizer failures must not break text rendering; surface them as diagnostics if possible.
- Token rendering should produce `RichText`/inline layout spans per visible line.
- Untokenized text uses the editor default style.
- Tokenization should be cacheable by text revision and invalidated on edit.

### Validator

```java
public interface CodeValidator {
    CodeValidator NONE = text -> List.of();
    List<CodeDiagnostic> validate(String text);
}

public record CodeDiagnostic(
        Severity severity,
        int startOffset,
        int endOffset,
        int line,
        int column,
        String message,
        String source) {}
```

Rules:

- Offset range is preferred for rendering; line/column exists for external parsers and messages.
- Diagnostics render using measured positions, not approximate columns.
- Validation can be synchronous for MVP; async/debounced validation can be added later.
- Existing `CodeEditor.Diagnostic` should migrate or adapt to `CodeDiagnostic` without breaking call sites immediately.

### Completion Provider

```java
public interface CompletionProvider {
    CompletionProvider NONE = context -> List.of();
    List<CompletionItem> complete(CompletionContext context);
}

public record CompletionContext(
        String text,
        int cursorOffset,
        int selectionStart,
        int selectionEnd,
        String prefix,
        String languageId) {}

public record CompletionItem(
        String label,
        String insertText,
        String detail,
        String documentation,
        int priority) {}
```

Rules:

- Provider is optional and pluggable.
- Completion popup is anchored to measured caret bounds.
- Accepting a completion replaces the current prefix or selection through `TextEditorModel`.
- Completion must respect read-only mode.
- Autocomplete UI is allowed to be a later phase as long as the provider contract is ready.

## Read-Only Mode

Read-only is a flag on `TextArea` / `CodeEditor`, not a separate widget.

API:

```java
boolean readOnly();
TextArea readOnly(boolean readOnly);
```

Rules:

- Cursor movement, selection, copy and scrolling are allowed.
- Typing, paste, cut, delete, backspace, tab indentation and completion insertion are blocked.
- Programmatic `text(String)` still works; read-only controls user interaction, not owner updates.
- Visual state should make read-only clear only if the active skin chooses to style it.

## Inline Rich Content

All widgets that consume `String` or `RichText` should eventually be able to render inline icons or custom draw spans through the same text layout pipeline.

### Model

Extend the rich text model from text-only runs to inline spans:

```java
sealed interface RichTextSpan permits TextSpan, InlineContentSpan {}

record TextSpan(TextRun run) implements RichTextSpan {}

record InlineContentSpan(
        String id,
        String fallbackText,
        float width,
        float height,
        BaselineAlignment baselineAlignment,
        InlineContentRenderer renderer) implements RichTextSpan {}

@FunctionalInterface
interface InlineContentRenderer {
    void render(DrawScope draw, InlineContentContext context);
}
```

Alternative implementation can keep `TextRun` and add a sibling `InlineSpan` if sealed interfaces are too invasive for the target Java level.

Rules:

- Inline spans are immutable layout content, not child widgets.
- Each inline span provides a measured width and height.
- Each inline span provides fallback plain text for search, clipboard, accessibility/debug, XML serialization or non-rich renderers.
- Inline draw renderers receive final bounds after text layout has placed the span.
- Inline spans can render textures, icons or arbitrary `DrawCommand` sequences.
- Layout, wrapping and hit testing treat an inline span as one atomic code-point-like unit.
- Editing a text field with inline spans is out of scope for the first slice; display widgets come first.

### String Marker Resolver

For widgets that only receive a `String`, add an optional resolver layer:

```java
public interface InlineContentResolver {
    RichText resolve(String text);
}
```

Example future syntax, owned by the resolver and not by core text parsing:

```text
"Cost: {item:minecraft:diamond} x3"
"Status: {icon:warning} Invalid XML"
```

Rules:

- Core widgets should not hardcode item/icon token syntax.
- Mods can register resolvers for their own marker formats.
- Plain strings without a resolver behave exactly as today.
- XML attributes remain strings; rich parsing happens through widget or screen options.

## Rendering Integration

The target text pipeline should be:

```text
String / RichText
  -> optional InlineContentResolver
  -> RichText spans
  -> TextLayout lines and positions
  -> DrawCommand text/icon/custom spans
  -> RenderBackend
```

Affected widgets include at minimum:

- `TextWidget`
- `TextArea`
- `CodeEditor`
- `Button` label rendering
- menu/dropdown labels
- table/list labels
- tooltip text

Rules:

- Widgets should not manually multiply character count by `APPROX_CHAR_WIDTH` unless explicitly using a fallback path.
- `DrawScope.text(...)` can remain as the backend entry point for plain text, but rich inline content needs a layout renderer that can emit multiple draw calls.
- Existing skins should continue to work with plain `RichText`.

## XML And Editor Metadata

Add XML-facing attributes only when they are stable runtime properties.

Recommended stable attributes:

- `fontSize`
- `lineHeight`
- `readOnly`
- `tabSize`
- `tabInputMode`
- `tabStorageMode`
- `lineNumbers`
- `languageId`

Not recommended as direct XML attributes for MVP:

- Java tokenizer instance;
- Java validator instance;
- Java completion provider instance;
- arbitrary inline renderer lambdas.

Those should be registered through runtime/editor options and referenced by stable ids when XML support is needed later.

## Implementation Phases

### Phase 1 - Text Size And Safe Text Metrics

- Add `fontSize` API/XML support to `TextWidget`.
- Preserve default font and size across `text(String)` calls.
- Add matching `fontSize()` / `fontSize(float)` APIs to `TextArea` if missing.
- Add tests for measurement invalidation and XML descriptor visibility.

### Phase 2 - TextArea Layout And Cursor Correctness

- Replace char-index `prefixWidths` with code-point boundary positions.
- Add shared text line layout helper used by hit testing, caret drawing and diagnostics.
- Fix mouse selection offset for measured text.
- Ensure selection and cursor offsets never split surrogate pairs.
- Add tests for ASCII, tabs, mixed-width fallback and surrogate pairs.

### Phase 3 - Independent Scroll And Scrollbars

- Stop calling `ensureCursorVisible()` from render.
- Trigger cursor-follow only from edit/navigation commands.
- Render vertical and horizontal scrollbars with hit testing and dragging.
- Add tests for scroll persistence after render and scrollbar value mapping.

### Phase 4 - CodeEditor Policies And Read-Only

- Add `readOnly` flag.
- Add `TabPolicy` APIs and XML attributes.
- Implement Tab / Shift+Tab according to policy.
- Add tests for blocked edits in read-only mode and explicit indentation conversion.

### Phase 5 - Tokenizer And Validator

- Add `CodeTokenizer`, `CodeToken`, `TokenStyle`.
- Add `CodeValidator`, `CodeDiagnostic`.
- Render highlighted tokens through measured line layout.
- Render diagnostics using measured ranges.
- Add simple XML tokenizer/validator adapter for the XML editor pane later.

### Phase 6 - Completion Provider

- Add `CompletionProvider`, `CompletionContext`, `CompletionItem`.
- Add popup anchoring to caret bounds.
- Accept completion by replacing prefix/selection.
- Keep provider optional and no-op by default.

### Phase 7 - Inline Rich Content

- Extend `RichText` with inline content spans or an equivalent compatible model.
- Add layout support for atomic inline spans.
- Add renderer bridge for texture/icon/custom draw command spans.
- Roll support into display widgets first, then editable surfaces where appropriate.

## Test Plan

Required self-tests should cover:

- `TextWidget.fontSize` changes desired size and survives `text(String)` updates.
- XML descriptors expose `fontSize` for `TextWidget`, `TextArea` and `CodeEditor`.
- Mouse hit testing selects expected offsets for long ASCII lines.
- Mouse hit testing handles surrogate pairs without invalid offsets.
- Tab layout uses configured width without mutating text.
- Tab input inserts tabs or spaces according to policy.
- Manual scroll remains stable across render calls.
- Vertical and horizontal scrollbar thumbs map to scroll offsets.
- Read-only blocks user mutations but allows selection and scrolling.
- Diagnostics render from measured offset ranges.
- Tokenizer failure does not crash rendering.
- Inline spans contribute width, render at measured bounds and expose fallback text.

## Definition Of Done

The widget upgrade is ready for the XML editor when:

- `TextWidget` has editable font size from Java and XML.
- `CodeEditor` can handle large multiline documents without caret-bound scroll snapping.
- Pointer selection maps to measured text positions.
- Scrollbars are visible and interactive when content overflows.
- Tabs, read-only mode, tokenizer, validator and completion provider have stable public contracts.
- Unicode cursor/selection/delete behavior is covered by tests.
- Inline icon/custom draw support has a shared `RichText` path instead of widget-specific hacks.