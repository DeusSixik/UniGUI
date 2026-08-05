package dev.sixik.unigui.api.style;

public interface Theme {
    Theme EMPTY = widgetType -> Style.EMPTY;

    Style styleFor(String widgetType);
}
