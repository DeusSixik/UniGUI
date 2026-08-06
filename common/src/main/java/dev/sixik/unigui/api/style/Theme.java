package dev.sixik.unigui.api.style;

public interface Theme {
    String WILDCARD = "*";

    Theme EMPTY = widgetType -> Style.EMPTY;

    default long version() {
        return 0L;
    }

    Style styleFor(String widgetType);
}
