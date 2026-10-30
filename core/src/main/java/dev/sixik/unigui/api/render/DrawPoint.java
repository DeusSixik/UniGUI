package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Immutable 2D-точка в логических UI-координатах.
 *
 * @param x X-координата
 * @param y Y-координата
 */
@AiDoc(
        summary = "Immutable 2D floating-point coordinate point used for polylines, ribbon paths, and vector geometry.",
        purpose = "Represents an unmodifiable point (x, y) in canvas/screen coordinates.",
        useWhen = {
                "Supplying polyline nodes to RibbonMesh.builder or DrawScope#ribbon.",
                "Defining points for line strips and polygon boundaries."
        },
        criticalRules = {
                "DrawPoint is an immutable record.",
                "Coordinates should be finite floating-point values."
        },
        related = {"RibbonMesh", "DrawVertex", "VectorPath"}
)
public record DrawPoint(float x, float y) {
}
