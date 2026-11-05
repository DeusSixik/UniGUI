package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawList;

import java.util.List;

@FunctionalInterface
public interface DrawBatcher {
    List<DrawBatch> batch(DrawList drawList);
}
