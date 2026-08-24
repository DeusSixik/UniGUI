package dev.sixik.unigui.widgets.effects;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectBackend;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.ScaledCustomDraw;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.PanelWidget;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@XmlWidgetName("PostProcessingLayer")
public class PostProcessingLayer extends PanelWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostProcessingLayer.class);
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.POST_PROCESSING_LAYER;

    private UiPostEffectChain postEffect = UiPostEffectChain.none();
    private float effectScale = 1.0f;
    private boolean fallbackUnderlay = true;

    /** @return текущая цепочка post-processing pass'ов */
    public UiPostEffectChain postEffect() {
        return postEffect.copy();
    }

    /**
     * Задаёт post-processing chain напрямую из кода.
     *
     * @param postEffect chain эффекта; {@code null} отключает эффект
     * @return этот слой для fluent-настройки
     */
    public PostProcessingLayer postEffect(UiPostEffectChain postEffect) {
        UiPostEffectChain normalized = postEffect == null ? UiPostEffectChain.none() : postEffect.copy();
        this.postEffect = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** @return id зарегистрированного эффекта или пустая строка для inline/none chain */
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
    public PostProcessingLayer effect(String effectId) {
        return postEffect(UiPostEffectChain.of(effectId));
    }

    /** @return дополнительный scale offscreen target'а */
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
    public PostProcessingLayer effectScale(float effectScale) {
        float normalized = Float.isFinite(effectScale) && effectScale > 0.0f ? effectScale : 1.0f;
        if (this.effectScale == normalized) return this;
        this.effectScale = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }
    /** @return {@code true}, если слой сначала рисует обычный subtree под post-effect как fail-soft fallback */
    public boolean fallbackUnderlay() {
        return fallbackUnderlay;
    }

    /**
     * Включает fail-soft подложку под post-effect.
     *
     * <p>Когда режим включён, содержимое слоя сначала рисуется обычным способом, а затем поверх него
     * пробуется offscreen post-effect. Это защищает UI от полностью пустого блока, если backend,
     * shader или render target временно не смогли отрисовать эффект.</p>
     *
     * @param fallbackUnderlay {@code true}, чтобы рисовать обычный subtree под эффектом
     * @return этот слой для fluent-настройки
     */
    @XmlAttribute(value = "fallbackUnderlay", category = "Post Effect", defaultValue = "true",
            description = "Draw the original subtree under the post-effect as a fail-soft fallback.")
    public PostProcessingLayer fallbackUnderlay(boolean fallbackUnderlay) {
        if (this.fallbackUnderlay == fallbackUnderlay) return this;
        this.fallbackUnderlay = fallbackUnderlay;
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
        boolean drawFallbackUnderlay = fallbackUnderlay;
        if (drawFallbackUnderlay) {
            append(context.drawList(), fallbackCommands);
        }
        DrawCommand layerCommand = DrawCommand.custom(new LayerPostEffectDraw(
                fallbackCommands,
                effectCommands,
                layerBounds,
                effectSnapshot,
                drawFallbackUnderlay));
        layerCommand.bounds(new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
        context.drawList().add(layerCommand);
    }

    private static final class LayerPostEffectDraw implements ScaledCustomDraw {
        private final DrawList fallbackCommands;
        private final DrawList effectCommands;
        private final UiLayerBounds layerBounds;
        private final UiPostEffectChain effectSnapshot;
        private final boolean drawFallbackUnderlay;

        private LayerPostEffectDraw(DrawList fallbackCommands,
                                    DrawList effectCommands,
                                    UiLayerBounds layerBounds,
                                    UiPostEffectChain effectSnapshot,
                                    boolean drawFallbackUnderlay) {
            this.fallbackCommands = copy(fallbackCommands);
            this.effectCommands = copy(effectCommands);
            this.layerBounds = layerBounds;
            this.effectSnapshot = effectSnapshot == null ? UiPostEffectChain.none() : effectSnapshot.copy();
            this.drawFallbackUnderlay = drawFallbackUnderlay;
        }

        @Override
        public void draw(RenderBackend renderBackend, float scale) {
            if (renderBackend == null) return;
            float safeScale = sanitizeScale(scale);
            DrawList scaledFallback = safeScale == 1.0f ? fallbackCommands : scale(fallbackCommands, safeScale);
            DrawList scaledEffect = safeScale == 1.0f ? effectCommands : scale(effectCommands, safeScale);
            UiLayerBounds scaledBounds = safeScale == 1.0f ? layerBounds : scale(layerBounds, safeScale);
            boolean effectRendered = false;
            try {
                if (renderBackend instanceof UiPostEffectBackend postBackend && postBackend.supportsPostEffects()) {
                    postBackend.renderWithPostEffect(scaledEffect, scaledBounds, effectSnapshot);
                    effectRendered = true;
                }
            } catch (Throwable failure) {
                LOGGER.warn("UniGUI PostProcessingLayer failed; rendering subtree without post-effect", failure);
            }
            if (effectRendered || drawFallbackUnderlay) return;
            renderFallback(renderBackend, scaledFallback);
        }
    }

    private static void append(DrawList target, DrawList source) {
        if (target == null || source == null || source.size() == 0) return;
        Object[] rawCommands = source.commandElements();
        for (int i = 0, size = source.size(); i < size; i++) {
            target.add((DrawCommand) rawCommands[i]);
        }
    }

    private static void renderFallback(RenderBackend renderBackend, DrawList fallbackCommands) {
        try {
            renderBackend.renderNested(fallbackCommands);
        } catch (Throwable fallbackFailure) {
            LOGGER.warn("UniGUI PostProcessingLayer fallback render failed", fallbackFailure);
        }
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

    private static DrawList scale(DrawList source, float scale) {
        DrawList scaled = new DrawList();
        if (source == null || source.size() == 0) return scaled;
        Object[] rawCommands = source.commandElements();
        for (int i = 0, size = source.size(); i < size; i++) {
            DrawCommand command = ((DrawCommand) rawCommands[i]).copy();
            scaleCommand(command, scale);
            scaled.add(command);
        }
        return scaled;
    }

    private static UiLayerBounds scale(UiLayerBounds bounds, float scale) {
        if (bounds == null) return null;
        return new UiLayerBounds(
                bounds.x() * scale,
                bounds.y() * scale,
                bounds.width() * scale,
                bounds.height() * scale,
                bounds.scale());
    }

    private static void scaleCommand(DrawCommand command, float scale) {
        RectView bounds = command.bounds();
        command.bounds().set(
                bounds.x() * scale,
                bounds.y() * scale,
                bounds.width() * scale,
                bounds.height() * scale);
        command.radius(command.radius() * scale);
        command.paint(command.paint().strokeWidth(command.paint().strokeWidth() * scale));

        command.transform().position().set(
                command.transform().position().x() * scale,
                command.transform().position().y() * scale);
        command.transform().pivot().set(
                command.transform().pivot().x() * scale,
                command.transform().pivot().y() * scale);

        if (command.mesh() != null) {
            command.mesh(scaleMesh(command.mesh(), scale));
        }
        if (command.path() != null) {
            command.path(scalePath(command.path(), scale));
        }

        if (command.transformStackSize() == 0) return;
        ObjectArrayList<TransformLayer> scaled = new ObjectArrayList<>(command.transformStackSize());
        Object[] rawLayers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            TransformLayer layer = (TransformLayer) rawLayers[i];
            if (layer == null) continue;
            RectView layerBounds = layer.bounds();
            var transform = layer.transform().copy();
            transform.position().set(
                    transform.position().x() * scale,
                    transform.position().y() * scale);
            transform.pivot().set(
                    transform.pivot().x() * scale,
                    transform.pivot().y() * scale);
            scaled.add(new TransformLayer(
                    new MutableRect(
                            layerBounds.x() * scale,
                            layerBounds.y() * scale,
                            layerBounds.width() * scale,
                            layerBounds.height() * scale),
                    transform));
        }
        command.transformStack(scaled);
    }

    private static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    private static void offsetCommand(DrawCommand command, float originX, float originY) {
        RectView bounds = command.bounds();
        command.bounds().set(bounds.x() - originX, bounds.y() - originY, bounds.width(), bounds.height());

        if (command.mesh() != null) {
            command.mesh(offsetMesh(command.mesh(), originX, originY));
        }
        if (command.path() != null) {
            command.path(offsetPath(command.path(), originX, originY));
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

    private static DrawMesh offsetMesh(DrawMesh mesh, float originX, float originY) {
        ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>(mesh.vertexCount());
        Object[] rawVertices = mesh.vertexElements();
        for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
            DrawVertex vertex = (DrawVertex) rawVertices[i];
            if (vertex != null) {
                vertices.add(new DrawVertex(
                        vertex.x() - originX,
                        vertex.y() - originY,
                        vertex.u(),
                        vertex.v(),
                        vertex.color()));
            }
        }
        return DrawMesh.triangles(vertices);
    }

    private static DrawMesh scaleMesh(DrawMesh mesh, float scale) {
        ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>(mesh.vertexCount());
        Object[] rawVertices = mesh.vertexElements();
        for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
            DrawVertex vertex = (DrawVertex) rawVertices[i];
            if (vertex != null) {
                vertices.add(new DrawVertex(
                        vertex.x() * scale,
                        vertex.y() * scale,
                        vertex.u(),
                        vertex.v(),
                        vertex.color()));
            }
        }
        return DrawMesh.triangles(vertices);
    }

    private static VectorPath offsetPath(VectorPath path, float originX, float originY) {
        VectorPath shifted = new VectorPath();
        Object[] rawElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawElements[i];
            if (element == null) continue;
            switch (element.verb()) {
                case MOVE_TO -> shifted.moveTo(element.x1() - originX, element.y1() - originY);
                case LINE_TO -> shifted.lineTo(element.x1() - originX, element.y1() - originY);
                case QUADRATIC_TO -> shifted.quadraticTo(
                        element.x1() - originX,
                        element.y1() - originY,
                        element.x2() - originX,
                        element.y2() - originY);
                case CUBIC_TO -> shifted.cubicTo(
                        element.x1() - originX,
                        element.y1() - originY,
                        element.x2() - originX,
                        element.y2() - originY,
                        element.x3() - originX,
                        element.y3() - originY);
                case CLOSE -> shifted.close();
            }
        }
        return shifted;
    }

    private static VectorPath scalePath(VectorPath path, float scale) {
        VectorPath scaled = new VectorPath();
        Object[] rawElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawElements[i];
            if (element == null) continue;
            switch (element.verb()) {
                case MOVE_TO -> scaled.moveTo(element.x1() * scale, element.y1() * scale);
                case LINE_TO -> scaled.lineTo(element.x1() * scale, element.y1() * scale);
                case QUADRATIC_TO -> scaled.quadraticTo(
                        element.x1() * scale,
                        element.y1() * scale,
                        element.x2() * scale,
                        element.y2() * scale);
                case CUBIC_TO -> scaled.cubicTo(
                        element.x1() * scale,
                        element.y1() * scale,
                        element.x2() * scale,
                        element.y2() * scale,
                        element.x3() * scale,
                        element.y3() * scale);
                case CLOSE -> scaled.close();
            }
        }
        return scaled;
    }
}
