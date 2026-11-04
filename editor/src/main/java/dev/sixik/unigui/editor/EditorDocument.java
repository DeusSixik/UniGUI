package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.Optional;

/**
 * То, что редактируется: UI-документ, граф, стиль-пак. У каждого вида свой формат и свои панели, но каркасу
 * нужен один общий набор возможностей: имя, признак несохранённых правок, отмена и сохранение.
 */
public interface EditorDocument {
    /** @return стабильный идентификатор (путь файла, идентификатор в паке) */
    String id();

    /** @return заголовок для вкладки и строки состояния */
    String title();

    /** @return вид документа ({@code xaml}, {@code graph}…): по нему панели решают, подходит ли им документ */
    String kind();

    /** @return {@code true}, если есть несохранённые правки */
    boolean dirty();

    /** @return {@code true}, если можно отменить правку */
    boolean canUndo();

    /** @return {@code true}, если можно повторить отменённую правку */
    boolean canRedo();

    /**
     * Отменяет последнюю правку.
     *
     * @return {@code true}, если что-то отменено
     */
    boolean undo();

    /**
     * Повторяет отменённую правку.
     *
     * @return {@code true}, если что-то повторено
     */
    boolean redo();

    /** @return подпись правки, которую отменит {@link #undo()} */
    default Optional<String> undoLabel() {
        return Optional.empty();
    }

    /** @return подпись правки, которую повторит {@link #redo()} */
    default Optional<String> redoLabel() {
        return Optional.empty();
    }

    /** @return {@code true}, если документ можно сохранить */
    boolean canSave();

    /**
     * Сохраняет документ.
     *
     * @return {@code true}, если сохранение удалось
     */
    boolean save();

    /**
     * Подписывается на любые изменения документа (правка, отмена, сохранение, замена текста).
     *
     * @param listener вызывается после изменения
     * @return подписка
     */
    EventSubscription onChanged(Runnable listener);
}
