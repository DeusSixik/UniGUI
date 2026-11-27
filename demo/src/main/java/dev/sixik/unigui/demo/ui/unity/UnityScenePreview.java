package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * An isolated, orbitable 3D preview for Minecraft models and prefabs.
 */
final class UnityScenePreview extends LinearBox {
    /**
     * Shared horizontal and vertical half-angle for a square preview, in radians.
     */
    private static final double HALF_FOV = Math.toRadians(20);
    /**
     * Independent graph and camera used by the preview.
     */
    private final Scene3D viewport = new Scene3D(new Scene()
            .setAmbientLight(new AmbientLight(0xDBE8FF, 0.55f))
            .setDirectionalLight(new DirectionalLight(-0.5f, -0.8f, -0.35f, 0xFFF1DB, 0.8f)));
    /**
     * Identifies the current preview.
     */
    private final Label title = new Label();
    /**
     * Detached instance owned by the preview scene, or null when it is empty.
     */
    private UnitySceneObject previewRoot;
    /**
     * Bounds used to frame the visible geometry, or null when no geometry is available.
     */
    private Aabb bounds;
    /**
     * Width-to-height ratio of the last camera fit, or zero if still pending.
     */
    private float fittedAspect;

    /**
     * Creates an empty preview that fills the available space by default.
     */
    UnityScenePreview() {
        super(Orientation.VERTICAL);
        themeEnabled(false);
        background(UnityStyles.BG_TOOLBAR);
        layout(style -> style.widthPercent(100).height(0).flexGrow(1).flexShrink(1));
        addChild(UnityStyles.separatorLine());
        title.color(UnityStyles.TEXT_BOLD);
        title.overflowMode(TextOverflowMode.ELLIPSIS);
        title.layout(style -> style.widthPercent(100).height(23).padding(8, 0).flexShrink(0));
        addChild(title);
        viewport.clearColor().set(0.09f, 0.105f, 0.135f, 1);
        viewport.layout(style -> style.widthPercent(100).height(0).flexGrow(1));
        viewport.painter((context, width, height) -> {
            if (bounds == null) {
                String message = previewRoot == null ? "Select an element to preview" : "No visible geometry";
                context.addText(message, 8, 6, Math.max(0, width - 16), 18, UnityStyles.TEXT_MUTED);
            }
        });
        addChild(viewport);
    }

    /**
     * Replaces the preview contents and resets the framing for the new model.
     *
     * @param root    detached object owned by the preview, or null to clear it
     * @param caption title shown above the viewport
     */
    void setContent(UnitySceneObject root, String caption) {
        if (previewRoot != null) previewRoot.detach();
        previewRoot = root;
        fittedAspect = 0;
        if (root != null) viewport.scene().addRoot(root);
        bounds = root == null ? null : subtreeBounds(root);
        title.text(caption);
        fitToViewport();
        viewport.requestRender();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void arrange(RectView area) {
        super.arrange(area);
        fitToViewport();
    }

    /**
     * Fits the model after a content change or a change in viewport proportions.
     */
    private void fitToViewport() {
        RectView canvas = viewport.layoutBounds();
        if (bounds == null || canvas.width() <= 0 || canvas.height() <= 0) return;
        float aspect = canvas.width() / canvas.height();
        if (Math.abs(aspect - fittedAspect) < 0.01f) return;
        fittedAspect = aspect;
        fitCamera(aspect);
    }

    /**
     * Fits the box's enclosing sphere in the tighter camera dimension.
     * {@code radius = length(max - min) / 2} and {@code distance = 1.15 * radius / sin(halfAngle)}.
     * The horizontal half-angle is {@code atan(tan(HALF_FOV) * aspect)}.
     *
     * @param aspect width divided by height of the preview
     */
    private void fitCamera(float aspect) {
        double dx = (double) bounds.maxX() - bounds.minX();
        double dy = (double) bounds.maxY() - bounds.minY();
        double dz = (double) bounds.maxZ() - bounds.minZ();
        double radius = Math.max(0.1, Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.5);
        double limitingAngle = Math.min(HALF_FOV, Math.atan(Math.tan(HALF_FOV) * aspect));
        double distance = 1.15 * radius / Math.sin(limitingAngle);
        if (distance * 4 + radius > Float.MAX_VALUE) return;
        float near = (float) Math.max(0.001, Math.min(0.05, radius * 0.01));
        float far = (float) (distance * 4 + radius);
        viewport.camera().setPerspective((float) (2 * HALF_FOV), near, far);
        viewport.maxZoomDistance((float) (distance * 2));
        viewport.orbit().setOrbit(new MutableVec3((float) (bounds.minX() + dx * 0.5),
                        (float) (bounds.minY() + dy * 0.5), (float) (bounds.minZ() + dz * 0.5)),
                (float) distance, 0.65, 0.42);
        viewport.requestRender();
    }

    /**
     * Finds the visible geometry's bounds by transforming the eight corners of each local bounding box.
     *
     * @param object subtree root
     * @return bounds in world units, or null if the subtree has no visible geometry
     */
    static Aabb subtreeBounds(SceneNode object) {
        var minimum = new MutableVec3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        var maximum = new MutableVec3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        expandBounds(object, minimum, maximum, new MutableMat4(), new MutableVec3());
        return minimum.x() > maximum.x() ? null
                : new Aabb(minimum.x(), minimum.y(), minimum.z(), maximum.x(), maximum.y(), maximum.z());
    }

    /**
     * Adds the visible geometry of one node and its descendants to a world-space box.
     *
     * @param node    subtree to visit
     * @param minimum accumulated lower corner in scene units
     * @param maximum accumulated upper corner in scene units
     * @param matrix  reusable world matrix
     * @param point   reusable corner in scene units
     */
    private static void expandBounds(SceneNode node, MutableVec3 minimum, MutableVec3 maximum,
                                     MutableMat4 matrix, MutableVec3 point) {
        if (!node.visible()) return;
        Aabb local = node.bounds();
        if (local != null) {
            node.worldMatrix(matrix);
            for (int corner = 0; corner < 8; corner++) {
                point.set((corner & 1) == 0 ? local.minX() : local.maxX(),
                        (corner & 2) == 0 ? local.minY() : local.maxY(),
                        (corner & 4) == 0 ? local.minZ() : local.maxZ());
                if (!matrix.transformPoint(point, point)) continue;
                minimum.set(Math.min(minimum.x(), point.x()), Math.min(minimum.y(), point.y()), Math.min(minimum.z(), point.z()));
                maximum.set(Math.max(maximum.x(), point.x()), Math.max(maximum.y(), point.y()), Math.max(maximum.z(), point.z()));
            }
        }
        for (SceneNode child : node.children()) expandBounds(child, minimum, maximum, matrix, point);
    }
}
