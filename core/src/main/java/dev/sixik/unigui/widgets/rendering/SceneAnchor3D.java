package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.HitTestChildrenOnly;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.List;

/**
 * Размещает обычный UI-виджет в проекции мировой или локальной точки узла.
 */
public final class SceneAnchor3D extends WidgetBase implements HitTestChildrenOnly {
    /**
     * Якорь использует собственный желаемый размер дочернего виджета.
     */
    private static final LayoutContext UNBOUNDED = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

    /**
     * Прикреплённый UI-виджет.
     */
    private final Widget widget;
    /**
     * Дочерний список для layout и маршрутизации событий.
     */
    private List<Widget> children;
    /**
     * Узел, в чьих координатах задана точка, или null для мировой точки.
     */
    private final SceneNode node;
    /**
     * Точка привязки в единицах сцены или локальных единицах узла.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Буфер границ в логических единицах UI.
     */
    private final MutableRect projectedBounds = new MutableRect();
    /**
     * Горизонтальная привязка в долях ширины, от 0 до 1.
     */
    private float pivotX = 0.5f;
    /**
     * Вертикальная привязка в долях высоты, от 0 до 1.
     */
    private float pivotY = 0.5f;
    /**
     * Попала ли точка якоря в видимую область текущей сцены.
     */
    private boolean projected;

    /**
     * Привязывает свободный виджет к точке. Владелец добавляет якорь в дерево UI.
     *
     * @param widget дочерний виджет.
     * @param node   узел привязки или null для мировой точки.
     * @param point  точка в соответствующей системе координат.
     */
    SceneAnchor3D(Widget widget, SceneNode node, Vec3View point) {
        this.widget = widget;
        this.node = node;
        position(point.x(), point.y(), point.z());
        children = List.of(widget);
        if (widget instanceof WidgetBase base) base.setParentInternal(this);
    }

    /**
     * Читает прикреплённый виджет.
     *
     * @return виджет, переданный при создании якоря.
     */
    public Widget widget() {
        return widget;
    }

    /**
     * Читает узел привязки.
     *
     * @return узел или null для мировой точки.
     */
    public SceneNode node() {
        return node;
    }

    /**
     * Копирует точку якоря.
     *
     * @param out переиспользуемый вектор в единицах сцены или узла.
     * @return out.
     */
    public MutableVec3 position(MutableVec3 out) {
        return out.set(point);
    }

    /**
     * Задаёт точку привязки.
     *
     * @param x конечная координата X.
     * @param y конечная координата Y.
     * @param z конечная координата Z.
     * @return этот якорь.
     * @throws IllegalArgumentException если хотя бы одна координата не конечна.
     */
    public SceneAnchor3D position(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Anchor position must be finite");
        }
        if (point.x() != x || point.y() != y || point.z() != z) {
            point.set(x, y, z);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * Указывает, какая точка прямоугольника UI совпадает с проекцией.
     *
     * @param x доля ширины от левого края, от 0 до 1.
     * @param y доля высоты от верхнего края, от 0 до 1.
     * @return этот якорь.
     * @throws IllegalArgumentException если координата вне диапазона.
     */
    public SceneAnchor3D pivot(float x, float y) {
        if (!(x >= 0 && x <= 1 && y >= 0 && y <= 1)) throw new IllegalArgumentException("Invalid anchor pivot");
        if (pivotX != x || pivotY != y) {
            pivotX = x;
            pivotY = y;
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Widget> children() {
        return children;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Visibility visibility() {
        return projected ? super.visibility() : Visibility.HIDDEN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visible() {
        return projected && super.visible();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUiContextInternal(UIContext context) {
        super.setUiContextInternal(context);
        if (!children.isEmpty() && widget instanceof WidgetBase base) base.setUiContextInternal(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void measure(LayoutContext context) {
        widget.measure(UNBOUNDED);
        setDesiredSize(widget.desiredSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        widget.arrange(bounds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (widget.visible()) widget.tick(frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderContext context) {
        renderChildWithInheritedTransform(context, widget);
    }

    /**
     * Размещает виджет по проекции с учётом pivot.
     *
     * @param x горизонтальная позиция проекции в координатах раскладки.
     * @param y вертикальная позиция проекции в координатах раскладки.
     */
    void projectAt(float x, float y) {
        boolean needsLayout = InvalidationFlags.has(subtreeInvalidationFlags(), InvalidationFlags.LAYOUT);
        if (needsLayout || !projected) measure(UNBOUNDED);
        float width = desiredSize().width();
        float height = desiredSize().height();
        float left = x - width * pivotX;
        float top = y - height * pivotY;
        RectView previous = layoutBounds();
        if (!projected || needsLayout || previous.x() != left || previous.y() != top
                || previous.width() != width || previous.height() != height) {
            projected = true;
            arrange(projectedBounds.set(left, top, width, height));
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    /**
     * Скрывает якорь и его детей от отрисовки и hit-testing.
     */
    void hideProjection() {
        if (projected) {
            projected = false;
            invalidate(InvalidationFlags.VISUAL);
            clearInteraction();
        }
    }

    /**
     * Снимает наведение и фокус детей, когда якорь уходит с экрана.
     */
    void clearInteraction() {
        UIContext context = uiContext();
        if (context == null) return;
        if (containsWidget(context.hoverManager().hoveredWidget())) context.hoverManager().clearHover();
        if (containsWidget(context.focusManager().focusedWidget())) context.focusManager().clearFocus();
    }

    /**
     * Проверяет принадлежность виджета поддереву якоря.
     *
     * @param candidate виджет под курсором или в фокусе, может быть null.
     * @return true, если якорь входит в его цепочку родителей.
     */
    private boolean containsWidget(Widget candidate) {
        for (Widget current = candidate; current != null; current = current.parent()) {
            if (current == this) return true;
        }
        return false;
    }

    /**
     * Освобождает дочерний виджет при удалении якоря из сцены.
     */
    void releaseWidget() {
        children = List.of();
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
        projected = false;
    }
}
