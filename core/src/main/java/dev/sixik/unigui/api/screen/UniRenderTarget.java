package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Host rendering hook for a neutral UniGUI render layer.
 */
@WikiPage(
        title = "UniRenderTarget",
        slug = "uni-render-target",
        category = "<key:uni.render.target.category>",
        summary = "<key:uni.render.target.summary>",
        order = 50
)
@WikiSection(
        title = "<key:uni.render.target.section.overview.title>",
        markdown = "<key:uni.render.target.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:uni.render.target.example.inspect.title>",
        code = "<key:uni.render.target.example.inspect.code>",
        order = 20
)
@AiDoc(
        summary = "Enumerates host graphic target stages where a UniRenderLayer can be attached.",
        purpose = "Decouples render layer hooks between in-game HUD, screen overlays, and loading screens.",
        useWhen = {"Registering render layers to specific game rendering pipeline stages."},
        related = {"UniHudRender", "UniScreenOverlayRender", "UniRenderLayer"}
)
public enum UniRenderTarget {
    HUD,
    SCREEN_OVERLAY,
    LOADING_SCREEN
}
