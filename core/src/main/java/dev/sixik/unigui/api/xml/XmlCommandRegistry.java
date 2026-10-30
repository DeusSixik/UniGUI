package dev.sixik.unigui.api.xml;

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
 * а приложение явно разрешает допустимые handlers через {@link #register(String, XmlCommand)}.
 * Это безопаснее, чем пытаться вызывать методы по reflection из XML.</p>
 */
public final class XmlCommandRegistry {
    private static final XmlCommandRegistry NONE = new XmlCommandRegistry(false);

    private final Map<String, XmlCommand> commands = new LinkedHashMap<>();
    private final boolean mutable;

    /**
     * Создаёт пустой mutable registry.
     */
    public XmlCommandRegistry() {
        this(true);
    }

    private XmlCommandRegistry(boolean mutable) {
        this.mutable = mutable;
    }

    /**
     * Создаёт новый пустой mutable registry.
     *
     * @return mutable command registry
     */
    public static XmlCommandRegistry empty() {
        return new XmlCommandRegistry();
    }

    /**
     * Возвращает immutable registry без команд.
     *
     * @return shared immutable empty registry
     */
    public static XmlCommandRegistry none() {
        return NONE;
    }

    /**
     * Регистрирует command handler по имени.
     *
     * @param name имя команды, на которое ссылается XML
     * @param command handler команды; не может быть {@code null}
     * @return этот registry для chained-регистрации
     */
    public XmlCommandRegistry register(String name, XmlCommand command) {
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
    public Optional<XmlCommand> command(String name) {
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
                .orElseThrow(() -> new XmlWidgetLoadException("Unknown XML command '" + name + "'."))
                .execute(source, event);
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML command name must not be blank");
        return normalized;
    }
}
