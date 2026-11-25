package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectBackend;
import dev.sixik.unigui.api.posteffect.UiPostEffectBlendMode;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectPass;
import dev.sixik.unigui.api.posteffect.UiPostEffectRegistry;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.DrawBatcher;
import dev.sixik.unigui.impl.render.RenderTargetCache;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Бэкенд рендера UniGUI для Minecraft 26.1.
 *
 * <p>Структура та же, что в 1.20/1.21: draw-list режется на батчи, батчи идут в специализированные рендереры
 * (фигуры, SDF, текст, текстуры, шейдеры), остальное рисуется по командам. Отличается только нижний слой:
 * вместо глобального GL-состояния {@code RenderSystem} отрисовки идут через {@link UiGpu} (пайплайны 26.1) и
 * {@link UiGl} (собственные GL-программы), а на экран нарисованное попадает через {@link UiLayerCompositor}.</p>
 *
 * <p>Ванильные элементы (предметы, тултипы, сущности) в 26.1 рисует отложенный {@code GuiRenderer}. Бэкенд ставит
 * их в очередь через {@link UniGuiGraphics#submit}, предварительно перенося уже нарисованный слой UniGUI, так что
 * порядок наложения совпадает с draw-list. В offscreen-цели ванильные элементы попасть не могут: там они
 * пропускаются.</p>
 */
public final class MinecraftGuiRenderBackend implements RenderBackend, UiPostEffectBackend, AutoCloseable {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(MinecraftGuiRenderBackend.class);
    /**
     * Vanilla Font treats colors with alpha 0..3 as "alpha not specified" and promotes them
     * to opaque, which causes fade-in text to flash on the first visible frame.
     */
    private static final int MIN_VANILLA_TEXT_ALPHA_CHANNEL = 4;
    private static final RenderTargetOptions POST_EFFECT_LAYER_TARGET =
            new RenderTargetOptions(false, true, "post_effect_layer");
    private static final RenderTargetOptions POST_EFFECT_PING_TARGET =
            new RenderTargetOptions(false, true, "post_effect_ping");
    /**
     * Ёмкость LRU-кэша разрешённых texture binding'ов: per-command fallback-пути
     * (одиночные текстуры, смешанные батчи, сетки иконок) ходят через него вместо
     * аллокации record на каждую команду.
     */
    private static final int TEXTURE_BINDING_CACHE_SIZE = 512;
    private static boolean offscreenVanillaWarned;

    private final Minecraft minecraft;
    private final DrawBatcher batcher;
    private ScissorStack scissorStack = new ScissorStack();
    private final ObjectArrayList<PostEffectRenderState> postEffectRenderStates = new ObjectArrayList<>();
    private int postEffectRenderStateDepth;
    private final MinecraftSdfTextRenderer sdfTextRenderer =
            new MinecraftSdfTextRenderer(DefaultFontRegistry.global());
    private final MinecraftMixedTextRenderer mixedTextRenderer;
    private final MinecraftShapeBatchRenderer shapeBatchRenderer = new MinecraftShapeBatchRenderer();
    private final MinecraftTextureBatchRenderer textureBatchRenderer = new MinecraftTextureBatchRenderer();
    private final MinecraftShaderQuadRenderer shaderQuadRenderer;
    private final RenderTargetCache postEffectLayerTargets = new RenderTargetCache(this);
    private final RenderTargetCache postEffectPingTargets = new RenderTargetCache(this);
    private final UiTextureBinding.Cache textureBindings = new UiTextureBinding.Cache(TEXTURE_BINDING_CACHE_SIZE);
    private final DrawList postEffectDrawList = new DrawList();
    private final FastItemRenderer fastItemRenderer;
    private final UniGuiGraphics graphics;
    private MinecraftRenderTarget activeRenderTarget;
    private float activeRenderTargetScaleX = 1.0f;
    private float activeRenderTargetScaleY = 1.0f;
    private float nestedClipCoordinateScale = 1.0f;
    private final List<Object> gpuTimerQueries = new ArrayList<>();
    private float lastFrameGpuMillis = -1.0f;

    public MinecraftGuiRenderBackend(GuiGraphicsExtractor graphics) {
        this(graphics, Minecraft.getInstance(), SimpleDrawBatcher.INSTANCE);
    }

    public MinecraftGuiRenderBackend(GuiGraphicsExtractor graphics, Minecraft minecraft, DrawBatcher batcher) {
        this.graphics = new UniGuiGraphics(Objects.requireNonNull(graphics, "graphics"));
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
        this.batcher = batcher == null ? SimpleDrawBatcher.INSTANCE : batcher;
        this.sdfTextRenderer.defaultFace(MinecraftFonts.defaultFace());
        this.mixedTextRenderer = new MinecraftMixedTextRenderer(this.minecraft, sdfTextRenderer);
        this.fastItemRenderer = new FastItemRenderer(this.minecraft);
        this.shaderQuadRenderer = new MinecraftShaderQuadRenderer(this.minecraft);
        UiPostEffects.ensureRegistered();
    }

    /**
     * Меняет extractor кадра. Поза UniGUI берётся из текущей позы extractor.
     */
    public MinecraftGuiRenderBackend graphics(GuiGraphicsExtractor graphics) {
        this.graphics.extractor(Objects.requireNonNull(graphics, "graphics"));
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        clearScissorStack();
        pollGpuTimer();
        fastItemRenderer.beginFrame();
    }

    public float lastFrameGpuMillis() {
        pollGpuTimer();
        return lastFrameGpuMillis;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    public UniGuiGraphics graphics() {
        return graphics;
    }

    /**
     * Font used by plain String text commands that do not provide a RichText face.
     */
    public FontFace defaultFont() {
        return sdfTextRenderer.defaultFace();
    }

    @Override
    public FontFace defaultTextFace() {
        return defaultFont();
    }

    public MinecraftGuiRenderBackend defaultFont(FontFace font) {
        sdfTextRenderer.defaultFace(font);
        return this;
    }

    public MinecraftGuiRenderBackend useVanillaDefaultFont() {
        return defaultFont(MinecraftFonts.defaultFace());
    }

    public MinecraftGuiRenderBackend useSdfDefaultFont() {
        return defaultFont(DefaultFontRegistry.global().defaultFace());
    }

    public void renderItemPreview(ItemStack stack, float x, float y, float size, float opacity, boolean decorations) {
        if (stack == null || stack.isEmpty() || size <= 0.0f) return;
        if (!decorations) {
            TextureHandle cached = fastItemRenderer.cachedTexture(stack, size);
            if (cached != null) {
                renderItemPreviewTexture(cached, x, y, size, opacity);
                return;
            }

            if (fastItemRenderer.prefersCachedPath(stack)) {
                TextureHandle baked = fastItemRenderer.bakeIfBudget(stack, size, activeRenderTarget);
                if (baked != null) {
                    renderItemPreviewTexture(baked, x, y, size, opacity);
                    return;
                }
            }
        }
        renderVanillaItemPreview(stack, x, y, size, opacity, decorations);
    }

    public boolean renderItemPreviewLazy(ItemStack stack, float x, float y, float size, float opacity) {
        if (stack == null || stack.isEmpty() || size <= 0.0f) return false;

        TextureHandle cached = fastItemRenderer.cachedTexture(stack, size);
        if (cached != null) {
            renderItemPreviewTexture(cached, x, y, size, opacity);
            return true;
        }

        if (fastItemRenderer.prefersCachedPath(stack)) {
            TextureHandle baked = fastItemRenderer.bakeIfBudget(stack, size, activeRenderTarget);
            if (baked != null) {
                renderItemPreviewTexture(baked, x, y, size, opacity);
                return true;
            }
            return false;
        }

        renderVanillaItemPreview(stack, x, y, size, opacity, false);
        return true;
    }

    public TextureHandle cachedItemPreviewTexture(ItemStack stack, float size) {
        return fastItemRenderer.cachedTexture(stack, size);
    }

    public void clearItemPreviewCache() {
        fastItemRenderer.clear();
    }

    public void renderVanillaTooltip(Component line, float x, float y) {
        if (line == null) return;
        renderVanillaTooltip(List.of(line), x, y);
    }

    public void renderVanillaTooltip(List<Component> lines, float x, float y) {
        if (lines == null || lines.isEmpty()) return;
        renderTooltipComponents(tooltipLines(lines), null, x, y);
    }

    public void renderVanillaItemTooltip(ItemStack stack, float x, float y) {
        renderVanillaTooltip(stack, x, y);
    }

    /**
     * Рисует ванильный тултип с прикреплённым image-компонентом: текстовые строки
     * сверху, под ними произвольный визуал (например, сетка предметных моделей).
     * Компонент должен быть зарегистрирован в фабрике tooltip-компонентов на стороне мода.
     */
    public void renderVanillaTooltipWithComponent(List<Component> lines,
                                                  net.minecraft.world.inventory.tooltip.TooltipComponent component,
                                                  float x, float y) {
        if (component == null) {
            renderVanillaTooltip(lines, x, y);
            return;
        }
        List<ClientTooltipComponent> components = tooltipLines(lines == null ? List.of() : lines);
        components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(component));
        renderTooltipComponents(components, null, x, y);
    }

    public void renderVanillaTooltip(ItemStack stack, float x, float y) {
        if (stack == null || stack.isEmpty()) return;
        List<ClientTooltipComponent> components = tooltipLines(Screen.getTooltipFromItem(minecraft, stack));
        Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> image = stack.getTooltipImage();
        image.ifPresent(component -> components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(component)));
        renderTooltipComponents(components, stack.get(DataComponents.TOOLTIP_STYLE), x, y);
    }

    private static List<ClientTooltipComponent> tooltipLines(List<Component> lines) {
        List<ClientTooltipComponent> components = new ArrayList<>(lines.size() + 1);
        for (Component line : lines) {
            if (line != null) components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        return components;
    }

    private void renderTooltipComponents(List<ClientTooltipComponent> components,
                                         net.minecraft.resources.Identifier style, float x, float y) {
        if (components.isEmpty() || !canSubmitVanilla()) return;
        inVanillaGuiSpace(x, y, (guiX, guiY) -> graphics.submitInGuiSpace(extractor ->
                MinecraftClientCompat.tooltip(extractor, minecraft.font, components, guiX, guiY, DefaultTooltipPositioner.INSTANCE, style)));
    }

    /**
     * Проверяет, можно ли сейчас отправить ванильный элемент: только при рисовании на экран, не в offscreen-цель.
     */
    private boolean canSubmitVanilla() {
        if (activeRenderTarget == null && (!UiGpu.hasTarget() || UiLayerCompositor.drawingToScreen())) return true;
        if (!offscreenVanillaWarned) {
            offscreenVanillaWarned = true;
            LOGGER.warn("UniGUI: vanilla items/tooltips/entities cannot be drawn into offscreen targets on Minecraft 26.1; skipped");
        }
        return false;
    }

    /**
     * Переводит точку из текущих единиц позы UniGUI в ванильные единицы GUI.
     *
     * <p>Ванильная подсказка сама зажимает позицию в экране по {@code guiWidth() x guiHeight()}, поэтому её надо
     * рисовать в ванильных единицах с единичной позой, а не в единицах UI.</p>
     *
     * @param x    X точки привязки в текущих единицах позы
     * @param y    Y точки привязки в текущих единицах позы
     * @param draw вызов ванильного рендера с координатами в ванильных единицах
     */
    private void inVanillaGuiSpace(float x, float y, VanillaTooltipDraw draw) {
        Vector4f point = graphics.pose().last().pose().transform(new Vector4f(x, y, 0.0f, 1.0f));
        draw.draw(Math.round(point.x()), Math.round(point.y()));
    }

    @FunctionalInterface
    private interface VanillaTooltipDraw {
        void draw(int guiX, int guiY);
    }

    private void renderItemPreviewTexture(TextureHandle texture, float x, float y, float size, float opacity) {
        DrawCommand command = DrawCommand.texture(
                texture,
                new MutableRect(x, y, size, size),
                Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, clamp01(opacity))));
        renderTexture(command);
    }

    /**
     * Ставит предмет в очередь GUI. Почти прозрачные предметы пропускаются; прозрачность остальных применяется
     * при переносе из атласа предметов ({@link UiItemAlpha}). Надписи (количество, прочность) остаются
     * непрозрачными.
     */
    private void renderVanillaItemPreview(ItemStack stack, float x, float y, float size, float opacity, boolean decorations) {
        if (clamp01(opacity) < 0.05f || !canSubmitVanilla()) return;
        float scale = Math.max(0.01f, size / 16.0f);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0.0f);
            pose.scale(scale, scale, 1.0f);
            float alpha = clamp01(opacity);
            graphics.submit(extractor -> {
                UiItemAlpha.pending(alpha);
                try {
                    extractor.item(stack, 0, 0);
                } finally {
                    UiItemAlpha.pending(1.0f);
                }
                if (decorations) {
                    extractor.itemDecorations(minecraft.font, stack, 0, 0);
                }
            });
        } finally {
            pose.popPose();
        }
    }

    /** Следующий ID сущности превью: отрицательный, уникальный в пределах запуска (при переполнении начинается заново). */
    private static int previewEntityId;

    private static int nextPreviewEntityId() {
        previewEntityId = previewEntityId <= Integer.MIN_VALUE + 1 ? -1 : previewEntityId - 1;
        return previewEntityId;
    }

    public boolean renderEntityPreview(EntityType<? extends LivingEntity> entityType, float x, float y, float size,
                                       float mouseX, float mouseY, float opacity) {
        if (entityType == null || minecraft.level == null || size <= 0.0f) return false;
        if (!canSubmitVanilla()) return false;
        LivingEntity entity = MinecraftClientCompat.previewEntity(entityType, minecraft.level);
        if (entity == null) return false;
        // Сущность превью не добавляется в мир, и ID ей никто не выдаёт. В 26.2 ID 0 значит «не назначен», и
        // getId() (его вызывает, например, ItemModelResolver при сборке состояния) бросает исключение. Отрицательные
        // ID не пересекаются с настоящими сущностями (те получают положительные).
        entity.setId(nextPreviewEntityId());

        // Picture-in-picture не учитывает позу extractor'а: прямоугольник, масштаб и курсор переводятся
        // в единицы GUI здесь.
        Matrix4f matrix = graphics.pose().last().pose();
        Vector4f topLeft = matrix.transform(new Vector4f(x, y, 0.0f, 1.0f));
        Vector4f bottomRight = matrix.transform(new Vector4f(x + size, y + size, 0.0f, 1.0f));
        Vector4f mouse = matrix.transform(new Vector4f(x + mouseX, y + mouseY, 0.0f, 1.0f));
        int x0 = Math.round(Math.min(topLeft.x(), bottomRight.x()));
        int y0 = Math.round(Math.min(topLeft.y(), bottomRight.y()));
        int x1 = Math.round(Math.max(topLeft.x(), bottomRight.x()));
        int y1 = Math.round(Math.max(topLeft.y(), bottomRight.y()));
        if (x1 <= x0 || y1 <= y0) return false;
        int entityScale = Math.max(1, Math.round((y1 - y0) * 0.72f));
        graphics.submitClippedInGuiSpace(extractor -> MinecraftEntityPreviewCompat.render(extractor, x0, y0, x1, y1,
                entityScale, mouse.x(), mouse.y(), entity));
        return true;
    }

    @Override
    public float measureTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(
                RichText.of(text, defaultFont(), TextRun.DEFAULT_PIXEL_SIZE));
    }

    @Override
    public float measureTextWidth(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(resolveDefaultFont(text));
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        return new MinecraftRenderTarget(width, height, options);
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        clearScissorStack();
        if (target == null) {
            renderToCurrentTarget(drawList);
            return;
        }

        if (!(target instanceof MinecraftRenderTarget minecraftTarget)) {
            throw new IllegalArgumentException("Unsupported render target for Minecraft backend: " + target.getClass().getName());
        }

        renderToTarget(drawList, minecraftTarget);
    }

    /**
     * Рендерит draw-list в target с явным масштабом координат.
     *
     * <p>Метод нужен offscreen-сценариям вроде UI на блоке в мире: layout может жить в логических
     * координатах 256x144, а texture target быть 1024x576. В таком случае {@code coordinateScale}
     * сообщает backend'у, сколько texel'ов приходится на одну UI-единицу.</p>
     */
    public void render(DrawList drawList, RenderTarget target, float coordinateScale) {
        clearScissorStack();
        if (target == null) {
            renderToCurrentTarget(drawList);
            return;
        }

        if (!(target instanceof MinecraftRenderTarget minecraftTarget)) {
            throw new IllegalArgumentException("Unsupported render target for Minecraft backend: " + target.getClass().getName());
        }

        float scale = sanitizeScale(coordinateScale);
        renderToTarget(drawList, minecraftTarget, scale, scale);
    }

    /**
     * Рисует в текущую цель. Если цели ещё нет (верхний уровень отрисовки экрана), открывается слой экрана
     * {@link UiLayerCompositor}, а в конце нарисованное переносится в очередь GUI.
     */
    private void renderToCurrentTarget(DrawList drawList) {
        boolean openedLayer = !UiGpu.hasTarget();
        if (openedLayer) {
            graphics.syncPoseFromExtractor();
            UiLayerCompositor.begin(graphics.rawExtractor());
        }
        Object gpuTimer = beginGpuTimer();
        try {
            renderBatches(drawList);
        } finally {
            endGpuTimer(gpuTimer);
            clearScissorStack();
            if (openedLayer) UiLayerCompositor.end();
        }
    }


    @Override
    public boolean supportsPostEffects() {
        return true;
    }

    @Override
    public void renderWithPostEffect(DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain, RenderTarget target) {
        UiPostEffects.ensureRegistered();
        UiLayerBounds layerBounds = bounds == null ? currentPostEffectBounds() : bounds;
        float layerScale = layerBounds.safeScale();
        if (drawList == null || chain == null || chain.isNone()) {
            render(drawList, target, layerScale);
            return;
        }

        List<UiPostEffectPass> passes = chain.resolve(UiPostEffectRegistry.global());
        if (passes.isEmpty()) {
            render(drawList, target, layerScale);
            return;
        }

        PostEffectRenderState renderState = pushPostEffectRenderState();
        try {
            int targetWidth = layerBounds.targetWidth();
            int targetHeight = layerBounds.targetHeight();
            float layerScaleX = targetWidth / layerBounds.safeWidth();
            float layerScaleY = targetHeight / layerBounds.safeHeight();
            RenderTarget layerTarget = postEffectLayerTargets.acquire(targetWidth, targetHeight, POST_EFFECT_LAYER_TARGET);
            RenderTarget pingTarget = postEffectPingTargets.acquire(targetWidth, targetHeight, POST_EFFECT_PING_TARGET);
            renderPostEffectLayer(drawList, layerTarget, layerScaleX, layerScaleY);
            if (isPassthroughOnly(passes)) {
                renderState.prepareComposite(this);
                compositePostEffectTexture(layerTarget, layerBounds, target,
                        layerScaleX, layerScaleY, renderState);
                return;
            }
            RenderTarget current = layerTarget;
            for (UiPostEffectPass pass : passes) {
                RenderTarget destination = current == layerTarget ? pingTarget : layerTarget;
                // Fullscreen pass работает в texel/NDC-пространстве target и не должен
                // наследовать масштаб координат исходного UI-слоя.
                if (!renderPostEffectPass(pass, current, destination, 1.0f)) {
                    renderState.prepareComposite(this);
                    compositePostEffectTexture(current, layerBounds, target, layerScaleX, layerScaleY, renderState);
                    return;
                }
                current = destination;
            }
            renderState.prepareComposite(this);
            compositePostEffectTexture(current, layerBounds, target, layerScaleX, layerScaleY, renderState);
        } finally {
            popPostEffectRenderState(renderState);
        }
    }

    private static boolean isPassthroughOnly(List<UiPostEffectPass> passes) {
        if (passes == null || passes.size() != 1) return false;
        UiPostEffectPass pass = passes.get(0);
        return pass != null
                && UiPostEffects.PASSTHROUGH.equals(pass.shaderId())
                && pass.blendMode() == UiPostEffectBlendMode.REPLACE
                && pass.uniforms().isEmpty()
                && pass.textures().isEmpty();
    }

    private PostEffectRenderState pushPostEffectRenderState() {
        int index = postEffectRenderStateDepth++;
        while (postEffectRenderStates.size() <= index) {
            postEffectRenderStates.add(new PostEffectRenderState());
        }
        PostEffectRenderState state = postEffectRenderStates.get(index);
        state.capture(this);
        return state;
    }

    private void popPostEffectRenderState(PostEffectRenderState state) {
        try {
            state.restore(this);
        } finally {
            postEffectRenderStateDepth = Math.max(0, postEffectRenderStateDepth - 1);
        }
    }

    private void renderPostEffectLayer(DrawList drawList, RenderTarget target,
                                       float coordinateScaleX, float coordinateScaleY) {
        if (!(target instanceof MinecraftRenderTarget minecraftTarget)) {
            render(drawList, target);
            return;
        }

        renderToTarget(drawList, minecraftTarget, coordinateScaleX, coordinateScaleY);
    }

    private boolean renderPostEffectPass(UiPostEffectPass pass, RenderTarget source, RenderTarget destination,
                                         float coordinateScale) {
        if (!(destination instanceof MinecraftRenderTarget minecraftDestination) || source == null || pass == null) {
            return false;
        }
        ShaderUniforms uniforms = pass.uniforms().resolve();
        DrawCommand command = DrawCommand.shader(
                        pass.shader(),
                        new MutableRect(0.0f, 0.0f, destination.width(), destination.height()),
                        uniforms)
                .texture(source.colorTexture())
                .shaderTextures(pass.textures())
                .shaderOptions(ShaderDrawOptions.defaults()
                        .blend(pass.blendMode().blendEnabled())
                        .squareVertexOffset(0.0f));

        MinecraftRenderTarget previousTarget = activeRenderTarget;
        float previousScaleX = activeRenderTargetScaleX;
        float previousScaleY = activeRenderTargetScaleY;
        float scale = sanitizeScale(coordinateScale);
        UiGpu.pushTarget(minecraftDestination.beginWrite(),
                minecraftDestination.width() / scale, minecraftDestination.height() / scale);
        activeRenderTarget = minecraftDestination;
        activeRenderTargetScaleX = scale;
        activeRenderTargetScaleY = scale;
        try {
            return shaderQuadRenderer.render(
                    graphics,
                    minecraft,
                    command,
                    true,
                    shaderScreenWidth(),
                    shaderScreenHeight(),
                    shaderGuiScale());
        } finally {
            clearScissorStack();
            UiGpu.popTarget();
            activeRenderTarget = previousTarget;
            activeRenderTargetScaleX = previousScaleX;
            activeRenderTargetScaleY = previousScaleY;
        }
    }

    private void compositePostEffectTexture(RenderTarget source, UiLayerBounds bounds, RenderTarget target,
                                            float coordinateScaleX, float coordinateScaleY,
                                            PostEffectRenderState renderState) {
        postEffectDrawList.clear();
        postEffectDrawList.add(DrawCommand.texture(
                source.colorTexture(),
                new MutableRect(bounds.x(), bounds.y(), bounds.safeWidth(), bounds.safeHeight()),
                Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f))));
        if (target == null) {
            renderToCurrentTarget(postEffectDrawList);
            return;
        }

        renderState.isolateScissor(this);
        if (target instanceof MinecraftRenderTarget minecraftTarget) {
            renderToTarget(postEffectDrawList, minecraftTarget, coordinateScaleX, coordinateScaleY);
        } else {
            render(postEffectDrawList, target, coordinateScaleX);
        }
    }

    private UiLayerBounds currentPostEffectBounds() {
        if (minecraft.getWindow() == null) {
            return UiLayerBounds.viewport(1.0f, 1.0f);
        }
        return UiLayerBounds.viewport(
                Math.max(1.0f, minecraft.getWindow().getGuiScaledWidth()),
                Math.max(1.0f, minecraft.getWindow().getGuiScaledHeight()));
    }

    @Override
    public void endFrame() {
        UiLayerCompositor.flush();
    }

    private void renderToTarget(DrawList drawList, MinecraftRenderTarget target) {
        renderToTarget(drawList, target, renderTargetScaleX(target), renderTargetScaleY(target));
    }

    private void renderToTarget(DrawList drawList, MinecraftRenderTarget target, float coordinateScaleX, float coordinateScaleY) {
        MinecraftRenderTarget previousTarget = activeRenderTarget;
        float previousScaleX = activeRenderTargetScaleX;
        float previousScaleY = activeRenderTargetScaleY;
        activeRenderTarget = target;
        activeRenderTargetScaleX = sanitizeScale(coordinateScaleX);
        activeRenderTargetScaleY = sanitizeScale(coordinateScaleY);
        UiGpu.pushTarget(target.beginWrite(),
                Math.max(1.0f, target.width() / activeRenderTargetScaleX),
                Math.max(1.0f, target.height() / activeRenderTargetScaleY));
        try {
            renderBatches(drawList);
        } finally {
            clearScissorStack();
            UiGpu.popTarget();
            activeRenderTarget = previousTarget;
            activeRenderTargetScaleX = previousScaleX;
            activeRenderTargetScaleY = previousScaleY;
        }
    }

    private float renderTargetScaleX(MinecraftRenderTarget target) {
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledWidth() <= 0) return 1.0f;
        return target.width() == minecraft.getWindow().getWidth()
                ? target.width() / (float) minecraft.getWindow().getGuiScaledWidth()
                : 1.0f;
    }

    private float renderTargetScaleY(MinecraftRenderTarget target) {
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledHeight() <= 0) return 1.0f;
        return target.height() == minecraft.getWindow().getHeight()
                ? target.height() / (float) minecraft.getWindow().getGuiScaledHeight()
                : 1.0f;
    }

    private void renderBatches(DrawList drawList) {
        List<DrawBatch> batches = batcher.batch(drawList);
        if (batches instanceof ObjectArrayList<?> objectBatches) {
            Object[] rawBatches = objectBatches.elements();
            for (int i = 0, size = objectBatches.size(); i < size; i++) {
                renderBatch((DrawBatch) rawBatches[i]);
            }
            return;
        }

        for (int i = 0, size = batches.size(); i < size; i++) {
            renderBatch(batches.get(i));
        }
    }

    void renderNested(DrawList drawList) {
        renderNested(drawList, 1.0f);
    }

    public void renderNested(DrawList drawList, float coordinateScale) {
        float previousScale = nestedClipCoordinateScale;
        nestedClipCoordinateScale *= sanitizeScale(coordinateScale);
        try {
            if (UiGpu.hasTarget()) {
                renderBatches(drawList);
            } else {
                renderToCurrentTarget(drawList);
            }
        } finally {
            nestedClipCoordinateScale = previousScale;
        }
    }

    void sdfRuntimeStatsEnabled(boolean enabled) {
        shapeBatchRenderer.sdfRuntimeStatsEnabled(enabled);
    }

    MinecraftSdfShapeRenderer.SdfRuntimeStats consumeSdfRuntimeStats() {
        return shapeBatchRenderer.consumeSdfRuntimeStats();
    }

    /**
     * Начинает GPU-замер отрисовки через timer query (26.1) или timestamp-запросы (26.2). Результат читается в следующих кадрах.
     */
    private Object beginGpuTimer() {
        pollGpuTimer();
        if (gpuTimerQueries.size() >= 4) return null;
        try {
            return MinecraftGpuCompat.beginTimer();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void endGpuTimer(Object query) {
        if (query == null) return;
        try {
            MinecraftGpuCompat.endTimer(query);
            gpuTimerQueries.add(query);
        } catch (RuntimeException ignored) {
            MinecraftGpuCompat.closeTimer(query);
        }
    }

    private void pollGpuTimer() {
        while (!gpuTimerQueries.isEmpty()) {
            Object query = gpuTimerQueries.get(0);
            java.util.OptionalLong value = MinecraftGpuCompat.timerNanos(query);
            if (value.isEmpty()) return;
            lastFrameGpuMillis = Math.max(0.0f, value.getAsLong() / 1_000_000.0f);
            gpuTimerQueries.remove(0);
            MinecraftGpuCompat.closeTimer(query);
        }
    }

    /**
     * Переиспользуемый снимок UI-состояния для вложенного post-effect прохода.
     *
     * <p>PostProcessingLayer выполняется из CUSTOM-команды уже активного draw list. Поэтому
     * offscreen-рендер не должен очищать scissor stack родителя и наследовать его pose. Цели отрисовки
     * в 26.1 восстанавливает стек {@link UiGpu}, поэтому GL-состояние здесь не сохраняется.</p>
     */
    private static final class PostEffectRenderState {
        private final ScissorStack isolatedScissorStack = new ScissorStack();
        private ScissorStack outerScissorStack;
        private MinecraftRenderTarget activeRenderTarget;
        private float activeRenderTargetScaleX;
        private float activeRenderTargetScaleY;
        private float nestedClipCoordinateScale;
        private boolean poseIsolated;
        private boolean restored;

        private void capture(MinecraftGuiRenderBackend backend) {
            outerScissorStack = backend.scissorStack;
            activeRenderTarget = backend.activeRenderTarget;
            activeRenderTargetScaleX = backend.activeRenderTargetScaleX;
            activeRenderTargetScaleY = backend.activeRenderTargetScaleY;
            nestedClipCoordinateScale = backend.nestedClipCoordinateScale;
            restored = false;

            backend.graphics.pose().pushPose();
            backend.graphics.pose().last().pose().identity();
            poseIsolated = true;
            isolatedScissorStack.clear();
            backend.scissorStack = isolatedScissorStack;
            backend.nestedClipCoordinateScale = 1.0f;
        }

        private void prepareComposite(MinecraftGuiRenderBackend backend) {
            restoreParentState(backend);
        }

        private void isolateScissor(MinecraftGuiRenderBackend backend) {
            isolatedScissorStack.clear();
            backend.scissorStack = isolatedScissorStack;
        }

        private void restore(MinecraftGuiRenderBackend backend) {
            restoreParentState(backend);
        }

        private void restoreParentState(MinecraftGuiRenderBackend backend) {
            if (backend.scissorStack == isolatedScissorStack) {
                isolatedScissorStack.clear();
            }
            if (poseIsolated) {
                backend.graphics.pose().popPose();
                poseIsolated = false;
            }
            backend.scissorStack = outerScissorStack;
            backend.activeRenderTarget = activeRenderTarget;
            backend.activeRenderTargetScaleX = activeRenderTargetScaleX;
            backend.activeRenderTargetScaleY = activeRenderTargetScaleY;
            backend.nestedClipCoordinateScale = nestedClipCoordinateScale;
            if (!restored) {
                restored = true;
                backend.reapplyScissor();
            }
        }
    }

    private void renderBatch(DrawBatch batch) {
        boolean renderingToPremultipliedTarget = activeRenderTarget != null;
        if (MinecraftShapeBatchRenderer.supports(batch.type())
                && shapeBatchRenderer.render(graphics, batch, renderingToPremultipliedTarget, shaderGuiScale())) {
            return;
        }
        if (batch.type() == DrawCommandType.MESH && renderMeshBatch(batch)) {
            return;
        }
        float textGuiScale = shaderGuiScale();
        if (batch.type() == DrawCommandType.TEXT
                && mixedTextRenderer.render(graphics, batch, graphics.pose(), renderingToPremultipliedTarget, textGuiScale)) {
            return;
        }
        if (batch.type() == DrawCommandType.TEXT
                && sdfTextRenderer.render(graphics, batch, graphics.pose(), renderingToPremultipliedTarget, textGuiScale)) {
            return;
        }
        if ((batch.type() == DrawCommandType.TEXTURE
                || batch.type() == DrawCommandType.TEXTURED_QUAD)
                && textureBatchRenderer.render(graphics, batch, renderingToPremultipliedTarget)) {
            return;
        }
        Object[] rawCommands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            renderCommand((DrawCommand) rawCommands[i]);
        }
    }

    /**
     * Рендерит соседние mesh-команды одним буфером. Команды не сортируются:
     * каждая вершина заранее получает матрицу своей команды, поэтому порядок
     * прозрачных треугольников остаётся тем же, что и в draw list.
     */
    private boolean renderMeshBatch(DrawBatch batch) {
        if (batch == null || batch.size() == 0) return false;

        UiTextureBinding binding = batch.texture() == null ? null : textureBindings.resolve(batch.texture());
        if (batch.texture() != null && binding == null) return false;

        if (binding == null) {
            Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES,
                    DefaultVertexFormat.POSITION_COLOR);
            appendMeshBatchVertices(buffer, batch, graphics.pose().last().pose(), false, null);
            MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
        } else {
            Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES,
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            appendMeshBatchVertices(buffer, batch, graphics.pose().last().pose(), true, binding);
            MinecraftBufferCompat.drawTexture(buffer,
                    MinecraftUiBlend.texture(binding.premultipliedAlpha(), activeRenderTarget != null), binding);
        }
        return true;
    }

    private static void appendMeshBatchVertices(Object buffer, DrawBatch batch, Matrix4f basePose,
                                                boolean textured, UiTextureBinding binding) {
        Object[] rawCommands = batch.commandElements();
        for (int commandIndex = 0, commandCount = batch.size(); commandIndex < commandCount; commandIndex++) {
            DrawCommand command = (DrawCommand) rawCommands[commandIndex];
            if (command == null || command.mesh() == null || command.mesh().isEmpty()) continue;

            Matrix4f matrix = MinecraftTransform.commandMatrix(basePose, command);
            DrawMesh mesh = command.mesh();
            Object[] rawVertices = mesh.vertexElements();
            for (int vertexIndex = 0, vertexCount = mesh.vertexCount(); vertexIndex < vertexCount; vertexIndex++) {
                DrawVertex vertex = (DrawVertex) rawVertices[vertexIndex];
                if (textured) {
                    float v = binding.flipY() ? 1.0f - vertex.v() : vertex.v();
                    addTextureVertex(buffer, matrix, vertex.x(), vertex.y(), vertex.u(), v, vertex.color());
                } else {
                    addColorVertex(buffer, matrix, vertex.x(), vertex.y(), argb(vertex.color()));
                }
            }
        }
    }

    @Override
    public void close() {
        clearScissorStack();
        textureBindings.clear();
        fastItemRenderer.close();
        shapeBatchRenderer.close();
        shaderQuadRenderer.close();
        postEffectLayerTargets.close();
        postEffectPingTargets.close();
        sdfTextRenderer.close();
        for (Object query : gpuTimerQueries) MinecraftGpuCompat.closeTimer(query);
        gpuTimerQueries.clear();
    }

    private void renderCommand(DrawCommand command) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            MinecraftTransform.apply(command, pose);
            switch (command.type()) {
                case RECT -> renderRect(command);
                case ROUNDED_RECT -> renderRoundedRect(command);
                case LINE -> renderLine(command);
                case CIRCLE -> renderCircle(command);
                case PATH -> renderPath(command);
                case TEXTURE -> renderTexture(command);
                case TEXTURED_QUAD -> renderTexturedQuad(command);
                case SHADER -> renderShader(command);
                case TEXT -> renderText(command);
                case PUSH_CLIP -> pushClip(command);
                case POP_CLIP -> popClip();
                case DRAW_CMD -> {
                }
                case CUSTOM -> {
                    // В 26.1 глобального GL-состояния, которое пришлось бы восстанавливать, у UniGUI нет:
                    // цели и scissor хранит UiGpu, а пайплайны задают остальное.
                    if (command.customDraw() != null) {
                        command.customDraw().draw(this);
                    }
                }
                case MESH -> renderMesh(command);
            }
        } finally {
            pose.popPose();
        }
    }

    private void renderShader(DrawCommand command) {
        shaderQuadRenderer.render(
                graphics,
                minecraft,
                command,
                activeRenderTarget != null,
                shaderScreenWidth(),
                shaderScreenHeight(),
                shaderGuiScale());
    }

    private float shaderScreenWidth() {
        if (activeRenderTarget != null) {
            return Math.max(1.0f, activeRenderTarget.width() / sanitizeScale(activeRenderTargetScaleX));
        }
        return minecraft.getWindow() == null
                ? 1.0f
                : Math.max(1.0f, minecraft.getWindow().getGuiScaledWidth());
    }

    private float shaderScreenHeight() {
        if (activeRenderTarget != null) {
            return Math.max(1.0f, activeRenderTarget.height() / sanitizeScale(activeRenderTargetScaleY));
        }
        return minecraft.getWindow() == null
                ? 1.0f
                : Math.max(1.0f, minecraft.getWindow().getGuiScaledHeight());
    }

    private float shaderGuiScale() {
        if (activeRenderTarget != null) return sanitizeScale(activeRenderTargetScaleX);
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledWidth() <= 0) return 1.0f;
        return sanitizeScale(minecraft.getWindow().getWidth() / (float) minecraft.getWindow().getGuiScaledWidth());
    }

    private void applyTransform(RectView bounds, Transform transform, PoseStack pose) {
        if (transform == null) return;

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();

        pose.translate(transform.position().x(), transform.position().y(), 0.0f);
        pose.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            MinecraftTransform.rotateZ(pose, transform.rotationDegrees());
        }
        pose.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        pose.translate(-pivotX, -pivotY, 0.0f);
    }

    private void renderRect(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        int color = argb(paint.color());
        float x1 = round(bounds.x());
        float y1 = round(bounds.y());
        float x2 = round(bounds.x() + bounds.width());
        float y2 = round(bounds.y() + bounds.height());
        if (paint.isStroke()) {
            // Как GuiGraphics.renderOutline: рамка толщиной в одну единицу GUI.
            drawColoredQuad(x1, y1, x2, y1 + 1.0f, color);
            drawColoredQuad(x1, y2 - 1.0f, x2, y2, color);
            drawColoredQuad(x1, y1 + 1.0f, x1 + 1.0f, y2 - 1.0f, color);
            drawColoredQuad(x2 - 1.0f, y1 + 1.0f, x2, y2 - 1.0f, color);
        } else {
            drawColoredQuad(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
        }
    }

    private void pushClip(DrawCommand command) {
        RectView bounds = scaledClipBounds(transformedClipBounds(command));
        ScissorStack.Rect next = scissorStack.push(bounds);
        applyScissor(next);
    }

    private RectView transformedClipBounds(DrawCommand command) {
        if (command == null) return new MutableRect();
        RectView bounds = command.bounds();
        Matrix4f matrix = MinecraftTransform.commandMatrix(new Matrix4f(), command);
        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();
        float tx1 = transformX(matrix, x1, y1);
        float ty1 = transformY(matrix, x1, y1);
        float tx2 = transformX(matrix, x2, y1);
        float ty2 = transformY(matrix, x2, y1);
        float tx3 = transformX(matrix, x2, y2);
        float ty3 = transformY(matrix, x2, y2);
        float tx4 = transformX(matrix, x1, y2);
        float ty4 = transformY(matrix, x1, y2);
        float left = Math.min(Math.min(tx1, tx2), Math.min(tx3, tx4));
        float top = Math.min(Math.min(ty1, ty2), Math.min(ty3, ty4));
        float right = Math.max(Math.max(tx1, tx2), Math.max(tx3, tx4));
        float bottom = Math.max(Math.max(ty1, ty2), Math.max(ty3, ty4));
        return new MutableRect(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }

    private static float transformX(Matrix4f matrix, float x, float y) {
        return matrix.m00() * x + matrix.m10() * y + matrix.m30();
    }

    private static float transformY(Matrix4f matrix, float x, float y) {
        return matrix.m01() * x + matrix.m11() * y + matrix.m31();
    }

    private RectView scaledClipBounds(RectView bounds) {
        float scale = nestedClipCoordinateScale;
        if (scale == 1.0f) return bounds;
        return new MutableRect(
                bounds.x() * scale,
                bounds.y() * scale,
                bounds.width() * scale,
                bounds.height() * scale);
    }

    private static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    private void popClip() {
        if (scissorStack.isEmpty()) {
            clearScissorStack();
            return;
        }

        ScissorStack.Rect parent = scissorStack.pop();
        if (parent == null) {
            UiGpu.clearScissor();
        } else {
            applyScissor(parent);
        }
    }

    /**
     * Включает scissor текущей цели {@link UiGpu}. Прямоугольник задан в единицах UI текущей цели,
     * а в пиксели переводится по отношению размера цели к её координатному пространству.
     */
    private void applyScissor(ScissorStack.Rect rect) {
        if (!UiGpu.hasTarget()) return;
        UiGpu.Target target = UiGpu.target();
        int targetWidth = target.width();
        int targetHeight = target.height();
        float scaleX = targetWidth / UiGpu.coordinateWidth();
        float scaleY = targetHeight / UiGpu.coordinateHeight();
        int x1 = Math.max(0, Math.min(targetWidth, (int) Math.floor(rect.x1() * scaleX)));
        int y1 = Math.max(0, Math.min(targetHeight, (int) Math.floor(rect.y1() * scaleY)));
        int x2 = Math.max(x1, Math.min(targetWidth, (int) Math.ceil(rect.x2() * scaleX)));
        int y2 = Math.max(y1, Math.min(targetHeight, (int) Math.ceil(rect.y2() * scaleY)));
        UiGpu.setScissor(x1, targetHeight - y2, x2 - x1, y2 - y1);
    }

    /**
     * Заново применяет верх стека scissor (после восстановления родительского состояния).
     */
    private void reapplyScissor() {
        ScissorStack.Rect top = scissorStack.current();
        if (top == null) {
            UiGpu.clearScissor();
        } else {
            applyScissor(top);
        }
    }

    private void clearScissorStack() {
        scissorStack.clear();
        UiGpu.clearScissor();
    }

    private void renderRoundedRect(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        float radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f);

        if (width <= 0.0f || height <= 0.0f) return;
        if (radius <= 0.5f) {
            renderRect(command);
            return;
        }

        if (paint.isStroke()) {
            renderRoundedRectStroke(x1, y1, x2, y2, radius, paint);
        } else {
            renderRoundedRectFill(x1, y1, x2, y2, radius, paint);
        }
    }

    private void renderLine(DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();
        drawLine(x1, y1, x2, y2, command.paint());
    }

    private void renderCircle(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        float cx = bounds.x() + bounds.width() * 0.5f;
        float cy = bounds.y() + bounds.height() * 0.5f;
        float rx = Math.abs(bounds.width()) * 0.5f;
        float ry = Math.abs(bounds.height()) * 0.5f;
        if (rx <= 0.0f || ry <= 0.0f) return;

        if (paint.isStroke()) {
            int segments = command.segments() > 0
                    ? command.segments()
                    : segmentsFor(Math.max(rx, ry));
            float prevX = cx + rx;
            float prevY = cy;
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0 * i / segments;
                float x = cx + (float) Math.cos(angle) * rx;
                float y = cy + (float) Math.sin(angle) * ry;
                drawLine(prevX, prevY, x, y, paint);
                prevX = x;
                prevY = y;
            }
            return;
        }

        drawArcFan(cx, cy, rx, ry, 0.0f, (float) (Math.PI * 2.0), argb(paint.color()), segmentsFor(Math.max(rx, ry)));
    }

    private void renderPath(DrawCommand command) {
        VectorPath path = command.path();
        if (path == null || path.isEmpty()) return;

        if (!command.paint().isStroke()) {
            renderFilledPath(command);
            return;
        }

        RectView bounds = command.bounds();
        float currentX = bounds.x();
        float currentY = bounds.y();
        float startX = currentX;
        float startY = currentY;

        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
            switch (element.verb()) {
                case MOVE_TO -> {
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    startX = currentX;
                    startY = currentY;
                }
                case LINE_TO -> {
                    float nextX = bounds.x() + element.x1();
                    float nextY = bounds.y() + element.y1();
                    drawLine(currentX, currentY, nextX, nextY, command.paint());
                    currentX = nextX;
                    currentY = nextY;
                }
                case QUADRATIC_TO -> {
                    float controlX = bounds.x() + element.x1();
                    float controlY = bounds.y() + element.y1();
                    float nextX = bounds.x() + element.x2();
                    float nextY = bounds.y() + element.y2();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    float lastX = currentX;
                    float lastY = currentY;
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        float x = quadraticX(currentX, controlX, nextX, t);
                        float y = quadraticX(currentY, controlY, nextY, t);
                        drawLine(lastX, lastY, x, y, command.paint());
                        lastX = x;
                        lastY = y;
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CUBIC_TO -> {
                    float controlX1 = bounds.x() + element.x1();
                    float controlY1 = bounds.y() + element.y1();
                    float controlX2 = bounds.x() + element.x2();
                    float controlY2 = bounds.y() + element.y2();
                    float nextX = bounds.x() + element.x3();
                    float nextY = bounds.y() + element.y3();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    float lastX = currentX;
                    float lastY = currentY;
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        float x = cubicX(currentX, controlX1, controlX2, nextX, t);
                        float y = cubicX(currentY, controlY1, controlY2, nextY, t);
                        drawLine(lastX, lastY, x, y, command.paint());
                        lastX = x;
                        lastY = y;
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    drawLine(currentX, currentY, startX, startY, command.paint());
                    currentX = startX;
                    currentY = startY;
                }
            }
        }
    }

    private void renderRoundedRectFill(float x1, float y1, float x2, float y2, float radius, Paint paint) {
        int color = argb(paint.color());
        float innerLeft = x1 + radius;
        float innerRight = x2 - radius;
        float innerTop = y1 + radius;
        float innerBottom = y2 - radius;
        int cornerSegments = Math.max(4, segmentsFor(radius) / 4);

        drawColoredQuad(innerLeft, y1, innerRight, y2, color);
        drawColoredQuad(x1, innerTop, innerLeft, innerBottom, color);
        drawColoredQuad(innerRight, innerTop, x2, innerBottom, color);
        drawArcFan(innerLeft, innerTop, radius, radius, (float) Math.PI, (float) (Math.PI * 1.5), color, cornerSegments);
        drawArcFan(innerRight, innerTop, radius, radius, (float) (Math.PI * 1.5), (float) (Math.PI * 2.0), color, cornerSegments);
        drawArcFan(innerRight, innerBottom, radius, radius, 0.0f, (float) (Math.PI * 0.5), color, cornerSegments);
        drawArcFan(innerLeft, innerBottom, radius, radius, (float) (Math.PI * 0.5), (float) Math.PI, color, cornerSegments);
    }

    private void renderRoundedRectStroke(float x1, float y1, float x2, float y2, float radius, Paint paint) {
        float innerLeft = x1 + radius;
        float innerRight = x2 - radius;
        float innerTop = y1 + radius;
        float innerBottom = y2 - radius;
        int cornerSegments = Math.max(4, segmentsFor(radius) / 4);

        drawLine(innerLeft, y1, innerRight, y1, paint);
        drawLine(x2, innerTop, x2, innerBottom, paint);
        drawLine(innerRight, y2, innerLeft, y2, paint);
        drawLine(x1, innerBottom, x1, innerTop, paint);
        drawArcStroke(innerLeft, innerTop, radius, radius, (float) Math.PI, (float) (Math.PI * 1.5), paint, cornerSegments);
        drawArcStroke(innerRight, innerTop, radius, radius, (float) (Math.PI * 1.5), (float) (Math.PI * 2.0), paint, cornerSegments);
        drawArcStroke(innerRight, innerBottom, radius, radius, 0.0f, (float) (Math.PI * 0.5), paint, cornerSegments);
        drawArcStroke(innerLeft, innerBottom, radius, radius, (float) (Math.PI * 0.5), (float) Math.PI, paint, cornerSegments);
    }

    private void renderFilledPath(DrawCommand command) {
        RectView bounds = command.bounds();
        FloatPathBuilder points = new FloatPathBuilder();
        float currentX = bounds.x();
        float currentY = bounds.y();
        int color = argb(command.paint().color());

        for (VectorPath.Element element : command.path().elements()) {
            switch (element.verb()) {
                case MOVE_TO -> {
                    drawPolygonFan(points, color);
                    points.clear();
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    points.add(currentX, currentY);
                }
                case LINE_TO -> {
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    points.add(currentX, currentY);
                }
                case QUADRATIC_TO -> {
                    float controlX = bounds.x() + element.x1();
                    float controlY = bounds.y() + element.y1();
                    float nextX = bounds.x() + element.x2();
                    float nextY = bounds.y() + element.y2();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        points.add(quadraticX(currentX, controlX, nextX, t), quadraticX(currentY, controlY, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CUBIC_TO -> {
                    float controlX1 = bounds.x() + element.x1();
                    float controlY1 = bounds.y() + element.y1();
                    float controlX2 = bounds.x() + element.x2();
                    float controlY2 = bounds.y() + element.y2();
                    float nextX = bounds.x() + element.x3();
                    float nextY = bounds.y() + element.y3();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        points.add(cubicX(currentX, controlX1, controlX2, nextX, t), cubicX(currentY, controlY1, controlY2, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    drawPolygonFan(points, color);
                    points.clear();
                }
            }
        }

        drawPolygonFan(points, color);
    }

    private void renderTexture(DrawCommand command) {
        TextureHandle texture = command.texture();
        if (texture == null) return;

        UiTextureBinding binding = textureBindings.resolve(texture);
        if (binding != null) {
            renderTextureBinding(binding, command);
        }
    }

    private void renderTextureBinding(UiTextureBinding binding, DrawCommand command) {
        RectView bounds = command.bounds();
        RectView uv = command.uv();
        ColorView tint = command.paint().color();

        float minU = uv.x();
        float maxU = uv.x() + uv.width();
        float minV = binding.flipY() ? uv.y() + uv.height() : uv.y();
        float maxV = binding.flipY() ? uv.y() : uv.y() + uv.height();
        int x1 = round(bounds.x());
        int y1 = round(bounds.y());
        int x2 = round(bounds.x() + bounds.width());
        int y2 = round(bounds.y() + bounds.height());

        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addTextureVertex(buffer, matrix, x1, y1, minU, minV, tint);
        addTextureVertex(buffer, matrix, x1, y2, minU, maxV, tint);
        addTextureVertex(buffer, matrix, x2, y2, maxU, maxV, tint);
        addTextureVertex(buffer, matrix, x2, y1, maxU, minV, tint);
        MinecraftBufferCompat.drawTexture(buffer, textureBlend(binding), binding);
    }

    private MinecraftUiBlend textureBlend(UiTextureBinding binding) {
        return MinecraftUiBlend.texture(binding.premultipliedAlpha(), activeRenderTarget != null);
    }

    private void renderTexturedQuad(DrawCommand command) {
        TextureHandle texture = command.texture();
        if (texture == null) return;

        UiTextureBinding binding = textureBindings.resolve(texture);
        if (binding == null) return;

        float minV = Math.min(Math.min(command.quadV1(), command.quadV2()),
                Math.min(command.quadV3(), command.quadV4()));
        float maxV = Math.max(Math.max(command.quadV1(), command.quadV2()),
                Math.max(command.quadV3(), command.quadV4()));
        float v1 = binding.flipY() ? minV + maxV - command.quadV1() : command.quadV1();
        float v2 = binding.flipY() ? minV + maxV - command.quadV2() : command.quadV2();
        float v3 = binding.flipY() ? minV + maxV - command.quadV3() : command.quadV3();
        float v4 = binding.flipY() ? minV + maxV - command.quadV4() : command.quadV4();

        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        addTextureVertex(buffer, matrix, command.quadX1(), command.quadY1(), command.quadU1(), v1, command.paint().color());
        addTextureVertex(buffer, matrix, command.quadX2(), command.quadY2(), command.quadU2(), v2, command.paint().color());
        addTextureVertex(buffer, matrix, command.quadX3(), command.quadY3(), command.quadU3(), v3, command.paint().color());
        addTextureVertex(buffer, matrix, command.quadX1(), command.quadY1(), command.quadU1(), v1, command.paint().color());
        addTextureVertex(buffer, matrix, command.quadX3(), command.quadY3(), command.quadU3(), v3, command.paint().color());
        addTextureVertex(buffer, matrix, command.quadX4(), command.quadY4(), command.quadU4(), v4, command.paint().color());
        MinecraftBufferCompat.drawTexture(buffer, textureBlend(binding), binding);
    }

    private void renderMesh(DrawCommand command) {
        DrawMesh mesh = command.mesh();
        if (mesh == null || mesh.isEmpty()) return;
        if (command.texture() == null) {
            renderColoredMesh(mesh);
            return;
        }
        UiTextureBinding binding = textureBindings.resolve(command.texture());
        if (binding != null) {
            renderTexturedMesh(mesh, binding);
        }
    }

    private void renderColoredMesh(DrawMesh mesh) {
        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Object[] rawVertices = mesh.vertexElements();
        for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
            DrawVertex vertex = (DrawVertex) rawVertices[i];
            addColorVertex(buffer, matrix, vertex.x(), vertex.y(), argb(vertex.color()));
        }
        MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private void renderTexturedMesh(DrawMesh mesh, UiTextureBinding binding) {
        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        Object[] rawVertices = mesh.vertexElements();
        for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
            DrawVertex vertex = (DrawVertex) rawVertices[i];
            float v = binding.flipY() ? 1.0f - vertex.v() : vertex.v();
            addTextureVertex(buffer, matrix, vertex.x(), vertex.y(), vertex.u(), v, vertex.color());
        }
        MinecraftBufferCompat.drawTexture(buffer, textureBlend(binding), binding);
    }

    private void renderText(DrawCommand command) {
        String text = command.text() != null
                ? command.text()
                : command.richText() == null ? null : command.richText().plainText();
        if (text == null || text.isEmpty()) return;
        if (alphaChannel(command.paint().color()) < MIN_VANILLA_TEXT_ALPHA_CHANNEL) return;
        RectView bounds = command.bounds();
        UiVanillaText.draw(minecraft.font, Component.literal(text), round(bounds.x()), round(bounds.y()),
                argb(command.paint().color()), false, graphics.pose().last().pose(),
                MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private RichText resolveDefaultFont(RichText text) {
        RichText.Builder builder = RichText.builder();
        for (TextRun run : text.runs()) {
            builder.font(run.font() == null ? defaultFont() : run.font())
                    .size(run.pixelSize())
                    .color(run.color())
                    .brush(run.brush())
                    .tracking(run.tracking())
                    .transform(run.transform())
                    .append(run.text());
        }
        return builder.build();
    }

    private void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
        int color = argb(paint.color());
        int thickness = Math.max(1, round(paint.strokeWidth()));
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, Math.max(Math.abs(round(dx)), Math.abs(round(dy))));
        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = round(x1 + dx * t);
            int y = round(y1 + dy * t);
            addColorVertex(buffer, matrix, x, y, color);
            addColorVertex(buffer, matrix, x, y + thickness, color);
            addColorVertex(buffer, matrix, x + thickness, y + thickness, color);
            addColorVertex(buffer, matrix, x + thickness, y, color);
        }
        MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private void drawArcStroke(float cx, float cy, float rx, float ry, float startAngle, float endAngle, Paint paint, int segments) {
        float prevX = cx + (float) Math.cos(startAngle) * rx;
        float prevY = cy + (float) Math.sin(startAngle) * ry;
        for (int i = 1; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float x = cx + (float) Math.cos(angle) * rx;
            float y = cy + (float) Math.sin(angle) * ry;
            drawLine(prevX, prevY, x, y, paint);
            prevX = x;
            prevY = y;
        }
    }

    private void drawArcFan(float cx, float cy, float rx, float ry, float startAngle, float endAngle, int color, int segments) {
        if (rx <= 0.0f || ry <= 0.0f || segments <= 0) return;

        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float prevX = cx + (float) Math.cos(startAngle) * rx;
        float prevY = cy + (float) Math.sin(startAngle) * ry;
        for (int i = 1; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float x = cx + (float) Math.cos(angle) * rx;
            float y = cy + (float) Math.sin(angle) * ry;
            addColorVertex(buffer, matrix, cx, cy, color);
            addColorVertex(buffer, matrix, prevX, prevY, color);
            addColorVertex(buffer, matrix, x, y, color);
            prevX = x;
            prevY = y;
        }
        MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private void drawColoredQuad(float x1, float y1, float x2, float y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;

        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addColorVertex(buffer, matrix, x1, y1, color);
        addColorVertex(buffer, matrix, x1, y2, color);
        addColorVertex(buffer, matrix, x2, y2, color);
        addColorVertex(buffer, matrix, x2, y1, color);
        MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private void drawPolygonFan(FloatPathBuilder points, int color) {
        if (points.size() < 3) return;

        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float originX = points.x(0);
        float originY = points.y(0);
        for (int i = 1; i < points.size() - 1; i++) {
            addColorVertex(buffer, matrix, originX, originY, color);
            addColorVertex(buffer, matrix, points.x(i), points.y(i), color);
            addColorVertex(buffer, matrix, points.x(i + 1), points.y(i + 1), color);
        }
        MinecraftBufferCompat.drawColor(buffer, MinecraftUiBlend.straight(activeRenderTarget != null));
    }

    private static void addTextureVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v, ColorView tint) {
        MinecraftBufferCompat.textureColorVertex(buffer, matrix, x, y, u, v, argb(tint));
    }

    private static void addColorVertex(Object buffer, Matrix4f matrix, float x, float y, int color) {
        MinecraftBufferCompat.colorVertex(buffer, matrix, x, y, color);
    }

    private static int argb(ColorView color) {
        int a = channel(color.a());
        int r = channel(color.r());
        int g = channel(color.g());
        int b = channel(color.b());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int alphaChannel(ColorView color) {
        return color == null ? 255 : channel(color.a());
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private static int round(float value) {
        return Math.round(value);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int segmentsFor(float radius) {
        return Math.max(16, Math.min(96, Math.round(radius * 0.5f)));
    }

    private static int curveSegments(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return Math.max(8, Math.min(48, Math.round(distance * 0.25f)));
    }

    private static float quadraticX(float x0, float x1, float x2, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * x0 + 2.0f * inverse * t * x1 + t * t * x2;
    }

    private static float cubicX(float x0, float x1, float x2, float x3, float t) {
        float inverse = 1.0f - t;
        float inverse2 = inverse * inverse;
        float t2 = t * t;
        return inverse2 * inverse * x0 + 3.0f * inverse2 * t * x1 + 3.0f * inverse * t2 * x2 + t2 * t * x3;
    }

    private static final class FloatPathBuilder {
        private float[] values = new float[16];
        private int size;

        void add(float x, float y) {
            ensureCapacity(size + 2);
            values[size++] = x;
            values[size++] = y;
        }

        void clear() {
            size = 0;
        }

        int size() {
            return size / 2;
        }

        float x(int index) {
            return values[index * 2];
        }

        float y(int index) {
            return values[index * 2 + 1];
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) return;
            values = Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }
}
