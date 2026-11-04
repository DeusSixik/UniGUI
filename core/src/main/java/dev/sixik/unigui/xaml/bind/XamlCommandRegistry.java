package dev.sixik.unigui.xaml.bind;

import dev.sixik.unigui.xaml.load.XamlLoadException;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Изменяемый реестр безопасных именованных команд, на которые могут ссылаться event-атрибуты XML.
 *
 * <p>Registry отделяет XML от Java callbacks: документ указывает только строковое имя команды,
 * а приложение явно разрешает допустимые handlers через {@link #register(String, XamlCommand)}.
 * Это безопаснее, чем пытаться вызывать методы по reflection из XML.</p>
 */
public final class XamlCommandRegistry {
    private static final XamlCommandRegistry NONE = new XamlCommandRegistry(false);

    private final Map<String, XamlCommand> commands = new LinkedHashMap<>();
    private final boolean mutable;

    /**
     * Создаёт пустой mutable registry.
     */
    public XamlCommandRegistry() {
        this(true);
    }

    private XamlCommandRegistry(boolean mutable) {
        this.mutable = mutable;
    }

    /**
     * Создаёт новый пустой mutable registry.
     *
     * @return mutable command registry
     */
    public static XamlCommandRegistry empty() {
        return new XamlCommandRegistry();
    }

    /**
     * Возвращает immutable registry без команд.
     *
     * @return shared immutable empty registry
     */
    public static XamlCommandRegistry none() {
        return NONE;
    }

    /**
     * Регистрирует command handler по имени.
     *
     * @param name имя команды, на которое ссылается XML
     * @param command handler команды; не может быть {@code null}
     * @return этот registry для chained-регистрации
     */
    public XamlCommandRegistry register(String name, XamlParameterCommand command) {
        return register(name, (XamlCommand) command);
    }

    /**
     * Регистрирует command handler по имени.
     *
     * @param name    имя команды, на которое ссылается разметка
     * @param command handler команды
     * @return этот registry
     * @see #register(String, XamlParameterCommand)
     */
    public XamlCommandRegistry register(String name, XamlCommand command) {
        if (!mutable) {
            throw new UnsupportedOperationException("Default XML command registry is immutable.");
        }
        String normalized = normalizeName(name);
        if (command == null) throw new IllegalArgumentException("XML command handler must not be null");
        commands.put(normalized, command);
        return this;
    }

    /**
     * Проверяет наличие команды.
     *
     * @param name имя команды
     * @return {@code true}, если command зарегистрирован
     */
    public boolean contains(String name) {
        return command(name).isPresent();
    }

    /**
     * Возвращает command handler по имени.
     *
     * @param name имя команды
     * @return handler или empty
     */
    public Optional<XamlCommand> command(String name) {
        String normalized = normalizeName(name);
        return Optional.ofNullable(commands.get(normalized));
    }

    /**
     * Возвращает имена зарегистрированных команд.
     *
     * @return immutable set command names
     */
    public Set<String> names() {
        return Set.copyOf(commands.keySet());
    }

    /**
     * Выполняет команду по имени.
     *
     * @param name имя команды
     * @param source widget-источник события
     * @param event исходное событие
     */
    public void execute(String name, Widget source, Event event) {
        command(name)
                .orElseThrow(() -> new XamlLoadException("Unknown XML command '" + name + "'."))
                .execute(source, event);
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML command name must not be blank");
        return normalized;
    }
}
