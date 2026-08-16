package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

/** Именованный XML-command handler, который предоставляет Java code-behind. */
@FunctionalInterface
public interface XmlCommand {
    void execute(Widget source, Event event);
}
