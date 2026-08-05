package dev.sixik.unigui.impl.debug;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.ProfileScopeSnapshot;
import dev.sixik.unigui.api.debug.ProfilerSnapshot;
import dev.sixik.unigui.api.debug.UiDebugSnapshot;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

import java.util.Locale;

public final class DebugOverlayRenderer {
    private static final MutableColor BACKGROUND = new MutableColor(0.0f, 0.0f, 0.0f, 0.62f);
    private static final MutableColor HEADER = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private static final MutableColor TEXT = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor WARN = new MutableColor(1.0f, 0.68f, 0.15f, 1.0f);

    private DebugOverlayRenderer() {
    }

    public static void render(RenderContext context, UIContext uiContext) {
        if (context == null || uiContext == null) return;
        if (!DebugFlags.has(uiContext.debugFlags(), DebugFlags.PROFILER_OVERLAY)) return;

        UiDebugSnapshot debug = uiContext.debugCounters().snapshot();
        ProfilerSnapshot profiler = uiContext.profiler().snapshot();
        int scopeLines = Math.min(5, profiler.scopes().size());
        float x = 4.0f;
        float y = 4.0f;
        float width = 276.0f;
        float height = 40.0f + scopeLines * 10.0f;

        context.rect(x, y, width, height, Paint.fill(BACKGROUND));
        line(context, "UniGUI frame " + debug.frameIndex(), x, y, 0, HEADER);
        line(context, "draw=" + debug.drawCommandCount() + " batches=" + debug.batchCount(), x, y, 1, TEXT);
        line(context, "cache hit=" + debug.textureCacheHits() + " miss=" + debug.textureCacheMisses()
                + " tex=" + debug.textureRenders() + " last=" + debug.lastTextureCacheMissReason(), x, y, 2,
                debug.textureCacheMisses() > 0L ? WARN : TEXT);

        int line = 3;
        for (ProfileScopeSnapshot scope : profiler.scopes()) {
            if (line >= 3 + scopeLines) break;
            line(context, scope.name() + " " + formatMillis(scope.totalMillis()) + "ms x" + scope.calls(), x, y, line, TEXT);
            line++;
        }
    }

    private static void line(RenderContext context, String text, float x, float y, int line, MutableColor color) {
        context.text(text, x + 5.0f, y + 5.0f + line * 10.0f, 266.0f, 9.0f, Paint.fill(color));
    }

    private static String formatMillis(float millis) {
        return String.format(Locale.ROOT, "%.2f", millis);
    }
}
