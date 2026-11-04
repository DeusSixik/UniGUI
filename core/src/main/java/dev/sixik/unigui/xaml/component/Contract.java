package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.xaml.SourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Что компонент показывает снаружи: параметры, которые ему задают, и события, которыми он обменивается с
 * внешним миром.
 *
 * <p>Контракт описывает только форму. Он одинаково проверяет документ, Java-вызов и (позже) граф нод, поэтому
 * опечатка в имени параметра или несовпадение типа находятся сразу и с позицией в документе.</p>
 */
public final class Contract {
    /** Имя события жизненного цикла: компонент построен, значения параметров применены. */
    public static final String CONSTRUCT = "Construct";
    /** Имя события жизненного цикла: компонент освобождается. */
    public static final String DESTRUCT = "Destruct";

    /** Направление события относительно компонента. */
    public enum Direction {
        /** Событие приходит в компонент (Java или внешний граф вызывает {@code send}). */
        IN,
        /** Компонент сам сообщает наружу (кнопка нажата, товар куплен). */
        OUT
    }

    /**
     * Параметр.
     *
     * @param name        имя; совпадает с именем свойства при использовании компонента как тега
     * @param type        тип
     * @param defaultText значение по умолчанию текстом или {@code null}, если задан тип по умолчанию
     * @param value       значение по умолчанию после разбора
     * @param description подсказка для редактора
     * @param location    позиция в документе
     */
    public record Param(String name, ParamType type, String defaultText, Object value, String description,
                        SourceLocation location) {
    }

    /**
     * Аргумент события.
     *
     * @param name имя
     * @param type тип
     */
    public record Arg(String name, ParamType type) {
    }

    /**
     * Событие.
     *
     * @param name      имя
     * @param direction направление
     * @param args      аргументы по порядку объявления
     * @param coalesce  {@code true}: несколько входящих событий за кадр сливаются в последнее
     * @param location  позиция в документе
     */
    public record Event(String name, Direction direction, List<Arg> args, boolean coalesce,
                        SourceLocation location) {
        public Event {
            args = List.copyOf(args);
        }

        /**
         * @param argument имя аргумента
         * @return аргумент или пусто
         */
        public Optional<Arg> arg(String argument) {
            for (Arg arg : args) if (arg.name().equals(argument)) return Optional.of(arg);
            return Optional.empty();
        }
    }

    private final Map<String, Param> params = new LinkedHashMap<>();
    private final Map<String, Param> vars = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();

    Contract(List<Param> params, List<Event> events) {
        this(params, List.of(), events);
    }

    Contract(List<Param> params, List<Param> vars, List<Event> events) {
        for (Param param : params) this.params.put(param.name(), param);
        for (Param variable : vars) this.vars.put(variable.name(), variable);
        for (Event event : events) this.events.put(event.name(), event);
    }

    /** @return пустой контракт */
    public static Contract empty() {
        return new Contract(List.of(), List.of());
    }

    /** @return параметры по порядку объявления */
    public List<Param> params() {
        return List.copyOf(params.values());
    }

    /**
     * Переменные состояния графа: внутренние значения компонента, которые не видны снаружи и не являются свойствами
     * тега (счётчик нажатий, признак «идёт загрузка»).
     *
     * @return переменные по порядку объявления
     */
    public List<Param> vars() {
        return List.copyOf(vars.values());
    }

    /**
     * @param name имя переменной
     * @return переменная или пусто
     */
    public Optional<Param> var(String name) {
        return Optional.ofNullable(vars.get(name));
    }

    /** @return события по порядку объявления */
    public List<Event> events() {
        return List.copyOf(events.values());
    }

    /**
     * @param name имя параметра
     * @return параметр или пусто
     */
    public Optional<Param> param(String name) {
        return Optional.ofNullable(params.get(name));
    }

    /**
     * @param name имя события
     * @return событие или пусто
     */
    public Optional<Event> event(String name) {
        return Optional.ofNullable(events.get(name));
    }

    /** @return имена параметров через запятую, для сообщений об ошибках */
    String paramNames() {
        return params.isEmpty() ? "none" : String.join(", ", params.keySet());
    }

    /** @return имена событий через запятую, для сообщений об ошибках */
    String eventNames() {
        return events.isEmpty() ? "none" : String.join(", ", events.keySet());
    }
}
