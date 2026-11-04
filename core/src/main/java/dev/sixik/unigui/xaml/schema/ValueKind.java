package dev.sixik.unigui.xaml.schema;

/**
 * Редакторская категория значения свойства: определяет, какой виджет-редактор показать в инспекторе.
 */
public enum ValueKind {
    STRING,
    NUMBER,
    BOOLEAN,
    ENUM,
    COLOR,
    INSETS,
    SIZE_VALUE,
    RESOURCE_ID,
    BINDING_OR_ACTION
}
