package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectRenderer;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Offscreen surface для рендера UniGUI-дерева в Minecraft texture.
 *
 * <p>Класс предназначен для world-сценариев: экран телевизора на блоке, монитор в кабине,
 * голографическая панель и другие места, где UI должен быть заранее отрисован в texture, а
 * затем уже использован обычным world renderer'ом. Surface не открывает {@link net.minecraft.client.gui.screens.Screen}
 * и не занимается input: он только обновляет дерево, собирает {@link DrawList}, прогоняет
 * optional {@link UiPostEffectChain} и возвращает готовую {@link TextureHandle}.</p>
 *
 * <p>Для совместимости с Iris/Sodium важно, что PostEffect выполняется до world draw. В мире
 * должен рисоваться уже готовый texture id на quad-е блока, без fullscreen pass'ов внутри
 * world shader pipeline.</p>
 */
public final class MinecraftWorldUiSurface implements AutoCloseable {
    private static final AtomicInteger SURFACE_IDS = new AtomicInteger();
    private static final RenderTargetOptions DEFAULT_TARGET_OPTIONS =
            new RenderTargetOptions(true, true, "world_ui_surface");

    private final Minecraft minecraft;
    private final Widget root;
    private final UIContext uiContext;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final MutableRect layoutBounds = new MutableRect();
    private final ResourceLocation textureLocation;
    private final SurfaceTexture textureWrapper;

    private MinecraftGuiRenderBackend backend;
    private MinecraftRenderTarget target;
    private UiPostEffectChain postEffect = UiPostEffectChain.none();
    private RenderTargetOptions targetOptions = DEFAULT_TARGET_OPTIONS;
    private float logicalWidth;
    private float logicalHeight;
    private float textureScale;
    private long frameIndex;
    private long lastFrameNanos;
    private boolean textureLocationRegistered;

    /**
     * Создаёт surface, где размер layout совпадает с размером texture.
     *
     * @param root корневой виджет UI-дерева
     * @param uiContext runtime-контекст; если {@code null}, будет создан {@link DefaultUIContext}
     * @param textureWidth ширина texture в texel'ах
     * @param textureHeight высота texture в texel'ах
     */
    public MinecraftWorldUiSurface(Widget root, UIContext uiContext, int textureWidth, int textureHeight) {
        this(root, uiContext, textureWidth, textureHeight, 1.0f);
    }

    /**
     * Создаёт surface с отдельным logical-размером и texture scale.
     *
     * @param root корневой виджет UI-дерева
     * @param uiContext runtime-контекст; если {@code null}, будет создан {@link DefaultUIContext}
     * @param logicalWidth ширина UI layout в логических единицах
     * @param logicalHeight высота UI layout в логических единицах
     * @param textureScale сколько texel'ов приходится на одну логическую UI-единицу
     */
    public MinecraftWorldUiSurface(Widget root, UIContext uiContext,
                                   float logicalWidth, float logicalHeight, float textureScale) {
        this.minecraft = Minecraft.getInstance();
        this.root = Objects.requireNonNull(root, "root");
        this.uiContext = uiContext == null ? new DefaultUIContext(new MinecraftClipboardService()) : uiContext;
        this.logicalWidth = sanitizeSize(logicalWidth, 256.0f);
        this.logicalHeight = sanitizeSize(logicalHeight, 144.0f);
        this.textureScale = sanitizeScale(textureScale);
        this.textureLocation = createTextureLocation();
        this.textureWrapper = new SurfaceTexture(this);
        attachContext(this.root, this.uiContext);
    }

    /** Удобная фабрика для texture-size surface. */
    public static MinecraftWorldUiSurface create(Widget root, UIContext uiContext, int textureWidth, int textureHeight) {
        return new MinecraftWorldUiSurface(root, uiContext, textureWidth, textureHeight);
    }

    /** Удобная фабрика для logical-size surface. */
    public static MinecraftWorldUiSurface create(Widget root, UIContext uiContext,
                                                 float logicalWidth, float logicalHeight, float textureScale) {
        return new MinecraftWorldUiSurface(root, uiContext, logicalWidth, logicalHeight, textureScale);
    }

    /** @return корневой виджет, который рендерится в texture */
    public Widget root() {
        return root;
    }

    /** @return UIContext surface'а */
    public UIContext uiContext() {
        return uiContext;
    }

    /** @return logical width UI layout */
    public float logicalWidth() {
        return logicalWidth;
    }

    /** @return logical height UI layout */
    public float logicalHeight() {
        return logicalHeight;
    }

    /** @return texel scale offscreen target'а */
    public float textureScale() {
        return textureScale;
    }

    /**
     * Меняет logical-размер UI. Texture будет пересоздана/resize'нута на следующем render.
     */
    public MinecraftWorldUiSurface logicalSize(float width, float height) {
        this.logicalWidth = sanitizeSize(width, logicalWidth);
        this.logicalHeight = sanitizeSize(height, logicalHeight);
        return this;
    }

    /**
     * Меняет разрешение texture относительно logical-размера.
     */
    public MinecraftWorldUiSurface textureScale(float textureScale) {
        this.textureScale = sanitizeScale(textureScale);
        return this;
    }

    /** @return текущая цепочка PostEffect pass'ов */
    public UiPostEffectChain postEffect() {
        return postEffect.copy();
    }

    /**
     * Задаёт PostEffect, который будет применяться к UI перед выдачей texture.
     */
    public MinecraftWorldUiSurface postEffect(UiPostEffectChain postEffect) {
        this.postEffect = postEffect == null ? UiPostEffectChain.none() : postEffect.copy();
        return this;
    }

    /** @return options финального render target'а */
    public RenderTargetOptions targetOptions() {
        return targetOptions;
    }

    /**
     * Меняет options финального render target'а. Target будет пересоздан на следующем render.
     */
    public MinecraftWorldUiSurface targetOptions(RenderTargetOptions targetOptions) {
        RenderTargetOptions normalized = targetOptions == null ? DEFAULT_TARGET_OPTIONS : targetOptions;
        if (!normalized.equals(this.targetOptions)) {
            this.targetOptions = normalized;
            releaseTarget();
        }
        return this;
    }

    /**
     * Обновляет surface и возвращает texture текущего кадра.
     *
     * @param partialTick Minecraft partial tick для анимаций, которым он нужен
     * @return texture с уже применённым PostEffect
     */
    public TextureHandle render(float partialTick) {
        long now = System.nanoTime();
        float deltaSeconds = lastFrameNanos == 0L
                ? 1.0f / 60.0f
                : Math.max(0.0f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        return render(deltaSeconds, partialTick);
    }

    /**
     * Обновляет surface с явно заданным delta time.
     */
    public TextureHandle render(float deltaSeconds, float partialTick) {
        ensureBackend();
        ensureTarget();
        updateScaleProviderViewport();

        if (uiContext.dispatcher() != null) {
            uiContext.dispatcher().drain();
        }

        FrameContext animationFrame = new FrameContext(frameIndex, Math.max(0.0f, deltaSeconds), partialTick, FramePhase.ANIMATION);
        FrameContext renderFrame = new FrameContext(frameIndex, 0.0f, partialTick, FramePhase.RENDER);

        root.tick(animationFrame);
        root.measure(new LayoutContext(logicalWidth, logicalHeight));
        layoutBounds.set(0.0f, 0.0f, logicalWidth, logicalHeight);
        root.arrange(layoutBounds);

        backend.beginFrame(renderFrame);
        renderContext.backend(backend);
        drawList.clear();
        root.renderCached(renderContext);

        if (postEffect == null || postEffect.isNone()) {
            backend.render(drawList, target, textureScale);
        } else {
            UiLayerBounds bounds = new UiLayerBounds(0.0f, 0.0f, logicalWidth, logicalHeight, textureScale);
            UiPostEffectRenderer.render(backend, drawList, bounds, postEffect, target);
        }
        backend.endFrame();
        frameIndex++;
        return target.colorTexture();
    }

    /**
     * Возвращает texture последнего render'а или {@code null}, если surface ещё не рендерился.
     */
    public TextureHandle texture() {
        return target == null ? null : target.colorTexture();
    }

    /**
     * Возвращает raw OpenGL texture id последнего render'а.
     *
     * <p>Это удобно для world renderer'а, который сам рисует quad. Если texture ещё не создана,
     * метод вернёт {@code 0}.</p>
     */
    public int textureId() {
        TextureHandle texture = texture();
        Object nativeHandle = texture == null ? null : texture.nativeHandle();
        return nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture
                ? colorTexture.textureId()
                : 0;
    }

    /**
     * Возвращает стабильный {@link ResourceLocation}, привязанный к texture этого surface.
     *
     * <p>Метод нужен, когда world renderer ожидает обычную Minecraft-текстуру, например для
     * {@code RenderType.entityTranslucent(surface.textureLocation())}. Сам {@link ResourceLocation}
     * остаётся стабильным на всё время жизни surface, а texture id внутри него берётся из текущего
     * {@link MinecraftRenderTarget}. Если target был resize'нут, биндинг автоматически начнёт
     * отдавать новый color texture id.</p>
     *
     * <p>Обычно перед world draw нужно вызвать {@link #render(float)} или
     * {@link #render(float, float)}, чтобы FBO уже содержал свежий кадр.</p>
     */
    public ResourceLocation textureLocation() {
        ensureTextureLocationRegistered();
        return textureLocation;
    }

    /** @return текущий final render target или {@code null}, если render ещё не вызывался */
    public MinecraftRenderTarget target() {
        return target;
    }

    /** @return backend surface'а или {@code null}, если render ещё не вызывался */
    public MinecraftGuiRenderBackend backend() {
        return backend;
    }

    private void ensureBackend() {
        GuiGraphics graphics = new GuiGraphics(minecraft, MinecraftBufferCompat.immediate(4096));
        if (backend == null) {
            backend = new MinecraftGuiRenderBackend(graphics, minecraft, null);
        } else {
            backend.graphics(graphics);
        }
    }

    private void ensureTarget() {
        int width = targetWidth();
        int height = targetHeight();
        if (target == null) {
            target = new MinecraftRenderTarget(width, height, targetOptions);
            return;
        }
        if (target.width() != width || target.height() != height) {
            target.resize(width, height);
        }
    }

    private int targetWidth() {
        return Math.max(1, Math.round(logicalWidth * textureScale));
    }

    private int targetHeight() {
        return Math.max(1, Math.round(logicalHeight * textureScale));
    }

    private void updateScaleProviderViewport() {
        UIScaleProvider provider = uiContext.scaleProvider();
        if (provider != null) {
            provider.viewportSize(targetWidth(), targetHeight());
        }
    }

    private void ensureTextureLocationRegistered() {
        if (!textureLocationRegistered) {
            minecraft.getTextureManager().register(textureLocation, textureWrapper);
            textureLocationRegistered = true;
        }
    }

    private void unregisterTextureLocation() {
        if (textureLocationRegistered) {
            minecraft.getTextureManager().release(textureLocation);
            textureLocationRegistered = false;
        }
    }

    private void releaseTarget() {
        if (target != null) {
            target.close();
            target = null;
        }
    }

    private static ResourceLocation createTextureLocation() {
        int id = SURFACE_IDS.incrementAndGet();
        ResourceLocation location = ResourceLocation.tryBuild("unigui", "dynamic/world_ui_surface_" + id);
        if (location == null) {
            throw new IllegalStateException("Failed to create UniGUI world UI texture location");
        }
        return location;
    }

    private static void attachContext(Widget widget, UIContext uiContext) {
        if (widget == null) return;
        if (widget instanceof WidgetBase base) {
            base.setUiContextInternal(uiContext);
        }
        List<Widget> children = widget.children();
        if (children == null || children.isEmpty()) return;
        for (Widget child : children) {
            attachContext(child, uiContext);
        }
    }

    private static float sanitizeSize(float value, float fallback) {
        if (!Float.isFinite(value) || value <= 0.0f) return Math.max(1.0f, fallback);
        return value;
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) && value > 0.0f ? value : 1.0f;
    }

    @Override
    public void close() {
        unregisterTextureLocation();
        releaseTarget();
        if (backend != null) {
            backend.close();
            backend = null;
        }
        attachContext(root, null);
    }

    /**
     * TextureManager-обёртка над color texture текущего FBO.
     *
     * <p>Владельцем OpenGL texture остаётся {@link MinecraftRenderTarget}. Поэтому {@link #close()}
     * и {@link #releaseId()} намеренно ничего не удаляют: TextureManager только получает способ
     * забиндить FBO как обычную Minecraft-текстуру.</p>
     */
    private static final class SurfaceTexture extends AbstractTexture {
        private final MinecraftWorldUiSurface surface;

        private SurfaceTexture(MinecraftWorldUiSurface surface) {
            this.surface = Objects.requireNonNull(surface, "surface");
        }

        @Override
        public int getId() {
            return surface.textureId();
        }

        @Override
        public void load(ResourceManager resourceManager) throws IOException {
            // FBO texture создаётся MinecraftRenderTarget, resource manager здесь не участвует.
        }

        @Override
        public void releaseId() {
            // Нельзя удалять texture id: им владеет MinecraftRenderTarget.
        }

        @Override
        public void close() {
            // Освобождение делает MinecraftWorldUiSurface.releaseTarget().
        }
    }
}
