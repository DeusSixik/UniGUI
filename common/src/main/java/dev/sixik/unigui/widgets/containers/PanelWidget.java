package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.AbsoluteLayoutEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Базовый контейнер для виджетов с несколькими дочерними элементами.
 *
 * <p>{@code PanelWidget} отвечает за владение детьми: назначает parent,
 * прокидывает {@link UIContext}, вызывает {@code measure/arrange/tick/render}
 * и корректно освобождает дочерние виджеты при очистке. Мутации списка детей
 * ставятся в очередь, поэтому {@link #addChild(Widget)} и
 * {@link #removeChild(Widget)} безопасно вызывать во время обработки событий
 * или render/tick-прохода.</p>
 *
 * <p>Дефолтная раскладка ведёт себя как stack/overlay: все обычные дети
 * получают content-bounds контейнера с учётом margin/alignment, а absolute-дети
 * раскладываются через {@link AbsoluteLayoutEngine}. Специализированные
 * контейнеры переопределяют только measurement/arrange, сохраняя общий
 * механизм владения и рендера детей.</p>
 *
 * @see StackPanel
 * @see Box
 */
public class PanelWidget extends WidgetBase {
    private static final ThreadLocal<RectView> RENDER_CULL_BOUNDS = new ThreadLocal<>();

    private final List<Widget> children = new ObjectArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);
    /** Очередь отложенных изменений, чтобы не менять список детей во время обхода. */
    private final Queue<ChildMutation> mutations = new ConcurrentLinkedQueue<>();
    /** Кэшированный массив для горячих циклов tick/render/layout. */
    private Widget[] childSnapshot = new Widget[0];
    private boolean childSnapshotDirty = true;

    /**
     * Добавляет дочерний виджет в конец списка.
     *
     * <p>Фактическое добавление выполняется при ближайшем
     * {@link #applyQueuedMutations()}. Повторное добавление того же экземпляра
     * игнорируется.</p>
     *
     * @param child виджет для добавления; {@code null} игнорируется
     */
    public void addChild(Widget child) {
        if (child == null) return;
        mutations.add(new ChildMutation(ChildMutationType.ADD, child));
        invalidate(InvalidationFlags.LAYOUT);
    }

    /**
     * Вставляет дочерний виджет на заданную позицию.
     *
     * <p>Индекс зажимается в допустимый диапазон при применении queued mutation.</p>
     *
     * @param index желаемая позиция вставки
     * @param child виджет для вставки; {@code null} игнорируется
     */
    public void insertChild(int index, Widget child) {
        if (child == null) return;
        mutations.add(new ChildMutation(ChildMutationType.INSERT, child, index));
        invalidate(InvalidationFlags.LAYOUT);
    }

    /**
     * Удаляет дочерний виджет из контейнера.
     *
     * <p>При фактическом удалении у {@link WidgetBase}-ребёнка сбрасываются
     * parent и UI context. Если виджет не является ребёнком, операция не меняет
     * состояние контейнера.</p>
     *
     * @param child виджет для удаления; {@code null} игнорируется
     */
    public void removeChild(Widget child) {
        if (child == null) return;
        mutations.add(new ChildMutation(ChildMutationType.REMOVE, child));
        invalidate(InvalidationFlags.LAYOUT);
    }

    /**
     * Удаляет и dispose'ит всех детей контейнера.
     */
    public void clearChildren() {
        mutations.add(new ChildMutation(ChildMutationType.CLEAR, null));
        invalidate(InvalidationFlags.LAYOUT);
    }

    /**
     * Применяет все отложенные изменения списка детей.
     *
     * <p>Контейнеры вызывают этот метод перед layout/render/tick-проходами.
     * Если наследник напрямую зависит от актуального списка детей, он должен
     * вызвать этот метод перед чтением {@link #children()} или
     * {@link #childSnapshot()}.</p>
     */
    public void applyQueuedMutations() {
        ChildMutation mutation;
        while ((mutation = mutations.poll()) != null) {
            switch (mutation.type) {
                case ADD -> applyAdd(mutation.child);
                case INSERT -> applyInsert(mutation.index, mutation.child);
                case REMOVE -> applyRemove(mutation.child);
                case CLEAR -> applyClear();
            }
        }
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        for (Widget child : children) {
            if (child instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
            }
        }
    }

    /**
     * Возвращает неизменяемое live-представление дочерних виджетов.
     *
     * @return список детей в порядке layout/render обхода
     */
    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutContext childContext = AbsoluteLayoutEngine.contentContext(this, context);
        float desiredWidth = 0.0f;
        float desiredHeight = 0.0f;
        for (Widget child : childSnapshot()) {
            if (child.visibility() != Visibility.COLLAPSED) {
                child.measure(childContext);
                if (AbsoluteLayoutEngine.isAbsolute(child)) continue;
                EdgeInsets margin = child.layoutConstraints().margin();
                desiredWidth = Math.max(desiredWidth, child.desiredSize().width() + margin.horizontal());
                desiredHeight = Math.max(desiredHeight, child.desiredSize().height() + margin.vertical());
            }
        }
        EdgeInsets padding = layoutStyle().padding();
        setDesiredSize(resolveDesiredSize(context,
                desiredWidth + padding.horizontal(),
                desiredHeight + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        MutableRect contentBounds = AbsoluteLayoutEngine.contentBounds(this, bounds);
        for (Widget child : childSnapshot()) {
            if (child.visibility() != Visibility.COLLAPSED) {
                if (AbsoluteLayoutEngine.isAbsolute(child)) {
                    AbsoluteLayoutEngine.arrange(child, contentBounds);
                } else {
                    StackPanel.arrangeChild(child,
                            contentBounds.x(), contentBounds.y(),
                            contentBounds.width(), contentBounds.height());
                }
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            renderChildren(context);
        } finally {
            popOpacity(context);
        }
    }

    /**
     * Рендерит всех видимых детей с учётом overflow clipping.
     *
     * @param context текущий render context
     */
    protected void renderChildren(RenderContext context) {
        applyQueuedMutations();
        boolean clipsChildren = layoutStyle().overflowX() != Overflow.VISIBLE
                || layoutStyle().overflowY() != Overflow.VISIBLE;
        if (clipsChildren) {
            context.pushClip(
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height());
        }
        try {
            RectView cullBounds = RENDER_CULL_BOUNDS.get();
            for (Widget child : childSnapshot()) {
                if (cullBounds != null && !intersects(child.layoutBounds(), cullBounds)) continue;
                renderChildWithInheritedTransform(context, child);
            }
        } finally {
            if (clipsChildren) {
                context.popClip();
            }
        }
    }

    static RectView pushRenderCullBounds(RectView bounds) {
        RectView previous = RENDER_CULL_BOUNDS.get();
        RectView next = previous == null ? copyRect(bounds) : intersection(previous, bounds);
        RENDER_CULL_BOUNDS.set(next);
        return previous;
    }

    static void restoreRenderCullBounds(RectView previous) {
        if (previous == null) {
            RENDER_CULL_BOUNDS.remove();
        } else {
            RENDER_CULL_BOUNDS.set(previous);
        }
    }

    private static RectView copyRect(RectView bounds) {
        if (bounds == null) return new MutableRect();
        return new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static RectView intersection(RectView first, RectView second) {
        if (first == null) return copyRect(second);
        if (second == null) return copyRect(first);

        float left = Math.max(first.x(), second.x());
        float top = Math.max(first.y(), second.y());
        float right = Math.min(first.x() + first.width(), second.x() + second.width());
        float bottom = Math.min(first.y() + first.height(), second.y() + second.height());
        return new MutableRect(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }

    private static boolean intersects(RectView bounds, RectView cullBounds) {
        if (bounds == null || cullBounds == null) return true;
        return bounds.x() < cullBounds.x() + cullBounds.width()
                && bounds.x() + bounds.width() > cullBounds.x()
                && bounds.y() < cullBounds.y() + cullBounds.height()
                && bounds.y() + bounds.height() > cullBounds.y();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        applyQueuedMutations();
        for (Widget child : childSnapshot()) {
            if (child.visibility() == Visibility.VISIBLE) {
                child.tick(frame);
            }
        }
    }

    @Override
    public void dispose() {
        applyClear();
    }

    /**
     * Возвращает стабильный snapshot детей для горячих циклов.
     *
     * <p>Snapshot пересоздаётся только после изменения списка детей. Это
     * снижает количество iterator/allocation в tick/render/layout-проходах.</p>
     *
     * @return массив детей в текущем порядке обхода
     */
    protected final Widget[] childSnapshot() {
        if (childSnapshotDirty) {
            childSnapshot = children.toArray(new Widget[children.size()]);
            childSnapshotDirty = false;
        }
        return childSnapshot;
    }

    /**
     * Меняет порядок детей без смены parent/UI context.
     *
     * <p>Метод полезен для overlay-контейнеров, которым нужно сортировать детей
     * по z-index перед render-проходом.</p>
     *
     * @param comparator comparator нового порядка; {@code null} игнорируется
     */
    protected final void reorderChildren(Comparator<? super Widget> comparator) {
        if (comparator != null && children.size() > 1) {
            children.sort(comparator);
            childSnapshotDirty = true;
        }
    }

    private void applyAdd(Widget child) {
        if (children.contains(child)) return;
        children.add(child);
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void applyInsert(int index, Widget child) {
        if (children.contains(child)) return;
        children.add(Math.max(0, Math.min(index, children.size())), child);
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void applyRemove(Widget child) {
        if (!children.remove(child)) return;
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private void applyClear() {
        for (Widget child : children) {
            if (child instanceof WidgetBase base) {
                base.setParentInternal(null);
                base.setUiContextInternal(null);
            }
            child.dispose();
        }
        children.clear();
        childSnapshotDirty = true;
    }

    private enum ChildMutationType {
        ADD,
        INSERT,
        REMOVE,
        CLEAR
    }

    private static final class ChildMutation {
        private final ChildMutationType type;
        private final Widget child;
        private final int index;

        private ChildMutation(ChildMutationType type, Widget child) {
            this(type, child, -1);
        }

        private ChildMutation(ChildMutationType type, Widget child, int index) {
            this.type = type;
            this.child = child;
            this.index = index;
        }
    }
}
