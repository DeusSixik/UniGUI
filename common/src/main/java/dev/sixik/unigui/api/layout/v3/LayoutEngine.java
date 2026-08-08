package dev.sixik.unigui.api.layout.v3;

/**
 * Stable Layout V3 backend contract.
 *
 * <p>The public UniGUI API should depend on this abstraction, not on a concrete
 * Yoga/Taffy binding. That keeps the layout backend replaceable while widgets
 * continue to speak in UniGUI's own layout model.</p>
 */
public interface LayoutEngine {
    LayoutOutput compute(LayoutNode root, LayoutInput input);
}
