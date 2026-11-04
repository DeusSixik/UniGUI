package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.load.MarkupExtension;

import java.util.Map;

/**
 * Шаблон: поддерево разметки, из которого загрузчик по требованию создаёт новые виджеты.
 *
 * <p>В отличие от обычного ребёнка, шаблон не превращается в виджет при загрузке документа. Он нужен там, где
 * виджетов должно быть много и заранее неизвестно сколько: строки списка, карточки каталога. Каждый экземпляр
 * получает собственные привязки, а закрытие экземпляра освобождает их.</p>
 */
public final class Template {
    /** Создаёт экземпляр шаблона; реализует загрузчик, потому что он знает схему, ресурсы и настройки. */
    @FunctionalInterface
    public interface Instantiator {
        /**
         * @param overrides расширения, которые на время этого экземпляра заменяют одноимённые (например, {@code Item})
         * @return новый экземпляр
         */
        Instance create(Map<String, MarkupExtension> overrides);
    }

    /**
     * Один созданный из шаблона виджет вместе с освобождением его привязок.
     *
     * @param widget корень созданного поддерева
     * @param closer освобождает привязки экземпляра
     */
    public record Instance(Widget widget, Runnable closer) implements AutoCloseable {
        @Override
        public void close() {
            closer.run();
        }
    }

    private final UiNode node;
    private final SourceLocation location;
    private final Instantiator instantiator;

    /**
     * Создаёт шаблон.
     *
     * @param node         поддерево разметки (не изменяется)
     * @param location     позиция слота в документе
     * @param instantiator как из поддерева делается виджет
     */
    public Template(UiNode node, SourceLocation location, Instantiator instantiator) {
        this.node = node;
        this.location = location == null ? SourceLocation.UNKNOWN : location;
        this.instantiator = instantiator;
    }

    /** @return поддерево разметки шаблона */
    public UiNode node() {
        return node;
    }

    /** @return позиция слота в документе */
    public SourceLocation location() {
        return location;
    }

    /**
     * Создаёт экземпляр.
     *
     * @param overrides расширения, заменяющие одноимённые на время этого экземпляра
     * @return экземпляр; вызывающий обязан закрыть его, когда виджет больше не нужен
     */
    public Instance instantiate(Map<String, MarkupExtension> overrides) {
        return instantiator.create(overrides);
    }
}
