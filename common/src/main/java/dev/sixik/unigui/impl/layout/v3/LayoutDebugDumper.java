package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutOutput;
import dev.sixik.unigui.api.layout.v3.LayoutResult;

import java.util.Locale;

/** Stable text dump helper for Layout V3 snapshot-style tests. */
public final class LayoutDebugDumper {
    private LayoutDebugDumper() {
    }

    public static String dump(LayoutOutput output) {
        if (output == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (LayoutResult result : output.orderedResults()) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(result.id())
                    .append(' ')
                    .append(format(result.x()))
                    .append(',')
                    .append(format(result.y()))
                    .append(' ')
                    .append(format(result.width()))
                    .append('x')
                    .append(format(result.height()));
        }
        return builder.toString();
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
