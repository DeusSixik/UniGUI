package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.widget.Widget;

import java.util.Optional;

@FunctionalInterface
public interface HitTester {
    HitTester NONE = (root, rootX, rootY) -> Optional.empty();

    Optional<HitTestResult> hitTest(Widget root, float rootX, float rootY);
}
