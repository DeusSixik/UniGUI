package dev.sixik.unigui.api.text;

/**
 * Backend-independent font face handle.
 *
 * <p>A face describes font data and metrics. GPU atlas ownership belongs to a
 * renderer and must not be exposed through this contract.</p>
 */
public interface FontFace {
    String id();

    FontMetrics metrics(float pixelSize);

    float advance(int codePoint, float pixelSize);
}
