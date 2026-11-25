package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Слои экрана UniGUI в Minecraft 26.1.
 *
 * <p>В 26.1 GUI отложенный: во время {@code extractRenderState} экраны только описывают элементы
 * ({@code GuiRenderState}), а рисует их позже {@code GuiRenderer}. UniGUI же рисует draw-list сразу, как и в
 * других версиях. Чтобы совместить одно с другим, UniGUI рисует в собственную текстуру размером с окно
 * («слой»), а перед каждым ванильным элементом (предмет, тултип, сущность) и в конце отрисовки переносит
 * изменённую часть слоя в очередь GUI обычным {@code blit} с premultiplied alpha. Порядок элементов
 * {@code GuiRenderState} сохраняет порядок наложения: следующий перенос слоя ляжет поверх предмета, который
 * был отправлен до него.</p>
 *
 * <p>Перенос не копирует весь слой: учитывается только «грязная» область ({@link UiGpu.Target#dirty()}).
 * Небольшие области копируются в общие страницы-атласы, большой слой отдаётся в очередь целиком, а для
 * продолжения рисования берётся новая страница. Страницы живут до следующего кадра: {@code GuiRenderer}
 * читает их уже после окончания извлечения GUI.</p>
 */
final class UiLayerCompositor {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(UiLayerCompositor.class);
    /** Доля площади слоя, начиная с которой слой отдаётся целиком, без копирования. */
    private static final float WHOLE_PAGE_AREA_FRACTION = 0.35f;
    /** Предел страниц на кадр; дальше слои склеиваются (возможен неверный порядок с ванильными элементами). */
    private static final int MAX_PAGES_PER_FRAME = 48;
    /** Отступ между прямоугольниками в атласе, в пикселях. */
    private static final int ATLAS_PADDING = 1;

    private static final List<LayerTexture> PAGES = new ArrayList<>();
    private static int pagesUsed;
    private static GuiGraphicsExtractor frameExtractor;
    private static GuiGraphicsExtractor currentExtractor;
    private static LayerTexture scratch;
    private static UiGpu.Target scratchTarget;
    private static int depth;
    private static boolean pageLimitWarned;

    private static LayerTexture atlas;
    private static int atlasCursorX;
    private static int atlasCursorY;
    private static int atlasRowHeight;

    private UiLayerCompositor() {
    }

    /**
     * Начинает рисование UniGUI на экран. Вложенные вызовы (вложенные draw-list'ы) используют тот же слой.
     *
     * @param graphics extractor текущего кадра
     */
    static void begin(GuiGraphicsExtractor graphics) {
        if (graphics != frameExtractor) {
            newFrame(graphics);
        }
        if (depth++ > 0) return;
        currentExtractor = graphics;
        ensureScratch();
        Window window = Minecraft.getInstance().getWindow();
        UiGpu.pushTarget(scratchTarget, Math.max(1, window.getGuiScaledWidth()), Math.max(1, window.getGuiScaledHeight()));
    }

    /**
     * Завершает рисование на экран: переносит остаток слоя в очередь GUI.
     */
    static void end() {
        if (depth <= 0) return;
        if (depth > 1) {
            depth--;
            return;
        }
        try {
            // flush() требует depth > 0 (drawingToScreen), поэтому глубина уменьшается только после него.
            flush();
        } finally {
            depth = 0;
            UiGpu.popTarget();
            currentExtractor = null;
        }
    }

    /**
     * @return {@code true}, если сейчас UniGUI рисует в слой экрана (а не в offscreen-цель)
     */
    static boolean drawingToScreen() {
        return depth > 0 && UiGpu.hasTarget() && UiGpu.target() == scratchTarget;
    }

    /**
     * Переносит нарисованное в очередь GUI. Вызывается перед отправкой ванильного элемента, чтобы он лёг поверх
     * уже нарисованного UniGUI, а следующее рисование UniGUI — поверх него.
     */
    static void flush() {
        if (!drawingToScreen() || currentExtractor == null || !scratchTarget.dirty()) return;
        Window window = Minecraft.getInstance().getWindow();
        int scale = Math.max(1, window.getGuiScale());
        int pageWidth = scratchTarget.width();
        int pageHeight = scratchTarget.height();

        int guiX0 = Math.floorDiv(scratchTarget.dirtyMinX(), scale);
        int guiY0 = Math.floorDiv(scratchTarget.dirtyMinY(), scale);
        int guiX1 = Math.min(window.getGuiScaledWidth(), (scratchTarget.dirtyMaxX() + scale - 1) / scale);
        int guiY1 = Math.min(window.getGuiScaledHeight(), (scratchTarget.dirtyMaxY() + scale - 1) / scale);
        if (guiX1 <= guiX0 || guiY1 <= guiY0) {
            scratchTarget.resetDirty();
            return;
        }
        int pixelX0 = guiX0 * scale;
        int pixelY0 = guiY0 * scale;
        int pixelX1 = Math.min(pageWidth, guiX1 * scale);
        int pixelY1 = Math.min(pageHeight, guiY1 * scale);
        int pixelWidth = pixelX1 - pixelX0;
        int pixelHeight = pixelY1 - pixelY0;
        if (pixelWidth <= 0 || pixelHeight <= 0) {
            scratchTarget.resetDirty();
            return;
        }
        // Строки GL-текстуры идут снизу вверх: верх прямоугольника слоя — строка (height - pixelY0).
        int glBottom = pageHeight - pixelY1;

        float area = (float) pixelWidth * pixelHeight / ((float) pageWidth * pageHeight);
        if (area >= WHOLE_PAGE_AREA_FRACTION || pagesUsed >= MAX_PAGES_PER_FRAME - 1) {
            if (pagesUsed >= MAX_PAGES_PER_FRAME) {
                warnPageLimit();
                return;
            }
            submitBlit(scratch, guiX0, guiY0, guiX1, guiY1, pixelX0, glBottom, pixelWidth, pixelHeight);
            scratch = null;
            ensureScratch();
            boolean scissor = UiGpu.scissorEnabled();
            int scissorX = scissor ? UiGpu.scissorX() : 0;
            int scissorY = scissor ? UiGpu.scissorY() : 0;
            int scissorWidth = scissor ? UiGpu.scissorWidth() : 0;
            int scissorHeight = scissor ? UiGpu.scissorHeight() : 0;
            UiGpu.popTarget();
            UiGpu.pushTarget(scratchTarget, Math.max(1, window.getGuiScaledWidth()), Math.max(1, window.getGuiScaledHeight()));
            if (scissor) UiGpu.setScissor(scissorX, scissorY, scissorWidth, scissorHeight);
            return;
        }

        int[] slot = allocateAtlas(pixelWidth, pixelHeight, pageWidth, pageHeight);
        if (slot == null) {
            warnPageLimit();
            return;
        }
        if (UiDebugCapture.capturing()) UiDebugCapture.onLayerBlit(scratch.gpuTexture(), "scratch before copy");
        MinecraftGpuCompat.copyColor(scratch.gpuTexture(), atlas.gpuTexture(),
                slot[0], slot[1], pixelX0, glBottom, pixelWidth, pixelHeight);
        submitBlit(atlas, guiX0, guiY0, guiX1, guiY1, slot[0], slot[1], pixelWidth, pixelHeight);
        UiGpu.clear(scratchTarget);
    }

    private static void submitBlit(LayerTexture page, int guiX0, int guiY0, int guiX1, int guiY1,
                                   int textureX, int textureBottom, int textureWidth, int textureHeight) {
        GuiGraphicsExtractor graphics = currentExtractor;
        if (UiDebugCapture.capturing()) {
            UiDebugCapture.onLayerBlit(page.gpuTexture(), "gui " + guiX0 + "," + guiY0 + ".." + guiX1 + "," + guiY1
                    + " tex " + textureX + "," + textureBottom + " " + textureWidth + "x" + textureHeight);
        }
        graphics.pose().pushMatrix();
        try {
            graphics.pose().identity();
            graphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, page.id(),
                    guiX0, guiY0,
                    textureX, textureBottom + textureHeight,
                    guiX1 - guiX0, guiY1 - guiY0,
                    textureWidth, -textureHeight,
                    page.width(), page.height());
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static int[] allocateAtlas(int width, int height, int pageWidth, int pageHeight) {
        int paddedWidth = width + ATLAS_PADDING;
        int paddedHeight = height + ATLAS_PADDING;
        if (atlas != null) {
            if (atlasCursorX + paddedWidth > pageWidth) {
                atlasCursorX = 0;
                atlasCursorY += atlasRowHeight;
                atlasRowHeight = 0;
            }
            if (atlasCursorY + paddedHeight > pageHeight) {
                atlas = null;
            }
        }
        if (atlas == null) {
            atlas = acquirePage();
            if (atlas == null) return null;
            atlasCursorX = 0;
            atlasCursorY = 0;
            atlasRowHeight = 0;
        }
        int[] slot = {atlasCursorX, atlasCursorY};
        atlasCursorX += paddedWidth;
        atlasRowHeight = Math.max(atlasRowHeight, paddedHeight);
        return slot;
    }

    private static void ensureScratch() {
        if (scratch == null) {
            scratch = acquirePage();
            if (scratch == null) {
                // Предел страниц: продолжаем рисовать в последнюю страницу.
                scratch = PAGES.get(PAGES.size() - 1);
            }
            scratchTarget = new UiGpu.Target(scratch.getTextureView(), null, scratch.width(), scratch.height(), true);
            UiGpu.clear(scratchTarget);
        }
    }

    private static LayerTexture acquirePage() {
        if (pagesUsed >= MAX_PAGES_PER_FRAME) return null;
        Window window = Minecraft.getInstance().getWindow();
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());
        LayerTexture page;
        if (pagesUsed < PAGES.size()) {
            page = PAGES.get(pagesUsed);
        } else {
            page = new LayerTexture(Identifier.fromNamespaceAndPath("unigui", "dynamic/layer_" + PAGES.size()));
            Minecraft.getInstance().getTextureManager().register(page.id(), page);
            PAGES.add(page);
        }
        pagesUsed++;
        page.ensureSize(width, height);
        return page;
    }

    private static void newFrame(GuiGraphicsExtractor graphics) {
        frameExtractor = graphics;
        if (depth > 0) {
            // Незавершённый прошлый кадр (исключение во время рендера): сбрасываем стек.
            while (depth > 0) {
                depth--;
                if (UiGpu.hasTarget()) UiGpu.popTarget();
            }
        }
        pagesUsed = 0;
        scratch = null;
        scratchTarget = null;
        atlas = null;
        pageLimitWarned = false;
    }

    private static void warnPageLimit() {
        if (pageLimitWarned) return;
        pageLimitWarned = true;
        LOGGER.warn("UniGUI: too many screen layers in one frame ({}); layers are merged", MAX_PAGES_PER_FRAME);
    }

    /**
     * Страница слоя: текстура размером с окно, зарегистрированная в {@code TextureManager}, чтобы её можно было
     * отдать в {@code blit}.
     */
    private static final class LayerTexture extends AbstractTexture {
        private final Identifier id;
        private int width;
        private int height;

        private LayerTexture(Identifier id) {
            this.id = id;
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }

        Identifier id() {
            return id;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        GpuTexture gpuTexture() {
            return getTexture();
        }

        void ensureSize(int width, int height) {
            if (texture != null && !texture.isClosed() && this.width == width && this.height == height) return;
            close();
            texture = MinecraftGpuCompat.createRgbaTexture(() -> "UniGUI layer " + id,
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                            | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                    width, height);
            textureView = RenderSystem.getDevice().createTextureView(texture);
            this.width = width;
            this.height = height;
        }
    }
}
