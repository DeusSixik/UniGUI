package dev.sixik.unigui.editor;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Действие редактора, которое можно вызвать из меню, с панели инструментов или кодом.
 *
 * <p>Команда не знает, откуда её вызвали: она получает {@link EditorContext} и работает с текущим документом и
 * выбором. Доступность ({@link #enabled(EditorContext)}) проверяется при каждом открытии меню, поэтому
 * «Отменить» сама становится серой, когда отменять нечего.</p>
 */
public final class EditorCommand {
    private final String id;
    private final String title;
    private final String menu;
    private final int order;
    private final String shortcut;
    private final EditorShortcut binding;
    private final Predicate<EditorContext> enabled;
    private final Consumer<EditorContext> action;

    private EditorCommand(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.menu = builder.menu;
        this.order = builder.order;
        this.shortcut = builder.shortcut;
        this.binding = builder.shortcut.isBlank() ? null : EditorShortcut.parse(builder.shortcut);
        this.enabled = builder.enabled;
        this.action = builder.action;
    }

    /**
     * Начинает описание команды.
     *
     * @param id уникальный идентификатор, например {@code edit.undo}
     * @return билдер
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** @return идентификатор */
    public String id() {
        return id;
    }

    /** @return заголовок пункта меню */
    public String title() {
        return title;
    }

    /** @return имя меню верхнего уровня или пустая строка, если команда не в меню */
    public String menu() {
        return menu;
    }

    /** @return порядок внутри меню: меньшие выше, при равенстве — по порядку регистрации */
    public int order() {
        return order;
    }

    /** @return подпись сочетания клавиш для пункта меню или пустая строка */
    public String shortcut() {
        return shortcut;
    }

    /** @return разобранное сочетание клавиш или {@code null}, если оно не задано */
    public EditorShortcut binding() {
        return binding;
    }

    /**
     * @param context контекст редактора
     * @return {@code true}, если команду сейчас можно выполнить
     */
    public boolean enabled(EditorContext context) {
        return enabled.test(context);
    }

    /**
     * Выполняет команду, если она доступна.
     *
     * @param context контекст редактора
     * @return {@code true}, если команда выполнена
     */
    public boolean run(EditorContext context) {
        if (!enabled.test(context)) return false;
        action.accept(context);
        return true;
    }

    /** Билдер {@link EditorCommand}. */
    public static final class Builder {
        private final String id;
        private String title;
        private String menu = "";
        private int order;
        private String shortcut = "";
        private Predicate<EditorContext> enabled = context -> true;
        private Consumer<EditorContext> action = context -> { };

        private Builder(String id) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("command id must not be blank");
            this.id = id.trim();
            this.title = this.id;
        }

        /**
         * @param value заголовок пункта меню
         * @return этот билдер
         */
        public Builder title(String value) {
            this.title = Objects.requireNonNull(value, "title");
            return this;
        }

        /**
         * @param value имя меню верхнего уровня («File», «Edit»…)
         * @return этот билдер
         */
        public Builder menu(String value) {
            this.menu = value == null ? "" : value.trim();
            return this;
        }

        /**
         * @param value порядок внутри меню
         * @return этот билдер
         */
        public Builder order(int value) {
            this.order = value;
            return this;
        }

        /**
         * Сочетание клавиш команды: оно показывается в меню и срабатывает по нажатию в окне редактора.
         *
         * @param value запись вроде {@code Ctrl+S}; пустая строка — без сочетания
         * @return этот билдер
         * @throws IllegalArgumentException если запись не разбирается
         */
        public Builder shortcut(String value) {
            String normalized = value == null ? "" : value.trim();
            if (!normalized.isEmpty()) EditorShortcut.parse(normalized); // опечатка находится сразу, а не при нажатии
            this.shortcut = normalized;
            return this;
        }

        /**
         * @param value условие доступности
         * @return этот билдер
         */
        public Builder enabledWhen(Predicate<EditorContext> value) {
            this.enabled = Objects.requireNonNull(value, "enabled");
            return this;
        }

        /**
         * @param value выполняемое действие
         * @return этот билдер
         */
        public Builder action(Consumer<EditorContext> value) {
            this.action = Objects.requireNonNull(value, "action");
            return this;
        }

        /** @return готовая команда */
        public EditorCommand build() {
            return new EditorCommand(this);
        }
    }
}
