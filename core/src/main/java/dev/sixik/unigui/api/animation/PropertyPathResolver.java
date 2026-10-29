package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Locale;
import java.util.Objects;

/**
 * Реестр скомпилированных property path accessor'ов для storyboard.
 *
 * <p>Встроенные пути не используют reflection. Пользовательские виджеты могут зарегистрировать
 * собственный типизированный accessor через {@link #register(String, PropertyAccessor)}.</p>
 */
public final class PropertyPathResolver {
    private final Object2ObjectOpenHashMap<String, PropertyAccessor<?>> accessors =
            new Object2ObjectOpenHashMap<>();

    public PropertyPathResolver() {
        registerBuiltIns();
    }

    /** Создаёт resolver со стандартными путями UniGUI. */
    public static PropertyPathResolver builtIns() {
        return new PropertyPathResolver();
    }

    /** Регистрирует или заменяет accessor пути. */
    public <T> PropertyPathResolver register(String propertyPath, PropertyAccessor<T> accessor) {
        accessors.put(normalizeRequired(propertyPath), Objects.requireNonNull(accessor, "accessor"));
        return this;
    }

    /** Регистрирует primitive float-accessor. */
    public PropertyPathResolver registerFloat(String propertyPath, FloatPropertyAccessor accessor) {
        return register(propertyPath, accessor);
    }

    /** Возвращает accessor или {@code null}, если путь не зарегистрирован. */
    public PropertyAccessor<?> resolve(String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) return null;
        return accessors.get(normalize(propertyPath));
    }

    /** Возвращает обязательный accessor с проверкой типа. */
    public <T> PropertyAccessor<T> require(String propertyPath, Class<T> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        PropertyAccessor<?> accessor = resolve(propertyPath);
        if (accessor == null) {
            throw new IllegalArgumentException("Неизвестный storyboard property path: " + propertyPath);
        }
        if (!valueType.isAssignableFrom(accessor.valueType())) {
            throw new IllegalArgumentException("Property path '" + propertyPath + "' имеет тип "
                    + accessor.valueType().getName() + ", ожидался " + valueType.getName());
        }
        @SuppressWarnings("unchecked")
        PropertyAccessor<T> typed = (PropertyAccessor<T>) accessor;
        return typed;
    }

    /** Возвращает обязательный primitive float-accessor. */
    public FloatPropertyAccessor requireFloat(String propertyPath) {
        PropertyAccessor<?> accessor = resolve(propertyPath);
        if (accessor instanceof FloatPropertyAccessor floatAccessor) return floatAccessor;
        if (accessor == null) {
            throw new IllegalArgumentException("Неизвестный storyboard property path: " + propertyPath);
        }
        throw new IllegalArgumentException("Property path '" + propertyPath + "' не является float-свойством.");
    }

    private void registerBuiltIns() {
        FloatPropertyAccessor opacity = floatAccessor(
                widget -> widget instanceof WidgetBase base ? base.opacity() : 1.0f,
                (widget, value) -> requireWidgetBase(widget, "Opacity").opacity(value));
        registerFloat("Opacity", opacity);

        FloatPropertyAccessor positionX = floatAccessor(
                widget -> widget.transform().position().x(),
                (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()));
        FloatPropertyAccessor positionY = floatAccessor(
                widget -> widget.transform().position().y(),
                (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value));
        FloatPropertyAccessor scaleX = floatAccessor(
                widget -> widget.transform().scale().x(),
                (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()));
        FloatPropertyAccessor scaleY = floatAccessor(
                widget -> widget.transform().scale().y(),
                (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value));
        FloatPropertyAccessor rotation = floatAccessor(
                widget -> widget.transform().rotationDegrees(),
                (widget, value) -> widget.transform().setRotationDegrees(value));
        FloatPropertyAccessor pivotX = floatAccessor(
                widget -> widget.transform().pivot().x(),
                (widget, value) -> widget.transform().pivot().set(value, widget.transform().pivot().y()));
        FloatPropertyAccessor pivotY = floatAccessor(
                widget -> widget.transform().pivot().y(),
                (widget, value) -> widget.transform().pivot().set(widget.transform().pivot().x(), value));

        registerAliases(positionX, "RenderTransform.X", "RenderTransform.Position.X");
        registerAliases(positionY, "RenderTransform.Y", "RenderTransform.Position.Y");
        registerAliases(scaleX, "RenderTransform.ScaleX", "RenderTransform.Scale.X");
        registerAliases(scaleY, "RenderTransform.ScaleY", "RenderTransform.Scale.Y");
        registerAliases(rotation, "RenderTransform.Rotation", "RenderTransform.RotationDegrees");
        registerAliases(pivotX, "RenderTransform.PivotX", "RenderTransform.Pivot.X");
        registerAliases(pivotY, "RenderTransform.PivotY", "RenderTransform.Pivot.Y");
    }

    private void registerAliases(FloatPropertyAccessor accessor, String... aliases) {
        for (String alias : aliases) registerFloat(alias, accessor);
    }

    private static FloatPropertyAccessor floatAccessor(FloatGetter getter, FloatSetter setter) {
        return new FloatPropertyAccessor() {
            @Override
            public float getFloat(Widget widget) { return getter.get(widget); }

            @Override
            public void setFloat(Widget widget, float value) { setter.set(widget, value); }
        };
    }

    private static WidgetBase requireWidgetBase(Widget widget, String propertyPath) {
        if (widget instanceof WidgetBase base) return base;
        throw new IllegalArgumentException("Property path '" + propertyPath
                + "' поддерживается только WidgetBase: " + widget.getClass().getName());
    }

    private static String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) throw new IllegalArgumentException("propertyPath не должен быть пустым.");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface FloatGetter {
        float get(Widget widget);
    }

    @FunctionalInterface
    private interface FloatSetter {
        void set(Widget widget, float value);
    }
}
