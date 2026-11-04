package dev.sixik.unigui.xaml.schema;

/**
 * Редакторские метаданные свойства: то, что инспектор показывает рядом с полем ввода.
 *
 * <p>Все поля необязательны. Что не задано, {@link PropertyInfo} выводит сам: заголовок — из имени
 * ({@code backgroundTexture → Background Texture}), категорию — из смысла имени, тип редактора — из
 * конвертера. Класс иммутабелен, каждый метод возвращает новый экземпляр.</p>
 */
public final class Meta {
    private static final Meta EMPTY = new Meta(null, null, null, null, null);

    private final String displayName;
    private final String category;
    private final String defaultValue;
    private final String description;
    private final ValueKind kind;

    private Meta(String displayName, String category, String defaultValue, String description, ValueKind kind) {
        this.displayName = displayName;
        this.category = category;
        this.defaultValue = defaultValue;
        this.description = description;
        this.kind = kind;
    }

    /**
     * Возвращает пустые метаданные.
     *
     * @return общий неизменяемый экземпляр
     */
    public static Meta of() {
        return EMPTY;
    }

    /**
     * Создаёт метаданные с категорией.
     *
     * @param category категория («Layout», «Appearance», «Behavior», «Content» …)
     * @return метаданные
     */
    public static Meta category(String category) {
        return EMPTY.withCategory(category);
    }

    public Meta displayName(String value) {
        return new Meta(value, category, defaultValue, description, kind);
    }

    public Meta withCategory(String value) {
        return new Meta(displayName, value, defaultValue, description, kind);
    }

    public Meta defaultValue(String value) {
        return new Meta(displayName, category, value, description, kind);
    }

    public Meta description(String value) {
        return new Meta(displayName, category, defaultValue, value, kind);
    }

    /**
     * Явно задаёт тип редактора, когда конвертер его не определяет (например, строка, которая на самом деле
     * имя команды).
     *
     * @param value тип редактора
     * @return метаданные
     */
    public Meta kind(ValueKind value) {
        return new Meta(displayName, category, defaultValue, description, value);
    }

    String displayName() {
        return displayName;
    }

    String category() {
        return category;
    }

    String defaultValue() {
        return defaultValue;
    }

    String description() {
        return description;
    }

    ValueKind kind() {
        return kind;
    }
}
