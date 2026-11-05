package dev.sixik.unigui.xaml.bind;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Именованный XML-command handler, который предоставляет Java code-behind.
 *
 * <p>XML хранит только имя команды, например {@code onClick="save"}. Реальный Java callback
 * регистрируется в {@link XamlCommandRegistry}, а built-in XML glue вызывает его при событии.</p>
 */
@FunctionalInterface
public interface XamlCommand {
    /**
     * Выполняет команду для widget source и исходного события.
     *
     * @param source widget, который инициировал команду
     * @param event  событие UI, если оно доступно
     */
    void execute(Widget source, Event event);
}
