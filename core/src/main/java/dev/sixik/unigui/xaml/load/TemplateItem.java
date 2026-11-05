package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.xaml.schema.Template;

/**
 * Обёртка над виджетом, созданным из шаблона.
 *
 * <p>Контейнеры списков освобождают убранные из показа строки вызовом {@code dispose()}; обёртка использует это,
 * чтобы закрыть привязки экземпляра шаблона, иначе они жили бы дольше самой строки.</p>
 */
public final class TemplateItem extends PanelWidget {
    private final Template.Instance instance;

    /**
     * Оборачивает экземпляр шаблона.
     *
     * @param instance созданный экземпляр; его корень становится единственным ребёнком
     */
    public TemplateItem(Template.Instance instance) {
        this.instance = instance;
        addChild(instance.widget());
        applyQueuedMutations();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutSize measured = LayoutV3StackAdapter.measure(children(), context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), bounds, layoutStyle());
    }

    @Override
    public void dispose() {
        instance.close();
        super.dispose();
    }
}
