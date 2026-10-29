package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class HitTestResult {
    private final Widget widget;
    private final float rootX;
    private final float rootY;
    private final float localX;
    private final float localY;

    public HitTestResult(Widget widget, float rootX, float rootY, float localX, float localY) {
        this.widget = Objects.requireNonNull(widget, "widget");
        this.rootX = rootX;
        this.rootY = rootY;
        this.localX = localX;
        this.localY = localY;
    }

    public Widget widget() {
        return widget;
    }

    public float rootX() {
        return rootX;
    }

    public float rootY() {
        return rootY;
    }

    public float localX() {
        return localX;
    }

    public float localY() {
        return localY;
    }
}
