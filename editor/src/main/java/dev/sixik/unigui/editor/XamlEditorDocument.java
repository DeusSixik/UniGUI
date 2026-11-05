package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.xaml.edit.EditorSession;

import java.util.Objects;
import java.util.Optional;

/**
 * Документ XAML, который редактирует {@link EditorSession}.
 *
 * <p>Адаптер ничего не добавляет к сессии: отмена, сохранение и признак правок берутся из неё, а панели UI-редактора
 * получают саму сессию через {@link #session()}.</p>
 */
public final class XamlEditorDocument implements EditorDocument {
    /** Значение {@link #kind()} для документов XAML. */
    public static final String KIND = "xaml";

    private final String id;
    private final String title;
    private final EditorSession session;

    /**
     * Оборачивает сессию.
     *
     * @param id      идентификатор документа
     * @param title   заголовок
     * @param session сессия редактирования
     */
    public XamlEditorDocument(String id, String title, EditorSession session) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.session = Objects.requireNonNull(session, "session");
    }

    /** @return сессия редактирования: выбранный узел, диагностика, правки */
    public EditorSession session() {
        return session;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public boolean dirty() {
        return session.dirty();
    }

    @Override
    public boolean canUndo() {
        return session.undoCount() > 0;
    }

    @Override
    public boolean canRedo() {
        return session.redoCount() > 0;
    }

    @Override
    public boolean undo() {
        return session.undo();
    }

    @Override
    public boolean redo() {
        return session.redo();
    }

    @Override
    public Optional<String> undoLabel() {
        return session.undoLabel();
    }

    @Override
    public Optional<String> redoLabel() {
        return session.redoLabel();
    }

    @Override
    public boolean canSave() {
        return session.canSave();
    }

    @Override
    public boolean save() {
        return session.save();
    }

    @Override
    public EventSubscription onChanged(Runnable listener) {
        // Одна правка порождает несколько событий сессии (текст, стек отмены…); документу достаточно по одному
        // уведомлению на изменение содержимого, сохранение и возврат.
        return session.onChanged(change -> {
            switch (change.kind()) {
                case DOCUMENT_CHANGED, SAVED, REVERTED -> listener.run();
                default -> { }
            }
        });
    }
}
