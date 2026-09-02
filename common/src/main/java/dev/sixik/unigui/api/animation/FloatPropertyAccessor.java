package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.widget.Widget;

/** Primitive-accessor float-свойства без boxing на кадровом пути. */
public interface FloatPropertyAccessor extends PropertyAccessor<Float> {
    float getFloat(Widget widget);

    void setFloat(Widget widget, float value);

    @Override
    default Class<Float> valueType() { return Float.class; }

    @Override
    default Float get(Widget widget) { return getFloat(widget); }

    @Override
    default void set(Widget widget, Float value) {
        setFloat(widget, value == null ? 0.0f : value);
    }
}
