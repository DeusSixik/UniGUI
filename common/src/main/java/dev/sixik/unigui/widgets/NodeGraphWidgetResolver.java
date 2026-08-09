package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.widget.Widget;

@FunctionalInterface
public interface NodeGraphWidgetResolver {
    Widget resolve(String itemId, String contentType);
}

