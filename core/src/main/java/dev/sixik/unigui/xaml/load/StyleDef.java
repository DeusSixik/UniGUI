package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Стиль-ресурс: именованный набор значений свойств, который применяется к виджету одной записью
 * {@code Style="{StaticResource Primary}"}.
 *
 * <pre>{@code
 * <VBox.Resources>
 *   <Style x:Key="Base" TargetType="Button">
 *     <Setter Property="textColor" Value="#FFFFFF"/>
 *   </Style>
 *   <Style x:Key="Danger" BasedOn="Base">
 *     <Setter Property="background" Value="#B00020"/>
 *   </Style>
 * </VBox.Resources>
 * <Button Style="{StaticResource Danger}" text="Delete"/>
 * }</pre>
 *
 * <p>Значения стиля применяются раньше атрибутов виджета, поэтому явный атрибут всегда побеждает значение стиля.
 * Стиль наследуется через {@code BasedOn}: свойства базового стиля действуют, если производный их не переопределил.
 * При сохранении живого виджета стиль не восстанавливается: его значения записываются обычными атрибутами.</p>
 *
 * @param key        ключ ресурса
 * @param targetType тип виджета, к которому стиль применим, или {@code null} для любого
 * @param basedOn    ключ базового стиля или {@code null}
 * @param setters    значения свойств по порядку документа
 * @param location   позиция в документе
 */
public record StyleDef(String key, UiName targetType, String basedOn, List<Setter> setters, SourceLocation location) {
    /**
     * Одно значение свойства.
     *
     * @param name     имя свойства виджета
     * @param value    значение: текст, привязка или команда
     * @param location позиция в документе
     */
    public record Setter(String name, UiValue value, SourceLocation location) {
    }

    public StyleDef {
        setters = List.copyOf(setters);
    }

    /**
     * Читает элемент {@code <Style>}.
     *
     * @param entry узел стиля
     * @param fail  создаёт исключение с позицией
     * @return стиль
     */
    public static StyleDef read(UiNode entry, BiFunction<SourceLocation, String, RuntimeException> fail) {
        String key = null;
        UiName target = null;
        String basedOn = null;
        for (UiProperty property : entry.properties()) {
            String local = property.name().local();
            boolean language = XamlNamespaces.isLanguage(property.name().namespace());
            if (property.owner() != null) {
                throw fail.apply(property.location(), "<Style> only takes attributes, got '" + property.displayName() + "'.");
            }
            if (language && local.equals("Key")) {
                key = literal(property, fail);
            } else if (property.name().namespace().isEmpty() && local.equals("TargetType")) {
                target = UiName.of(literal(property, fail));
            } else if (property.name().namespace().isEmpty() && local.equals("BasedOn")) {
                basedOn = reference(property, fail);
            } else {
                throw fail.apply(property.location(), "Unknown attribute '" + property.displayName()
                        + "' on <Style>. Allowed: x:Key, TargetType, BasedOn.");
            }
        }
        if (key == null || key.isEmpty()) throw fail.apply(entry.location(), "<Style> requires x:Key.");
        List<Setter> setters = new ArrayList<>();
        for (UiNode child : entry.children()) {
            if (!child.type().local().equals("Setter")) {
                throw fail.apply(child.location(), "<Style> can hold <Setter>, got <" + child.type().qualified() + ">.");
            }
            String name = null;
            UiValue value = null;
            for (UiProperty property : child.properties()) {
                if (property.owner() != null || !property.name().namespace().isEmpty()) {
                    throw fail.apply(property.location(), "<Setter> only takes Property and Value.");
                }
                switch (property.name().local()) {
                    case "Property" -> name = literal(property, fail);
                    case "Value" -> value = property.value();
                    default -> throw fail.apply(property.location(), "Unknown attribute '" + property.displayName()
                            + "' on <Setter>. Allowed: Property, Value.");
                }
            }
            if (name == null || name.isEmpty()) throw fail.apply(child.location(), "<Setter> requires Property.");
            if (value == null || value instanceof UiValue.Elements) {
                throw fail.apply(child.location(), "<Setter Property=\"" + name + "\"> requires a Value attribute.");
            }
            setters.add(new Setter(name, value, child.location()));
        }
        return new StyleDef(key, target, basedOn, setters, entry.location());
    }

    private static String literal(UiProperty property, BiFunction<SourceLocation, String, RuntimeException> fail) {
        if (property.value() instanceof UiValue.Literal literal) return literal.text().trim();
        throw fail.apply(property.location(), "'" + property.displayName() + "' must be plain text.");
    }

    /**
     * Ключ базового стиля: простой текст {@code Base} либо {@code {StaticResource Base}}.
     */
    private static String reference(UiProperty property, BiFunction<SourceLocation, String, RuntimeException> fail) {
        if (property.value() instanceof UiValue.Literal literal) return literal.text().trim();
        if (property.value() instanceof UiValue.Extension extension && extension.name().local().equals("StaticResource")
                && extension.argument("ResourceKey", 0) instanceof UiValue.Literal key) {
            return key.text().trim();
        }
        throw fail.apply(property.location(), "BasedOn must be a style key or {StaticResource key}.");
    }

    /**
     * Ключ стиля из значения атрибута {@code Style}: простой текст либо {@code {StaticResource Key}}.
     *
     * @param value значение атрибута
     * @return ключ или {@code null}, если значение другого вида
     */
    public static String keyOf(UiValue value) {
        if (value instanceof UiValue.Literal literal) return literal.text().trim();
        if (value instanceof UiValue.Extension extension && extension.name().local().equals("StaticResource")
                && extension.argument("ResourceKey", 0) instanceof UiValue.Literal key) {
            return key.text().trim();
        }
        return null;
    }
}
