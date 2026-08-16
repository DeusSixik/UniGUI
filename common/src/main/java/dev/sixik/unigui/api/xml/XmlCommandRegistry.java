package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Изменяемый реестр безопасных именованных команд, на которые могут ссылаться event-атрибуты XML. */
public final class XmlCommandRegistry {
    private static final XmlCommandRegistry NONE = new XmlCommandRegistry(false);

    private final Map<String, XmlCommand> commands = new LinkedHashMap<>();
    private final boolean mutable;

    public XmlCommandRegistry() {
        this(true);
    }

    private XmlCommandRegistry(boolean mutable) {
        this.mutable = mutable;
    }

    public static XmlCommandRegistry empty() {
        return new XmlCommandRegistry();
    }

    public static XmlCommandRegistry none() {
        return NONE;
    }

    public XmlCommandRegistry register(String name, XmlCommand command) {
        if (!mutable) {
            throw new UnsupportedOperationException("Default XML command registry is immutable.");
        }
        String normalized = normalizeName(name);
        if (command == null) throw new IllegalArgumentException("XML command handler must not be null");
        commands.put(normalized, command);
        return this;
    }

    public boolean contains(String name) {
        return command(name).isPresent();
    }

    public Optional<XmlCommand> command(String name) {
        String normalized = normalizeName(name);
        return Optional.ofNullable(commands.get(normalized));
    }

    public Set<String> names() {
        return Set.copyOf(commands.keySet());
    }

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
