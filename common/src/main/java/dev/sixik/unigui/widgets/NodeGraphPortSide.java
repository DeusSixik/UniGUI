package dev.sixik.unigui.widgets;

/**
 * Visual placement edge for a node-graph port.
 *
 * <p>PortSide describes where the port is rendered and hit-tested on the node
 * bounds. It does not define input/output connection direction; use
 * {@link NodeGraphPortKind} for that.</p>
 */
public enum NodeGraphPortSide {
    /** Render the port on the left edge. */
    LEFT,

    /** Render the port on the right edge. */
    RIGHT,

    /** Render the port on the top edge. */
    TOP,

    /** Render the port on the bottom edge. */
    BOTTOM
}