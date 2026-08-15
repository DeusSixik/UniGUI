package dev.sixik.unigui.widgets.graph;



/**
 * Connection direction for a node-graph port.
 *
 * <p>PortKind controls connection validation: {@link #OUTPUT} and
 * {@link #BIDIRECTIONAL} can start connection drags, while {@link #INPUT} and
 * {@link #BIDIRECTIONAL} can receive them. It is not a Blueprint-style
 * EXEC/DATA category; use {@link NodeGraphPort#type()} today, or introduce a
 * separate data/flow enum if the node model needs that later.</p>
 */
public enum NodeGraphPortKind {
    /** Receives connections. */
    INPUT,

    /** Starts outgoing connections. */
    OUTPUT,

    /** Can both start and receive connections. */
    BIDIRECTIONAL
}