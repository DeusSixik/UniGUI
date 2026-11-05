package dev.sixik.unigui.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр команд редактора. Меню строятся из него, поэтому панель или мод добавляет пункт, зарегистрировав команду.
 */
public final class EditorCommands {
    private final Map<String, EditorCommand> commands = new LinkedHashMap<>();
    private final List<String> menuOrder = new ArrayList<>();

    /**
     * Регистрирует команду.
     *
     * @param command команда
     * @return этот реестр
     * @throws IllegalArgumentException если идентификатор уже занят
     */
    public EditorCommands register(EditorCommand command) {
        if (commands.putIfAbsent(command.id(), command) != null) {
            throw new IllegalArgumentException("Duplicate command id: " + command.id());
        }
        declareMenu(command.menu());
        return this;
    }

    /**
     * Объявляет меню верхнего уровня, даже если команд в нём пока нет. Порядок объявления — порядок в строке меню.
     *
     * @param name имя меню
     * @return этот реестр
     */
    public EditorCommands declareMenu(String name) {
        if (name != null && !name.isBlank() && !menuOrder.contains(name)) menuOrder.add(name);
        return this;
    }

    /**
     * @param id идентификатор
     * @return команда или пусто
     */
    public Optional<EditorCommand> find(String id) {
        return Optional.ofNullable(commands.get(id));
    }

    /**
     * Выполняет команду по идентификатору.
     *
     * @param id      идентификатор
     * @param context контекст
     * @return {@code true}, если команда есть, доступна и выполнена
     */
    public boolean run(String id, EditorContext context) {
        EditorCommand command = commands.get(id);
        return command != null && command.run(context);
    }

    /** @return все команды в порядке регистрации */
    public List<EditorCommand> all() {
        return List.copyOf(commands.values());
    }

    /** @return имена меню в порядке объявления */
    public List<String> menus() {
        return List.copyOf(menuOrder);
    }

    /**
     * Команды меню в порядке {@link EditorCommand#order()}, затем регистрации.
     *
     * @param menu имя меню
     * @return команды меню
     */
    public List<EditorCommand> menu(String menu) {
        List<EditorCommand> result = new ArrayList<>();
        for (EditorCommand command : commands.values()) if (command.menu().equals(menu)) result.add(command);
        result.sort(Comparator.comparingInt(EditorCommand::order)); // сортировка устойчива: ничья сохраняет регистрацию
        return result;
    }
}
