package dev.sixik.unigui.api.xml.editor;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsModel;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdit;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdits;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentResult;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetHierarchy;
import dev.sixik.unigui.api.xml.XmlWidgetInspector;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetNode;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetSelectionModel;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Shared source-of-truth state owner for the XML UI editor MVP. */
public final class XmlEditorSession {
    private static final XmlWidgetSerializationOptions EDITOR_XML =
            XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false);

    private final XmlWidgetRegistry registry;
    private final XmlWidgetSelectionModel selection = new XmlWidgetSelectionModel();
    private final EnumMap<XmlEditorDiagnosticChannel, List<XmlWidgetDiagnostic>> diagnostics =
            new EnumMap<>(XmlEditorDiagnosticChannel.class);
    private final ArrayDeque<XmlWidgetDocumentEdit> undoStack = new ArrayDeque<>();
    private final ArrayDeque<XmlWidgetDocumentEdit> redoStack = new ArrayDeque<>();
    private final List<Consumer<XmlEditorSessionChange>> listeners = new ArrayList<>();

    private XmlEditorDocumentSource source;
    private XmlWidgetDocument document;
    private String text;
    private String savedText;
    private XmlEditorMode mode = XmlEditorMode.DESIGN;
    private XmlCommandRegistry commands = XmlCommandRegistry.none();

    private XmlEditorSession(XmlEditorDocumentSource source, String text, XmlWidgetRegistry registry) {
        this.source = source;
        this.registry = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        this.text = normalizeText(text);
        this.savedText = this.text;
        XmlWidgetDocumentResult result = XmlWidgetDocument.parseEditor(this.text, this.registry);
        this.document = result.document();
        setChannel(XmlEditorDiagnosticChannel.PARSE, List.of());
        setChannel(XmlEditorDiagnosticChannel.VALIDATION, result.diagnostics());
    }

    public static XmlEditorSession create(String xml) {
        return create(xml, XmlWidgetRegistry.builtIns());
    }

    public static XmlEditorSession create(String xml, XmlWidgetRegistry registry) {
        return new XmlEditorSession(null, xml, registry);
    }

    public static XmlEditorSession open(XmlEditorDocumentSource source) {
        return open(source, XmlWidgetRegistry.builtIns());
    }

    public static XmlEditorSession open(XmlEditorDocumentSource source, XmlWidgetRegistry registry) {
        XmlEditorDocumentSource normalized = Objects.requireNonNull(source, "source");
        return new XmlEditorSession(normalized, normalized.readText(), registry);
    }

    public XmlWidgetRegistry registry() {
        return registry;
    }

    public XmlCommandRegistry commands() {
        return commands;
    }

    public XmlEditorSession commands(XmlCommandRegistry commands) {
        XmlCommandRegistry next = commands == null ? XmlCommandRegistry.none() : commands;
        if (this.commands == next) return this;
        this.commands = next;
        emit(XmlEditorSessionChange.Kind.RUNTIME_OPTIONS_CHANGED, mode, "Runtime command registry changed");
        return this;
    }

    public Optional<XmlEditorDocumentSource> source() {
        return Optional.ofNullable(source);
    }

    public XmlWidgetDocument document() {
        return document;
    }

    public String text() {
        return text;
    }

    public String savedText() {
        return savedText;
    }

    public boolean dirty() {
        return !Objects.equals(text, savedText);
    }

    public XmlEditorMode mode() {
        return mode;
    }

    public XmlEditorSession mode(XmlEditorMode mode) {
        XmlEditorMode next = mode == null ? XmlEditorMode.DESIGN : mode;
        if (this.mode == next) return this;
        XmlEditorMode previous = this.mode;
        this.mode = next;
        emit(XmlEditorSessionChange.Kind.MODE_CHANGED, previous, "Mode changed");
        return this;
    }

    public XmlWidgetSelectionModel selection() {
        return selection;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return selection.selectedPath();
    }

    public Optional<XmlWidgetInspector.Inspection> selectedInspection() {
        return selection.selectedElement(document).map(element -> XmlWidgetInspector.inspect(element, registry));
    }

    public XmlWidgetHierarchy hierarchy() {
        return XmlWidgetHierarchy.from(document);
    }

    public List<XmlWidgetDiagnostic> diagnostics() {
        List<XmlWidgetDiagnostic> merged = new ArrayList<>();
        for (XmlEditorDiagnosticChannel channel : XmlEditorDiagnosticChannel.values()) {
            merged.addAll(diagnostics.getOrDefault(channel, List.of()));
        }
        return List.copyOf(merged);
    }

    public List<XmlWidgetDiagnostic> diagnostics(XmlEditorDiagnosticChannel channel) {
        if (channel == null) return List.of();
        return diagnostics.getOrDefault(channel, List.of());
    }

    public XmlWidgetDiagnosticsModel diagnosticsModel() {
        return XmlWidgetDiagnosticsModel.errors(diagnostics());
    }

    public boolean hasDiagnostics() {
        return !diagnostics().isEmpty();
    }

    public int undoCount() {
        return undoStack.size();
    }

    public int redoCount() {
        return redoStack.size();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public EventSubscription onChanged(Consumer<XmlEditorSessionChange> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public XmlEditorSession select(XmlWidgetNodePath path) {
        Optional<XmlWidgetNodePath> previous = selection.selectedPath();
        if (path == null) {
            selection.clear();
        } else {
            selection.selectIfPresent(document, path);
        }
        Optional<XmlWidgetNodePath> next = selection.selectedPath();
        if (!Objects.equals(previous, next)) {
            emit(XmlEditorSessionChange.Kind.SELECTION_CHANGED, mode, "Selection changed");
        }
        return this;
    }

    public XmlEditorSession selectRoot() {
        return select(XmlWidgetNodePath.root());
    }

    public boolean replaceText(String nextText) {
        text = normalizeText(nextText);
        try {
            XmlWidgetDocumentResult result = XmlWidgetDocument.parseEditor(text, registry);
            document = result.document();
            undoStack.clear();
            redoStack.clear();
            setChannel(XmlEditorDiagnosticChannel.PARSE, List.of());
            setChannel(XmlEditorDiagnosticChannel.VALIDATION, result.diagnostics());
            keepSelectionPathIfPresent();
            emit(XmlEditorSessionChange.Kind.TEXT_CHANGED, mode, "XML text parsed");
            emit(XmlEditorSessionChange.Kind.UNDO_STACK_CHANGED, mode, "Undo stack cleared after source replacement");
            return true;
        } catch (XmlWidgetLoadException failure) {
            setChannel(XmlEditorDiagnosticChannel.PARSE, failure.diagnostics());
            setChannel(XmlEditorDiagnosticChannel.VALIDATION, List.of());
            emit(XmlEditorSessionChange.Kind.TEXT_CHANGED, mode, "XML text parse failed");
            emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Parse diagnostics changed");
            return false;
        }
    }

    public boolean formatXml() {
        if (!replaceText(text)) return false;
        text = document.toXmlString(EDITOR_XML);
        emit(XmlEditorSessionChange.Kind.TEXT_CHANGED, mode, "XML text formatted");
        return true;
    }

    public boolean applyEdit(XmlWidgetDocumentEdit edit) {
        Objects.requireNonNull(edit, "edit");
        XmlWidgetNode selectedNode = selection.selectedNode(document).orElse(null);
        try {
            edit.apply(document);
            undoStack.push(edit);
            redoStack.clear();
            afterDocumentMutation(selectedNode, "Applied edit: " + edit.description());
            return true;
        } catch (RuntimeException failure) {
            setChannel(XmlEditorDiagnosticChannel.EDIT, List.of(new XmlWidgetDiagnostic(failure.getMessage())));
            emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Edit diagnostics changed");
            return false;
        }
    }

    public boolean insertChild(XmlWidgetNodePath parentPath, int index, XmlWidgetElement child) {
        XmlWidgetNodePath normalizedParent = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
        XmlWidgetElement normalizedChild = Objects.requireNonNull(child, "child").copy();
        XmlWidgetDocument candidate = document.copy();
        XmlWidgetDocumentEdit edit = XmlWidgetDocumentEdits.addChild(normalizedParent, index, normalizedChild);
        try {
            XmlWidgetElement parent = normalizedParent.resolveElement(candidate)
                    .orElseThrow(() -> new XmlWidgetLoadException("XML insert target '" + normalizedParent + "' is not an element."));
            int appliedIndex = Math.max(0, Math.min(index, parent.children().size()));
            edit.apply(candidate);
            XmlWidgetDocumentResult result = candidate.validate(registry);
            if (!result.valid()) {
                setChannel(XmlEditorDiagnosticChannel.EDIT, result.diagnostics());
                emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Insert diagnostics changed");
                return false;
            }

            document = result.document();
            undoStack.push(edit);
            redoStack.clear();
            text = document.toXmlString(EDITOR_XML);
            setChannel(XmlEditorDiagnosticChannel.PARSE, List.of());
            setChannel(XmlEditorDiagnosticChannel.VALIDATION, List.of());
            setChannel(XmlEditorDiagnosticChannel.EDIT, List.of());
            selection.selectIfPresent(document, normalizedParent.child(appliedIndex));
            emit(XmlEditorSessionChange.Kind.DOCUMENT_CHANGED, mode, "Inserted child: " + normalizedChild.name());
            emit(XmlEditorSessionChange.Kind.UNDO_STACK_CHANGED, mode, "Undo stack changed");
            emit(XmlEditorSessionChange.Kind.SELECTION_CHANGED, mode, "Selection changed");
            return true;
        } catch (RuntimeException failure) {
            setChannel(XmlEditorDiagnosticChannel.EDIT, List.of(new XmlWidgetDiagnostic(failure.getMessage())));
            emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Insert diagnostics changed");
            return false;
        }
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        XmlWidgetDocumentEdit edit = undoStack.pop();
        XmlWidgetNode selectedNode = selection.selectedNode(document).orElse(null);
        try {
            edit.undo(document);
            redoStack.push(edit);
            afterDocumentMutation(selectedNode, "Undo: " + edit.description());
            return true;
        } catch (RuntimeException failure) {
            undoStack.push(edit);
            setChannel(XmlEditorDiagnosticChannel.EDIT, List.of(new XmlWidgetDiagnostic(failure.getMessage())));
            emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Undo diagnostics changed");
            return false;
        }
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        XmlWidgetDocumentEdit edit = redoStack.pop();
        XmlWidgetNode selectedNode = selection.selectedNode(document).orElse(null);
        try {
            edit.apply(document);
            undoStack.push(edit);
            afterDocumentMutation(selectedNode, "Redo: " + edit.description());
            return true;
        } catch (RuntimeException failure) {
            redoStack.push(edit);
            setChannel(XmlEditorDiagnosticChannel.EDIT, List.of(new XmlWidgetDiagnostic(failure.getMessage())));
            emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Redo diagnostics changed");
            return false;
        }
    }

    public boolean save() {
        if (source == null || !source.writable()) return false;
        source.writeText(text);
        savedText = text;
        emit(XmlEditorSessionChange.Kind.SAVED, mode, "XML source saved");
        return true;
    }

    public boolean saveAs(XmlEditorDocumentSource source) {
        XmlEditorDocumentSource next = Objects.requireNonNull(source, "source");
        if (!next.writable()) return false;
        next.writeText(text);
        this.source = next;
        savedText = text;
        emit(XmlEditorSessionChange.Kind.SOURCE_CHANGED, mode, "XML source changed");
        emit(XmlEditorSessionChange.Kind.SAVED, mode, "XML source saved");
        return true;
    }

    public boolean revert() {
        if (source == null) return false;
        String sourceText = source.readText();
        boolean parsed = replaceText(sourceText);
        if (parsed) {
            savedText = text;
            undoStack.clear();
            redoStack.clear();
            emit(XmlEditorSessionChange.Kind.REVERTED, mode, "XML source reverted");
        }
        return parsed;
    }

    public XmlEditorSession markClean() {
        savedText = text;
        emit(XmlEditorSessionChange.Kind.SAVED, mode, "XML session marked clean");
        return this;
    }

    public XmlEditorSession setDiagnostics(XmlEditorDiagnosticChannel channel, List<XmlWidgetDiagnostic> diagnostics) {
        if (channel == null) return this;
        List<XmlWidgetDiagnostic> previous = this.diagnostics.getOrDefault(channel, List.of());
        List<XmlWidgetDiagnostic> next = diagnostics == null || diagnostics.isEmpty()
                ? List.of()
                : List.copyOf(diagnostics);
        if (previous.equals(next)) return this;
        setChannel(channel, next);
        emit(XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED, mode, "Diagnostics changed");
        return this;
    }

    private void afterDocumentMutation(XmlWidgetNode selectedNode, String description) {
        remapSelection(selectedNode);
        text = document.toXmlString(EDITOR_XML);
        XmlWidgetDocumentResult result = document.validate(registry);
        setChannel(XmlEditorDiagnosticChannel.PARSE, List.of());
        setChannel(XmlEditorDiagnosticChannel.VALIDATION, result.diagnostics());
        setChannel(XmlEditorDiagnosticChannel.EDIT, List.of());
        emit(XmlEditorSessionChange.Kind.DOCUMENT_CHANGED, mode, description);
        emit(XmlEditorSessionChange.Kind.UNDO_STACK_CHANGED, mode, "Undo stack changed");
    }

    private void keepSelectionPathIfPresent() {
        selection.selectedPath().ifPresent(path -> selection.selectIfPresent(document, path));
    }

    private void remapSelection(XmlWidgetNode previousSelectedNode) {
        if (previousSelectedNode == null) return;
        Optional<XmlWidgetNodePath> remapped = pathOf(document.root(), previousSelectedNode, XmlWidgetNodePath.root());
        if (remapped.isPresent()) {
            selection.select(remapped.get());
        } else {
            selection.clear();
        }
    }

    private Optional<XmlWidgetNodePath> pathOf(XmlWidgetNode current, XmlWidgetNode target, XmlWidgetNodePath path) {
        if (current == target) return Optional.of(path);
        if (!(current instanceof dev.sixik.unigui.api.xml.XmlWidgetElement element)) return Optional.empty();
        List<XmlWidgetNode> children = element.children();
        for (int i = 0; i < children.size(); i++) {
            Optional<XmlWidgetNodePath> found = pathOf(children.get(i), target, path.child(i));
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private void setChannel(XmlEditorDiagnosticChannel channel, List<XmlWidgetDiagnostic> next) {
        if (next == null || next.isEmpty()) {
            diagnostics.remove(channel);
        } else {
            diagnostics.put(channel, List.copyOf(next));
        }
    }

    private void emit(XmlEditorSessionChange.Kind kind, XmlEditorMode previousMode, String description) {
        XmlEditorSessionChange change = new XmlEditorSessionChange(this, kind, previousMode, mode, description);
        for (Consumer<XmlEditorSessionChange> listener : List.copyOf(listeners)) {
            listener.accept(change);
        }
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text;
    }
}
