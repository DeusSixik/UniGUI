package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.RectView;

@FunctionalInterface
@AiDoc(summary = "Contract for floating or overlay widgets that automatically position and constrain themselves within host bounds.")
@WikiPage(
        title = "OverlayHostAware",
        slug = "overlay-host-aware",
        category = "<key:feedback.category>",
        summary = "<key:overlay.host.aware.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:overlay.host.aware.section.overview.body>",
        order = 10
)
public interface OverlayHostAware {
    @AiDoc(summary = "Positions and arranges this floating widget within the given host boundaries.")
    @WikiMethod(summary = "<key:overlay.host.aware.method.arrangeInHost>")
    void arrangeInHost(
            @WikiParameter(description = "<key:overlay.host.aware.param.hostBounds>")
            RectView hostBounds
    );
}
