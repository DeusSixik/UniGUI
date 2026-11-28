package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
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
 * Positions a UI widget at the projection of a world-space or node-local point.
 */
@WikiPage(
        title = "SceneAnchor3D", slug = "scene-anchor-3d",
        category = "API/3D Scene", order = 70,
        summary = "Projects a world-space or node-local point to an ordinary UI widget.")
@WikiSection(title = "Position and pivot", order = 10, markdown = """
        Use a world point for a fixed label or a node-local point to follow scene transforms. `(0.5, 1)` aligns the
        bottom center of the widget to the projected point. Remove the anchor with `Scene3D.removeAnchor` to detach
        its widget for reuse.
        """)
@WikiSection(title = "<key:scene.anchor.section.timeline.title>", order = 20,
        markdown = "<key:scene.anchor.section.timeline.body>")
@WikiExample(title = "Place a label above an object", code = """
        SceneAnchor3D badge = viewport.addAnchor(new Label("Furnace"), furnace,
                new MutableVec3(0.5f, 1.2f, 0.5f)).pivot(0.5f, 1);
        badge.position(0.5f, 1.4f, 0.5f);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@AiDoc(
        summary = "Projects a world-space or node-local point to an ordinary UI widget.",
        purpose = "Attach labels and controls to points that move with a rendered scene.",
        criticalRules = {
                "Create anchors with `Scene3D.addAnchor` and pass a detached widget.",
                "The anchor follows node and ancestor transforms and visibility. A projection outside the viewport hides the widget.",
                "Anchor placement uses camera projection, not scene occlusion.",
                "Pivot is a fraction of the child width and height measured from its top left.",
                "Use `timeline.trackAnchor(anchor)` for seekable position, screen offset and visibility keys. Screen offset is measured in logical UI units after projection, with positive X to the right and positive Y down."
        },
        related = {"Scene3D", "SceneNode", "Camera3D"},
        examples = @AiDoc.Example(title = "Place a label above an object", code = """
                SceneAnchor3D badge = viewport.addAnchor(new Label("Furnace"), furnace,
                        new MutableVec3(0.5f, 1.2f, 0.5f)).pivot(0.5f, 1);
                badge.position(0.5f, 1.4f, 0.5f);
                """))
public final class SceneAnchor3D extends WidgetBase implements HitTestChildrenOnly {
    /**
     * Layout constraints for measuring the anchored widget at its own desired size.
     */
    private static final LayoutContext UNBOUNDED = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

    /**
     * Attached UI widget.
     */
    private final Widget widget;
    /**
     * Child list for layout and event routing.
     */
    private List<Widget> children;
    /**
     * Node defining the point's coordinate space, or null for world space.
     */
    private final SceneNode node;
    /**
     * Anchor point in scene or node-local units.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Reusable bounds in logical UI units.
     */
    private final MutableRect projectedBounds = new MutableRect();
    /**
     * Horizontal pivot as a fraction of width, from zero to one.
     */
    private float pivotX = 0.5f;
    /**
     * Vertical pivot as a fraction of height, from zero to one.
     */
    private float pivotY = 0.5f;
    /**
     * Horizontal displacement after projection, in logical UI units.
     */
    private float offsetX;
    /**
     * Vertical displacement after projection, in logical UI units.
     */
    private float offsetY;
    /**
     * Whether the anchor projects into the visible viewport.
     */
    private boolean projected;

    /**
     * Attaches a detached widget to a point. The owning viewport adds the anchor to the UI tree.
     *
     * @param widget detached UI widget.
     * @param node   anchoring node, or null for world space.
     * @param point  world-space or node-local position.
     */
    SceneAnchor3D(Widget widget, SceneNode node, Vec3View point) {
        this.widget = widget;
        this.node = node;
        position(point.x(), point.y(), point.z());
        children = List.of(widget);
        if (widget instanceof WidgetBase base) base.setParentInternal(this);
    }

    /**
     * Reads the anchored widget.
     *
     * @return the widget supplied when the anchor was created.
     */
    @WikiMethod(section = "Anchor", sectionOrder = 10, order = 10,
            summary = "Reads the anchored widget.")
    @WikiReturn("the widget supplied when the anchor was created")
    @AiDoc(section = "Anchor", sectionOrder = 10, order = 10,
            summary = "Reads the anchored widget.")
    public Widget widget() {
        return widget;
    }

    /**
     * Reads the anchoring node.
     *
     * @return the node, or null for a world-space point.
     */
    @WikiMethod(section = "Anchor", sectionOrder = 10, order = 20,
            summary = "Reads the anchoring node.")
    @WikiReturn("the node, or null for a world-space point")
    @AiDoc(section = "Anchor", sectionOrder = 10, order = 20,
            summary = "Reads the anchoring node.")
    public SceneNode node() {
        return node;
    }

    /**
     * Copies the anchor point.
     *
     * @param out reusable vector in scene or node-local units.
     * @return out.
     */
    @WikiMethod(section = "Anchor", sectionOrder = 10, order = 30,
            summary = "Copies the anchor point.")
    @WikiReturn("out")
    @AiDoc(section = "Anchor", sectionOrder = 10, order = 30,
            summary = "Copies the anchor point.")
    public MutableVec3 position(
            @WikiParameter(description = "reusable vector in scene or node-local units.") MutableVec3 out) {
        return out.set(point);
    }

    /**
     * Moves the anchor in its existing coordinate space.
     *
     * @param x finite X in scene or node-local units.
     * @param y finite Y in scene or node-local units.
     * @param z finite Z in scene or node-local units.
     * @return this anchor.
     * @throws IllegalArgumentException if a coordinate is nonfinite.
     */
    @WikiMethod(section = "Anchor", sectionOrder = 10, order = 30,
            summary = "Moves the anchor in its existing coordinate space.")
    @WikiReturn("this anchor")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a coordinate is nonfinite.")
    @AiDoc(section = "Anchor", sectionOrder = 10, order = 30,
            summary = "Moves the anchor in its existing coordinate space.")
    public SceneAnchor3D position(
            @WikiParameter(description = "finite X in scene or node-local units.") float x,
            @WikiParameter(description = "finite Y in scene or node-local units.") float y,
            @WikiParameter(description = "finite Z in scene or node-local units.") float z) {
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
     * Sets which point of the UI rectangle aligns with the projection.
     *
     * @param x fraction of the width from the left edge, from zero to one.
     * @param y fraction of the height from the top edge, from zero to one.
     * @return this anchor.
     * @throws IllegalArgumentException if either fraction is outside [0, 1].
     */
    @WikiMethod(section = "Anchor", sectionOrder = 10, order = 40,
            summary = "Sets which point of the UI rectangle aligns with the projection.")
    @WikiReturn("this anchor")
    @WikiThrows(type = IllegalArgumentException.class, description = "if either fraction is outside [0, 1].")
    @AiDoc(section = "Anchor", sectionOrder = 10, order = 40,
            summary = "Sets which point of the UI rectangle aligns with the projection.")
    public SceneAnchor3D pivot(
            @WikiParameter(description = "fraction of the width from the left edge, from zero to one.") float x,
            @WikiParameter(description = "fraction of the height from the top edge, from zero to one.") float y) {
        if (!(x >= 0 && x <= 1 && y >= 0 && y <= 1)) throw new IllegalArgumentException("Invalid anchor pivot");
        if (pivotX != x || pivotY != y) {
            pivotX = x;
            pivotY = y;
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * Sets a screen-space displacement from the projected point.
     *
     * @param x finite horizontal offset in logical UI units, positive to the right
     * @param y finite vertical offset in logical UI units, positive down
     * @return this anchor
     * @throws IllegalArgumentException if either offset is not finite
     */
    public SceneAnchor3D screenOffset(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y))
            throw new IllegalArgumentException("Anchor offset must be finite");
        if (offsetX != x || offsetY != y) {
            offsetX = x;
            offsetY = y;
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
    public SceneAnchor3D visibility(Visibility visibility) {
        boolean wasVisible = visible();
        super.visibility(visibility);
        if (wasVisible && !visible()) clearInteraction();
        return this;
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
     * Places the widget at projection plus screen offset minus the pivot fraction times widget size,
     * independently on each axis in logical UI units.
     *
     * @param x projected horizontal position in layout coordinates.
     * @param y projected vertical position in layout coordinates.
     */
    void projectAt(float x, float y) {
        boolean needsLayout = InvalidationFlags.has(subtreeInvalidationFlags(), InvalidationFlags.LAYOUT);
        if (needsLayout || !projected) measure(UNBOUNDED);
        float width = desiredSize().width();
        float height = desiredSize().height();
        float left = x - width * pivotX + offsetX;
        float top = y - height * pivotY + offsetY;
        RectView previous = layoutBounds();
        if (!projected || needsLayout || previous.x() != left || previous.y() != top
                || previous.width() != width || previous.height() != height) {
            projected = true;
            arrange(projectedBounds.set(left, top, width, height));
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    /**
     * Hides the anchor and its children from rendering and hit testing.
     */
    void hideProjection() {
        if (projected) {
            projected = false;
            invalidate(InvalidationFlags.VISUAL);
            clearInteraction();
        }
    }

    /**
     * Clears child focus and hover when the anchor leaves the viewport.
     */
    void clearInteraction() {
        UIContext context = uiContext();
        if (context == null) return;
        if (containsWidget(context.hoverManager().hoveredWidget())) context.hoverManager().clearHover();
        if (containsWidget(context.focusManager().focusedWidget())) context.focusManager().clearFocus();
    }

    /**
     * Checks whether a widget belongs to the anchor subtree.
     *
     * @param candidate widget under the pointer or in focus, or null.
     * @return true if the anchor is in its parent chain.
     */
    private boolean containsWidget(Widget candidate) {
        for (Widget current = candidate; current != null; current = current.parent()) {
            if (current == this) return true;
        }
        return false;
    }

    /**
     * Detaches the child widget when the anchor is removed.
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
