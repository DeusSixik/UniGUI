package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.math.MutableColor;

/**
 * Внутренние helper'ы цвета для пакета нод.
 */
final class NodeColors {
    private NodeColors() {
    }

    /**
     * Преобразует {@code 0xAARRGGBB} в изменяемый цвет с компонентами {@code 0..1}.
     */
    static MutableColor argb(int argb) {
        return new MutableColor(
                ((argb >>> 16) & 0xFF) / 255.0f,
                ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f);
    }
}
