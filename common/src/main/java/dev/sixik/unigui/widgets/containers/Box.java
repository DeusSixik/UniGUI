package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.Surface;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

/**
 * Визуальный контейнер с фоном, текстурой, шейдером, рамкой, радиусом и дочерними виджетами.
 *
 * <p>Тонкий наследник {@link SurfaceWidget}: вся поверхность (фон, рамка, радиус,
 * theme/style pipeline) реализована в базовом классе через композируемый
 * {@link Surface}. Источник фона выбирается через {@link BackgroundKind} и может
 * переключаться стилем ({@code background.kind}), кодом или XML без смены
 * renderer-класса.</p>
 *
 * <p>Custom {@link WidgetRender} полностью заменяет стандартный визуал виджета.
 * Порядок отрисовки: custom renderer, затем поверхность, затем дети.</p>
 *
 * <pre>{@code
 * Box card = new Box()
 *         .backgroundVisible(true)
 *         .borderVisible(true)
 *         .radius(4.0f);
 * card.background().set(0.08f, 0.09f, 0.11f, 0.95f);
 * card.addChild(content);
 * }</pre>
 *
 * @see Surface
 * @see BackgroundKind
 * @see WidgetRender
 * @see SurfaceWidget
 */
@XmlWidgetName("Box")
public class Box extends SurfaceWidget<Box> {
    /** Style type id для StylePack selector/binding. */
    public static final String STYLE_TYPE = StyleIds.Widget.BOX;

    /** Style property id, используемые поверхностью Box. */
    public static final class StyleProperties {
        public static final String BACKGROUND_KIND = StyleIds.Key.BACKGROUND_KIND;
        public static final String BACKGROUND_COLOR = StyleIds.Key.BACKGROUND_COLOR;
        public static final String BACKGROUND_TEXTURE = StyleIds.Key.BACKGROUND_TEXTURE;
        public static final String BACKGROUND_TEXTURE_TINT = StyleIds.Key.BACKGROUND_TEXTURE_TINT;
        public static final String BACKGROUND_TEXTURE_FIT = StyleIds.Key.BACKGROUND_TEXTURE_FIT;
        public static final String BACKGROUND_SHADER = StyleIds.Key.BACKGROUND_SHADER;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;
        public static final String TRANSITION_DURATION = StyleIds.Key.TRANSITION_DURATION;
        public static final String TRANSITION_EASING = StyleIds.Key.TRANSITION_EASING;

        private StyleProperties() {
        }
    }

    /** Animation property id для Box и его визуальной подложки. */
    public static final class AnimationProperties {
        public static final String BACKGROUND_COLOR = StyleAnimationIds.Property.BACKGROUND_COLOR;
        public static final String BACKGROUND_TEXTURE_TINT = StyleAnimationIds.Property.BACKGROUND_TEXTURE_TINT;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.BOX;

        private AnimationProperties() {
        }
    }

    /**
     * Создаёт пустой box с композируемой поверхностью.
     */
    public Box() {
    }
}
