package dev.sixik.unigui.xaml.component;

import dev.sixik.unigui.api.event.BaseEvent;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

/**
 * Исходящее событие компонента, как его видят команды ({@code Buy="{Command BuyItem}"}).
 */
public final class ComponentEvent extends BaseEvent {
    /** Тип события для общей шины. */
    public static final EventType<ComponentEvent> TYPE = EventType.create("component.event");

    private final Widget source;
    private final String name;
    private final Args args;

    /**
     * Создаёт событие.
     *
     * @param source виджет-хост компонента
     * @param name   имя события из контракта
     * @param args   аргументы
     */
    public ComponentEvent(Widget source, String name, Args args) {
        this.source = Objects.requireNonNull(source, "source");
        this.name = Objects.requireNonNull(name, "name");
        this.args = Objects.requireNonNull(args, "args");
    }

    @Override
    public EventType<ComponentEvent> type() {
        return TYPE;
    }

    /** @return хост компонента, который сообщил о событии */
    public Widget source() {
        return source;
    }

    /** @return имя события из контракта */
    public String name() {
        return name;
    }

    /** @return аргументы события */
    public Args args() {
        return args;
    }
}
