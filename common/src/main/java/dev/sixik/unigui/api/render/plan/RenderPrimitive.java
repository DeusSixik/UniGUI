package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Инспектируемый декларативный draw primitive, созданный render plan builder'ом.
 *
 * <p>Каждый primitive знает, как записать себя в {@link DrawScope}, но остаётся обычными данными:
 * координаты, paint, texture placement, clip children и text block. Это делает StylePack-рендер
 * редактируемым и предсказуемым для tooling.</p>
 */
public sealed interface RenderPrimitive permits RenderPrimitive.Rect, RenderPrimitive.RoundedRect, RenderPrimitive.Circle, RenderPrimitive.Line, RenderPrimitive.Texture, RenderPrimitive.RichTextBlock, RenderPrimitive.Clip {
    /**
     * Записывает primitive в draw scope.
     *
     * @param draw draw scope текущего виджета
     */
    void render(DrawScope draw);

    record Rect(float x,
                float y,
                float width,
                float height,
                Paint paint) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || paint == null) return;
            draw.rect(x, y, width, height, paint);
        }
    }

    record RoundedRect(float x,
                       float y,
                       float width,
                       float height,
                       float radius,
                       Paint paint) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || paint == null) return;
            draw.roundedRect(x, y, width, height, radius, paint);
        }
    }

    record Circle(float x,
                  float y,
                  float width,
                  float height,
                  Paint paint) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || paint == null) return;
            draw.circle(x, y, width, height, paint);
        }
    }

    record Line(float x1,
                float y1,
                float x2,
                float y2,
                Paint paint) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || paint == null) return;
            draw.line(x1, y1, x2, y2, paint);
        }
    }

    record Texture(TextureHandle texture,
                   TexturePlacement placement,
                   float radius,
                   Paint paint) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || texture == null || placement == null || paint == null) return;
            draw.texture(texture, placement, radius, paint);
        }
    }

    record RichTextBlock(RichText text,
                         float x,
                         float y,
                         float width,
                         float height,
                         Paint paint,
                         float clipX,
                         float clipY,
                         float clipWidth,
                         float clipHeight,
                         boolean textClip) implements RenderPrimitive {
        @Override
        public void render(DrawScope draw) {
            if (draw == null || text == null || text.isEmpty() || paint == null) return;
            boolean clipped = clipWidth > 0.0f && clipHeight > 0.0f;
            if (clipped) {
                if (textClip) {
                    draw.pushTextClip(clipX, clipY, clipWidth, clipHeight);
                } else {
                    draw.pushClip(clipX, clipY, clipWidth, clipHeight);
                }
            }
            try {
                TextEngine.drawInline(draw, text, x, y, width, height, paint);
            } finally {
                if (clipped) draw.popClip();
            }
        }
    }

    record Clip(float x,
                float y,
                float width,
                float height,
                boolean textClip,
                List<RenderPrimitive> children) implements RenderPrimitive {
        public Clip {
            if (children == null || children.isEmpty()) {
                children = List.of();
            } else {
                List<RenderPrimitive> normalized = new ArrayList<>(children.size());
                for (RenderPrimitive child : children) {
                    if (child != null) normalized.add(child);
                }
                children = List.copyOf(normalized);
            }
        }

        @Override
        public void render(DrawScope draw) {
            if (draw == null || children.isEmpty()) return;
            boolean clipped = width > 0.0f && height > 0.0f;
            if (clipped) {
                if (textClip) {
                    draw.pushTextClip(x, y, width, height);
                } else {
                    draw.pushClip(x, y, width, height);
                }
            }
            try {
                for (RenderPrimitive child : children) {
                    child.render(draw);
                }
            } finally {
                if (clipped) draw.popClip();
            }
        }
    }
}
