package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;

import java.util.Objects;

public record TexturePlacement(float x, float y, float width, float height,
                               float u, float v, float uWidth, float vHeight) {
    public static TexturePlacement fit(TextureHandle texture, RectView destination, ImageFit fit) {
        return fit(texture, new MutableRect(0.0f, 0.0f, 1.0f, 1.0f), destination, fit);
    }

    public static TexturePlacement fit(TextureHandle texture, RectView source,
                                       RectView destination, ImageFit fit) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;

        float x = destination.x();
        float y = destination.y();
        float width = Math.max(0.0f, destination.width());
        float height = Math.max(0.0f, destination.height());
        float u = source.x();
        float v = source.y();
        float uWidth = Math.max(0.0f, source.width());
        float vHeight = Math.max(0.0f, source.height());
        if (width <= 0.0f || height <= 0.0f || uWidth <= 0.0f || vHeight <= 0.0f) {
            return new TexturePlacement(x, y, 0.0f, 0.0f, u, v, 0.0f, 0.0f);
        }

        float sourceWidth = Math.max(1.0f, texture.width()) * uWidth;
        float sourceHeight = Math.max(1.0f, texture.height()) * vHeight;
        float sourceAspect = sourceWidth / sourceHeight;
        float destinationAspect = width / height;

        if (effectiveFit == ImageFit.CONTAIN) {
            if (sourceAspect > destinationAspect) {
                float fittedHeight = width / sourceAspect;
                y += (height - fittedHeight) * 0.5f;
                height = fittedHeight;
            } else {
                float fittedWidth = height * sourceAspect;
                x += (width - fittedWidth) * 0.5f;
                width = fittedWidth;
            }
        } else if (effectiveFit == ImageFit.COVER) {
            if (sourceAspect > destinationAspect) {
                float visibleWidth = uWidth * destinationAspect / sourceAspect;
                u += (uWidth - visibleWidth) * 0.5f;
                uWidth = visibleWidth;
            } else {
                float visibleHeight = vHeight * sourceAspect / destinationAspect;
                v += (vHeight - visibleHeight) * 0.5f;
                vHeight = visibleHeight;
            }
        }

        return new TexturePlacement(x, y, width, height, u, v, uWidth, vHeight);
    }
}
