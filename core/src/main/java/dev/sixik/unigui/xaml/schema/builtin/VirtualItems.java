package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.xaml.bind.XamlObservableList;
import dev.sixik.unigui.xaml.load.ItemExtension;
import dev.sixik.unigui.xaml.load.TemplateItem;
import dev.sixik.unigui.xaml.schema.Template;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;

/**
 * Связывает {@link VirtualListView} со списком данных и шаблоном строки.
 *
 * <p>Виджет сам создаёт строки только для видимого диапазона, поэтому контроллер лишь сообщает ему число
 * элементов и фабрику строк; для каждой строки шаблон создаётся заново с подстановкой {@code {Item}} её элемента.
 * Когда {@link XamlObservableList} меняется, число элементов обновляется, а показанные строки пересоздаются.</p>
 */
final class VirtualItems {
    private static final Map<VirtualListView, VirtualItems> BY_VIEW =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Слабая ссылка: иначе значение в {@link WeakHashMap} удерживало бы свой же ключ и запись не исчезала бы.
     */
    private final java.lang.ref.WeakReference<VirtualListView> viewRef;
    private List<?> source;
    private Template template;
    private EventSubscription subscription;

    private VirtualItems(VirtualListView view) {
        this.viewRef = new java.lang.ref.WeakReference<>(view);
    }

    static VirtualItems of(VirtualListView view) {
        return BY_VIEW.computeIfAbsent(view, VirtualItems::new);
    }

    /**
     * @return текущий список данных или {@code null}
     */
    List<?> source() {
        return source;
    }

    /**
     * @return текущий шаблон или {@code null}
     */
    Template template() {
        return template;
    }

    void source(List<?> next) {
        if (subscription != null) subscription.unsubscribe();
        subscription = null;
        source = next;
        if (next instanceof XamlObservableList<?> observable) subscription = observable.onChanged(this::changed);
        rebuild();
    }

    void template(Template next) {
        template = next;
        rebuild();
    }

    private void rebuild() {
        VirtualListView view = viewRef.get();
        if (view == null) return;
        if (source == null || template == null) {
            view.itemCount(0);
            return;
        }
        view.itemCount(source.size());
        view.itemFactory(this::create);
    }

    private void changed(XamlObservableList.Change change) {
        VirtualListView view = viewRef.get();
        if (view == null) {
            // Виджет уже собран сборщиком мусора: слушатель в долгоживущем списке больше не нужен.
            if (subscription != null) subscription.unsubscribe();
            return;
        }
        if (template == null || source == null) return;
        boolean appendOnly = change.kind() == XamlObservableList.Kind.ADDED
                && change.index() + change.count() == source.size();
        if (appendOnly) {
            view.itemCount(source.size());
        } else {
            rebuild();
        }
    }

    private Widget create(int index) {
        Object item = index >= 0 && index < source.size() ? source.get(index) : null;
        return new TemplateItem(template.instantiate(Map.of("Item", ItemExtension.forItem(item))));
    }
}
