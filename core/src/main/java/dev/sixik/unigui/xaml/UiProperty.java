package dev.sixik.unigui.xaml;

/**
 * Свойство узла: {@code Width="10"}, {@code x:Name="root"}, {@code Grid.Row="1"} или {@code <Button.Content>}.
 *
 * <p>{@code owner} задан для attached-свойств ({@code Grid.Row}) и для property-элементов
 * ({@code Button.Content}); для обычного атрибута он {@code null}. {@code form} запоминает, как свойство было
 * записано, чтобы редактор не переписывал стиль документа при сохранении.</p>
 *
 * @param name     имя члена ({@code Row}, {@code Content}, {@code Name})
 * @param owner    тип-владелец или {@code null}
 * @param value    значение
 * @param form     форма записи
 * @param location позиция имени свойства в исходном тексте
 */
public record UiProperty(UiName name, UiName owner, UiValue value, Form form, SourceLocation location) {
    /**
     * Форма записи свойства.
     */
    public enum Form {
        /**
         * {@code <Type Prop="value"/>}.
         */
        ATTRIBUTE,
        /**
         * {@code <Type><Type.Prop>value</Type.Prop></Type>}.
         */
        ELEMENT
    }

    public UiProperty {
        if (name == null) throw new IllegalArgumentException("Property name must not be null");
        if (value == null) throw new IllegalArgumentException("Property value must not be null");
        form = form == null ? Form.ATTRIBUTE : form;
        location = location == null ? SourceLocation.UNKNOWN : location;
    }

    /**
     * Создаёт обычное атрибутное свойство со строковым значением.
     *
     * @param name    имя без namespace
     * @param literal значение
     * @return свойство
     */
    public static UiProperty attribute(String name, String literal) {
        return new UiProperty(UiName.of(name), null, new UiValue.Literal(literal), Form.ATTRIBUTE, null);
    }

    /**
     * Создаёт атрибутное свойство с произвольным значением.
     *
     * @param name  имя
     * @param value значение
     * @return свойство
     */
    public static UiProperty attribute(UiName name, UiValue value) {
        return new UiProperty(name, null, value, Form.ATTRIBUTE, null);
    }

    /**
     * Показывает, задано ли свойство через owner (attached или property-элемент).
     *
     * @return {@code true}, если {@code owner != null}
     */
    public boolean qualified() {
        return owner != null;
    }

    /**
     * Сравнивает идентичность свойства (owner + имя), игнорируя значение и форму.
     *
     * @param other другое свойство
     * @return {@code true}, если это одно и то же свойство
     */
    public boolean sameKey(UiProperty other) {
        return other != null && sameKey(other.owner, other.name);
    }

    /**
     * Сравнивает идентичность свойства с заданным ключом.
     *
     * @param otherOwner владелец или {@code null}
     * @param otherName  имя члена
     * @return {@code true}, если ключи совпадают
     */
    public boolean sameKey(UiName otherOwner, UiName otherName) {
        boolean ownerEqual = owner == null ? otherOwner == null : owner.sameAs(otherOwner);
        return ownerEqual && name.sameAs(otherName);
    }

    /**
     * Возвращает имя так, как оно пишется в документе: {@code x:Name} или {@code Grid.Row}.
     *
     * @return текстовое имя
     */
    public String displayName() {
        return owner == null ? name.qualified() : owner.qualified() + "." + name.local();
    }
}
