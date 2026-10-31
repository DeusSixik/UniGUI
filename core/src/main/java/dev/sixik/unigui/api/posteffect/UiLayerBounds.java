package dev.sixik.unigui.api.posteffect;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Bounds UI-слоя, который можно отрисовать во временную текстуру и прогнать через PostEffect.
 *
 * <p>Координаты задаются в той же системе, в которой backend уже исполняет {@code DrawList}.
 * Core не хранит window/player/render-hook объектов и поэтому остаётся независимым от Minecraft.</p>
 *
 * @param x левая координата слоя
 * @param y верхняя координата слоя
 * @param width ширина слоя
 * @param height высота слоя
 * @param scale дополнительный масштаб target'а; {@code 1} означает один texel на одну UI-единицу
 */
@WikiPage(
        title = "UiLayerBounds",
        slug = "ui-layer-bounds",
        category = "<key:ui.layer.bounds.category>",
        summary = "<key:ui.layer.bounds.summary>",
        order = 80
)
@WikiSection(
        title = "<key:ui.layer.bounds.section.overview.title>",
        markdown = "<key:ui.layer.bounds.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:ui.layer.bounds.example.viewport.title>",
        code = "<key:ui.layer.bounds.example.viewport.code>",
        order = 20
)
@AiDoc(
        summary = "Record holding coordinates, dimensions, and resolution scale for an offscreen post-processing layer target.",
        purpose = "Provides normalized layer boundaries and calculates clamped offscreen texture pixel dimensions.",
        useWhen = {"Allocating intermediate render targets or calculating post-effect viewport UVs."},
        related = {"UiPostEffectRenderer", "UiPostEffectBackend"}
)
public record UiLayerBounds(float x, float y, float width, float height, float scale) {
    /** Создаёт bounds без дополнительного scale. */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.of.title>",
            summary = "<key:ui.layer.bounds.method.of.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.of.return>")
    public static UiLayerBounds of(
            @WikiParameter(description = "<key:ui.layer.bounds.method.of.x>") float x,
            @WikiParameter(description = "<key:ui.layer.bounds.method.of.y>") float y,
            @WikiParameter(description = "<key:ui.layer.bounds.method.of.width>") float width,
            @WikiParameter(description = "<key:ui.layer.bounds.method.of.height>") float height) {
        return new UiLayerBounds(x, y, width, height, 1.0f);
    }

    /** Создаёт bounds полного viewport'а. */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.viewport.title>",
            summary = "<key:ui.layer.bounds.method.viewport.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.viewport.return>")
    public static UiLayerBounds viewport(
            @WikiParameter(description = "<key:ui.layer.bounds.method.viewport.width>") float width,
            @WikiParameter(description = "<key:ui.layer.bounds.method.viewport.height>") float height) {
        return of(0.0f, 0.0f, width, height);
    }

    /** @return безопасная ширина не меньше {@code 1} */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.safe_width.title>",
            summary = "<key:ui.layer.bounds.method.safe_width.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.safe_width.return>")
    public float safeWidth() {
        return Math.max(1.0f, finite(width, 1.0f));
    }

    /** @return безопасная высота не меньше {@code 1} */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.safe_height.title>",
            summary = "<key:ui.layer.bounds.method.safe_height.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.safe_height.return>")
    public float safeHeight() {
        return Math.max(1.0f, finite(height, 1.0f));
    }

    /** @return безопасный scale не меньше {@code 0.01} */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.safe_scale.title>",
            summary = "<key:ui.layer.bounds.method.safe_scale.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.safe_scale.return>")
    public float safeScale() {
        return Math.max(0.01f, finite(scale, 1.0f));
    }

    /** @return ширина offscreen target'а в texel'ах */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.target_width.title>",
            summary = "<key:ui.layer.bounds.method.target_width.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.target_width.return>")
    public int targetWidth() {
        return Math.max(1, Math.round(safeWidth() * safeScale()));
    }

    /** @return высота offscreen target'а в texel'ах */
    @WikiMethod(
            title = "<key:ui.layer.bounds.method.target_height.title>",
            summary = "<key:ui.layer.bounds.method.target_height.summary>"
    )
    @WikiReturn("<key:ui.layer.bounds.method.target_height.return>")
    public int targetHeight() {
        return Math.max(1, Math.round(safeHeight() * safeScale()));
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
