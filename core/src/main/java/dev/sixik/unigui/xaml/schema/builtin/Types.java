package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.function.Supplier;

/**
 * Общие приёмы описания встроенных типов.
 */
final class Types {
    private Types() {
    }

    /**
     * Начинает описание встроенного типа в {@link Schema#UI_NAMESPACE}.
     *
     * @param name    имя в документе
     * @param type    runtime-класс
     * @param factory фабрика
     * @param <T>     тип виджета
     * @return билдер
     */
    static <T extends Widget> TypeInfo.Builder<T> type(String name, Class<T> type, Supplier<T> factory) {
        return TypeInfo.of(Schema.name(name), type, factory);
    }

    /**
     * Делает тип контейнером: прямые дети и слот {@code Children} добавляются через {@code addChild}, после чего
     * применяются отложенные мутации, чтобы дети были видны сразу после загрузки.
     */
    static <T extends PanelWidget> TypeInfo.Builder<T> panel(TypeInfo.Builder<T> builder) {
        ChildSink<T> children = (parent, child) -> {
            parent.addChild(child);
            parent.applyQueuedMutations();
        };
        return builder.content(children)
                .slot(SlotInfo.many("Children", children, "Child widgets hosted by this container."));
    }

    /**
     * Делает тип хостом одного содержимого: прямой ребёнок и слот {@code Content} идут через один и тот же
     * {@code sink}.
     */
    static <T extends Widget> TypeInfo.Builder<T> singleContent(TypeInfo.Builder<T> builder, ChildSink<T> sink,
                                                                String description) {
        return builder.content(sink).slot(SlotInfo.single("Content", sink, description));
    }
}
