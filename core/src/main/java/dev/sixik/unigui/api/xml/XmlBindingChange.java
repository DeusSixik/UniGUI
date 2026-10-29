package dev.sixik.unigui.api.xml;

/**
 * Снимок изменения типизированного XML observable-значения.
 *
 * <p>Change создаётся один раз на фактическое изменение и передаётся всем
 * слушателям. Значения не копируются глубоко: если {@code T} изменяемый объект,
 * вызывающий код сам отвечает за его snapshot-семантику.</p>
 *
 * @param name имя/path источника
 * @param valueType runtime-тип значения
 * @param oldValue значение до изменения
 * @param newValue значение после изменения
 * @param <T> тип значения
 */
public record XmlBindingChange<T>(String name, Class<T> valueType, T oldValue, T newValue) {
    /** Нормализует имя и проверяет обязательный runtime-тип. */
    public XmlBindingChange {
        name = name == null ? "" : name.trim();
        if (valueType == null) throw new IllegalArgumentException("XML binding value type must not be null");
    }
}
