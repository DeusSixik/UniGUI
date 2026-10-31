package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Visual style applied to a tokenizer range in {@link CodeEditor}. */
@AiDoc(
        summary = "Visual styling descriptor for syntax tokens including foreground color.",
        purpose = "Carries visual attributes like text color for rendered syntax tokens.",
        useWhen = {
                "Defining color schemes and syntax themes for CodeTokenizer."
        },
        criticalRules = {
                "Uses ColorView for color representation."
        },
        related = {"CodeToken", "CodeTokenizer"})
@WikiPage(
        title = "<key:token.style.title>",
        slug = "token-style",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:token.style.summary>",
        order = 320)
@WikiSection(
        title = "<key:token.style.section.overview.title>",
        markdown = "<key:token.style.section.overview.body>",
        order = 10)
public record TokenStyle(ColorView color) {
    public static final TokenStyle NONE = new TokenStyle(null);

    public TokenStyle {
        color = color == null ? null : new SnapshotColor(color.r(), color.g(), color.b(), color.a());
    }

    public static TokenStyle color(ColorView color) {
        return new TokenStyle(color);
    }

    public boolean hasColor() {
        return color != null && color.a() > 0.0f;
    }

    private record SnapshotColor(float r, float g, float b, float a) implements ColorView {
    }
}