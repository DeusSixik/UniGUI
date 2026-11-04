package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Общие службы одного редактора: шина событий, выбор, команды, текущий документ и реестр сервисов.
 *
 * <p>Панели получают контекст при создании и общаются только через него. Контекст не зависит от виджетов, поэтому
 * его можно создавать и проверять без экрана.</p>
 */
public final class EditorContext {
    /**
     * Текущий документ сменился.
     *
     * @param previous прежний документ или {@code null}
     * @param current  новый документ или {@code null}
     */
    public record DocumentChanged(EditorDocument previous, EditorDocument current) {
    }

    /**
     * Текущий документ изменился (правка, отмена, сохранение).
     *
     * @param document документ
     */
    public record DocumentEdited(EditorDocument document) {
    }

    private final EditorEvents events = new EditorEvents();
    private final EditorSelection selection = new EditorSelection(events);
    private final EditorCommands commands = new EditorCommands();
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private EditorDocument document;
    private EventSubscription documentSubscription;

    /** Создаёт пустой контекст. */
    public EditorContext() {
    }

    /** @return шина событий */
    public EditorEvents events() {
        return events;
    }

    /** @return общий выбор */
    public EditorSelection selection() {
        return selection;
    }

    /** @return реестр команд */
    public EditorCommands commands() {
        return commands;
    }

    /** @return текущий документ */
    public Optional<EditorDocument> document() {
        return Optional.ofNullable(document);
    }

    /**
     * Делает документ текущим. Выбор сбрасывается, потому что выбранные объекты принадлежали прежнему документу.
     *
     * @param next документ или {@code null}, чтобы закрыть текущий
     */
    public void document(EditorDocument next) {
        if (next == document) return;
        if (documentSubscription != null) documentSubscription.unsubscribe();
        documentSubscription = null;
        EditorDocument previous = document;
        document = next;
        selection.clear();
        if (next != null) documentSubscription = next.onChanged(() -> events.post(new DocumentEdited(next)));
        events.post(new DocumentChanged(previous, next));
    }

    /**
     * Публикует сервис для панелей.
     *
     * @param type    ключ — класс или интерфейс сервиса
     * @param service реализация
     * @param <T>     тип сервиса
     * @return этот контекст
     * @throws IllegalArgumentException если сервис этого типа уже опубликован
     */
    public <T> EditorContext provide(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        if (services.putIfAbsent(type, service) != null) {
            throw new IllegalArgumentException("Service already provided: " + type.getName());
        }
        return this;
    }

    /**
     * @param type класс сервиса
     * @param <T>  тип
     * @return сервис или пусто
     */
    public <T> Optional<T> service(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    /**
     * @param type класс сервиса
     * @param <T>  тип
     * @return сервис
     * @throws IllegalStateException если сервис не опубликован
     */
    public <T> T require(Class<T> type) {
        return service(type).orElseThrow(
                () -> new IllegalStateException("Editor service is not provided: " + type.getName()));
    }
}
