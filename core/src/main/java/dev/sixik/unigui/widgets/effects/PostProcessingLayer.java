package dev.sixik.unigui.widgets.effects;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectBackend;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

/**
 * Контейнер, который применяет UI PostEffect только к своим дочерним виджетам.
 *
 * <p>Этот слой нужен для случаев, когда эффект должен затрагивать не весь screen, а только часть
 * дерева: например, фон панели с CRT/barrel shader, а кнопки и интерактивные элементы поверх остаются
 * обычными. Виджет остаётся обычным {@link PanelWidget}: layout, hit-test и input идут через детей,
 * а меняется только render-проход.</p>
 *
 * <p>На render-проходе дочерние команды собираются в отдельный {@link DrawList}. Для post-processing
 * они дополнительно переводятся из экранных координат в локальные координаты offscreen target'а слоя.
 * Если backend не поддерживает {@link UiPostEffectBackend}, слой отрисует исходный subtree без эффекта.</p>
 */
@AiDoc(
        summary = "Post-processing layer widget rendering child elements into an offscreen target and executing GPU shader passes such as blur, bloom, or CRT distortion."
)
@WikiPage(
        title = "PostProcessingLayer",
        slug = "post-processing-layer",
        category = "<key:effects.category>",
        summary = "<key:post.processing.layer.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:post.processing.layer.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:post.processing.layer.example.basic.title>",
        code = "PostProcessingLayer layer = new PostProcessingLayer();\nlayer.effect(\"unigui:bloom\");\nlayer.addChild(contentWidget);"
)
@WikiLink(
        label = "PanelWidget",
        target = "page:panel-widget",
        description = "<key:panel.widget.summary>"
)
@XmlWidgetName("PostProcessingLayer")
public class PostProcessingLayer extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.POST_PROCESSING_LAYER;

    private UiPostEffectChain postEffect = UiPostEffectChain.none();
    private float effectScale = 1.0f;

    /** @return текущая цепочка post-processing pass'ов */
        @AiDoc(summary = "Returns a copy of the active post-processing effect chain.")
    @WikiMethod(summary = "<key:post.processing.layer.method.postEffectGet>")
    @WikiReturn("<key:post.processing.layer.return.postEffect>")
    public UiPostEffectChain postEffect() {
        return postEffect.copy();
    }

    /**
     * Задаёт post-processing chain напрямую из кода.
     *
     * @param postEffect chain эффекта; {@code null} отключает эффект
     * @return этот слой для fluent-настройки
     */
        @AiDoc(summary = "Sets the post-processing effect chain applied to this layer's subtree.")
    @WikiMethod(summary = "<key:post.processing.layer.method.postEffectSet>")
    @WikiReturn("<key:post.processing.layer.return.this>")
    public PostProcessingLayer postEffect(
            @WikiParameter(description = "<key:post.processing.layer.param.postEffect>")
            UiPostEffectChain postEffect
    ) {
        UiPostEffectChain normalized = postEffect == null ? UiPostEffectChain.none() : postEffect.copy();
        this.postEffect = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** @return id зарегистрированного эффекта или пустая строка для inline/none chain */
        @AiDoc(summary = "Returns the effect identifier of the current post-processing chain, or empty string if none.")
    @WikiMethod(summary = "<key:post.processing.layer.method.effectGet>")
    @WikiReturn("<key:post.processing.layer.return.effectId>")
    public String effect() {
        String id = postEffect.effectId();
        return id == null ? "" : id;
    }

    /**
     * Задаёт post effect по id из {@link dev.sixik.unigui.api.posteffect.UiPostEffectRegistry}.
     *
     * <p>Пустая строка, {@code null} и {@code none} отключают эффект.</p>
     *
     * @param effectId id эффекта, например {@code unigui:barrel_distortion}
     * @return этот слой для fluent-настройки
     */
    @XmlAttribute(value = "effect", category = "Post Effect", defaultValue = "none",
            description = "Registered UI post-effect id applied to this layer only.")
        @AiDoc(summary = "Configures a registered post-processing effect by its identifier string.")
    @WikiMethod(summary = "<key:post.processing.layer.method.effectSet>")
    @WikiReturn("<key:post.processing.layer.return.this>")
    public PostProcessingLayer effect(
            @WikiParameter(description = "<key:post.processing.layer.param.effectId>")
            String effectId
    ) {
        return postEffect(UiPostEffectChain.of(effectId));
    }

    /** @return дополнительный scale offscreen target'а */
        @AiDoc(summary = "Returns the offscreen target rendering scale factor.")
    @WikiMethod(summary = "<key:post.processing.layer.method.effectScaleGet>")
    @WikiReturn("<key:post.processing.layer.return.effectScale>")
    public float effectScale() {
        return effectScale;
    }

    /**
     * Задаёт разрешение offscreen target'а относительно bounds слоя.
     *
     * <p>{@code 1.0} означает один texel на один UI-пиксель. Значения больше {@code 1.0} дают более
     * чёткий результат ценой производительности, меньше {@code 1.0} — дешевле, но мягче/пиксельнее.</p>
     *
     * @param effectScale scale target'а; некорректные значения заменяются на {@code 1.0}
     * @return этот слой для fluent-настройки
     */
    @XmlAttribute(value = "effectScale", category = "Post Effect", defaultValue = "1.0",
            description = "Offscreen target scale for the post-effect layer.")
        @AiDoc(summary = "Sets the offscreen render target scale factor relative to layer bounds.")
    @WikiMethod(summary = "<key:post.processing.layer.method.effectScaleSet>")
    @WikiReturn("<key:post.processing.layer.return.this>")
    public PostProcessingLayer effectScale(
            @WikiParameter(description = "<key:post.processing.layer.param.effectScale>")
            float effectScale
    ) {
        float normalized = Float.isFinite(effectScale) && effectScale > 0.0f ? effectScale : 1.0f;
        if (this.effectScale == normalized) return this;
        this.effectScale = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }


    @Override
    public void render(RenderContext context) {
        if (context == null || visibility() != Visibility.VISIBLE) return;
        if (postEffect == null || postEffect.isNone()) {
            super.render(context);
            return;
        }

        pushOpacity(context);
        try {
            renderPostProcessed(context);
        } finally {
            popOpacity(context);
        }
    }

    private void renderPostProcessed(RenderContext context) {
        RenderBackend backend = context.backend();
        if (backend == null) {
            renderChildren(context);
            return;
        }

        DrawList subtree = new DrawList();
        DefaultRenderContext subtreeContext = new DefaultRenderContext(subtree).backend(backend);
        inheritRenderState(context, subtreeContext);
        renderChildren(subtreeContext);
        if (subtree.size() == 0) return;

        RectView bounds = layoutBounds();
        DrawList fallbackCommands = copy(subtree);
        DrawList effectCommands = offset(subtree, bounds.x(), bounds.y());
        UiLayerBounds layerBounds = new UiLayerBounds(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                effectScale);
        UiPostEffectChain effectSnapshot = postEffect.copy();
        context.custom(renderBackend -> {
            if (renderBackend == null) return;
            if (renderBackend instanceof UiPostEffectBackend postBackend && postBackend.supportsPostEffects()) {
                postBackend.renderWithPostEffect(effectCommands, layerBounds, effectSnapshot);
                return;
            }
            renderBackend.render(fallbackCommands, null);
        });
    }

    private static void inheritRenderState(RenderContext source, DefaultRenderContext target) {
        target.pushOpacity(source.opacityMultiplier());
        List<TransformLayer> layers = source.transformStack();
        if (layers == null || layers.isEmpty()) return;
        for (TransformLayer layer : layers) {
            if (layer != null) {
                target.pushTransform(layer.bounds(), layer.transform());
            }
        }
    }

    private static DrawList copy(DrawList source) {
        DrawList copy = new DrawList();
        if (source == null || source.size() == 0) return copy;
        Object[] rawCommands = source.commandElements();
        for (int i = 0, size = source.size(); i < size; i++) {
            copy.add((DrawCommand) rawCommands[i]);
        }
        return copy;
    }

    private static DrawList offset(DrawList source, float originX, float originY) {
        DrawList offset = new DrawList();
        if (source == null || source.size() == 0) return offset;
        Object[] rawCommands = source.commandElements();
        for (int i = 0, size = source.size(); i < size; i++) {
            DrawCommand command = ((DrawCommand) rawCommands[i]).copy();
            offsetCommand(command, originX, originY);
            offset.add(command);
        }
        return offset;
    }

    private static void offsetCommand(DrawCommand command, float originX, float originY) {
        RectView bounds = command.bounds();
        command.bounds().set(bounds.x() - originX, bounds.y() - originY, bounds.width(), bounds.height());
        if (command.hasBrushBounds()) {
            RectView brush = command.brushBounds();
            command.brushBounds(brush.x() - originX, brush.y() - originY, brush.width(), brush.height());
        }

        if (command.transformStackSize() == 0) return;
        ObjectArrayList<TransformLayer> shifted = new ObjectArrayList<>(command.transformStackSize());
        Object[] rawLayers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            TransformLayer layer = (TransformLayer) rawLayers[i];
            if (layer == null) continue;
            RectView layerBounds = layer.bounds();
            shifted.add(new TransformLayer(
                    new MutableRect(
                            layerBounds.x() - originX,
                            layerBounds.y() - originY,
                            layerBounds.width(),
                            layerBounds.height()),
                    layer.transform()));
        }
        command.transformStack(shifted);
    }
}
