package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Сессия редактирования одного документа: дерево разметки, текст, выбор, история отмены и диагностика.
 *
 * <p>Сессия держит две согласованные формы документа: дерево ({@link #document()}) для визуального редактора и
 * текст ({@link #text()}) для режима кода. Правки через {@link #apply(Edit)} меняют дерево и пересобирают текст;
 * {@link #replaceText(String)} разбирает текст заново. Если текст синтаксически некорректен, сессия сохраняет
 * последнее корректное дерево, а сам текст остаётся в буфере, поэтому пользователь не теряет набранное.</p>
 *
 * <p>Диагностика разделена на каналы ({@link Channel}): ошибки разбора, проверки по схеме и отклонённых правок
 * показываются независимо.</p>
 *
 * <p>Класс не потокобезопасен: сессию ведёт поток интерфейса.</p>
 */
public final class EditorSession {
    /** Режим представления документа. */
    public enum Mode {
        /** Визуальный редактор. */
        VISUAL,
        /** Текст документа. */
        CODE,
        /** Оба представления рядом. */
        SPLIT
    }

    /** Источник диагностики. */
    public enum Channel {
        /** Синтаксические ошибки текста. */
        PARSE,
        /** Проверка документа по схеме. */
        VALIDATION,
        /** Причина, по которой последняя правка была отклонена. */
        EDIT
    }

    private final Schema schema;
    private final DocumentSource source;
    private final EditHistory history = new EditHistory();
    private final List<Consumer<SessionChange>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Channel, List<UiDiagnostic>> diagnostics = new EnumMap<>(Channel.class);

    private UiDocument document;
    private String text;
    private String savedText;
    private UiNode selection;
    private Mode mode = Mode.VISUAL;

    private EditorSession(Schema schema, DocumentSource source, String initialText) {
        this.schema = schema == null ? Xaml.defaultSchema() : schema;
        this.source = source;
        this.document = parseOrThrow(initialText);
        this.text = initialText;
        this.savedText = initialText;
        for (Channel channel : Channel.values()) diagnostics.put(channel, List.of());
        revalidate();
    }

    // ------------------------------------------------------------------ создание

    /**
     * Создаёт сессию для документа в памяти.
     *
     * @param xaml текст документа
     * @return сессия
     * @throws XamlLoadException если текст синтаксически некорректен
     */
    public static EditorSession create(String xaml) {
        return create(xaml, Xaml.defaultSchema());
    }

    /**
     * Создаёт сессию с собственным реестром типов.
     *
     * @param xaml   текст документа
     * @param schema реестр типов
     * @return сессия
     * @throws XamlLoadException если текст синтаксически некорректен
     */
    public static EditorSession create(String xaml, Schema schema) {
        return new EditorSession(schema, null, xaml);
    }

    /**
     * Открывает документ из источника.
     *
     * @param source источник
     * @return сессия, у которой {@link #save()} пишет обратно в источник
     * @throws XamlLoadException если сохранённый текст синтаксически некорректен
     */
    public static EditorSession open(DocumentSource source) {
        return open(source, Xaml.defaultSchema());
    }

    /**
     * Открывает документ из источника с собственным реестром типов.
     *
     * @param source источник
     * @param schema реестр типов
     * @return сессия
     */
    public static EditorSession open(DocumentSource source, Schema schema) {
        return new EditorSession(schema, source, source.readText());
    }

    private static UiDocument parseOrThrow(String xaml) {
        try {
            return XamlFormat.INSTANCE.read(xaml);
        } catch (UiParseException failure) {
            throw new XamlLoadException("Cannot parse XAML source: " + failure.reason(), failure.location(), failure);
        }
    }

    // ------------------------------------------------------------------ состояние

    /** @return последнее корректное дерево документа */
    public UiDocument document() {
        return document;
    }

    /** @return текст документа; при ошибке разбора это то, что набрал пользователь */
    public String text() {
        return text;
    }

    /** @return {@code true}, если текст отличается от сохранённого */
    public boolean dirty() {
        return !text.equals(savedText);
    }

    /** @return реестр типов, по которому проверяется документ */
    public Schema schema() {
        return schema;
    }

    /** @return текущий режим представления */
    public Mode mode() {
        return mode;
    }

    /**
     * Переключает режим представления.
     *
     * @param next новый режим
     */
    public void mode(Mode next) {
        if (next == null || next == mode) return;
        mode = next;
        emit(SessionChange.Kind.MODE_CHANGED);
    }

    // ------------------------------------------------------------------ выбор

    /** @return выбранный узел */
    public Optional<UiNode> selected() {
        return Optional.ofNullable(selection);
    }

    /** @return адрес выбранного узла */
    public Optional<NodePath> selectedPath() {
        return NodePath.find(document, selection);
    }

    /**
     * Выбирает узел. Повторный выбор того же узла событий не вызывает.
     *
     * @param node узел документа или {@code null}, чтобы снять выбор
     */
    public void select(UiNode node) {
        if (node == selection) return;
        selection = node;
        emit(SessionChange.Kind.SELECTION_CHANGED);
    }

    /**
     * Выбирает узел по адресу.
     *
     * @param path адрес или {@code null}, чтобы снять выбор
     */
    public void select(NodePath path) {
        select(path == null ? null : path.resolve(document).orElse(null));
    }

    // ------------------------------------------------------------------ правки

    /**
     * Применяет правку, если она допустима.
     *
     * @param edit правка
     * @return {@code true}, если правка применена; иначе документ не изменён, а причина доступна в
     * {@link Channel#EDIT}
     */
    public boolean apply(Edit edit) {
        Optional<String> problem = edit.check(schema);
        if (problem.isPresent()) {
            setDiagnostics(Channel.EDIT, List.of(new UiDiagnostic(problem.get())));
            return false;
        }
        int undoBefore = history.undoCount();
        int redoBefore = history.redoCount();
        history.execute(edit);
        afterDocumentChange(undoBefore, redoBefore);
        return true;
    }

    /**
     * Отменяет последнюю правку.
     *
     * @return {@code true}, если было что отменять
     */
    public boolean undo() {
        int undoBefore = history.undoCount();
        int redoBefore = history.redoCount();
        if (history.undo().isEmpty()) return false;
        afterDocumentChange(undoBefore, redoBefore);
        return true;
    }

    /**
     * Повторяет последнюю отменённую правку.
     *
     * @return {@code true}, если было что повторять
     */
    public boolean redo() {
        int undoBefore = history.undoCount();
        int redoBefore = history.redoCount();
        if (history.redo().isEmpty()) return false;
        afterDocumentChange(undoBefore, redoBefore);
        return true;
    }

    /** @return число шагов отмены */
    public int undoCount() {
        return history.undoCount();
    }

    /** @return число шагов повтора */
    public int redoCount() {
        return history.redoCount();
    }

    /** @return описание правки, которую отменит {@link #undo()} */
    public Optional<String> undoLabel() {
        return history.undoLabel();
    }

    /** @return описание правки, которую повторит {@link #redo()} */
    public Optional<String> redoLabel() {
        return history.redoLabel();
    }

    private void afterDocumentChange(int undoBefore, int redoBefore) {
        text = XamlFormat.INSTANCE.write(document);
        setDiagnosticsQuietly(Channel.PARSE, List.of());
        setDiagnosticsQuietly(Channel.EDIT, List.of());
        keepSelectionInDocument();
        revalidate();
        emit(SessionChange.Kind.DOCUMENT_CHANGED);
        if (history.undoCount() != undoBefore || history.redoCount() != redoBefore) {
            emit(SessionChange.Kind.UNDO_STACK_CHANGED);
        }
    }

    /** Если выбранный узел исчез из документа (удалён или откат вставки), выбор снимается. */
    private void keepSelectionInDocument() {
        if (selection != null && NodePath.find(document, selection).isEmpty()) {
            selection = null;
            emit(SessionChange.Kind.SELECTION_CHANGED);
        }
    }

    // ------------------------------------------------------------------ текст

    /**
     * Заменяет текст документа (режим кода).
     *
     * <p>Корректный текст заменяет дерево и очищает историю отмены, потому что старые правки ссылались на узлы
     * прежнего дерева. Выбор переносится на узел с тем же адресом. Некорректный текст остаётся в буфере, дерево
     * не меняется, ошибка попадает в {@link Channel#PARSE}.</p>
     *
     * @param newText новый текст
     * @return {@code true}, если текст разобран и принят как документ
     */
    public boolean replaceText(String newText) {
        String value = newText == null ? "" : newText;
        UiDocument parsed;
        try {
            parsed = XamlFormat.INSTANCE.read(value);
        } catch (UiParseException failure) {
            text = value;
            setDiagnostics(Channel.PARSE, List.of(new UiDiagnostic(
                    "Cannot parse XAML source: " + failure.reason(), failure.location())));
            return false;
        }
        install(parsed, value);
        return true;
    }

    private void install(UiDocument parsed, String newText) {
        NodePath keptSelection = selectedPath().orElse(null);
        boolean hadHistory = history.undoCount() > 0 || history.redoCount() > 0;
        history.clear();
        document = parsed;
        text = newText;
        setDiagnosticsQuietly(Channel.PARSE, List.of());
        setDiagnosticsQuietly(Channel.EDIT, List.of());
        UiNode before = selection;
        selection = keptSelection == null ? null : keptSelection.resolve(document).orElse(null);
        revalidate();
        emit(SessionChange.Kind.DOCUMENT_CHANGED);
        if (hadHistory) emit(SessionChange.Kind.UNDO_STACK_CHANGED);
        if (before != selection) emit(SessionChange.Kind.SELECTION_CHANGED);
    }

    // ------------------------------------------------------------------ сохранение

    /** @return {@code true}, если у сессии есть источник и он принимает запись */
    public boolean canSave() {
        return source != null && source.writable();
    }

    /**
     * Записывает текст в источник.
     *
     * @return {@code true}, если источник доступен для записи и запись выполнена
     */
    public boolean save() {
        if (source == null || !source.writable()) return false;
        source.writeText(text);
        savedText = text;
        emit(SessionChange.Kind.SAVED);
        return true;
    }

    /**
     * Возвращает документ к сохранённому состоянию.
     *
     * @return {@code true}, если сохранённый текст разобран
     */
    public boolean revert() {
        String saved = source != null ? source.readText() : savedText;
        UiDocument parsed;
        try {
            parsed = XamlFormat.INSTANCE.read(saved);
        } catch (UiParseException failure) {
            text = saved;
            savedText = saved;
            setDiagnostics(Channel.PARSE, List.of(new UiDiagnostic(
                    "Cannot parse XAML source: " + failure.reason(), failure.location())));
            return false;
        }
        savedText = saved;
        install(parsed, saved);
        emit(SessionChange.Kind.REVERTED);
        return true;
    }

    /** Считает текущий текст сохранённым, не обращаясь к источнику. */
    public void markClean() {
        savedText = text;
    }

    // ------------------------------------------------------------------ диагностика

    /**
     * Диагностика одного канала.
     *
     * @param channel канал
     * @return неизменяемый список
     */
    public List<UiDiagnostic> diagnostics(Channel channel) {
        return diagnostics.get(channel);
    }

    /** @return диагностика всех каналов в порядке: разбор, проверка, правка */
    public List<UiDiagnostic> allDiagnostics() {
        List<UiDiagnostic> all = new ArrayList<>();
        for (Channel channel : Channel.values()) all.addAll(diagnostics.get(channel));
        return all;
    }

    private void revalidate() {
        setDiagnosticsQuietly(Channel.VALIDATION,
                new DocumentValidator(schema).validate(document).diagnostics());
    }

    private void setDiagnostics(Channel channel, List<UiDiagnostic> next) {
        if (setDiagnosticsQuietly(channel, next)) emit(SessionChange.Kind.DIAGNOSTICS_CHANGED);
    }

    private boolean setDiagnosticsQuietly(Channel channel, List<UiDiagnostic> next) {
        List<UiDiagnostic> copy = List.copyOf(next);
        if (diagnostics.get(channel).equals(copy)) return false;
        diagnostics.put(channel, copy);
        return true;
    }

    // ------------------------------------------------------------------ события

    /**
     * Подписывается на изменения сессии.
     *
     * @param listener обработчик, вызывается в потоке, который меняет сессию
     * @return подписка; закрытие её отменяет
     */
    public EventSubscription onChanged(Consumer<SessionChange> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void emit(SessionChange.Kind kind) {
        SessionChange change = new SessionChange(kind);
        for (Consumer<SessionChange> listener : listeners) listener.accept(change);
    }
}
