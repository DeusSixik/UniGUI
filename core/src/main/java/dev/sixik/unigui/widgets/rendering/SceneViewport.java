package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TexturePlacement;

/**
 * Maps coordinates between the logical canvas, physical texture and widget rectangle.
 */
final class SceneViewport {
    /**
     * Latest widget bounds in layout coordinates.
     */
    private final MutableRect bounds = new MutableRect();
    /**
     * Texture placement with the displayed UV range.
     */
    private TexturePlacement placement;
    /**
     * Texture fit mode.
     */
    private ImageFit fit;
    /**
     * Texture width in physical pixels.
     */
    private int width;
    /**
     * Texture height in physical pixels.
     */
    private int height;
    /**
     * Logical canvas width.
     */
    private float canvasWidth;
    /**
     * Logical canvas height.
     */
    private float canvasHeight;
    /**
     * 3D pass width in physical pixels.
     */
    private float passWidth;
    /**
     * 3D pass height in physical pixels.
     */
    private float passHeight;
    /**
     * Revision of the coordinate mapping.
     */
    private long revision;

    /**
     * Creates an uninitialized mapping for the first layout pass.
     */
    SceneViewport() {
    }

    /**
     * Updates texture placement and 3D pass dimensions from the render-to-texture configuration.
     *
     * @param rect widget bounds in layout coordinates.
     * @param fit texture fit mode.
     * @param size logical canvas and physical target dimensions.
     */
    void update(RectView rect, ImageFit fit, RenderToTexture.TargetSize size) {
        float nextPassWidth = Math.min(size.width(), size.logicalWidth() * size.coordinateScale());
        float nextPassHeight = Math.min(size.height(), size.logicalHeight() * size.coordinateScale());
        boolean placementChanged = placement == null || width != size.width() || height != size.height()
                || this.fit != fit || bounds.x() != rect.x() || bounds.y() != rect.y()
                || bounds.width() != rect.width() || bounds.height() != rect.height();
        if (!placementChanged && passWidth == nextPassWidth && passHeight == nextPassHeight
                && canvasWidth == size.logicalWidth() && canvasHeight == size.logicalHeight()) {
            return;
        }
        revision++;
        canvasWidth = size.logicalWidth();
        canvasHeight = size.logicalHeight();
        passWidth = nextPassWidth;
        passHeight = nextPassHeight;
        if (placementChanged) {
            width = size.width();
            height = size.height();
            bounds.set(rect);
            this.fit = fit;
            placement = TexturePlacement.fit(width, height, bounds, fit);
        }
    }

    /**
     * Maps a widget-local point to the canvas. Interpolates UVs within the placed texture and scales
     * each UV by its texture dimension divided by the corresponding 3D pass dimension.
     *
     * @param x widget-local X.
     * @param y widget-local Y.
     * @param out result in logical canvas units.
     * @return true for a point in the displayed canvas area.
     */
    boolean toCanvas(float x, float y, MutableVec2 out) {
        float nx = (x + bounds.x() - placement.x()) / placement.width();
        float ny = (y + bounds.y() - placement.y()) / placement.height();
        if (!(nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1)) return false;
        float u = (placement.u() + nx * placement.uWidth()) * width / passWidth;
        float v = (placement.v() + ny * placement.vHeight()) * height / passHeight;
        if (!(u >= 0 && u <= 1 && v >= 0 && v <= 1)) return false;
        out.set(u * canvasWidth, v * canvasHeight);
        return true;
    }

    /**
     * Maps a canvas point through UVs to layout coordinates.
     *
     * @param x canvas X.
     * @param y canvas Y.
     * @param out result in layout coordinates.
     * @return true for a point in the displayed canvas area.
     */
    boolean fromCanvas(float x, float y, MutableVec2 out) {
        if (!(x >= 0 && x <= canvasWidth && y >= 0 && y <= canvasHeight
                && placement.width() > 0 && placement.height() > 0)) {
            return false;
        }
        float nx = (x / canvasWidth * passWidth / width - placement.u()) / placement.uWidth();
        float ny = (y / canvasHeight * passHeight / height - placement.v()) / placement.vHeight();
        if (!(nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1)) return false;
        out.set(placement.x() + nx * placement.width(), placement.y() + ny * placement.height());
        return true;
    }

    /**
     * Converts horizontal UI displacement to canvas displacement.
     *
     * @param dx X displacement in UI units.
     * @return X displacement in canvas units.
     */
    float canvasDeltaX(float dx) {
        return dx * placement.uWidth() / placement.width() * width / passWidth * canvasWidth;
    }

    /**
     * Converts vertical UI displacement to canvas displacement.
     *
     * @param dy Y displacement in UI units.
     * @return Y displacement in canvas units.
     */
    float canvasDeltaY(float dy) {
        return dy * placement.vHeight() / placement.height() * height / passHeight * canvasHeight;
    }

    /**
     * Reads the placement and size revision.
     *
     * @return revision of the mapping between canvas and widget.
     */
    long revision() {
        return revision;
    }

    /**
     * Reads the logical canvas height.
     *
     * @return height in UI units.
     */
    float canvasHeight() {
        return canvasHeight;
    }
}
