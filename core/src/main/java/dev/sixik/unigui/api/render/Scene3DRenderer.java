package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;

/** Renders a backend-neutral 3D scene into an offscreen target with depth. */
@FunctionalInterface
@WikiPage(
        title = "Scene3DRenderer", slug = "scene-3d-renderer",
        category = "API/3D Scene", order = 190,
        summary = "Render backend contract for drawing a scene into a depth-enabled target.")
@WikiSection(title = "Backend integration", order = 10, markdown = """
        `RenderBackend.scene3D()` supplies this renderer. The target has depth and automatic 2D clearing is
        disabled. The 3D pass clears the entire target, draws into the viewport starting at its upper-left corner,
        then leaves the backend ready for UI commands. The default highlight overload delegates to the base pass.
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "Mesh3D", target = "page:mesh-3d")
@AiDoc(
        summary = "Render backend contract for drawing a scene into a depth-enabled target.",
        purpose = "Implement a 3D render pass that can be composed with the 2D widget pass.",
        criticalRules = {
                "Read the scene, camera, clear color and highlight synchronously.",
                "The camera uses logical canvas dimensions. Render viewport width and height are physical pixels within the target.",
                "Clear color and depth before the 3D pass and preserve the target for subsequent 2D composition.",
                "Support `Highlight` with the same scene depth when implementing hover rendering."
        },
        related = {"Scene3D", "Scene", "Camera3D", "ScenePlatform", "Mesh3D"})
public interface Scene3DRenderer {
    /**
     * Clears the entire target's color and depth, renders the scene and restores state for the following
     * 2D pass. The viewport starts at the target's top left. Read inputs synchronously.
     *
     * @param target target with depth and automatic 2D clearing disabled.
     * @param scene scene graph.
     * @param camera camera sized to the logical viewport.
     * @param viewportWidth physical viewport width in target pixels.
     * @param viewportHeight physical viewport height in target pixels.
     * @param clearColor clear color including alpha.
     * @param partialTick fraction of the current tick for animated content.
     */
    @WikiMethod(section = "Rendering", sectionOrder = 10, order = 10,
            summary = "Clears the entire target's color and depth, renders the scene and restores state for the following 2D pass. The viewport starts at the target's top left. Read inputs synchronously.")
    @AiDoc(section = "Rendering", sectionOrder = 10, order = 10,
            summary = "Clears the entire target's color and depth, renders the scene and restores state for the following 2D pass. The viewport starts at the target's top left. Read inputs synchronously.")
    void render(
            @WikiParameter(description = "target with depth and automatic 2D clearing disabled.") RenderTarget target,
            @WikiParameter(description = "scene graph.") Scene scene,
            @WikiParameter(description = "camera sized to the logical viewport.") Camera3D camera,
            @WikiParameter(description = "physical viewport width in target pixels.") float viewportWidth,
            @WikiParameter(description = "physical viewport height in target pixels.") float viewportHeight,
            @WikiParameter(description = "clear color including alpha.") ColorView clearColor,
            @WikiParameter(description = "fraction of the current tick for animated content.") float partialTick);

    /**
     * Renders the scene with an optional highlight against the same depth buffer.
     *
     * @param target target with depth and automatic 2D clearing disabled.
     * @param scene scene graph.
     * @param camera camera sized to the logical viewport.
     * @param viewportWidth physical viewport width in target pixels.
     * @param viewportHeight physical viewport height in target pixels.
     * @param clearColor clear color including alpha.
     * @param partialTick fraction of the current tick.
     * @param highlight viewport highlight geometry, or null.
     */
    @WikiMethod(section = "Rendering", sectionOrder = 10, order = 10,
            summary = "Renders the scene with an optional highlight against the same depth buffer.")
    @AiDoc(section = "Rendering", sectionOrder = 10, order = 10,
            summary = "Renders the scene with an optional highlight against the same depth buffer.")
    default void render(
            @WikiParameter(description = "target with depth and automatic 2D clearing disabled.") RenderTarget target,
            @WikiParameter(description = "scene graph.") Scene scene,
            @WikiParameter(description = "camera sized to the logical viewport.") Camera3D camera,
            @WikiParameter(description = "physical viewport width in target pixels.") float viewportWidth,
            @WikiParameter(description = "physical viewport height in target pixels.") float viewportHeight,
            @WikiParameter(description = "clear color including alpha.") ColorView clearColor,
            @WikiParameter(description = "fraction of the current tick.") float partialTick,
            @WikiParameter(description = "viewport highlight geometry, or null.") Highlight highlight) {
        render(target, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick);
    }

    /**
     * Highlight data for a single viewport. Read it synchronously during rendering.
     *
     * @param mesh geometry.
     * @param material surface settings.
     * @param world transform from highlight-local space to scene space.
     */
    record Highlight(Mesh3D mesh, Material material, Mat4View world) {}
}
