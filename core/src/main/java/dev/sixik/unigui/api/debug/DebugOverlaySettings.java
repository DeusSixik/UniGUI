package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Mutable settings for the built-in performance overlay.",
        purpose = "Controls overlay placement, UI scale, chart history length, and viewport margin through `UIContext.debugOverlaySettings()`.",
        useWhen = {
                "Customize the built-in debug overlay without replacing its renderer.",
                "Increase or reduce the retained frame history used by overlay charts."
        },
        criticalRules = {
                "`scale` is clamped to 0.25..4.0 and non-finite values reset to the default.",
                "`sampleWindow` is clamped to 1..10,000; larger windows retain more frame samples.",
                "A null anchor falls back to `TOP_LEFT`, and margin cannot be negative."
        },
        related = {"UIContext", "DebugOverlayAnchor", "UiDebugSnapshot", "UiRenderSnapshot"},
        examples = @AiDoc.Example(
                title = "Move and resize the overlay",
                code = """
                        context.debugOverlaySettings()
                                .anchor(DebugOverlayAnchor.TOP_RIGHT)
                                .scale(1.0f)
                                .sampleWindow(240)
                                .margin(8.0f);
                        """))
@WikiPage(
        title = "DebugOverlaySettings",
        slug = "debug-overlay-settings",
        category = "<key:debug.overlay.settings.category>",
        summary = "<key:debug.overlay.settings.summary>",
        order = 10)
@WikiSection(
        title = "<key:debug.overlay.settings.section.overview.title>",
        markdown = "<key:debug.overlay.settings.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:debug.overlay.settings.section.hotkeys.title>",
        markdown = "<key:debug.overlay.settings.section.hotkeys.body>",
        order = 20)
@WikiExample(
        title = "<key:debug.overlay.settings.example.configure.title>",
        code = "<key:debug.overlay.settings.example.configure.code>",
        order = 10)
public final class DebugOverlaySettings {
    public static final int DEFAULT_SAMPLE_WINDOW = 100;
    public static final int MAX_SAMPLE_WINDOW = 10_000;
    public static final float DEFAULT_SCALE = 0.75f;

    private DebugOverlayAnchor anchor = DebugOverlayAnchor.TOP_LEFT;
    private float scale = DEFAULT_SCALE;
    private int sampleWindow = DEFAULT_SAMPLE_WINDOW;
    private float margin = 4.0f;

    public DebugOverlayAnchor anchor() {
        return anchor;
    }

    @AiDoc(section = "Placement", sectionOrder = 10, order = 10,
            summary = "Sets the viewport corner used to anchor the overlay; null selects `TOP_LEFT`.")
    @WikiMethod(title = "anchor(DebugOverlayAnchor)",
            summary = "<key:debug.overlay.settings.method.anchor.summary>",
            section = "<key:debug.overlay.settings.api.section.configuration>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:debug.overlay.settings.method.fluent.return>")
    public DebugOverlaySettings anchor(
            @WikiParameter(name = "anchor", description = "<key:debug.overlay.settings.method.anchor.value>") DebugOverlayAnchor anchor) {
        this.anchor = anchor == null ? DebugOverlayAnchor.TOP_LEFT : anchor;
        return this;
    }

    public float scale() {
        return scale;
    }

    @AiDoc(section = "Appearance", sectionOrder = 20, order = 10,
            summary = "Sets overlay scale, clamped to the inclusive range 0.25..4.0.")
    @WikiMethod(title = "scale(float)",
            summary = "<key:debug.overlay.settings.method.scale.summary>",
            section = "<key:debug.overlay.settings.api.section.configuration>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:debug.overlay.settings.method.fluent.return>")
    public DebugOverlaySettings scale(
            @WikiParameter(name = "scale", description = "<key:debug.overlay.settings.method.scale.value>") float scale) {
        this.scale = Float.isFinite(scale) ? clamp(scale, 0.25f, 4.0f) : DEFAULT_SCALE;
        return this;
    }

    public int sampleWindow() {
        return sampleWindow;
    }

    @AiDoc(section = "Sampling", sectionOrder = 30, order = 10,
            summary = "Sets retained chart history in frames, clamped to 1..10,000.")
    @WikiMethod(title = "sampleWindow(int)",
            summary = "<key:debug.overlay.settings.method.sample_window.summary>",
            section = "<key:debug.overlay.settings.api.section.configuration>", sectionOrder = 10, order = 30)
    @WikiReturn("<key:debug.overlay.settings.method.fluent.return>")
    public DebugOverlaySettings sampleWindow(
            @WikiParameter(name = "sampleWindow", description = "<key:debug.overlay.settings.method.sample_window.value>") int sampleWindow) {
        this.sampleWindow = Math.max(1, Math.min(MAX_SAMPLE_WINDOW, sampleWindow));
        return this;
    }

    public float margin() {
        return margin;
    }

    @AiDoc(section = "Placement", sectionOrder = 10, order = 20,
            summary = "Sets non-negative distance from the selected viewport corner in UI pixels.")
    @WikiMethod(title = "margin(float)",
            summary = "<key:debug.overlay.settings.method.margin.summary>",
            section = "<key:debug.overlay.settings.api.section.configuration>", sectionOrder = 10, order = 40)
    @WikiReturn("<key:debug.overlay.settings.method.fluent.return>")
    public DebugOverlaySettings margin(
            @WikiParameter(name = "margin", description = "<key:debug.overlay.settings.method.margin.value>") float margin) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 4.0f;
        return this;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
